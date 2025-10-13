package model;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Maneja el canal de voz de un usuario (notas de voz, llamadas).
 * Se ejecuta en un hilo separado del canal de texto.
 */
public class VoiceChannelHandler extends Thread {
    private final String username;
    private ServerSocket serverSocket;
    private boolean running = true;
    private Socket voiceSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public VoiceChannelHandler(String username) {
        this.username = username;
    }

    @Override
    public void run() {
        try {
            // cada cliente tiene su propio puerto de voz
            int port = findAvailablePort();
            serverSocket = new ServerSocket(port);
            System.out.println("🎧 Canal de voz para " + username + " en puerto " + port);

            ChatServer.registerUdpPort(username,
                    serverSocket.getInetAddress() == null ?
                            java.net.InetAddress.getLocalHost() :
                            serverSocket.getInetAddress(), port);

            voiceSocket = serverSocket.accept();
            out = new ObjectOutputStream(voiceSocket.getOutputStream());
            in = new ObjectInputStream(voiceSocket.getInputStream());

            while (running) {
                Object obj = in.readObject();
                if (obj instanceof VoiceNote note) {
                    System.out.println("🎤 Nota de voz recibida de " + note.getFromUser());
                    ChatServer.forwardVoiceNote(note);
                }
            }
        } catch (Exception e) {
            if (running) System.err.println("Error en canal de voz de " + username + ": " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    public synchronized void sendVoice(VoiceNote note) {
        try {
            if (out != null) {
                out.writeObject(note);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Error enviando nota de voz a " + username + ": " + e.getMessage());
        }
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket tmp = new ServerSocket(0)) {
            return tmp.getLocalPort();
        }
    }

    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
            if (voiceSocket != null) voiceSocket.close();
        } catch (IOException ignored) {}
    }
}
