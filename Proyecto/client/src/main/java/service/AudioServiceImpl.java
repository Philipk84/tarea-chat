package service;

import interfaces.AudioService;
import model.CallAudio;
import java.net.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * Implementación del servicio de audio del cliente.
 * Maneja la captura, envío, recepción y reproducción de audio (UDP).
 */
public class AudioServiceImpl implements AudioService {
    private DatagramSocket udpSocket;
    private final ExecutorService audioThreads = Executors.newCachedThreadPool();

    private CallAudio.CallSender sender;
    private CallAudio.CallReceiver receiver;
    private Future<?> senderFuture;
    private Future<?> receiverFuture;

    @Override
    public void setUdpSocket(DatagramSocket udpSocket) {
        this.udpSocket = udpSocket;
    }

    @Override
    public void startSending(List<InetSocketAddress> peers) {
        if (udpSocket == null) {
            throw new IllegalStateException("UDP socket no configurado");
        }

        stopSending(); // Detener envío anterior si existe
        
        sender = new CallAudio.CallSender(udpSocket, peers);
        senderFuture = audioThreads.submit(sender);
    }

    @Override
    public void startReceiving() {
        if (udpSocket == null) {
            throw new IllegalStateException("UDP socket no configurado");
        }

        stopReceiving(); // Detener recepción anterior si existe
        
        receiver = new CallAudio.CallReceiver(udpSocket);
        receiverFuture = audioThreads.submit(receiver);
    }

    @Override
    public void stopAudio() {
        stopSending();
        stopReceiving();
    }

    private void stopSending() {
        try {
            if (sender != null) sender.stop();
            if (senderFuture != null) senderFuture.cancel(true);
        } catch (Exception ignored) {}
        sender = null;
        senderFuture = null;
    }

    private void stopReceiving() {
        try {
            if (receiver != null) receiver.stop();
            if (receiverFuture != null) receiverFuture.cancel(true);
        } catch (Exception ignored) {}
        receiver = null;
        receiverFuture = null;
    }

    /**
     * Cierra el servicio de audio y libera todos los recursos.
     */
    public void shutdown() {
        stopAudio();
        audioThreads.shutdownNow();
    }
}
