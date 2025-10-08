package model;

import java.util.*;

/**
 * Gestor de llamadas activas para el sistema de chat.
 * Administra el estado de las llamadas y la relación entre usuarios y llamadas.
 * Solo maneja la señalización; el audio UDP se gestiona directamente entre clientes.
 */
public class CallManager {

    /**
     * Mapa de llamadas activas: ID de llamada -> conjunto de participantes
     */
    private final Map<String, Set<String>> calls = new HashMap<>();

    /**
     * Mapa de usuarios en llamadas: nombre de usuario -> ID de llamada
     */
    private final Map<String, String> userToCall = new HashMap<>();

    /**
     * Crea una nueva llamada con el conjunto de participantes especificado.
     * 
     * @param participants Conjunto de nombres de usuario que participarán en la llamada
     * @return ID único de la llamada creada
     */
    public synchronized String createCall(Set<String> participants) {
        String callId = UUID.randomUUID().toString();
        calls.put(callId, new HashSet<>(participants));
        for (String u : participants) userToCall.put(u, callId);
        return callId;
    }

    /**
     * Termina una llamada activa y libera a todos los participantes.
     * 
     * @param callId ID de la llamada a terminar
     */
    public synchronized void endCall(String callId) {
        Set<String> parts = calls.remove(callId);
        if (parts != null) {
            for (String u : parts) userToCall.remove(u);
        }
    }

    /**
     * Verifica si un usuario está actualmente en una llamada.
     * 
     * @param username Nombre del usuario a verificar
     * @return true si el usuario está en una llamada, false en caso contrario
     */
    public synchronized boolean isInCall(String username) {
        return userToCall.containsKey(username);
    }

    /**
     * Obtiene el ID de la llamada en la que participa un usuario.
     * 
     * @param username Nombre del usuario
     * @return ID de la llamada o null si el usuario no está en ninguna llamada
     */
    public synchronized String getCallOfUser(String username) {
        return userToCall.get(username);
    }

    /**
     * Obtiene el conjunto de participantes de una llamada específica.
     * 
     * @param callId ID de la llamada
     * @return Conjunto de nombres de usuario participantes o conjunto vacío si la llamada no existe
     */
    public synchronized Set<String> getParticipants(String callId) {
        return calls.getOrDefault(callId, Collections.emptySet());
    }
}
