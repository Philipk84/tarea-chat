package model;

import java.io.*;
import java.net.Socket;

/**
 * Canal de voz para cada cliente.
 * Permite enviar y recibir objetos VoiceNote de manera independiente.
 */
public class VoiceChannelHandler implements Runnable {
    private final Socket clientSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private volatile boolean running = true;
    private final String username;

    public VoiceChannelHandler(Socket clientSocket, String username) throws IOException {
        this.clientSocket = clientSocket;
        this.username = username;
        this.out = new ObjectOutputStream(clientSocket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(clientSocket.getInputStream());
    }

    public VoiceChannelHandler(Socket clientSocket, String username,
                               ObjectInputStream in, ObjectOutputStream out) {
        this.clientSocket = clientSocket;
        this.username = username;
        this.in = in;
        this.out = out;
    }




    @Override
    public void run() {
        try {
            while (running) {
                Object obj = in.readObject();
                if (obj instanceof VoiceNote note) {
                    System.out.println("🎤 Nota de voz de " + note.getFromUser() +
                            " → " + note.getTarget());
                    ChatServer.forwardVoiceNote(note);
                }
            }
        } catch (Exception e) {
            if (running)
                System.err.println("❌ Error en canal de voz de " + username + ": " + e.getMessage());
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
            System.err.println("❌ Error enviando nota de voz a " + username + ": " + e.getMessage());
        }
    }

    public void shutdown() {
        running = false;
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException ignored) {
        }
    }
}
