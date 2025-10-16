package service;

import interfaces.AudioService;
import interfaces.MessageHandler;
import interfaces.NetworkService;
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
    private BufferedReader tcpIn;
    private PrintWriter tcpOut;
    private InputStream rawIn;
    private boolean connected = false;
    private AudioService audioService;

    private MessageHandler messageHandler;
    private Thread listenerThread;

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
            tcpIn = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            tcpOut = new PrintWriter(tcpSocket.getOutputStream(), true);
            rawIn = tcpSocket.getInputStream();

            tcpOut.println(username);
            String welcomeMessage = tcpIn.readLine();

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
            String line;
            while (connected && (line = tcpIn.readLine()) != null) {

                // ======= 📢 Nota de voz entrante =======
                if (line.startsWith("VOICE_NOTE_START") || line.startsWith("VOICE_NOTE_GROUP_START")) {
                    processIncomingVoice(line);
                    continue;
                }

                // ======= 🗨️ Mensajes normales =======
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

            System.out.println("📥 Recibiendo nota de voz de " + sender + " (" + fileSize + " bytes)");

            File receivedFile = new File("voice_from_" + sender + ".wav");
            try (FileOutputStream fos = new FileOutputStream(receivedFile)) {
                byte[] buffer = new byte[4096];
                long remaining = fileSize;
                while (remaining > 0) {
                    int bytesRead = rawIn.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (bytesRead == -1) break;
                    fos.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
            }

            // Leer la línea de cierre
            String endLine = tcpIn.readLine();
            if (!("VOICE_NOTE_END".equals(endLine) || "VOICE_NOTE_GROUP_END".equals(endLine))) {
                System.err.println("⚠️ Fin de nota de voz no detectado correctamente (recibido: " + endLine + ")");
            }

            System.out.println("✅ Nota de voz recibida de " + sender + ": " + receivedFile.getName());
            if (messageHandler != null) {
                messageHandler.handleMessage("[Nota de voz recibida de " + sender + "]");
            }

        } catch (Exception e) {
            System.err.println("Error procesando nota de voz: " + e.getMessage());
        }
    }

    // ===================
    // Notas de voz (TCP)
    // ===================
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
                dos.write("\nVOICE_NOTE_END\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                dos.flush();
                System.out.println("📤 Nota de voz enviada a " + pendingVoiceTargetUser);
            } else if (pendingVoiceTargetGroup != null) {
                // Protocolo: VOICE_NOTE_GROUP_START <grupo> <tamaño>
                String header = "VOICE_NOTE_GROUP_START " + pendingVoiceTargetGroup + " " + audioData.length + "\n";
                dos.write(header.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                dos.write(audioData);
                dos.write("\nVOICE_NOTE_GROUP_END\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                dos.flush();
                System.out.println("📤 Nota de voz grupal enviada a '" + pendingVoiceTargetGroup + "'");
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
        } catch (IOException ignored) {
            System.err.println("Error cerrando socket TCP: " + ignored.getMessage());
        }
    }

    // ==========================================================
    // ============= 🎙️ ENVÍO DE NOTA DE VOZ PRIVADA ===========
    // ==========================================================
    public void sendVoiceNote(String username, File audioFile) {
        if (!connected || tcpSocket == null) {
            System.out.println("No conectado al servidor.");
            return;
        }

        try {
            long fileSize = audioFile.length();

            // 1️⃣ Enviar comando al servidor
            tcpOut.println("/voice " + username);
            tcpOut.flush();

            // 2️⃣ Enviar tamaño y datos del archivo
            DataOutputStream dos = new DataOutputStream(tcpSocket.getOutputStream());
            dos.writeInt((int) fileSize);

            try (FileInputStream fis = new FileInputStream(audioFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
            }

            dos.flush();
            System.out.println("📤 Nota de voz enviada a " + username);

        } catch (IOException e) {
            System.err.println("Error enviando nota de voz: " + e.getMessage());
        }
    }

    // ==========================================================
    // ============= 🎧 ENVÍO DE NOTA DE VOZ GRUPAL ============
    // ==========================================================
    public void sendGroupVoiceNote(String groupName, File audioFile) {
        if (!connected || tcpSocket == null) {
            System.out.println("No conectado al servidor.");
            return;
        }

        try {
            long fileSize = audioFile.length();

            // 1️⃣ Enviar comando de grupo
            tcpOut.println("/voicegroup " + groupName);
            tcpOut.flush();

            // 2️⃣ Enviar tamaño del archivo y datos
            DataOutputStream dos = new DataOutputStream(tcpSocket.getOutputStream());
            dos.writeInt((int) fileSize);

            try (FileInputStream fis = new FileInputStream(audioFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
            }

            dos.flush();
            System.out.println("📤 Nota de voz grupal enviada al grupo '" + groupName + "'");

        } catch (IOException e) {
            System.err.println("Error enviando nota de voz grupal: " + e.getMessage());
        }
    }
}
