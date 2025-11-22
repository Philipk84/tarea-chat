package rpc;

import Chat.Call;
import Chat.VoiceEntry;
import Chat.VoiceObserverPrx;
import model.ChatServer;
import service.HistoryService;

import com.zeroc.Ice.Current;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CallI implements Call {

    private final ChatServer chatServer;

    // username -> callback registrado
    private final Map<String, VoiceObserverPrx> observers = new ConcurrentHashMap<>();

    public CallI(ChatServer chatServer) {
        this.chatServer = chatServer;
    }

    @Override
    public void subscribe(String username, VoiceObserverPrx obs, Current current) {
        observers.put(username, obs);
        System.out.println("[ICE] Usuario suscrito a voz: " + username);
    }

    @Override
    public void unsubscribe(String username, VoiceObserverPrx obs, Current current) {
        observers.remove(username);
        System.out.println("[ICE] Usuario desuscrito de voz: " + username);
    }

    @Override
    public void sendVoiceNoteToUser(String fromUser, String toUser, byte[] audio, Current current) {
        try {
            // 1) Guardar WAV usando tu HistoryService
            HistoryService.SavedAudio saved = HistoryService.saveVoiceBytes(audio);

            // 2) Registrar en el historial (para /history y Chat.js)
            HistoryService.logVoiceNote(fromUser, toUser, saved.relativePath(), saved.sizeBytes());

            // 3) Construir entrada compatible con appendHistoryItem
            VoiceEntry entry = new VoiceEntry();
            entry.type = "voice_note";
            entry.scope = "private";
            entry.sender = fromUser;
            entry.recipient = toUser;
            entry.group = "";
            entry.audioFile = saved.relativePath();

            // 4) Notificar en tiempo real al emisor y receptor (si están suscritos)
            notifyUser(fromUser, entry);
            if (!fromUser.equals(toUser)) {
                notifyUser(toUser, entry);
            }

        } catch (IOException e) {
            System.err.println("[ICE] Error guardando nota de voz: " + e.getMessage());
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

            // Tomamos los miembros del grupo desde tu ChatServer
            Set<String> members = chatServer.getGroupManager().getGroupMembers(groupName);

            // Notificar a todos los suscritos que pertenezcan al grupo
            for (String u : members) {
                notifyUser(u, entry);
            }

            // Aseguramos que el emisor también vea su propia nota
            notifyUser(fromUser, entry);

        } catch (IOException e) {
            System.err.println("[ICE] Error guardando nota de voz de grupo: " + e.getMessage());
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
