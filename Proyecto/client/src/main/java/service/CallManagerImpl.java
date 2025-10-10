package service;

import interfaces.AudioService;
import interfaces.CallManager;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * Implementación del gestor de llamadas del cliente.
 * Coordina el inicio, manejo y finalización de llamadas de audio.
 */
public class CallManagerImpl implements CallManager {
    private String activeCallId;
    private AudioService audioService;

    /**
     * Constructor por defecto que inicializa el gestor sin llamada activa.
     */
    public CallManagerImpl() {
        this.activeCallId = null;
    }

    @Override
    public boolean startCall(String callId, List<InetSocketAddress> peers) {
        if (audioService == null) {
            System.err.println("AudioService no configurado");
            return false;
        }

        if (hasActiveCall()) {
            endCall(); // Terminar llamada anterior
        }

        try {
            activeCallId = callId;
            audioService.startSending(peers);
            audioService.startReceiving();
            
            System.out.println("Llamada activa: " + callId + ".");
            return true;
            
        } catch (Exception e) {
            System.err.println("Error iniciando llamada: " + e.getMessage());
            activeCallId = null;
            return false;
        }
    }

    @Override
    public void endCall() {
        if (audioService != null) {
            audioService.stopAudio();
        }
        activeCallId = null;
        System.out.println("Llamada finalizada.");
    }

    @Override
    public boolean hasActiveCall() {
        return activeCallId != null;
    }

    @Override
    public String getActiveCallId() {
        return activeCallId;
    }

    @Override
    public void setAudioService(AudioService audioService) {
        this.audioService = audioService;
    }
}