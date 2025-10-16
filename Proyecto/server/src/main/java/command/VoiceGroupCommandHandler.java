package command;

import interfaces.CommandHandler;
import model.ClientHandler;
import model.ChatServer;

import java.io.*;
import java.util.Set;

/**
 * Manejador del comando /voicegroup que reenvía notas de voz por TCP
 * a todos los miembros de un grupo, usando el protocolo con encabezados.
 *
 * Protocolo:
 * CLIENTE → SERVIDOR:
 *   VOICE_NOTE_GROUP_START <grupo> <tamaño>
 *   [bytes del archivo]
 *   VOICE_NOTE_GROUP_END
 *
 * SERVIDOR → MIEMBROS DEL GRUPO:
 *   VOICE_NOTE_GROUP_START <remitente> <grupo> <tamaño>
 *   [bytes del archivo]
 *   VOICE_NOTE_GROUP_END
 */
public class VoiceGroupCommandHandler implements CommandHandler {

    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/voicegroup ");
    }

    @Override
    public void execute(String command, String sender, ClientHandler senderHandler) {
        String[] parts = command.split(" ", 2);
        if (parts.length < 2) {
            senderHandler.sendMessage("Error: Formato correcto -> /voicegroup <grupo>");
            return;
        }

        String groupName = parts[1].trim();
        if (groupName.isEmpty()) {
            senderHandler.sendMessage("Error: Debes especificar un nombre de grupo válido");
            return;
        }
        
        // Obtener miembros del grupo
        Set<String> members = ChatServer.getGroupMembers(groupName);
        if (members == null || members.isEmpty()) {
            senderHandler.sendMessage("Error: El grupo '" + groupName + "' no existe o está vacío");
            return;
        }
    }
}
