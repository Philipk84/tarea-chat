package command;

import interfaces.CommandHandler;
import model.ClientHandler;
import model.ChatServer;

/**
 * Manejador del comando /callgroup que permite iniciar llamadas grupales
 * con todos los miembros de un grupo específico.
 */
public class CallGroupCommandHandler implements CommandHandler {

    /**
     * Verifica si este manejador puede procesar el comando dado.
     * 
     * @param command El comando a verificar
     * @return true si el comando inicia con "/callgroup"
     */
    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/callgroup");
    }

    /**
     * Ejecuta el comando de llamada grupal.
     * 
     * @param command El comando completo (ej: "/callgroup miGrupo")
     * @param userName El nombre del usuario que inicia la llamada grupal
     * @param clientHandler El manejador del cliente
     */
    @Override
    public void execute(String command, String userName, Object clientHandler) {
        if (!(clientHandler instanceof ClientHandler)) return;
        
        ClientHandler handler = (ClientHandler) clientHandler;
        String[] parts = command.split(" ", 2);
        
        if (parts.length < 2) {
            handler.sendMessage("Uso: /callgroup <nombreGrupo>");
            return;
        }
        
        String groupName = parts[1].trim();
        
        if (groupName.isEmpty()) {
            handler.sendMessage("Error: Debes especificar un nombre de grupo válido");
            return;
        }
        
        String callId = ChatServer.startGroupCall(userName, groupName);
        if (callId == null) {
            handler.sendMessage("No se pudo iniciar la llamada grupal (pocos miembros en línea/udp).");
        } else {
            handler.sendMessage("Llamada grupal iniciada: " + callId);
        }
    }
}