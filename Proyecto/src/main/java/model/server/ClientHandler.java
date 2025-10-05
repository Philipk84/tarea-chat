package model.server;

import model.Message;
import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private CopyOnWriteArrayList<ClientHandler> clients;
    private String clientName;

    public ClientHandler(Socket socket, CopyOnWriteArrayList<ClientHandler> clients) {
        this.socket = socket;
        this.clients = clients;
    }

    @Override
    public void run() {
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());

            // El cliente envía su nombre al conectarse
            clientName = (String) input.readObject();
            System.out.println("Usuario conectado: " + clientName);

            Message message;
            while ((message = (Message) input.readObject()) != null) {
                System.out.println("[" + clientName + "]: " + message.getContent());
                broadcast(message);
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Cliente desconectado: " + clientName);
        } finally {
            clients.remove(this);
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private void broadcast(Message message) {
        for (ClientHandler client : clients) {
            try {
                client.output.writeObject(message);
                client.output.flush();
            } catch (IOException ignored) {}
        }
    }
}
