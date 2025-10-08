package interfaces;

/**
 * Interface que define las operaciones básicas de un servidor
 */
public interface ServerService {
    String startServer();
    String closeServer();
    boolean isRunning();
}