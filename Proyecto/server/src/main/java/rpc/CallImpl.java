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
import Chat.CallChunk;
import Chat.CallEvent;
import java.util.UUID;


public class CallImpl implements Call {
    // username -> observer proxy
    private final Map<String, VoiceObserverPrx> observers = new ConcurrentHashMap<>();
    // callId -> Set<participants>
    private final Map<String, Set<String>> activeCalls = new ConcurrentHashMap<>();

    @Override
    public void subscribe(String username, VoiceObserverPrx obs, Current current) {
        
        try {
            // CRÍTICO: Fijar el proxy a la conexión bidireccional actual
            // Esto es necesario para callbacks en Ice
            VoiceObserverPrx fixedProxy = obs.ice_fixed(current.con);

            // Agregar callback para limpiar cuando se cierre la conexión
            current.con.setCloseCallback(con -> {
                observers.remove(username);
            });
            
            observers.put(username, fixedProxy);
            
        } catch (Exception e) {
            System.err.println("[ICE] ✗ Error fijando proxy: " + e.getMessage());
            e.printStackTrace();
            // Intentar con el proxy original como fallback
            observers.put(username, obs);
        }
    }

    @Override
    public void unsubscribe(String username, VoiceObserverPrx obs, Current current) {
        observers.remove(username);
        System.out.println("[ICE] VoiceObserver desuscrito: " + username);
    }

    @Override
    public void sendVoiceNoteToUser(String fromUser, String toUser, byte[] audio, Current current) {
        try {
            // 1) Guardar bytes PCM16 como WAV
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

            // 4) Notificar en tiempo real
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
            System.out.println("[ICE] Nota de voz a grupo guardada: " + saved.relativePath());
            System.out.println("[ICE] Notificando a " + members.size() + " miembros del grupo: " + groupName);
            for (String u : members) {
                notifyUser(u, entry);
            }
            // Asegurar que el emisor también lo vea
            if (!members.contains(fromUser)) {
                System.out.println("[ICE] Notificando también al emisor: " + fromUser);
                notifyUser(fromUser, entry);
            }

        } catch (IOException e) {
            System.err.println("[ICE] Error guardando voice note group: " + e.getMessage());
        }
    }

    private void notifyUser(String username, VoiceEntry entry) {
        VoiceObserverPrx obs = observers.get(username);
        if (obs != null) {
            // Ejecutar la notificación en un thread separado para no bloquear
            new Thread(() -> {
                try {
                    System.out.println("[ICE] Intentando notificar a " + username);
                    System.out.println("[ICE]   - type: " + entry.type);
                    System.out.println("[ICE]   - sender: " + entry.sender);
                    System.out.println("[ICE]   - recipient: " + entry.recipient);
                    System.out.println("[ICE]   - audioFile: " + entry.audioFile);
                    
                    // Verificar si la conexión está activa
                    try {
                        obs.ice_getConnection();
                        System.out.println("[ICE] Conexión verificada para " + username);
                    } catch (Exception connEx) {
                        System.err.println("[ICE] Conexión perdida para " + username + ": " + connEx.getMessage());
                        observers.remove(username);
                        return;
                    }
                    
                    // Intentar enviar la notificación
                    obs.onVoice(entry);
                    System.out.println("[ICE] ✓ Notificación enviada exitosamente a: " + username);
                    
                } catch (com.zeroc.Ice.CloseConnectionException e) {
                    System.out.println("[ICE] ⚠ Conexión cerrada para " + username + " (usuario desconectado)");
                    observers.remove(username);
                } catch (com.zeroc.Ice.ConnectionLostException e) {
                    System.out.println("[ICE] ⚠ Conexión perdida con " + username + " (se reconectará automáticamente)");
                    observers.remove(username);
                } catch (Exception e) {
                    System.err.println("[ICE] ✗ Error notificando a " + username);
                    System.err.println("[ICE]   - Clase: " + e.getClass().getName());
                    System.err.println("[ICE]   - Mensaje: " + e.getMessage());
                    
                    // Remover observer inválido
                    observers.remove(username);
                    System.err.println("[ICE] Observer removido para " + username);
                }
            }, "ICE-Notify-" + username).start();
        } else {
            System.out.println("[ICE] ⚠ Usuario " + username + " no tiene observer suscrito");
        }
    }

    @Override
    public void sendCallChunk(String callId, String fromUser, byte[] audio, Current current) {
        // 1) Obtener el CallManager y los participantes de la llamada
        var callManager = ChatServer.getCallManagerImpl();
        if (callManager == null) {
            System.out.println("[ICE]   - No hay CallManager activo, se ignora chunk");
            return;
        }

        Set<String> participants = callManager.getParticipants(callId);
        
        if (participants == null || participants.isEmpty()) {
            // Solo logear la primera vez que falla
            if (!activeCalls.containsKey(callId)) {
                System.out.println("[ICE CHUNK ERROR] No hay participantes para la llamada " + callId);
                System.out.println("[ICE CHUNK ERROR]   - activeCalls contiene: " + activeCalls.keySet());
            }
            return;
        }

        // 2) Construir el CallChunk que se enviará a los demás
        CallChunk chunk = new CallChunk();
        chunk.callId = callId;
        chunk.fromUser = fromUser;
        chunk.audio = audio;

        // 3) Reenviar a todos los participantes excepto al emisor
        for (String user : participants) {
            if (user == null || user.equals(fromUser)) {
                continue;
            }

            VoiceObserverPrx obs = observers.get(user);
            if (obs == null) {
                System.out.println("[ICE]   - Usuario " + user + " no tiene observer suscrito (no se envía chunk)");
                continue;
            }

            final VoiceObserverPrx targetObs = obs;
            final String targetUser = user;

            // Enviar en un hilo separado para no bloquear la llamada Ice
            new Thread(() -> {
                try {
                    // Verificar conexión activa
                    try {
                        targetObs.ice_getConnection();
                    } catch (Exception connEx) {
                        System.err.println("[ICE] Conexión perdida para " + targetUser + " (call chunk): " + connEx.getMessage());
                        observers.remove(targetUser);
                        return;
                    }

                    // Enviar el chunk de audio
                    targetObs.onCallChunk(chunk);
                    System.out.println("[ICE] ✓ Chunk de llamada enviado a " + targetUser + " (callId=" + callId + ", bytes=" + (audio != null ? audio.length : 0) + ")");

                } catch (com.zeroc.Ice.CloseConnectionException e) {
                    System.out.println("[ICE] ⚠ Conexión cerrada para " + targetUser + " (call chunk)");
                    observers.remove(targetUser);
                } catch (com.zeroc.Ice.ConnectionLostException e) {
                    System.out.println("[ICE] ⚠ Conexión perdida con " + targetUser + " (call chunk)");
                    observers.remove(targetUser);
                } catch (Exception e) {
                    System.err.println("[ICE] ✗ Error enviando call chunk a " + targetUser);
                    System.err.println("[ICE]   - Clase: " + e.getClass().getName());
                    System.err.println("[ICE]   - Mensaje: " + e.getMessage());
                    observers.remove(targetUser);
                }
            }, "ICE-CallChunk-" + targetUser).start();
        }
    }

    @Override
    public String startCall(String caller, String callee, Current current) {
        String callId = UUID.randomUUID().toString();
        System.out.println("[ICE CALL] Iniciando llamada privada");
        System.out.println("[ICE CALL]   - callId: " + callId);
        System.out.println("[ICE CALL]   - caller: " + caller);
        System.out.println("[ICE CALL]   - callee: " + callee);

        // Crear set de participantes
        Set<String> participants = ConcurrentHashMap.newKeySet();
        participants.add(caller);
        participants.add(callee);
        activeCalls.put(callId, participants);

        // Registrar en el CallManager del ChatServer
        var callManager = ChatServer.getCallManagerImpl();
        if (callManager != null) {
            System.out.println("[ICE CALL]   - Registrando en CallManager...");
            callManager.createCall(callId, participants);
            System.out.println("[ICE CALL]   - ✓ Registrado en CallManager");
        } else {
            System.err.println("[ICE CALL]   - ✗ CallManager es null!");
        }

        // Registrar en historial
        HistoryService.logCallStarted(callId, participants);

        // Notificar evento de llamada entrante al receptor
        notifyCallEvent(callee, "call_incoming", callId, caller, callee, "", "private");
        
        // Notificar al emisor que la llamada está iniciada
        notifyCallEvent(caller, "call_started", callId, caller, callee, "", "private");

        System.out.println("[ICE CALL] ✓ Llamada creada: " + callId);
        System.out.println("[ICE CALL]   - Participantes: " + participants);
        return callId;
    }

    @Override
    public String startGroupCall(String caller, String groupName, Current current) {
        String callId = UUID.randomUUID().toString();
        System.out.println("[ICE CALL] Iniciando llamada grupal");
        System.out.println("[ICE CALL]   - callId: " + callId);
        System.out.println("[ICE CALL]   - caller: " + caller);
        System.out.println("[ICE CALL]   - group: " + groupName);

        // Obtener miembros del grupo
        Set<String> members = ChatServer.getGroupMembers(groupName);
        if (members == null || members.isEmpty()) {
            System.err.println("[ICE CALL] ✗ Grupo no encontrado o sin miembros: " + groupName);
            return "";
        }

        // Agregar al caller si no está en el grupo
        Set<String> participants = ConcurrentHashMap.newKeySet();
        participants.addAll(members);
        participants.add(caller);
        activeCalls.put(callId, participants);

        // Registrar en el CallManager
        var callManager = ChatServer.getCallManagerImpl();
        if (callManager != null) {
            callManager.createCall(callId, participants);
        }

        // Registrar en historial
        HistoryService.logCallStarted(callId, participants);

        // Notificar a todos los miembros del grupo
        for (String member : participants) {
            if (member.equals(caller)) {
                notifyCallEvent(member, "call_started", callId, caller, "", groupName, "group");
            } else {
                notifyCallEvent(member, "call_incoming", callId, caller, "", groupName, "group");
            }
        }

        System.out.println("[ICE CALL] ✓ Llamada grupal creada: " + callId);
        return callId;
    }

    @Override
    public void acceptCall(String callId, String user, Current current) {
        System.out.println("[ICE CALL] Usuario " + user + " aceptó llamada: " + callId);
        
        Set<String> participants = activeCalls.get(callId);
        if (participants == null) {
            System.err.println("[ICE CALL] ✗ Llamada no encontrada: " + callId);
            return;
        }

        // Agregar al usuario que acepta a los participantes activos
        participants.add(user);
        System.out.println("[ICE CALL] ✓ Usuario " + user + " agregado a la llamada");
        System.out.println("[ICE CALL]   - Participantes actuales: " + participants);

        // Notificar a todos los participantes que el usuario aceptó
        for (String participant : participants) {
            notifyCallEvent(participant, "call_accepted", callId, user, "", "", "private");
        }
    }

    @Override
    public void rejectCall(String callId, String user, Current current) {
        System.out.println("[ICE CALL] Usuario " + user + " rechazó llamada: " + callId);
        
        Set<String> participants = activeCalls.get(callId);
        if (participants == null) {
            System.err.println("[ICE CALL] ✗ Llamada no encontrada: " + callId);
            return;
        }

        // Notificar a todos los participantes que el usuario rechazó
        for (String participant : participants) {
            notifyCallEvent(participant, "call_rejected", callId, user, "", "", "private");
        }

        // Terminar la llamada
        endCall(callId, user, current);
    }

    @Override
    public void endCall(String callId, String user, Current current) {
        System.out.println("[ICE CALL] Terminando llamada: " + callId + " por " + user);
        
        Set<String> participants = activeCalls.remove(callId);
        if (participants == null) {
            System.err.println("[ICE CALL] ✗ Llamada no encontrada: " + callId);
            return;
        }

        // Registrar en historial
        HistoryService.logCallEnded(callId, participants, user);

        // Notificar a todos los participantes que la llamada terminó
        for (String participant : participants) {
            notifyCallEvent(participant, "call_ended", callId, user, "", "", "private");
        }

        // Limpiar del CallManager
        var callManager = ChatServer.getCallManagerImpl();
        if (callManager != null) {
            callManager.endCall(callId);
        }

        System.out.println("[ICE CALL] ✓ Llamada terminada: " + callId);
    }

    private void notifyCallEvent(String username, String type, String callId, String caller, String callee, String group, String scope) {
        VoiceObserverPrx obs = observers.get(username);
        if (obs != null) {
            new Thread(() -> {
                try {
                    // Verificar conexión
                    try {
                        obs.ice_getConnection();
                    } catch (Exception connEx) {
                        System.err.println("[ICE CALL] Conexión perdida para " + username);
                        observers.remove(username);
                        return;
                    }

                    // Crear evento
                    CallEvent event = new CallEvent();
                    event.type = type;
                    event.callId = callId;
                    event.caller = caller;
                    event.callee = callee;
                    event.group = group;
                    event.scope = scope;

                    // Enviar evento
                    obs.onCallEvent(event);
                    System.out.println("[ICE CALL] ✓ Evento enviado a " + username + ": " + type);

                } catch (Exception e) {
                    System.err.println("[ICE CALL] ✗ Error enviando evento a " + username + ": " + e.getMessage());
                    observers.remove(username);
                }
            }, "ICE-CallEvent-" + username).start();
        } else {
            System.out.println("[ICE CALL] ⚠ Usuario " + username + " no tiene observer suscrito");
        }
    }

}
