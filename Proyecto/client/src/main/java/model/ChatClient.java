package model;

import interfaces.*;
import service.*;
import java.net.*;

/**
 * Cliente de chat principal que coordina todos los servicios del cliente.
 * Actúa como coordinador central usando inyección de dependencias.
 */
public class ChatClient {
    private final NetworkService networkService;
    private final CallManagerImpl CallManagerImpl;
    private final AudioService audioService;
    private final MessageHandler messageHandler;
    
    private DatagramSocket udpSocket;

    public ChatClient(Config config) {
        // Crear servicios con inyección de dependencias
        this.networkService = new NetworkServiceImpl(config.getHost(), config.getPort());
        this.CallManagerImpl = new CallManagerImplImpl();
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
        try {
            // Configurar socket UDP
            udpSocket = new DatagramSocket(); // Sistema selecciona puerto libre
            int udpPort = udpSocket.getLocalPort();
            audioService.setUdpSocket(udpSocket);
            
            // Crear nuevo messageHandler con username correcto
            MessageHandlerImpl newMessageHandler = new MessageHandlerImpl(username);
            newMessageHandler.setCallManagerImpl(CallManagerImpl);
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
            CallManagerImpl.endCall();
        } else {
            networkService.sendCommand(command);
        }
    }

    /**
     * Desconecta del servidor y libera recursos.
     */
    public void disconnect() {
        CallManagerImpl.endCall();
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
        CallManagerImpl.setAudioService(audioService);
    }
}