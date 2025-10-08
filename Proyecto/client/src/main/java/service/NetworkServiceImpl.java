package service;

import interfaces.MessageHandler;
import interfaces.NetworkService;
import java.io.*;
import java.net.*;

/**
 * Implementación del servicio de comunicación de red del cliente.
 * Maneja la conexión TCP con el servidor y el envío de comandos.
 */
public class NetworkServiceImpl implements NetworkService {
    private final String serverHost;
    private final int serverPort;
    private String username;
    
    private Socket tcpSocket;
    private BufferedReader tcpIn;
    private PrintWriter tcpOut;
    private boolean connected = false;
    
    private MessageHandler messageHandler;
    private Thread listenerThread;

    /**
     * Constructor que configura la conexión con los parámetros del servidor.
     * 
     * @param serverHost Dirección del servidor
     * @param serverPort Puerto del servidor
     */
    public NetworkServiceImpl(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    @Override
    public String connect(String username) {
        this.username = username;
        try {
            tcpSocket = new Socket(serverHost, serverPort);
            tcpIn = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            tcpOut = new PrintWriter(tcpSocket.getOutputStream(), true);

            // Leer prompt del servidor y enviar nombre de usuario
            tcpIn.readLine(); // "Enter your name:" (ignorado)
            tcpOut.println(username);
            String welcomeMessage = tcpIn.readLine(); // mensaje de bienvenida

            connected = true;
            startMessageListener();
            
            return welcomeMessage;

        } catch (IOException e) {
            return "Error connecting to server: " + e.getMessage();
        }
    }

    @Override
    public void sendCommand(String command) {
        if (!connected || tcpOut == null) {
            System.out.println("No conectado al servidor");
            return;
        }

        if (command.equals("/quit")) {
            tcpOut.println(command);
            disconnect();
        } else {
            tcpOut.println(command);
        }
    }

    @Override
    public void disconnect() {
        connected = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        cleanup();
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    /**
     * Inicia el hilo que escucha mensajes del servidor.
     */
    private void startMessageListener() {
        listenerThread = new Thread(this::listenServer);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Escucha continuamente los mensajes del servidor y los delega al manejador.
     */
    private void listenServer() {
        String line;
        try {
            while (connected && (line = tcpIn.readLine()) != null) {
                if (messageHandler != null) {
                    messageHandler.handleMessage(line);
                } else {
                    System.out.println("[SERVER] " + line);
                }
            }
        } catch (IOException e) {
            if (connected) {
                System.out.println("Conexión con servidor cerrada.");
            }
        }
    }

    /**
     * Limpia los recursos de red utilizados.
     */
    private void cleanup() {
        try {
            if (tcpSocket != null && !tcpSocket.isClosed()) {
                tcpSocket.close();
            }
        } catch (IOException ignored) {}
    }
}