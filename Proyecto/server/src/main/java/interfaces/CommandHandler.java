package interfaces;

/**
 * Interfaz para el manejo de comandos en el sistema de chat.
 * Define el contrato para procesar diferentes tipos de comandos de usuario.
 */
public interface CommandHandler {
    /**
     * Determina si este manejador puede procesar el comando especificado.
     * 
     * @param command Comando a evaluar
     * @return true si este manejador puede procesar el comando, false en caso contrario
     */
    boolean canHandle(String command);
    
    /**
     * Ejecuta el comando especificado para el usuario dado.
     * 
     * @param command Comando completo a ejecutar
     * @param userName Nombre del usuario que ejecuta el comando
     * @param clientHandler Manejador del cliente que envió el comando
     */
    void execute(String command, String userName, Object clientHandler);
}