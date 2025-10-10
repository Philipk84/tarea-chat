package command;

import interfaces.CommandHandler;
import model.ClientHandler;
import model.ChatServer;

/**
 * Manejador del comando /creategroup que permite crear nuevos grupos de chat.
 * El usuario que crea el grupo automÃ¡ticamente se convierte en miembro.
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
    public void execute(String command, String userName, ClientHandler clientHandler) {
        String[] parts = command.split(" ", 2);
        String groupName = parts[1].trim();
        
        if (groupName.isEmpty()) {
            clientHandler.sendMessage("Error: El nombre del grupo no puede estar vacÃ­o");
            return;
        }
        
        if (groupName.length() > 50) {
            clientHandler.sendMessage("Error: El nombre del grupo no puede exceder 50 caracteres");
            return;
        }
        
        if (!isValidGroupName(groupName)) {
            clientHandler.sendMessage("Error: El nombre del grupo contiene caracteres no vÃ¡lidos. Use solo letras, nÃºmeros, guiones y guiones bajos");
            return;
        }
        
        ChatServer.createGroup(groupName, userName);
        clientHandler.sendMessage("Grupo creado: " + groupName);
    }

    /**
     * Valida que el nombre del grupo contenga solo caracteres permitidos.
     * 
     * @param groupName El nombre del grupo a validar
     * @return true si el nombre es vÃ¡lido, false en caso contrario
     */
    private boolean isValidGroupName(String groupName) {
        return groupName.matches("^[a-zA-Z0-9_-]+$");
    }
}