package command;

import interfaces.CommandHandler;
import model.ClientHandler;
import model.ChatServer;
import service.HistoryService;

/**
 * Manejador del comando /msg que permite enviar mensajes de texto privados
 * a un usuario especÃ­fico usando TCP (confiable).
 */
public class MessageCommandHandler implements CommandHandler {

    /**
     * Verifica si este manejador puede procesar el comando dado.
     * 
     * @param command El comando a verificar
     * @return true si el comando inicia con "/msg "
     */
    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/msg ");
    }

    /**
     * Ejecuta el comando de mensaje privado a otro usuario.
     * Formato: /msg <usuario> <mensaje>
     * 
     * @param command El comando completo
     * @param userName El nombre del usuario que envÃ­a el mensaje
     * @param clientHandler El manejador del cliente
     */
    @Override
    public void execute(String command, String userName, ClientHandler clientHandler) {
        String[] parts = command.split(" ", 3);
        String targetUser = parts[1].trim();
        String message = parts[2].trim();
        
        if (targetUser.isEmpty() || message.isEmpty()) {
            clientHandler.sendMessage("Error: Usuario y mensaje no pueden estar vacíos");
            return;
        }
        
        if (targetUser.equals(userName)) {
            clientHandler.sendMessage("Error: No puedes enviarte mensajes a ti mismo");
            return;
        }
        
        ClientHandler targetHandler = getUserHandler(targetUser);
        
        if (targetHandler == null) {
            clientHandler.sendMessage("Error: Usuario '" + targetUser + "' no está conectado");
            return;
        }
        
        targetHandler.sendMessage("MENSAJE_PRIVADO de " + userName + ": " + message);

        HistoryService.logTextPrivate(userName, targetUser, message);
    }
    
    /**
     * Obtiene el manejador de cliente para un usuario específico.
     * 
     * @param username Nombre del usuario
     * @return ClientHandler del usuario o null si no existe
     */
    private ClientHandler getUserHandler(String username) {
        try {
            return ChatServer.getClientHandler(username);
        } catch (Exception e) {
            return null;
        }
    }
}
