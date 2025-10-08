package com.chatapp.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {

    private static final int PORT = 5000;

    // Listado de clientes y grupos activos
    public static final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    public static final ConcurrentHashMap<String, Group> groups = new ConcurrentHashMap<>();

    public static void startServer() {
        System.out.println("Servidor iniciado en el puerto " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, clients, groups);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }
}
