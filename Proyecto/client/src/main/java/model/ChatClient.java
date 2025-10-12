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
                // 🔹 Iniciar canal de voz (TCP separado)
                startVoiceChannel(config.getHost(), config.getVoicePort());
            }

            return result;
        } catch (IOException e) {
            return "Error configurando conexión: " + e.getMessage();
        }
    }

    /** Inicia conexión TCP separada para notas de voz. */
    private void startVoiceChannel(String host, int voicePort) {
        try {
            voiceSocket = new Socket(host, voicePort);
            voiceOut = new ObjectOutputStream(voiceSocket.getOutputStream());
            voiceOut.flush();
            voiceIn = new ObjectInputStream(voiceSocket.getInputStream());
            running = true;

            // 🔹 Hilo que escucha notas de voz entrantes
            voiceListenerThread = new Thread(() -> {
                try {
                    while (running) {
                        Object obj = voiceIn.readObject();
                        if (obj instanceof VoiceNote note) {
                            System.out.println("\n🔔 Nota de voz recibida de " + note.getSender());
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

        } catch (IOException e) {
            System.err.println("Error iniciando canal de voz: " + e.getMessage());
        }
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
        System.out.println("🎯 Destinatario de nota de voz establecido: " + username);
    }

    /** Graba y envía una nota de voz al usuario actual. */
    public void sendVoiceNote(int seconds) {
        if (currentTargetUser == null) {
            System.err.println("⚠️ No hay destinatario definido. Usa /voice <usuario> primero.");
            return;
        }

        try {
            System.out.println("🎙 Grabando nota de voz (" + seconds + "s)...");
            byte[] audioData = AudioUtils.recordAudio(seconds);
            VoiceNote note = new VoiceNote(username, currentTargetUser, audioData);

            voiceOut.writeObject(note);
            voiceOut.flush();
            System.out.println("✅ Nota de voz enviada a " + currentTargetUser);

        } catch (IOException e) {
            System.err.println("Error enviando nota de voz: " + e.getMessage());
        }
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
