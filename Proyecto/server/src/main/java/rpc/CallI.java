package rpc;

import Chat.Call;
import Chat.VoiceEntry;
import Chat.VoiceObserverPrx;
import Chat.SessionDescription;
import Chat.Candidate;
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
        System.out.println("[ICE] VoiceObserver suscrito: " + username);
        System.out.println("[ICE]   - Proxy recibido: " + obs);
        System.out.println("[ICE]   - Connection: " + current.con);
        
        try {
            // CRÍTICO: Fijar el proxy a la conexión bidireccional actual
            // Esto es necesario para callbacks en Ice
            VoiceObserverPrx fixedProxy = obs.ice_fixed(current.con);
            System.out.println("[ICE]   - Proxy fijado: " + fixedProxy);
            
            // Agregar callback para limpiar cuando se cierre la conexión
            current.con.setCloseCallback(con -> {
                observers.remove(username);
                System.out.println("[ICE] ⚠ Conexión cerrada, observer removido automáticamente para: " + username);
            });
            
            observers.put(username, fixedProxy);
            System.out.println("[ICE] ✓ Observer almacenado correctamente para " + username);
            
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
            System.out.println("[ICE] Nota de voz guardada: " + saved.relativePath());
            System.out.println("[ICE] Notificando a emisor: " + fromUser);
            notifyUser(fromUser, entry);
            if (!fromUser.equals(toUser)) {
                System.out.println("[ICE] Notificando a receptor: " + toUser);
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

    // ============================================
    // MÉTODOS PARA LLAMADAS CON ICE
    // ============================================

    @Override
    public void initiateCall(String fromUser, String toUser, Current current) {
        System.out.println("[ICE] Llamada iniciada: " + fromUser + " → " + toUser);
        
        VoiceObserverPrx observer = observers.get(toUser);
        if (observer != null) {
            new Thread(() -> {
                try {
                    observer.onCallIncoming(fromUser);
                    System.out.println("[ICE] ✓ Notificación de llamada enviada a " + toUser);
                } catch (Exception e) {
                    System.err.println("[ICE] ✗ Error notificando llamada: " + e.getMessage());
                }
            }, "ICE-Call-Init-" + toUser).start();
        } else {
            System.err.println("[ICE] ✗ Observer no encontrado para: " + toUser);
        }
    }

    @Override
    public void acceptCall(String fromUser, String toUser, Current current) {
        System.out.println("[ICE] Llamada aceptada: " + toUser + " acepta llamada de " + fromUser);
        
        VoiceObserverPrx observer = observers.get(fromUser);
        if (observer != null) {
            new Thread(() -> {
                try {
                    observer.onCallIncoming(toUser); // Reutilizamos para notificar aceptación
                    System.out.println("[ICE] ✓ Aceptación notificada a " + fromUser);
                } catch (Exception e) {
                    System.err.println("[ICE] ✗ Error: " + e.getMessage());
                }
            }, "ICE-Call-Accept-" + fromUser).start();
        }
    }

    @Override
    public void rejectCall(String fromUser, String toUser, Current current) {
        System.out.println("[ICE] Llamada rechazada: " + toUser + " rechaza llamada de " + fromUser);
        
        VoiceObserverPrx observer = observers.get(fromUser);
        if (observer != null) {
            new Thread(() -> {
                try {
                    observer.onCallEnded(toUser);
                    System.out.println("[ICE] ✓ Rechazo notificado a " + fromUser);
                } catch (Exception e) {
                    System.err.println("[ICE] ✗ Error: " + e.getMessage());
                }
            }, "ICE-Call-Reject-" + fromUser).start();
        }
    }

    @Override
    public void endCall(String fromUser, String toUser, Current current) {
        System.out.println("[ICE] Llamada terminada: " + fromUser + " termina llamada con " + toUser);
        
        // Notificar a ambos usuarios
        VoiceObserverPrx observer1 = observers.get(toUser);
        VoiceObserverPrx observer2 = observers.get(fromUser);
        
        if (observer1 != null) {
            new Thread(() -> {
                try {
                    observer1.onCallEnded(fromUser);
                } catch (Exception e) {
                    System.err.println("[ICE] ✗ Error: " + e.getMessage());
                }
            }, "ICE-Call-End-1-" + toUser).start();
        }
        
        if (observer2 != null) {
            new Thread(() -> {
                try {
                    observer2.onCallEnded(toUser);
                } catch (Exception e) {
                    System.err.println("[ICE] ✗ Error: " + e.getMessage());
                }
            }, "ICE-Call-End-2-" + fromUser).start();
        }
    }

    @Override
    public void sendIceOffer(String fromUser, String toUser, SessionDescription offer, Current current) {
        System.out.println("[ICE] Oferta recibida de " + fromUser + " para " + toUser);
        
        VoiceObserverPrx observer = observers.get(toUser);
        if (observer != null) {
            new Thread(() -> {
                try {
                    observer.onIceOffer(fromUser, offer);
                    System.out.println("[ICE] ✓ Oferta reenviada a " + toUser);
                } catch (Exception e) {
                    System.err.println("[ICE] ✗ Error enviando oferta a " + toUser + ": " + e.getMessage());
                }
            }, "ICE-Offer-" + toUser).start();
        } else {
            System.err.println("[ICE] ✗ Observer no encontrado para: " + toUser);
        }
    }

    @Override
    public void sendIceAnswer(String fromUser, String toUser, SessionDescription answer, Current current) {
        System.out.println("[ICE] Respuesta recibida de " + fromUser + " para " + toUser);
        
        VoiceObserverPrx observer = observers.get(toUser);
        if (observer != null) {
            new Thread(() -> {
                try {
                    observer.onIceAnswer(fromUser, answer);
                    System.out.println("[ICE] ✓ Respuesta reenviada a " + toUser);
                } catch (Exception e) {
                    System.err.println("[ICE] ✗ Error enviando respuesta a " + toUser + ": " + e.getMessage());
                }
            }, "ICE-Answer-" + toUser).start();
        } else {
            System.err.println("[ICE] ✗ Observer no encontrado para: " + toUser);
        }
    }

    @Override
    public void sendIceCandidate(String fromUser, String toUser, Candidate candidate, Current current) {
        VoiceObserverPrx observer = observers.get(toUser);
        if (observer != null) {
            new Thread(() -> {
                try {
                    observer.onIceCandidate(fromUser, candidate);
                } catch (Exception e) {
                    System.err.println("[ICE] ✗ Error enviando candidato a " + toUser + ": " + e.getMessage());
                }
            }, "ICE-Candidate-" + toUser).start();
        }
    }
}
