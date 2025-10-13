package service;

import interfaces.UserManager;
import model.ClientHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación concreta del gestor de usuarios para el sistema de chat.
 * Administra el registro, autenticación y estado de los usuarios conectados.
 */
public class UserManagerImpl implements UserManager {
    /**
     * Mapa de usuarios conectados: nombre de usuario -> manejador de cliente
     */
    private final Map<String, ClientHandler> users = new ConcurrentHashMap<>();

    /**
     * Mapa de información UDP: nombre de usuario -> dirección IP y puerto UDP
     */
    private final Map<String, String> udpInfo = new ConcurrentHashMap<>();

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * @param name Nombre del usuario a registrar
     * @param handler Manejador del cliente asociado al usuario
     */
    @Override
    public void registerUser(String name, ClientHandler handler) {
        if (handler instanceof ClientHandler) {
            users.put(name, (ClientHandler) handler);
            System.out.println("Usuario registrado: " + name);
        }
    }

    /**
     * Remueve un usuario del sistema y limpia su información asociada.
     *
     * @param name Nombre del usuario a remover
     */
    @Override
    public void removeUser(String name) {
        users.remove(name);
        udpInfo.remove(name);
        System.out.println("Usuario removido: " + name);
    }

    /**
     * Registra la información UDP de un usuario para comunicación de audio.
     *
     * @param name Nombre del usuario
     * @param ipPort Cadena con formato "IP:Puerto" para comunicación UDP
     */
    @Override
    public void registerUdpInfo(String name, String ipPort) {
        udpInfo.put(name, ipPort);
        System.out.println("UDP registrado: " + name + " -> " + ipPort);
    }

    /**
     * Obtiene la información UDP de un usuario.
     *
     * @param name Nombre del usuario
     * @return Cadena con formato "IP:Puerto" o null si no existe información UDP
     */
    @Override
    public String getUdpInfo(String name) {
        return udpInfo.get(name);
    }

    /**
     * Verifica si un usuario está actualmente conectado.
     *
     * @param name Nombre del usuario a verificar
     * @return true si el usuario está en línea, false en caso contrario
     */
    @Override
    public boolean isUserOnline(String name) {
        return users.containsKey(name);
    }

    /**
     * Obtiene el manejador de cliente para un usuario específico.
     * Este método es específico de la implementación para permitir acceso directo.
     *
     * @param name Nombre del usuario
     * @return Manejador del cliente o null si el usuario no está conectado
     */
    @Override
    public ClientHandler getClientHandler(String username) {
        return users.get(username); // Asumiendo que tienes un Map<String, ClientHandler>
    }

}