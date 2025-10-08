package com.chatapp.server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {

    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private CopyOnWriteArrayList<ClientHandler> clients;
    private ConcurrentHashMap<String, Group> groups;
    private String name;

    public ClientHandler(Socket socket, CopyOnWriteArrayList<ClientHandler> clients, ConcurrentHashMap<String, Group> groups) {
        this.socket = socket;
        this.clients = clients;
        this.groups = groups;
    }

    @Override
    public void run() {
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
            name = (String) input.readObject();

            sendMessage("Bienvenido, " + name + "!");
            showMenu();

            while (true) {
                String option = (String) input.readObject();
                handleOption(option);
            }

        } catch (Exception e) {
            System.out.println(name + " se ha desconectado.");
        } finally {
            clients.remove(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void showMenu() throws IOException {
        sendMessage("\n=== MENÚ PRINCIPAL ===");
        sendMessage("1. Crear grupo");
        sendMessage("2. Enviar mensaje privado");
        sendMessage("3. Enviar mensaje a grupo");
        sendMessage("4. Salir");
        sendMessage("Seleccione una opción:");
    }

    private void handleOption(String option) throws IOException, ClassNotFoundException {
        switch (option) {
            case "1" -> createGroup();
            case "2" -> sendPrivateMessage();
            case "3" -> sendGroupMessage();
            case "4" -> {
                sendMessage("Desconectando...");
                socket.close();
            }
            default -> sendMessage("Opción inválida.");
        }
        showMenu();
    }

    private void createGroup() throws IOException, ClassNotFoundException {
        sendMessage("Ingrese el nombre del grupo:");
        String groupName = (String) input.readObject();

        if (groups.containsKey(groupName)) {
            sendMessage("El grupo ya existe.");
        } else {
            Group group = new Group(groupName);
            group.addMember(this);
            groups.put(groupName, group);
            sendMessage("Grupo '" + groupName + "' creado correctamente.");
        }
    }

    private void sendPrivateMessage() throws IOException, ClassNotFoundException {
        sendMessage("Ingrese el nombre del usuario destinatario:");
        String recipientName = (String) input.readObject();

        sendMessage("Escriba su mensaje:");
        String content = (String) input.readObject();

        for (ClientHandler client : clients) {
            if (client.name.equals(recipientName)) {
                client.sendMessage("[Privado de " + name + "]: " + content);
                return;
            }
        }
        sendMessage("Usuario no encontrado.");
    }

    private void sendGroupMessage() throws IOException, ClassNotFoundException {
        sendMessage("Ingrese el nombre del grupo:");
        String groupName = (String) input.readObject();

        if (!groups.containsKey(groupName)) {
            sendMessage("El grupo no existe.");
            return;
        }

        sendMessage("Escriba el mensaje para el grupo:");
        String content = (String) input.readObject();

        Group group = groups.get(groupName);
        group.broadcast(content, name);
    }

    public void sendMessage(String message) {
        try {
            output.writeObject(message);
            output.flush();
        } catch (IOException ignored) {}
    }
}
