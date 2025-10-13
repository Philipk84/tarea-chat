package interfaces;

import model.ClientHandler;

/**
 * Interfaz para la gestión de usuarios en el servidor de chat.
 * Define las operaciones básicas para registrar, eliminar y consultar usuarios.
 */
public interface UserManager {

    /**
     * Registra un nuevo usuario en el servidor.
     *
     * @param username nombre del usuario.
     * @param handler instancia de ClientHandler asociada.
     */
    void registerUser(String username, ClientHandler handler);

    /**
     * Elimina un usuario del servidor.
     *
     * @param username nombre del usuario a eliminar.
     */
    void removeUser(String username);

    /**
     * Verifica si un usuario está en línea.
     *
     * @param username nombre del usuario.
     * @return true si el usuario está conectado, false si no.
     */
    boolean isUserOnline(String username);

    /**
     * Obtiene el manejador de cliente asociado a un usuario.
     *
     * @param username nombre del usuario.
     * @return instancia de ClientHandler o null si no está conectado.
     */
    ClientHandler getClientHandler(String username);

    /**
     * Registra la información UDP (IP:puerto) de un usuario.
     *
     * @param username nombre del usuario.
     * @param ipPort información en formato "IP:puerto".
     */
    void registerUdpInfo(String username, String ipPort);

    /**
     * Obtiene la información UDP de un usuario.
     *
     * @param username nombre del usuario.
     * @return información en formato "IP:puerto", o null si no existe.
     */
    String getUdpInfo(String username);
}
