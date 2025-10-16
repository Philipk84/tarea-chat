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

        if (!members.contains(sender)) {
            senderHandler.sendMessage("Error: No eres miembro del grupo '" + groupName + "'");
            return;
        }

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(senderHandler.getClientSocket().getInputStream()));
            InputStream rawIn = senderHandler.getClientSocket().getInputStream();

            String header = in.readLine(); // VOICE_NOTE_GROUP_START <grupo> <tamaño>
            if (header == null || !header.startsWith("VOICE_NOTE_GROUP_START ")) {
                senderHandler.sendMessage("Error: No se recibió encabezado VOICE_NOTE_GROUP_START");
                return;
            }
            String[] h = header.split(" ");
            if (h.length < 3) {
                senderHandler.sendMessage("Error: Encabezado de grupo inválido");
                return;
            }
            long size = Long.parseLong(h[2]);

            byte[] buffer = new byte[4096];
            long remaining = size;

            // Preparar destinos
            java.util.List<ClientHandler> recipients = new java.util.ArrayList<>();
            for (String m : members) {
                if (!m.equals(sender)) {
                    ClientHandler ch = ChatServer.getClientHandler(m);
                    if (ch != null) recipients.add(ch);
                }
            }
            // Enviar encabezado a todos
            String forwardHeader = "VOICE_NOTE_GROUP_START " + sender + " " + groupName + " " + size + "\n";
            for (ClientHandler ch : recipients) {
                ch.getClientSocket().getOutputStream().write(forwardHeader.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            // Reenviar flujo binario a todos
            while (remaining > 0) {
                int n = rawIn.read(buffer, 0, (int)Math.min(buffer.length, remaining));
                if (n == -1) break;
                for (ClientHandler ch : recipients) {
                    ch.getClientSocket().getOutputStream().write(buffer, 0, n);
                }
                remaining -= n;
            }

            String end = in.readLine();
            if (end == null || !end.equals("VOICE_NOTE_GROUP_END")) {
                senderHandler.sendMessage("Advertencia: no se detectó VOICE_NOTE_GROUP_END correctamente");
            }
            for (ClientHandler ch : recipients) {
                ch.getClientSocket().getOutputStream().write("VOICE_NOTE_GROUP_END\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                ch.getClientSocket().getOutputStream().flush();
            }
            senderHandler.sendMessage("Nota de voz grupal enviada a '" + groupName + "'.");

        } catch (Exception e) {
            senderHandler.sendMessage("Error reenviando nota de voz grupal: " + e.getMessage());
        }
    }
}
