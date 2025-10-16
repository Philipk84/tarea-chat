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
    private final CallManager CallManagerImpl;
    private final AudioService audioService;
    private final MessageHandler messageHandler;
    private final String serverHost;
    private final int serverPort;
    
    private DatagramSocket udpSocket;

    public ChatClient(Config config) {
        this.serverHost = config.getHost();
        this.serverPort = config.getPort();
        this.networkService = new NetworkServiceImpl(serverHost, serverPort);
        this.CallManagerImpl = new CallManagerImpl();
        this.audioService = new AudioServiceImpl();
        this.messageHandler = new MessageHandlerImpl("");        
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
            udpSocket = new DatagramSocket();
            int udpPort = udpSocket.getLocalPort();
            audioService.setUdpSocket(udpSocket);
            
            MessageHandlerImpl newMessageHandler = new MessageHandlerImpl(username);
            newMessageHandler.setCallManagerImpl(CallManagerImpl);
            networkService.setMessageHandler(newMessageHandler);
            
            String result = networkService.connect(username);
            
            if (networkService.isConnected()) {
                networkService.sendCommand("/udpport " + udpPort);
                System.out.println("Puerto UDP local: " + udpPort + " (registrado con servidor)");

                try {
                    byte[] payload = username.getBytes();
                    DatagramPacket hello = new DatagramPacket(
                        payload, payload.length,
                        InetAddress.getByName(serverHost), serverPort + 1
                    );
                    udpSocket.send(hello);
                    System.out.println("[INFO] Enviado HELLO UDP al servidor para auto-registro IP:PUERTO");
                } catch (Exception ex) {
                    System.err.println("[WARN] No se pudo enviar HELLO UDP al servidor: " + ex.getMessage());
                }
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
        if (command.startsWith("/call ") || command.startsWith("/callgroup ")) {
            ensureUdpReadyAndRegistered();
        }

        if (command.startsWith("/endcall")) {
            networkService.sendCommand(command);
            CallManagerImpl.endCall();
        } else {
            networkService.sendCommand(command);
        }
    }

    /**
     * Inicia grabación de nota de voz para un usuario.
     */
    public void startVoiceNoteToUser(String username) {
        networkService.startVoiceNoteToUser(username);
    }

    /**
     * Inicia grabación de nota de voz para un grupo.
     */
    public void startVoiceNoteToGroup(String groupName) {
        networkService.startVoiceNoteToGroup(groupName);
    }

    /**
     * Detiene grabación y envía la nota de voz.
     */
    public void stopAndSendVoiceNote() {
        networkService.stopAndSendVoiceNote();
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

    /**
     * Garantiza que el socket UDP esté abierto y registrado con el servidor.
     * Si se recrea, vuelve a enviar /udpport <puerto>.
     */
    private void ensureUdpReadyAndRegistered() {
        try {
            if (udpSocket == null || udpSocket.isClosed()) {
                udpSocket = new DatagramSocket();
                int udpPort = udpSocket.getLocalPort();
                audioService.setUdpSocket(udpSocket);
                if (networkService.isConnected()) {
                    networkService.sendCommand("/udpport " + udpPort);
                    System.out.println("[INFO] Reabierto socket UDP en puerto " + udpPort + " y registrado con el servidor.");
                }
            }
        } catch (SocketException e) {
            System.err.println("No se pudo preparar UDP: " + e.getMessage());
        }
    }
}