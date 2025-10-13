package command;

import interfaces.CommandHandler;
import model.ChatServer;
import model.ClientHandler;
import model.VoiceNote;

public class VoiceNoteCommandHandler implements CommandHandler {

    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/voice");
    }

    @Override
    public void execute(String command, String sender, ClientHandler clientHandler) {
        String[] args = command.trim().split("\\s+");
        if (args.length < 2) {
            clientHandler.sendMessage("Uso correcto: /voice <usuario>");
            return;
        }

        String targetUser = args[1].trim();
        if (targetUser.equals(sender)) {
            clientHandler.sendMessage("❌ No puedes enviarte una nota de voz a ti mismo.");
            return;
        }

        if (ChatServer.getClientHandler(targetUser) == null) {
            clientHandler.sendMessage("❌ El usuario '" + targetUser + "' no está conectado.");
            return;
        }

        try {
            clientHandler.sendMessage("🎤 Grabando nota de voz para " + targetUser + "...");

            byte[] fakeAudio = "AUDIO_DATA".getBytes(); // Simulación temporal

            VoiceNote note = new VoiceNote(sender, targetUser, fakeAudio, false);
            ChatServer.forwardVoiceNote(note);

            clientHandler.sendMessage("✅ Nota de voz enviada a " + targetUser + ".");
            System.out.println("[VoiceNote] " + sender + " → " + targetUser);
        } catch (Exception e) {
            clientHandler.sendMessage("Error al enviar nota de voz: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
