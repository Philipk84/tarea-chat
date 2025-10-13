package command;

import interfaces.CommandHandler;
import model.ChatServer;
import model.ClientHandler;
import model.VoiceNote;
import java.util.Set;

public class VoiceGroupCommandHandler implements CommandHandler {

    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/voicegroup");
    }

    @Override
    public void execute(String command, String sender, ClientHandler clientHandler) {
        String[] args = command.trim().split("\\s+");
        if (args.length < 2) {
            clientHandler.sendMessage("Uso correcto: /voicegroup <nombre_grupo>");
            return;
        }

        String groupName = args[1].trim();
        Set<String> members = ChatServer.getGroupMembers(groupName);

        if (members == null || members.isEmpty()) {
            clientHandler.sendMessage("❌ El grupo '" + groupName + "' no existe o está vacío.");
            return;
        }

        try {
            clientHandler.sendMessage("🎤 Grabando nota de voz para el grupo '" + groupName + "'...");

            byte[] fakeAudio = "AUDIO_GROUP_DATA".getBytes(); // Simulación temporal

            VoiceNote note = new VoiceNote(sender, groupName, fakeAudio, true);
            ChatServer.forwardVoiceNote(note);

            clientHandler.sendMessage("✅ Nota de voz enviada al grupo '" + groupName + "'.");
            System.out.println("[VoiceGroup] " + sender + " → grupo " + groupName);
        } catch (Exception e) {
            clientHandler.sendMessage("Error al enviar nota de voz grupal: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
