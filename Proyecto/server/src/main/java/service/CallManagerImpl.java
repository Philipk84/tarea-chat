package service;

import java.util.*;

import interfaces.CallManager;

/**
 * Gestor de llamadas activas para el sistema de chat.
 * Administra el estado de las llamadas y la relación entre usuarios y llamadas.
 * Solo maneja la señalización; el audio UDP se gestiona directamente entre clientes.
 */
public class CallManagerImpl implements CallManager {

    /**
     * Mapa de llamadas activas: ID de llamada -> conjunto de participantes
     */
    private final Map<String, Set<String>> calls = new HashMap<>();

    /**
     * Mapa de usuarios en llamadas: nombre de usuario -> ID de llamada
     */
    private final Map<String, String> userToCall = new HashMap<>();

    /**
     * Crea una nueva llamada con el ID y conjunto de participantes especificado.
     * 
     * @param callId ID de la llamada
     * @param participants Conjunto de nombres de usuario que participarán en la llamada
     */
    @Override
    public synchronized void createCall(String callId, Set<String> participants) {
        System.out.println("[CallManagerImpl] createCall llamado");
        System.out.println("[CallManagerImpl]   - callId: " + callId);
        System.out.println("[CallManagerImpl]   - participants: " + participants);
        calls.put(callId, new HashSet<>(participants));
        System.out.println("[CallManagerImpl]   - Guardado en Map. Total llamadas: " + calls.size());
        System.out.println("[CallManagerImpl]   - CallIds en Map: " + calls.keySet());
        for (String u : participants) userToCall.put(u, callId);
    }

    /**
     * Termina una llamada activa y libera a todos los participantes.
     * 
     * @param callId ID de la llamada a terminar
     */
    @Override
    public synchronized void endCall(String callId) {
        Set<String> parts = calls.remove(callId);
        if (parts != null) {
            for (String u : parts) userToCall.remove(u);
        }
    }

    /**
     * Obtiene el ID de la llamada en la que participa un usuario.
     * 
     * @param username Nombre del usuario
     * @return ID de la llamada o null si el usuario no está en ninguna llamada
     */
    @Override
    public synchronized String getCallOfUser(String username) {
        return userToCall.get(username);
    }

    /**
     * Obtiene el conjunto de participantes de una llamada específica.
     * 
     * @param callId ID de la llamada
     * @return Conjunto de nombres de usuario participantes o conjunto vacío si la llamada no existe
     */
    @Override
    public synchronized Set<String> getParticipants(String callId) {
        System.out.println("[CallManagerImpl] getParticipants llamado");
        System.out.println("[CallManagerImpl]   - callId buscado: " + callId);
        System.out.println("[CallManagerImpl]   - CallIds disponibles: " + calls.keySet());
        Set<String> result = calls.getOrDefault(callId, Collections.emptySet());
        System.out.println("[CallManagerImpl]   - Resultado: " + result);
        return result;
    }
}
