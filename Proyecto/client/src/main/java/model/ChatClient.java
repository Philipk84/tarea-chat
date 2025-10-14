package model;

import interfaces.*;
import service.*;
import java.net.*;
import java.io.*;

/**
 * Cliente de chat principal que coordina todos los servicios del cliente.
 * Maneja texto, llamadas y notas de voz.
 */
public class ChatClient {
    private final NetworkService networkService;
    private final CallManager callManager;
    private final AudioService audioService;
    private final Config config;

    private DatagramSocket udpSocket;
    private Socket voiceSocket;
    private ObjectOutputStream voiceOut;
    private ObjectInputStream voiceIn;
    private Thread voiceListenerThread;
    private volatile boolean running = false;
    private boolean isGroupTarget = false;


    private String username;
    private String currentTargetUser = null;

    public ChatClient(Config config) {
        this.config = config;
        this.networkService = new NetworkServiceImpl(config.getHost(), config.getPort());
        this.callManager = new CallManagerImpl();
        this.audioService = new AudioServiceImpl();
    }

    /** Conecta el cliente al servidor principal (texto y control). */
    public String connect(String username) {
        this.username = username;
        try {
            // 🔹 Crear socket UDP local
            udpSocket = new DatagramSocket();
            int udpPort = udpSocket.getLocalPort();
            audioService.setUdpSocket(udpSocket);

            // 🔹 Conectarse al servidor TCP (texto)
            String result = networkService.connect(username);

            if (networkService.isConnected()) {
                networkService.sendCommand("/udpport " + udpPort);
                System.out.println("Puerto UDP local: " + udpPort + " (registrado con servidor)");

                // 🔹 Iniciar canal de voz en un hilo separado (no bloquea)
                new Thread(() -> startVoiceChannel(config.getHost()), "VoiceChannelStarter").start();
            }

            return result;
        } catch (IOException e) {
            return "Error configurando conexión: " + e.getMessage();
        }
    }


    /** Inicia conexión TCP separada para notas de voz (en segundo plano). */
    private void startVoiceChannel(String host) {
        int voicePort = config.getPort() + 2; // coincide con el puerto de voz del servidor
        int maxRetries = 10;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                voiceSocket = new Socket(host, voicePort);

                voiceOut = new ObjectOutputStream(voiceSocket.getOutputStream());
                voiceOut.flush();

                // 🔹 Enviar username al servidor de voz al conectarse
                voiceOut.writeObject(username);
                voiceOut.flush();

                voiceIn = new ObjectInputStream(voiceSocket.getInputStream());
                running = true;

                // 🔹 Hilo que escucha notas de voz entrantes
                voiceListenerThread = new Thread(() -> {
                    try {
                        while (running) {
                            Object obj = voiceIn.readObject();
                            if (obj instanceof VoiceNote note) {
                                System.out.println("\n🔔 Nota de voz recibida de " + note.getFromUser());
                                AudioUtils.playAudio(note.getAudioData());
                            }
                        }
                    } catch (EOFException ignored) {
                    } catch (Exception e) {
                        if (running)
                            System.err.println("Error recibiendo nota de voz: " + e.getMessage());
                    }
                }, "VoiceListener");

                voiceListenerThread.setDaemon(true);
                voiceListenerThread.start();

                System.out.println("🎧 Canal de voz conectado en puerto " + voicePort);
                return; // conexión exitosa

            } catch (IOException e) {
                attempt++;
                System.err.println("Intento " + attempt + " fallido conectando canal de voz: " + e.getMessage());
                try {
                    Thread.sleep(200); // espera 200 ms antes de reintentar
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        System.err.println("❌ No se pudo conectar al servidor de voz tras " + maxRetries + " intentos.");
    }



    /** Envía un comando normal de texto al servidor. */
    public void sendCommand(String command) {
        networkService.sendCommand(command);
    }

    /**
     * Define el usuario destino para notas de voz.
     * Se puede usar desde el comando /voice <usuario>
     */
    public void setVoiceTarget(String username) {
        this.currentTargetUser = username;
        this.isGroupTarget = false;
        System.out.println("🎯 Destinatario de nota de voz establecido (usuario): " + username);
    }

    public void setGroupVoiceTarget(String groupName) {
        this.currentTargetUser = groupName;
        this.isGroupTarget = true;
        System.out.println("🎯 Destinatario de nota de voz establecido (grupo): " + groupName);
    }


    /** Graba y envía una nota de voz al usuario actual. */
    public void sendVoiceNote(int seconds) {
        if (currentTargetUser == null) {
            System.err.println("⚠ No hay destinatario definido.");
            return;
        }

        new Thread(() -> {
            try {
                System.out.println("🎙 Grabando nota de voz (" + seconds + "s)...");
                byte[] audioData = AudioUtils.recordAudio(seconds);
                VoiceNote note = new VoiceNote(username, currentTargetUser, audioData, isGroupTarget);

                synchronized (voiceOut) {
                    voiceOut.writeObject(note);
                    voiceOut.flush();
                }
                System.out.println("✅ Nota de voz enviada a " + currentTargetUser);
            } catch (IOException e) {
                System.err.println("Error enviando nota de voz: " + e.getMessage());
            }
        }, "VoiceSender-" + currentTargetUser).start();
    }



    /** Cierra todos los recursos abiertos. */
    public void disconnect() {
        running = false;
        networkService.disconnect();
        callManager.endCall();

        try {
            if (voiceListenerThread != null) voiceListenerThread.interrupt();
            if (voiceIn != null) voiceIn.close();
            if (voiceOut != null) voiceOut.close();
            if (voiceSocket != null && !voiceSocket.isClosed()) voiceSocket.close();
            if (udpSocket != null && !udpSocket.isClosed()) udpSocket.close();
        } catch (IOException e) {
            System.err.println("Error cerrando recursos: " + e.getMessage());
        }

        if (audioService instanceof AudioServiceImpl impl) impl.shutdown();
    }

    public boolean isConnected() {
        return networkService.isConnected();
    }
}
