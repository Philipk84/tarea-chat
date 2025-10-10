package interfaces;

/**
 * Interface que define las operaciones básicas de un servidor
 */
public interface ServerService {

    /**
     * Inicia el servidor.
     * 
     * @return Mensaje de estado
     */
    String startServer();

    /**
     * Cierra el servidor.
     * 
     * @return Mensaje de estado
     */
    String closeServer();

    /**
     * Verifica si el servidor está en ejecución.
     * 
     * @return true si el servidor está en ejecución, false en caso contrario
     */
    boolean isRunning();
}