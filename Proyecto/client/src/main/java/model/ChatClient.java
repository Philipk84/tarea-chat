package model;

import interfaces.*;
import service.*;
import java.net.*;

/**
 * Cliente de chat principal que coordina todos los servicios del cliente.
 * Actúa como coordinador central usando inyección de dependencias.
 * Implementa el principio SOLID de Inversión de Dependencias (DIP).
 * 
 * @author Sistema de Chat
 * @version 1.0
 */
public class ChatClient {
    private final NetworkService networkService;
    private final CallManager callManager;
    private final AudioService audioService;
    private final MessageHandler messageHandler;
    
    private DatagramSocket udpSocket;
    private String username;

    public ChatClient(Config config) {
        // Crear servicios con inyección de dependencias
        this.networkService = new NetworkServiceImpl(config.getHost(), config.getPort());
        this.callManager = new CallManagerImpl();
        this.audioService = new AudioServiceImpl();
        this.messageHandler = new MessageHandlerImpl(""); // username se establece en connect()
        
        // Configurar relaciones entre servicios
        setupServiceDependencies();
    }

    /**
     * Establece conexión con el servidor usando el nombre de usuario especificado.
     * 
     * @param username Nombre de usuario para la conexión
     * @return Mensaje de bienvenida del servidor o error
     */
    public String connect(String username) {
        this.username = username;
        
        try {
            // Configurar socket UDP
            udpSocket = new DatagramSocket(); // Sistema selecciona puerto libre
            int udpPort = udpSocket.getLocalPort();
            audioService.setUdpSocket(udpSocket);
            
            // Crear nuevo messageHandler con username correcto
            MessageHandlerImpl newMessageHandler = new MessageHandlerImpl(username);
            newMessageHandler.setCallManager(callManager);
            networkService.setMessageHandler(newMessageHandler);
            
            // Conectar vía TCP
            String result = networkService.connect(username);
            
            if (networkService.isConnected()) {
                // Registrar puerto UDP con el servidor
                networkService.sendCommand("/udpport " + udpPort);
                System.out.println("Puerto UDP local: " + udpPort + " (registrado con servidor)");
            }
            
            return result;
            
        } catch (SocketException e) {
            return "Error configurando UDP: " + e.getMessage();
        }
    }

    /**
     * Envía un comando al servidor.
     * 
     * @param command Comando a enviar
     */
    public void sendCommand(String command) {
        if (command.startsWith("/endcall")) {
            networkService.sendCommand(command);
            callManager.endCall();
        } else {
            networkService.sendCommand(command);
        }
    }

    /**
     * Desconecta del servidor y libera recursos.
     */
    public void disconnect() {
        callManager.endCall();
        networkService.disconnect();
        if (audioService instanceof AudioServiceImpl) {
            ((AudioServiceImpl) audioService).shutdown();
        }
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
    }

    /**
     * Verifica si está conectado al servidor.
     * 
     * @return true si está conectado, false en caso contrario
     */
    public boolean isConnected() {
        return networkService.isConnected();
    }

    /**
     * Configura las dependencias entre servicios.
     */
    private void setupServiceDependencies() {
        networkService.setMessageHandler(messageHandler);
        callManager.setAudioService(audioService);
    }
}