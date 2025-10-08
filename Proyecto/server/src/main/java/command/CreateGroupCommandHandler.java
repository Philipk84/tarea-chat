package command;

import interfaces.CommandHandler;
import model.ClientHandler;
import model.ChatServer;

/**
 * Manejador del comando /creategroup que permite crear nuevos grupos de chat.
 * El usuario que crea el grupo automáticamente se convierte en miembro.
 */
public class CreateGroupCommandHandler implements CommandHandler {

    /**
     * Verifica si este manejador puede procesar el comando dado.
     * 
     * @param command El comando a verificar
     * @return true si el comando inicia con "/creategroup"
     */
    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/creategroup");
    }

    /**
     * Ejecuta el comando para crear un nuevo grupo.
     * 
     * @param command El comando completo (ej: "/creategroup miNuevoGrupo")
     * @param userName El nombre del usuario que crea el grupo
     * @param clientHandler El manejador del cliente
     */
    @Override
    public void execute(String command, String userName, Object clientHandler) {
        if (!(clientHandler instanceof ClientHandler)) return;
        
        ClientHandler handler = (ClientHandler) clientHandler;
        String[] parts = command.split(" ", 2);
        
        if (parts.length != 2) {
            handler.sendMessage("Uso: /creategroup <nombreGrupo>");
            return;
        }
        
        String groupName = parts[1].trim();
        
        if (groupName.isEmpty()) {
            handler.sendMessage("Error: El nombre del grupo no puede estar vacío");
            return;
        }
        
        if (groupName.length() > 50) {
            handler.sendMessage("Error: El nombre del grupo no puede exceder 50 caracteres");
            return;
        }
        
        if (!isValidGroupName(groupName)) {
            handler.sendMessage("Error: El nombre del grupo contiene caracteres no válidos. Use solo letras, números, guiones y guiones bajos");
            return;
        }
        
        ChatServer.createGroup(groupName, userName);
        handler.sendMessage("Grupo creado: " + groupName);
    }

    /**
     * Valida que el nombre del grupo contenga solo caracteres permitidos.
     * 
     * @param groupName El nombre del grupo a validar
     * @return true si el nombre es válido, false en caso contrario
     */
    private boolean isValidGroupName(String groupName) {
        return groupName.matches("^[a-zA-Z0-9_-]+$");
    }
}