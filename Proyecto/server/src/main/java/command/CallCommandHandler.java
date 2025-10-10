package command;

import interfaces.CommandHandler;
import model.ClientHandler;
import model.ChatServer;

/**
 * Manejador del comando /call que permite iniciar llamadas privadas
 * entre dos usuarios.
 */
public class CallCommandHandler implements CommandHandler {

    /**
     * Verifica si este manejador puede procesar el comando dado.
     * 
     * @param command El comando a verificar
     * @return true si el comando inicia con "/call"
     */
    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/call");
    }

    /**
     * Ejecuta el comando de llamada privada a otro usuario.
     * 
     * @param command El comando completo (ej: "/call usuario123")
     * @param userName El nombre del usuario que inicia la llamada
     * @param clientHandler El manejador del cliente
     */
    @Override
    public void execute(String command, String userName, ClientHandler clientHandler) {
        String[] parts = command.split(" ", 2);
        String targetUser = parts[1].trim();
        
        if (targetUser.isEmpty()) {
            clientHandler.sendMessage("Error: Debes especificar un nombre de usuario válido");
            return;
        }
        
        if (targetUser.equals(userName)) {
            clientHandler.sendMessage("Error: No puedes llamarte a ti mismo");
            return;
        }
        
        String callId = ChatServer.startIndividualCall(userName, targetUser);
        if (callId == null) {
            clientHandler.sendMessage("No se pudo iniciar la llamada (usuario no disponible o sin UDP).");
        } else {
            clientHandler.sendMessage("Llamada iniciada: " + callId);
        }
    }
}