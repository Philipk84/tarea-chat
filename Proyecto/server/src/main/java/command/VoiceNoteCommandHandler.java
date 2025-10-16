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

        model.ClientHandler targetHandler = ChatServer.getClientHandler(targetUser);
        if (targetHandler == null) {
            senderHandler.sendMessage("Error: Usuario '" + targetUser + "' no está conectado");
            return;
        }

        // Esperar encabezado VOICE_NOTE_START ... proveniente del cliente
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(senderHandler.getClientSocket().getInputStream()));
            InputStream rawIn = senderHandler.getClientSocket().getInputStream();
            OutputStream rawOut = targetHandler.getClientSocket().getOutputStream();

            String header = in.readLine(); // debería ser VOICE_NOTE_START <destino> <tamaño>
            if (header == null || !header.startsWith("VOICE_NOTE_START ")) {
                senderHandler.sendMessage("Error: No se recibió encabezado VOICE_NOTE_START");
                return;
            }

            String[] h = header.split(" ");
            if (h.length < 3) {
                senderHandler.sendMessage("Error: Encabezado inválido de nota de voz");
                return;
            }
            long size = Long.parseLong(h[2]);

            // Reenviar a destinatario con el nombre del remitente
            String forwardHeader = "VOICE_NOTE_START " + sender + " " + size + "\n";
            rawOut.write(forwardHeader.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            byte[] buffer = new byte[4096];
            long remaining = size;
            while (remaining > 0) {
                int n = rawIn.read(buffer, 0, (int)Math.min(buffer.length, remaining));
                if (n == -1) break;
                rawOut.write(buffer, 0, n);
                remaining -= n;
            }

            // Leer VOICE_NOTE_END del remitente y reenviarlo
            String end = in.readLine();
            if (end == null || !end.equals("VOICE_NOTE_END")) {
                senderHandler.sendMessage("Advertencia: no se detectó VOICE_NOTE_END correctamente");
            }
            rawOut.write("\nVOICE_NOTE_END\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            rawOut.flush();

            senderHandler.sendMessage("Nota de voz enviada a " + targetUser);

        } catch (Exception e) {
            senderHandler.sendMessage("Error reenviando nota de voz: " + e.getMessage());
        }
    }
}
