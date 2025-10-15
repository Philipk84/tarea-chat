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

        ClientHandler targetHandler = ChatServer.getClientHandler(targetUser);
        if (targetHandler == null) {
            senderHandler.sendMessage("Error: El usuario '" + targetUser + "' no está conectado");
            return;
        }

        try {
            BufferedReader senderReader = new BufferedReader(new InputStreamReader(senderHandler.getClientSocket().getInputStream()));
            InputStream senderRawIn = senderHandler.getClientSocket().getInputStream();

            PrintWriter targetOut = new PrintWriter(targetHandler.getClientSocket().getOutputStream(), true);
            OutputStream targetRawOut = targetHandler.getClientSocket().getOutputStream();

            // 📥 Esperar encabezado de inicio
            String startLine = senderReader.readLine();
            if (startLine == null || !startLine.startsWith("VOICE_NOTE_START")) {
                senderHandler.sendMessage("Error: No se recibió encabezado de nota de voz válido.");
                return;
            }

            String[] headerParts = startLine.split(" ");
            if (headerParts.length < 3) {
                senderHandler.sendMessage("Error: Encabezado de nota de voz incompleto.");
                return;
            }

            long fileSize = Long.parseLong(headerParts[2]);

            // 📤 Reenviar encabezado al destinatario
            targetOut.println("VOICE_NOTE_START " + sender + " " + fileSize);
            targetOut.flush();

            // 🔄 Transferir bytes directamente
            byte[] buffer = new byte[4096];
            long remaining = fileSize;
            while (remaining > 0) {
                int bytesRead = senderRawIn.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (bytesRead == -1) break;
                targetRawOut.write(buffer, 0, bytesRead);
                remaining -= bytesRead;
            }
            targetRawOut.flush();

            // 📥 Esperar línea de cierre
            String endLine = senderReader.readLine();
            if (endLine == null || !endLine.equals("VOICE_NOTE_END")) {
                senderHandler.sendMessage("Advertencia: No se detectó fin de nota de voz correcto.");
            }

            // 📤 Enviar cierre al receptor
            targetOut.println("VOICE_NOTE_END");
            targetOut.flush();

            senderHandler.sendMessage("✅ Nota de voz enviada correctamente a " + targetUser);
            targetHandler.sendMessage("📩 Nota de voz recibida de " + sender);

        } catch (IOException e) {
            senderHandler.sendMessage("Error enviando nota de voz: " + e.getMessage());
        } catch (NumberFormatException e) {
            senderHandler.sendMessage("Error: Tamaño de archivo inválido.");
        }
    }
}
