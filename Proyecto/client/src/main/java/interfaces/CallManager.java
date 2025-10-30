package interfaces;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Interfaz para la gestión de llamadas del cliente.
 * Define las operaciones para iniciar, manejar y finalizar llamadas de audio.
 */
public interface CallManager {
    /**
     * Inicia una nueva llamada con los participantes especificados.
     *
     * @param callId ID único de la llamada
     * @param peers  Lista de direcciones UDP de los participantes
     */
    void startCall(String callId, List<InetSocketAddress> peers);
    
    /**
     * Finaliza la llamada activa actual.
     */
    void endCall();
    
    /**
     * Verifica si hay una llamada activa en curso.
     * 
     * @return true si hay una llamada activa, false en caso contrario
     */
    boolean hasActiveCall();
    
    /**
     * Establece el servicio de audio para las llamadas.
     * 
     * @param audioService Servicio que manejará el audio de las llamadas
     */
    void setAudioService(AudioService audioService);
}