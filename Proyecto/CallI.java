// EJEMPLO: Métodos ICE para agregar a server/src/main/java/rpc/CallI.java
// Agrega estos métodos a la clase CallI existente

package rpc;

import Chat.*;
import com.zeroc.Ice.Current;
import model.ChatServer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CallI implements Call {
    
    private final ChatServer chatServer;
    private final Map<String, VoiceObserverPrx> observers = new ConcurrentHashMap<>();
    
    // ... código existente ...
    
    // ============================================
    // NUEVOS MÉTODOS PARA ICE
    // ============================================
    
    /**
     * Reenvía una oferta ICE de un usuario a otro.
     */
    @Override
    public void sendIceOffer(String fromUser, String toUser, SessionDescription offer, Current current) {
        System.out.println("[ICE] Oferta recibida de " + fromUser + " para " + toUser);
        
        VoiceObserverPrx observer = observers.get(toUser);
        if (observer != null) {
            try {
                observer.onIceOffer(fromUser, offer);
                System.out.println("[ICE] ✓ Oferta reenviada a " + toUser);
            } catch (Exception e) {
                System.err.println("[ICE] ✗ Error enviando oferta a " + toUser + ": " + e.getMessage());
            }
        } else {
            System.err.println("[ICE] ✗ Observer no encontrado para: " + toUser);
        }
    }
    
    /**
     * Reenvía una respuesta ICE de un usuario a otro.
     */
    @Override
    public void sendIceAnswer(String fromUser, String toUser, SessionDescription answer, Current current) {
        System.out.println("[ICE] Respuesta recibida de " + fromUser + " para " + toUser);
        
        VoiceObserverPrx observer = observers.get(toUser);
        if (observer != null) {
            try {
                observer.onIceAnswer(fromUser, answer);
                System.out.println("[ICE] ✓ Respuesta reenviada a " + toUser);
            } catch (Exception e) {
                System.err.println("[ICE] ✗ Error enviando respuesta a " + toUser + ": " + e.getMessage());
            }
        } else {
            System.err.println("[ICE] ✗ Observer no encontrado para: " + toUser);
        }
    }
    
    /**
     * Reenvía un candidato ICE de un usuario a otro.
     */
    @Override
    public void sendIceCandidate(String fromUser, String toUser, IceCandidate candidate, Current current) {
        VoiceObserverPrx observer = observers.get(toUser);
        if (observer != null) {
            try {
                observer.onIceCandidate(fromUser, candidate);
            } catch (Exception e) {
                System.err.println("[ICE] ✗ Error enviando candidato a " + toUser + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Reenvía una oferta ICE a un grupo (implementación básica).
     */
    @Override
    public void sendIceOfferToGroup(String fromUser, String groupName, SessionDescription offer, Current current) {
        System.out.println("[ICE] Oferta grupal de " + fromUser + " para grupo " + groupName);
        // TODO: Implementar lógica para grupos
        // 1. Obtener lista de miembros del grupo
        // 2. Reenviar oferta a cada miembro (excepto el remitente)
    }
    
    /**
     * Reenvía un candidato ICE a un grupo (implementación básica).
     */
    @Override
    public void sendIceCandidateToGroup(String fromUser, String groupName, IceCandidate candidate, Current current) {
        System.out.println("[ICE] Candidato grupal de " + fromUser + " para grupo " + groupName);
        // TODO: Implementar lógica para grupos
    }
}

