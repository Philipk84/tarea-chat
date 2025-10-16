package command;

import interfaces.CommandHandler;
import model.ClientHandler;
import model.ChatServer;

import java.io.*;

/**
 * Manejador del comando /voice que envía una nota de voz por TCP entre dos usuarios.
 *
 * Formato del protocolo:
 * CLIENTE → SERVIDOR:
 *   VOICE_NOTE_START <usuarioDestino> <tamaño>
 *   [bytes del archivo]
 *   VOICE_NOTE_END
 *
 * SERVIDOR → DESTINATARIO:
 *   VOICE_NOTE_START <remitente> <tamaño>
 *   [bytes del archivo]
 *   VOICE_NOTE_END
 */
public class VoiceNoteCommandHandler implements CommandHandler {

    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/voice ");
    }

    @Override
    public void execute(String command, String sender, ClientHandler senderHandler) {
        String[] parts = command.split(" ", 2);
        if (parts.length < 2) {
            senderHandler.sendMessage("Error: Formato correcto -> /voice <usuario>");
            return;
        }

        String targetUser = parts[1].trim();
        if (targetUser.isEmpty()) {
            senderHandler.sendMessage("Error: Debes especificar un nombre de usuario válido");
            return;
        }
        if (targetUser.equals(sender)) {
            senderHandler.sendMessage("Error: No puedes enviarte notas de voz a ti mismo");
            return;
        }
    }
}
