package rpc;

import Chat.Call;
import Chat.VoiceEntry;
import Chat.VoiceObserverPrx;
import com.zeroc.Ice.Current;
import model.ChatServer;
import service.HistoryService;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CallI implements Call {

    private final ChatServer chatServer;
    // username -> observer proxy
    private final Map<String, VoiceObserverPrx> observers = new ConcurrentHashMap<>();

    public CallI(ChatServer chatServer) {
        this.chatServer = chatServer;
    }

    @Override
    public void subscribe(String username, VoiceObserverPrx obs, Current current) {
        observers.put(username, obs);
        System.out.println("[ICE] VoiceObserver suscrito: " + username);
    }

    @Override
    public void unsubscribe(String username, VoiceObserverPrx obs, Current current) {
        observers.remove(username);
        System.out.println("[ICE] VoiceObserver desuscrito: " + username);
    }

    @Override
    public void sendVoiceNoteToUser(String fromUser, String toUser, byte[] audio, Current current) {
        try {
            // 1) Guardar bytes PCM16 como WAV (como hace audio_rep)
            HistoryService.SavedAudio saved = HistoryService.saveVoiceBytes(audio);

            // 2) Registrar en historial (history.jsonl)
            HistoryService.logVoiceNote(fromUser, toUser, saved.relativePath(), saved.sizeBytes());

            // 3) Armar entrada de voz para el front
            VoiceEntry entry = new VoiceEntry();
            entry.type = "voice_note";
            entry.scope = "private";
            entry.sender = fromUser;
            entry.recipient = toUser;
            entry.group = "";
            entry.audioFile = saved.relativePath();

            // 4) Notificar en tiempo real (igual que SubjectImpl.notifyObs)
            notifyUser(fromUser, entry);
            if (!fromUser.equals(toUser)) {
                notifyUser(toUser, entry);
            }

        } catch (IOException e) {
            System.err.println("[ICE] Error guardando voice note user: " + e.getMessage());
        }
    }

    @Override
    public void sendVoiceNoteToGroup(String fromUser, String groupName, byte[] audio, Current current) {
        try {
            HistoryService.SavedAudio saved = HistoryService.saveVoiceBytes(audio);

            HistoryService.logVoiceGroup(fromUser, groupName, saved.relativePath(), saved.sizeBytes());

            VoiceEntry entry = new VoiceEntry();
            entry.type = "voice_group";
            entry.scope = "group";
            entry.sender = fromUser;
            entry.recipient = "";
            entry.group = groupName;
            entry.audioFile = saved.relativePath();

            // Miembros del grupo desde ChatServer, igual a como ya lo usas
            Set<String> members = ChatServer.getGroupMembers(groupName);
            for (String u : members) {
                notifyUser(u, entry);
            }
            // Asegurar que el emisor también lo vea
            notifyUser(fromUser, entry);

        } catch (IOException e) {
            System.err.println("[ICE] Error guardando voice note group: " + e.getMessage());
        }
    }

    private void notifyUser(String username, VoiceEntry entry) {
        VoiceObserverPrx obs = observers.get(username);
        if (obs != null) {
            try {
                obs.onVoice(entry);
            } catch (Exception e) {
                System.err.println("[ICE] Error notificando a " + username + ": " + e.getMessage());
            }
        }
    }
}
