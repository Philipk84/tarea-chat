package interfaces;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * Interfaz para el servicio de audio del cliente.
 * Define las operaciones para capturar, enviar, recibir y reproducir audio UDP.
 */
public interface AudioService {
    /**
     * Configura el socket UDP para comunicación de audio.
     * 
     * @param udpSocket Socket UDP a utilizar para audio
     */
    void setUdpSocket(DatagramSocket udpSocket);
    
    /**
     * Inicia la captura y envío de audio a los destinatarios especificados.
     * 
     * @param peers Lista de direcciones UDP de los destinatarios
     */
    void startSending(List<InetSocketAddress> peers);
    
    /**
     * Inicia la recepción y reproducción de audio.
     */
    void startReceiving();
    
    /**
     * Detiene todas las operaciones de audio (envío y recepción).
     */
    void stopAudio();
}