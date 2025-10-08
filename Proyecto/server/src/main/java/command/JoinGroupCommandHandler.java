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
    public void execute(String command, String userName, Object clientHandler) {
        if (!(clientHandler instanceof ClientHandler)) return;
        
        ClientHandler handler = (ClientHandler) clientHandler;
        String[] parts = command.split(" ", 2);
        
        if (parts.length != 2) {
            handler.sendMessage("Uso: /joingroup <nombreGrupo>");
            return;
        }
        
        String groupName = parts[1].trim();
        
        if (groupName.isEmpty()) {
            handler.sendMessage("Error: Debes especificar un nombre de grupo válido");
            return;
        }
        
        // Verificar si el grupo existe
        if (!ChatServer.getGroups().contains(groupName)) {
            handler.sendMessage("Error: El grupo '" + groupName + "' no existe. Usa /listgroups para ver grupos disponibles");
            return;
        }
        
        // Verificar si el usuario ya es miembro del grupo
        if (ChatServer.getGroupMembers(groupName).contains(userName)) {
            handler.sendMessage("Ya eres miembro del grupo: " + groupName);
            return;
        }
        
        ChatServer.joinGroup(groupName, userName);
        handler.sendMessage("Te has unido al grupo: " + groupName);
    }
}