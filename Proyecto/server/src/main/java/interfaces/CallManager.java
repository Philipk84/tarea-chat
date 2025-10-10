package interfaces;

import java.util.Set;

/**
 * Interface para manejo de llamadas
 */
public interface CallManager {
    /**
     * Crea una nueva llamada con los participantes especificados.
     * 
     * @param participants Conjunto de nombres de usuario que participarán en la llamada
     * @return ID único de la llamada creada
     */
    String createCall(Set<String> participants);

    /**
     * Termina una llamada activa y libera a todos los participantes.
     * 
     * @param callId ID de la llamada a terminar
     */
    void endCall(String callId);

    /**
     * Verifica si un usuario está actualmente en una llamada.
     * 
     * @param username Nombre del usuario a verificar
     * @return true si el usuario está en una llamada, false en caso contrario
     */
    boolean isInCall(String username);

    /**
     * Obtiene el ID de la llamada en la que participa un usuario.
     * 
     * @param username Nombre del usuario
     * @return ID de la llamada o null si el usuario no está en ninguna llamada
     */
    String getCallOfUser(String username);

    /**
     * Obtiene el conjunto de participantes en una llamada específica.
     * 
     * @param callId ID de la llamada
     * @return Conjunto de nombres de usuario participantes o conjunto vacío si la llamada no existe
     */
    Set<String> getParticipants(String callId);
}
