package command;

import interfaces.CommandHandler;
import model.ClientHandler;
import model.ChatServer;

/**
 * Manejador del comando /udpport que permite a los clientes registrar
 * su puerto UDP local para comunicación de audio.
 */
public class UdpPortCommandHandler implements CommandHandler {
    
    /**
     * Verifica si este manejador puede procesar el comando dado.
     * 
     * @param command El comando a verificar
     * @return true si el comando inicia con "/udpport"
     */
    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/udpport");
    }

    /**
     * Ejecuta el comando de registro de puerto UDP.
     * 
     * @param command El comando completo (ej: "/udpport 12345")
     * @param userName El nombre del usuario que ejecuta el comando
     * @param clientHandler El manejador del cliente
     */
    @Override
    public void execute(String command, String userName, Object clientHandler) {
        if (!(clientHandler instanceof ClientHandler)) return;
        
        ClientHandler handler = (ClientHandler) clientHandler;
        String[] parts = command.split(" ", 2);
        
        if (parts.length == 2) {
            try {
                int port = Integer.parseInt(parts[1].trim());
                if (port <= 0 || port > 65535) {
                    handler.sendMessage("Error: El puerto debe estar entre 1 y 65535");
                    return;
                }
                
                String clientIp = handler.getClientSocket().getInetAddress().getHostAddress();
                String ipPort = clientIp + ":" + port;
                ChatServer.registerUdpInfo(userName, ipPort);
                handler.sendMessage("UDP registrado: " + ipPort);
            } catch (NumberFormatException e) {
                handler.sendMessage("Error: El puerto debe ser un número válido");
            }
        } else {
            handler.sendMessage("Uso: /udpport <puerto>");
        }
    }
}