package model;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    // Singleton
    private static Server instance;

    private DatagramSocket socket;
    private Map<SocketAddress, String> clients;
    private byte[] receiveData;
    private BufferedReader reader;
    private DatagramPacket packet;
    private Config config;
    private ExecutorService pool;
    private Thread receiveThread;
    
    private Server (Config config) throws SocketException, UnknownHostException {
        this.config = config;

        this.receiveData = new byte[256];
        
        this.pool = Executors.newFixedThreadPool(10);
        this.clients = new ConcurrentHashMap<>();

        this.socket = new DatagramSocket(config.getPort(), InetAddress.getByName(config.getHost()));
        this.packet = new DatagramPacket(receiveData, receiveData.length);
    }

    public static Server getInstance(Config config) throws SocketException, UnknownHostException {
        if (instance == null) {
            instance = new Server(config);
        }
        return instance;
    }

    public String startServer() {
        receiveThread = new Thread(() -> {
            while (!socket.isClosed()) {
                try { 
                    socket.receive(packet);
                    pool.submit(new ClientHandler(packet, socket, clients));
                } catch (Exception e) {
                    if (!socket.isClosed()) e.printStackTrace();
                }
            }
        });
        receiveThread.start();
        return "Servidor Iniciado en " + config.getHost() + " : " + config.getPort();
    }

    public String closeServer() {
        try {
            socket.close();
            pool.shutdownNow();
            
            if (receiveThread != null && receiveThread.isAlive()) {
                receiveThread.join(5000);
            }
            return "Servidor Cerrado";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Interrupción durante el cierre del servidor: " + e.getMessage();
        }
    }
}
