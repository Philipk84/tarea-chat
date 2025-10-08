package interfaces;

/**
 * Interfaz para el manejo de mensajes entrantes del servidor.
 * Define el contrato para procesar diferentes tipos de mensajes recibidos.
 */
public interface MessageHandler {
    /**
     * Procesa un mensaje recibido del servidor.
     * 
     * @param message Mensaje completo recibido del servidor
     */
    void handleMessage(String message);
    
    /**
     * Maneja la notificación de inicio de llamada.
     * 
     * @param callId ID de la llamada iniciada
     * @param participants Lista de participantes con sus datos UDP
     */
    void handleCallStarted(String callId, String participants);
    
    /**
     * Maneja la notificación de finalización de llamada.
     * 
     * @param callId ID de la llamada finalizada
     */
    void handleCallEnded(String callId);
}