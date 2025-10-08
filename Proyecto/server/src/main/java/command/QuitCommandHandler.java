package command;

import interfaces.CommandHandler;
import model.ClientHandler;

/**
 * Manejador del comando /quit que permite a los usuarios desconectarse
 * del servidor de manera ordenada.
 */
public class QuitCommandHandler implements CommandHandler {

    /**
     * Verifica si este manejador puede procesar el comando dado.
     * 
     * @param command El comando a verificar
     * @return true si el comando es exactamente "/quit"
     */
    @Override
    public boolean canHandle(String command) {
        return command.equals("/quit");
    }

    /**
     * Ejecuta el comando para desconectar al usuario del servidor.
     * 
     * @param command El comando completo (debe ser "/quit")
     * @param userName El nombre del usuario que se desconecta
     * @param clientHandler El manejador del cliente
     */
    @Override
    public void execute(String command, String userName, Object clientHandler) {
        if (!(clientHandler instanceof ClientHandler)) return;
        
        ClientHandler handler = (ClientHandler) clientHandler;
        handler.sendMessage("¡Adiós " + userName + "! Gracias por usar el sistema de chat.");
        handler.sendMessage("DISCONNECT");
    }
}