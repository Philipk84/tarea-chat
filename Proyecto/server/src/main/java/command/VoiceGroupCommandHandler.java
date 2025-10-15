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

        Set<String> members = ChatServer.getGroupMembers(groupName);
        if (members == null || members.isEmpty()) {
            senderHandler.sendMessage("Error: El grupo '" + groupName + "' no existe o está vacío");
            return;
        }

        if (!members.contains(sender)) {
            senderHandler.sendMessage("Error: No eres miembro del grupo '" + groupName + "'");
            return;
        }

        try {
            BufferedReader senderReader = new BufferedReader(new InputStreamReader(senderHandler.getClientSocket().getInputStream()));
            InputStream senderRawIn = senderHandler.getClientSocket().getInputStream();

            // 📥 Esperar encabezado VOICE_NOTE_GROUP_START
            String startLine = senderReader.readLine();
            if (startLine == null || !startLine.startsWith("VOICE_NOTE_GROUP_START")) {
                senderHandler.sendMessage("Error: No se recibió encabezado válido de nota de voz grupal.");
                return;
            }

            String[] headerParts = startLine.split(" ");
            if (headerParts.length < 3) {
                senderHandler.sendMessage("Error: Encabezado de grupo incompleto.");
                return;
            }

            long fileSize = Long.parseLong(headerParts[2]);

            // 📢 Reenviar a todos los miembros del grupo excepto el remitente
            for (String member : members) {
                if (member.equals(sender)) continue;

                ClientHandler memberHandler = ChatServer.getClientHandler(member);
                if (memberHandler != null) {
                    PrintWriter out = new PrintWriter(memberHandler.getClientSocket().getOutputStream(), true);
                    OutputStream rawOut = memberHandler.getClientSocket().getOutputStream();

                    // Encabezado de inicio hacia el miembro
                    out.println("VOICE_NOTE_GROUP_START " + sender + " " + groupName + " " + fileSize);
                    out.flush();
                }
            }

            // 🔄 Transferencia binaria del archivo a todos los miembros
            byte[] buffer = new byte[4096];
            long remaining = fileSize;
            while (remaining > 0) {
                int bytesRead = senderRawIn.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (bytesRead == -1) break;

                for (String member : members) {
                    if (member.equals(sender)) continue;
                    ClientHandler memberHandler = ChatServer.getClientHandler(member);
                    if (memberHandler != null) {
                        OutputStream memberOut = memberHandler.getClientSocket().getOutputStream();
                        memberOut.write(buffer, 0, bytesRead);
                    }
                }
                remaining -= bytesRead;
            }

            // Flush final
            for (String member : members) {
                if (member.equals(sender)) continue;
                ClientHandler memberHandler = ChatServer.getClientHandler(member);
                if (memberHandler != null) {
                    memberHandler.getClientSocket().getOutputStream().flush();

                    // Señal de fin
                    PrintWriter out = new PrintWriter(memberHandler.getClientSocket().getOutputStream(), true);
                    out.println("VOICE_NOTE_GROUP_END");
                    out.flush();

                    memberHandler.sendMessage("📥 Nota de voz grupal recibida de " + sender + " en " + groupName);
                }
            }

            // 📤 Confirmación al remitente
            senderHandler.sendMessage("✅ Nota de voz grupal enviada correctamente al grupo '" + groupName + "'");

        } catch (IOException e) {
            senderHandler.sendMessage("Error al enviar nota de voz grupal: " + e.getMessage());
        } catch (NumberFormatException e) {
            senderHandler.sendMessage("Error: Tamaño de archivo inválido.");
        }
    }
}
