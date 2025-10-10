package command;

import interfaces.CommandHandler;
import model.ClientHandler;
import model.ChatServer;

/**
 * Manejador del comando /voice que notifica al usuario destino que 
 * estÃ¡ por recibir una nota de voz vÃ­a UDP (apropiado para audio).
 */
public class VoiceNoteCommandHandler implements CommandHandler {

    /**
     * Verifica si este manejador puede procesar el comando dado.
     * 
     * @param command El comando a verificar
     * @return true si el comando inicia con "/voice "
     */
    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/voice ");
    }

    /**
     * Ejecuta el comando de nota de voz privada.
     * Formato: /voice <usuario>
     * 
     * @param command El comando completo
     * @param userName El nombre del usuario que envÃ­a la nota
     * @param clientHandler El manejador del cliente
     */
    @Override
    public void execute(String command, String userName, ClientHandler clientHandler) {
        String[] parts = command.split(" ", 2);
        
        if (parts.length < 2) {
            clientHandler.sendMessage("Error: Formato correcto -> /voice <usuario>");
            return;
        }
        
        String targetUser = parts[1].trim();
        
        if (targetUser.isEmpty()) {
            clientHandler.sendMessage("Error: Debes especificar un nombre de usuario vÃ¡lido");
            return;
        }
        
        if (targetUser.equals(userName)) {
            clientHandler.sendMessage("Error: No puedes enviarte notas de voz a ti mismo");
            return;
        }
        
        // Obtener informaciÃ³n UDP del usuario destino
        String targetUdpInfo = ChatServer.getUdpInfo(targetUser);
        
        if (targetUdpInfo == null) {
            clientHandler.sendMessage("Error: Usuario '" + targetUser + "' no estÃ¡ disponible o sin UDP");
            return;
        }
        
        // Obtener informaciÃ³n UDP del remitente
        String senderUdpInfo = ChatServer.getUdpInfo(userName);
        
        if (senderUdpInfo == null) {
            clientHandler.sendMessage("Error: Tu informaciÃ³n UDP no estÃ¡ registrada");
            return;
        }
        
        // Notificar al destinatario
        ClientHandler targetHandler = ChatServer.getClientHandler(targetUser);
        if (targetHandler != null) {
            targetHandler.sendMessage("VOICE_NOTE_INCOMING from " + userName + " " + senderUdpInfo);
        }
        
        // Enviar informaciÃ³n UDP del destinatario al remitente
        clientHandler.sendMessage("VOICE_NOTE_TARGET " + targetUser + " " + targetUdpInfo);
    }
}
