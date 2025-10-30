package service;

import interfaces.MessageHandler;
import interfaces.NetworkService;
import model.VoicePlayer;

import java.io.*;
import java.net.*;

/**
 * Implementación del servicio de comunicación de red del cliente.
 * Soporta:
 * - Mensajes normales
 * - Notas de voz individuales (/voice <usuario>)
 * - Notas de voz grupales (/voicegroup <grupo>)
 */
public class NetworkServiceImpl implements NetworkService {
    private final String serverHost;
    private final int serverPort;

    private Socket tcpSocket;
    // Eliminamos el uso de BufferedReader para evitar mezclar buffers con binarios
    private PrintWriter tcpOut;
    private InputStream rawIn;
    private boolean connected = false;

    private MessageHandler messageHandler;
    private Thread listenerThread;
    private final VoicePlayer voicePlayer = new VoicePlayer();

    // Estado para grabación de notas de voz
    private volatile boolean recordingVoice = false;
    private ByteArrayOutputStream voiceBuffer;
    private javax.sound.sampled.TargetDataLine micLine;
    private String pendingVoiceTargetUser;
    private String pendingVoiceTargetGroup;
    private Thread recordingThread;

    public NetworkServiceImpl(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    @Override
    public String connect(String username) {
        try {
            tcpSocket = new Socket(serverHost, serverPort);
            tcpOut = new PrintWriter(tcpSocket.getOutputStream(), true);
            rawIn = tcpSocket.getInputStream();
            // Registrar usuario leyendo/escribiendo líneas manualmente
            tcpOut.println(username);
            String welcomeMessage = readLine(rawIn);

            connected = true;
            startMessageListener();
            return welcomeMessage;

        } catch (IOException e) {
            return "Error connecting to server: " + e.getMessage();
        }
    }

    @Override
    public void sendCommand(String command) {
        if (!connected || tcpOut == null) {
            System.out.println("No conectado al servidor");
            return;
        }

        tcpOut.println(command);
        if (command.equals("/quit")) disconnect();
    }

    @Override
    public void disconnect() {
        connected = false;
        if (listenerThread != null) listenerThread.interrupt();
        cleanup();
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    private void startMessageListener() {
        listenerThread = new Thread(this::listenServer);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Escucha mensajes del servidor, incluyendo encabezados de notas de voz.
     */
    private void listenServer() {
        try {
            while (connected) {
                String line = readLine(rawIn);
                if (line == null) break;

                if (line.startsWith("VOICE_NOTE_START") || line.startsWith("VOICE_NOTE_GROUP_START")) {
                    processIncomingVoice(line);
                    continue;
                }

                if (messageHandler != null) {
                    messageHandler.handleMessage(line);
                } else {
                    System.out.println("[SERVER] " + line);
                }
            }

        } catch (IOException e) {
            if (connected) System.out.println("Conexión con servidor cerrada: " + e.getMessage());
        }
    }

    /**
     * Procesa la recepción de una nota de voz según encabezado del servidor.
     * Ejemplos:
     *  - "VOICE_NOTE_START <sender> <fileSize>"
     *  - "VOICE_NOTE_GROUP_START <sender> <group> <fileSize>"
     */
    private void processIncomingVoice(String header) {
        try {
            String[] parts = header.split(" ");
            if (parts.length < 3) {
                System.err.println("Encabezado inválido de nota de voz: " + header);
                return;
            }

            String sender = parts[1];
            long fileSize;
            if (header.startsWith("VOICE_NOTE_GROUP_START")) {
                if (parts.length < 4) {
                    System.err.println("Encabezado grupal inválido: " + header);
                    return;
                }
                fileSize = Long.parseLong(parts[3]);
            } else {
                fileSize = Long.parseLong(parts[2]);
            }

            System.out.println("Recibiendo nota de voz de " + sender + " (" + fileSize + " bytes)");

            byte[] audioData;
            try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                long remaining = fileSize;
                while (remaining > 0) {
                    int bytesRead = rawIn.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (bytesRead == -1) break;
                    baos.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
                audioData = baos.toByteArray();
            }

            // Leer la línea de cierre
            String endLine = readLine(rawIn);
            if (!("VOICE_NOTE_END".equals(endLine) || "VOICE_NOTE_GROUP_END".equals(endLine))) {
                System.err.println("Fin de nota de voz no detectado correctamente (recibido: " + endLine + ")");
            }

            System.out.println("Nota de voz recibida de " + sender + ": " + audioData.length + " bytes");
            if (messageHandler != null) {
                messageHandler.handleMessage("[Nota de voz recibida de " + sender + "]");
            }

            // Reproducción automática
            if (audioData.length > 0) {
                voicePlayer.playVoiceNote(audioData);
            }

        } catch (Exception e) {
            System.err.println("Error procesando nota de voz: " + e.getMessage());
        }
    }

    private String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') break;
            if (b != '\r') sb.append((char) b);
        }
        if (sb.isEmpty() && b == -1) return null;
        return sb.toString();
    }

    @Override
    public void startVoiceNoteToUser(String username) {
        if (!connected || tcpSocket == null) {
            System.out.println("No conectado al servidor.");
            return;
        }
        if (recordingVoice) {
            System.out.println("Ya hay una grabación en curso. Detenla antes de iniciar otra.");
            return;
        }
        pendingVoiceTargetUser = username;
        pendingVoiceTargetGroup = null;
        beginRecording();
    }

    @Override
    public void startVoiceNoteToGroup(String groupName) {
        if (!connected || tcpSocket == null) {
            System.out.println("No conectado al servidor.");
            return;
        }
        if (recordingVoice) {
            System.out.println("Ya hay una grabación en curso. Detenla antes de iniciar otra.");
            return;
        }
        pendingVoiceTargetUser = null;
        pendingVoiceTargetGroup = groupName;
        beginRecording();
    }

    @Override
    public void stopAndSendVoiceNote() {
        if (!recordingVoice) {
            System.out.println("No hay grabación activa.");
            return;
        }
        // Detener captura
        recordingVoice = false;
        if (micLine != null) {
            try { micLine.stop(); micLine.close(); } catch (Exception ignored) {}
        }
        if (recordingThread != null) {
            try { recordingThread.join(500); } catch (InterruptedException ignored) {}
        }

        byte[] audioData = voiceBuffer != null ? voiceBuffer.toByteArray() : new byte[0];
        voiceBuffer = null;

        if (audioData.length == 0) {
            System.out.println("No se capturó audio.");
            return;
        }

        try {
            DataOutputStream dos = new DataOutputStream(tcpSocket.getOutputStream());
            if (pendingVoiceTargetUser != null) {
                // Protocolo: VOICE_NOTE_START <destinatario> <tamaño> (usuario)
                String header = "VOICE_NOTE_START " + pendingVoiceTargetUser + " " + audioData.length + "\n";
                dos.write(header.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                dos.write(audioData);
                dos.write("VOICE_NOTE_END\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                dos.flush();
                System.out.println("Nota de voz enviada a " + pendingVoiceTargetUser);
            } else if (pendingVoiceTargetGroup != null) {
                // Protocolo: VOICE_NOTE_GROUP_START <grupo> <tamaño>
                String header = "VOICE_NOTE_GROUP_START " + pendingVoiceTargetGroup + " " + audioData.length + "\n";
                dos.write(header.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                dos.write(audioData);
                dos.write("VOICE_NOTE_GROUP_END\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                dos.flush();
                System.out.println("Nota de voz grupal enviada a '" + pendingVoiceTargetGroup + "'");
            }
        } catch (IOException e) {
            System.err.println("Error enviando nota de voz: " + e.getMessage());
        } finally {
            pendingVoiceTargetUser = null;
            pendingVoiceTargetGroup = null;
        }
    }

    private void beginRecording() {
        try {
            javax.sound.sampled.AudioFormat fmt = new javax.sound.sampled.AudioFormat(44100, 16, 1, true, false);
            javax.sound.sampled.DataLine.Info info = new javax.sound.sampled.DataLine.Info(javax.sound.sampled.TargetDataLine.class, fmt);
            micLine = (javax.sound.sampled.TargetDataLine) javax.sound.sampled.AudioSystem.getLine(info);
            micLine.open(fmt);
            micLine.start();

            voiceBuffer = new ByteArrayOutputStream();
            recordingVoice = true;
            recordingThread = new Thread(() -> {
                byte[] buf = new byte[1024];
                try {
                    while (recordingVoice) {
                        int n = micLine.read(buf, 0, buf.length);
                        if (n > 0) voiceBuffer.write(buf, 0, n);
                    }
                } catch (Exception ignored) {
                }
            }, "voice-recorder");
            recordingThread.setDaemon(true);
            recordingThread.start();
            System.out.println("🎙️ Grabación iniciada. Usa 'detener' para finalizar y enviar.");
        } catch (Exception e) {
            System.err.println("No se pudo iniciar la grabación: " + e.getMessage());
        }
    }

    private void cleanup() {
        try {
            if (tcpSocket != null && !tcpSocket.isClosed()) tcpSocket.close();
        } catch (IOException e) {
            System.err.println("Error cerrando socket TCP: " + e.getMessage());
        }
    }
}
