package interfaces;

import java.util.Set;

/**
 * Interface para manejo de usuarios
 */
public interface UserManager {
    
    /** Registra un nuevo usuario en el sistema.
     * 
     * @param name Nombre del usuario a registrar
     * @param handler Manejador del cliente asociado al usuario
     */
    void registerUser(String name, Object handler);

    /**
     * Elimina un usuario del sistema.
     * 
     * @param name Nombre del usuario a eliminar
     */
    void removeUser(String name);
    
    /** Registra la información de conexión UDP del usuario.
     * 
     * @param name Nombre del usuario
     * @param ipPort Información de IP y puerto en formato "IP:PUERTO"
     */
    void registerUdpInfo(String name, String ipPort);
    
    /** Obtiene la información de conexión UDP del usuario.
     * 
     * @param name Nombre del usuario
     * @return Información de IP y puerto en formato "IP:PUERTO" o null si no está registrada
     */
    String getUdpInfo(String name);

    /**
     * Verifica si un usuario está en línea.
     * 
     * @param name Nombre del usuario a verificar
     * @return true si el usuario está en línea, false en caso contrario
     */
    boolean isUserOnline(String name);

    /**
     * Obtiene la lista de todos los usuarios registrados.
     * 
     * @return Conjunto de nombres de usuarios
     */
    Set<String> getUsers();
}