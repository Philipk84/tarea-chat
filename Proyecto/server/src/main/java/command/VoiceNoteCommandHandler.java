package command;

import interfaces.CommandHandler;
import model.ClientHandler;
import model.ChatServer;
import java.net.*;
import model.VoiceNote;
import model.ChatGroup;


/**
 * Manejador del comando /voice <usuario>
 * Permite enviar una nota de voz privada vía UDP a otro usuario.
 */
public class VoiceNoteCommandHandler implements CommandHandler {

    @Override
    public String getCommandName() {
        return "/voice";
    }

    @Override
    public boolean execute(String[] args, String sender, ClientHandler clientHandler) {
        if (args.length < 2) {
            clientHandler.sendMessage("Uso correcto: /voice <usuario>");
            return true;
        }

        String targetUser = args[1].trim();

        if (targetUser.isEmpty()) {
            clientHandler.sendMessage("Error: Debes especificar un nombre de usuario válido.");
            return true;
        }

        if (targetUser.equals(sender)) {
            clientHandler.sendMessage("Error: No puedes enviarte notas de voz a ti mismo.");
            return true;
        }

        // Obtener información UDP de ambos usuarios
        int senderUdpPort = ChatServer.getUserUdpPort(sender);
        int targetUdpPort = ChatServer.getUserUdpPort(targetUser);

        if (senderUdpPort == -1) {
            clientHandler.sendMessage("Error: No has registrado tu puerto UDP. Usa /udpport <puerto>.");
            return true;
        }

        if (targetUdpPort == -1) {
            clientHandler.sendMessage("Error: El usuario '" + targetUser + "' no ha registrado su puerto UDP.");
            return true;
        }

        ClientHandler targetHandler = ChatServer.getClientHandler(targetUser);
        if (targetHandler == null) {
            clientHandler.sendMessage("Error: El usuario '" + targetUser + "' no está conectado.");
            return true;
        }

        try {
            // Dirección IP de ambos
            InetAddress senderAddress = clientHandler.getClientSocket().getInetAddress();
            InetAddress targetAddress = targetHandler.getClientSocket().getInetAddress();

            // Notificar a ambos para que establezcan conexión UDP directa
            targetHandler.sendMessage("VOICE_NOTE_INCOMING from " + sender + " " + senderAddress.getHostAddress() + ":" + senderUdpPort);
            clientHandler.sendMessage("VOICE_NOTE_TARGET " + targetUser + " " + targetAddress.getHostAddress() + ":" + targetUdpPort);

            System.out.println("[VoiceNote] " + sender + " → " + targetUser + " (" + targetAddress + ":" + targetUdpPort + ")");
        } catch (Exception e) {
            clientHandler.sendMessage("Error al iniciar la nota de voz: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}
