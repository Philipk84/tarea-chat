package command;

import interfaces.CommandHandler;
import model.ClientHandler;
import model.ChatServer;
import java.util.Set;

/**
 * Manejador del comando /msggroup que permite enviar mensajes de texto
 * a todos los miembros de un grupo usando TCP (confiable).
 */
public class MessageGroupCommandHandler implements CommandHandler {

    /**
     * Verifica si este manejador puede procesar el comando dado.
     * 
     * @param command El comando a verificar
     * @return true si el comando inicia con "/msggroup "
     */
    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/msggroup ");
    }

    /**
     * Ejecuta el comando de mensaje grupal.
     * Formato: /msggroup <grupo> <mensaje>
     * 
     * @param command El comando completo
     * @param userName El nombre del usuario que envÃ­a el mensaje
     * @param clientHandler El manejador del cliente
     */
    @Override
    public void execute(String command, String userName, ClientHandler clientHandler) {
        String[] parts = command.split(" ", 3);
        
        if (parts.length < 3) {
            clientHandler.sendMessage("Error: Formato correcto -> /msggroup <grupo> <mensaje>");
            return;
        }
        
        String groupName = parts[1].trim();
        String message = parts[2].trim();
        
        if (groupName.isEmpty() || message.isEmpty()) {
            clientHandler.sendMessage("Error: Grupo y mensaje no pueden estar vacÃ­os");
            return;
        }
        
        // Obtener miembros del grupo
        Set<String> members = ChatServer.getGroupMembers(groupName);
        
        if (members == null || members.isEmpty()) {
            clientHandler.sendMessage("Error: El grupo '" + groupName + "' no existe o no tiene miembros");
            return;
        }
        
        // Verificar que el usuario es miembro del grupo
        if (!members.contains(userName)) {
            clientHandler.sendMessage("Error: No eres miembro del grupo '" + groupName + "'");
            return;
        }
        
        // Enviar mensaje a todos los miembros del grupo excepto al remitente
        int sentCount = 0;
        for (String member : members) {
            if (!member.equals(userName)) {
                ClientHandler memberHandler = ChatServer.getClientHandler(member);
                if (memberHandler != null) {
                    memberHandler.sendMessage("MENSAJE_GRUPO [" + groupName + "] de " + userName + ": " + message);
                    sentCount++;
                }
            }
        }
        
        // Confirmar al remitente
        clientHandler.sendMessage("Mensaje enviado al grupo '" + groupName + "' (" + sentCount + " miembros)");
    }
}
