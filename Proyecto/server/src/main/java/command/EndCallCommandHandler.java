package command;

import interfaces.CommandHandler;
import model.ClientHandler;
import model.ChatServer;

/**
 * Manejador del comando /endcall que permite terminar una llamada activa.
 * Puede especificar un ID de llamada especÃ­fico o terminar la llamada actual del usuario.
 */
public class EndCallCommandHandler implements CommandHandler {

    /**
     * Verifica si este manejador puede procesar el comando dado.
     * 
     * @param command El comando a verificar
     * @return true si el comando inicia con "/endcall"
     */
    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/endcall");
    }

    /**
     * Ejecuta el comando para terminar una llamada.
     * 
     * @param command El comando completo (ej: "/endcall" o "/endcall callId123")
     * @param userName El nombre del usuario que termina la llamada
     * @param clientHandler El manejador del cliente
     */
    @Override
    public void execute(String command, String userName, ClientHandler clientHandler) {
        String[] parts = command.split(" ", 2);
        
        String callId = null;
        
        if (parts.length == 2) {
            callId = parts[1].trim();
        } else {
            if (ChatServer.getCallManagerImpl() != null) {
                callId = ChatServer.getCallManagerImpl().getCallOfUser(userName);
            }
        }
        
        if (callId == null || callId.isEmpty()) {
            clientHandler.sendMessage("No estás en ninguna llamada.");
            return;
        }
        
        ChatServer.endCall(callId, userName);
        clientHandler.sendMessage("Llamada terminada: " + callId);
    }
}