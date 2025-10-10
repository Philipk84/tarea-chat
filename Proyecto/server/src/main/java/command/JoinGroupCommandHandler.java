package command;

import interfaces.CommandHandler;
import model.ClientHandler;
import model.ChatServer;

/**
 * Manejador del comando /joingroup que permite a los usuarios unirse
 * a grupos existentes para participar en chats y llamadas grupales.
 */
public class JoinGroupCommandHandler implements CommandHandler {

    /**
     * Verifica si este manejador puede procesar el comando dado.
     * 
     * @param command El comando a verificar
     * @return true si el comando inicia con "/joingroup"
     */
    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/joingroup");
    }

    /**
     * Ejecuta el comando para unirse a un grupo existente.
     * 
     * @param command El comando completo (ej: "/joingroup nombreGrupo")
     * @param userName El nombre del usuario que se une al grupo
     * @param clientHandler El manejador del cliente
     */
    @Override
    public void execute(String command, String userName, ClientHandler clientHandler) {
        String[] parts = command.split(" ", 2);
        String groupName = parts[1].trim();
        
        if (groupName.isEmpty()) {
            clientHandler.sendMessage("Error: Debes especificar un nombre de grupo vÃ¡lido");
            return;
        }
        
        // Verificar si el grupo existe
        if (!ChatServer.getGroups().contains(groupName)) {
            clientHandler.sendMessage("Error: El grupo '" + groupName + "' no existe.");
            return;
        }
        
        // Verificar si el usuario ya es miembro del grupo
        if (ChatServer.getGroupMembers(groupName).contains(userName)) {
            clientHandler.sendMessage("Ya eres miembro del grupo: " + groupName);
            return;
        }
        
        ChatServer.joinGroup(groupName, userName);
        clientHandler.sendMessage("Te has unido al grupo: " + groupName);
    }
}