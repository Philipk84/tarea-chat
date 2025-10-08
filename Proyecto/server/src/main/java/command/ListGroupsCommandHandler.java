package command;

import interfaces.CommandHandler;
import model.ClientHandler;
import model.ChatServer;
import java.util.Set;

/**
 * Manejador del comando /listgroups que permite ver todos los grupos
 * disponibles en el servidor de chat.
 */
public class ListGroupsCommandHandler implements CommandHandler {

    /**
     * Verifica si este manejador puede procesar el comando dado.
     * 
     * @param command El comando a verificar
     * @return true si el comando es exactamente "/listgroups"
     */
    @Override
    public boolean canHandle(String command) {
        return command.equals("/listgroups");
    }

    /**
     * Ejecuta el comando para listar todos los grupos disponibles.
     * 
     * @param command El comando completo (debe ser "/listgroups")
     * @param userName El nombre del usuario que solicita la lista
     * @param clientHandler El manejador del cliente
     */
    @Override
    public void execute(String command, String userName, Object clientHandler) {
        if (!(clientHandler instanceof ClientHandler)) return;
        
        ClientHandler handler = (ClientHandler) clientHandler;
        Set<String> groups = ChatServer.getGroups();
        
        if (groups.isEmpty()) {
            handler.sendMessage("No hay grupos disponibles. Usa /creategroup <nombre> para crear uno.");
            return;
        }
        
        StringBuilder response = new StringBuilder("Grupos disponibles (");
        response.append(groups.size()).append("):\n");
        
        for (String group : groups) {
            Set<String> members = ChatServer.getGroupMembers(group);
            response.append("• ").append(group)
                .append(" (").append(members.size()).append(" miembros)");
            
            if (members.contains(userName)) {
                response.append(" [MIEMBRO]");
            }
            response.append("\n");
        }
        
        handler.sendMessage(response.toString().trim());
    }
}