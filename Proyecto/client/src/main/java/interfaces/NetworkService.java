package interfaces;

/**
 * Interfaz para el servicio de comunicación de red del cliente.
 * Define las operaciones de conexión TCP con el servidor y manejo de comandos.
 */
public interface NetworkService {
    /**
     * Establece conexión TCP con el servidor de chat.
     * 
     * @param username Nombre de usuario para la autenticación
     * @return Mensaje de bienvenida del servidor o error
     */
    String connect(String username);
    
    /**
     * Envía un comando al servidor a través de la conexión TCP.
     * 
     * @param command Comando a enviar al servidor
     */
    void sendCommand(String command);
    
    /**
     * Cierra la conexión TCP con el servidor.
     */
    void disconnect();
    
    /**
     * Verifica si existe una conexión activa con el servidor.
     * 
     * @return true si está conectado, false en caso contrario
     */
    boolean isConnected();
    
    /**
     * Establece el manejador de mensajes para procesar respuestas del servidor.
     * 
     * @param messageHandler Manejador que procesará los mensajes entrantes
     */
    void setMessageHandler(MessageHandler messageHandler);
}