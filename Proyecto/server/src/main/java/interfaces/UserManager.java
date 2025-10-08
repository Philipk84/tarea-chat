package interfaces;

/**
 * Interface para manejo de usuarios
 */
public interface UserManager {
    void registerUser(String name, Object handler);
    void removeUser(String name);
    void registerUdpInfo(String name, String ipPort);
    String getUdpInfo(String name);
    boolean isUserOnline(String name);
}