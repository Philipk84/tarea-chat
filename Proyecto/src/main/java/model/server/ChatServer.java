package model.server;

import java.io.*;
import java.net.*;

public class ChatServer {
    // Singleton
    private static ChatServer instance;

    private DatagramSocket socket;
    private Map<SocketAddress, String> clients;
    private byte[] receiveData;
    private BufferedReader reader;
    
    private ChatServer (Config config) {
        this.socket = new DatagramSocket(
                config.getPort(), InetAddress.getByName(config.getHost())
        );
        this.pool = Executors.newFixedThreadPool(10);
        this.clients = new ConcurrentHashMap<>();
        this.receiveData = new byte[256];
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    public static ChatServer getInstance(Config config) {
        if (instance == null) {
            instance = new ChatServer(config);
        }
        return instance;
    }

    private void startServer() {
        try {
            inicializeConfig();

            ExecutorService pool = Executors.newFixedThreadPool(10);
            Map<SocketAddress, String> clients = new ConcurrentHashMap<>();

        } catch (Exception e) {
            e.printStackTrace();
        }


        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Cliente conectado: " + socket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(socket, clients);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }
}
