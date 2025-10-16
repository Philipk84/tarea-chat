package command;

import interfaces.CommandHandler;
import model.ClientHandler;
import model.ChatServer;
import java.net.InetSocketAddress;

/**
 * Manejador del comando /udpport que permite a los clientes registrar
 * su puerto UDP local para comunicaciÃ³n de audio.
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
     * @param command       El comando completo (ej: "/udpport 12345")
     * @param userName      El nombre del usuario que ejecuta el comando
     * @param clientHandler El manejador del cliente
     */
    @Override
    public void execute(String command, String userName, ClientHandler clientHandler) {

        String[] parts = command.split(" ", 2);

        try {
            int port = Integer.parseInt(parts[1].trim());
            if (port <= 0 || port > 65535) {
                clientHandler.sendMessage("Error: El puerto debe estar entre 1 y 65535");
                return;
            }

            String clientIp = clientHandler.getClientSocket().getInetAddress().getHostAddress();
            String ipPort = clientIp + ":" + port;
            ChatServer.registerUdpInfo(userName, ipPort);
            clientHandler.sendMessage("UDP registrado: " + ipPort);

            InetSocketAddress udpAddr = new InetSocketAddress(clientIp, port);
            ChatServer.registerUdpClientAddress(udpAddr, userName);
        } catch (NumberFormatException e) {
            clientHandler.sendMessage("Error: El puerto debe ser un número válido");
        }
    }
}