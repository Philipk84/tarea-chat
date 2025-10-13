package model;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class VoiceChannelHandler extends Thread {

    private final String username;
    private final int port; // ← nuevo atributo
    private ServerSocket serverSocket;
    private Socket voiceSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private volatile boolean running = true;

    public VoiceChannelHandler(String username, int port) {
        this.username = username;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);

            System.out.println("🎧 Canal de voz para " + username + " escuchando en puerto fijo " + port);

            // Ya no registramos puertos UDP aquí
            // porque ahora el puerto es fijo y global

            voiceSocket = serverSocket.accept();

            out = new ObjectOutputStream(voiceSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(voiceSocket.getInputStream());

            while (running) {
                Object obj = in.readObject();
                if (obj instanceof VoiceNote note) {
                    System.out.println("🎤 Nota de voz recibida de " + note.getFromUser());
                    ChatServer.forwardVoiceNote(note);
                }
            }

        } catch (Exception e) {
            if (running)
                System.err.println("Error en canal de voz de " + username + ": " + e.getMessage());
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

    public void shutdown() {
        running = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (voiceSocket != null && !voiceSocket.isClosed()) voiceSocket.close();
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException ignored) {}
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket tmp = new ServerSocket(0)) {
            return tmp.getLocalPort();
        }
    }
}
