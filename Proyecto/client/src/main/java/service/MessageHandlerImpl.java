package service;

import interfaces.CallManager;
import interfaces.MessageHandler;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del manejador de mensajes del cliente.
 * Procesa y delega mensajes recibidos del servidor.
 */
public class MessageHandlerImpl implements MessageHandler {
    private CallManager callManager;
    private String username;

    /**
     * Constructor que inicializa el manejador con el nombre de usuario.
     * 
     * @param username Nombre del usuario actual
     */
    public MessageHandlerImpl(String username) {
        this.username = username;
    }

    @Override
    public void handleMessage(String message) {
        System.out.println("[SERVER] " + message);

        if (message.startsWith("CALL_STARTED")) {
            handleCallStartedMessage(message);
        } else if (message.startsWith("CALL_ENDED")) {
            handleCallEndedMessage(message);
        }
        // Otros tipos de mensajes se muestran directamente
    }

    @Override
    public void handleCallStarted(String callId, String participants) {
        if (callManager == null) {
            System.err.println("CallManager no configurado");
            return;
        }

        try {
            List<InetSocketAddress> peers = parseParticipants(participants);
            if (peers.isEmpty()) {
                System.out.println("No hay pares para la llamada.");
                return;
            }

            System.out.println("Iniciando llamada (callId=" + callId + ") con pares: " + peers);
            callManager.startCall(callId, peers);

        } catch (Exception e) {
            System.err.println("Error iniciando llamada: " + e.getMessage());
        }
    }

    @Override
    public void handleCallEnded(String callId) {
        if (callManager != null) {
            callManager.endCall();
        }
        System.out.println("Llamada terminada: " + callId);
    }

    /**
     * Establece el gestor de llamadas para manejar eventos de llamadas.
     * 
     * @param callManager Gestor de llamadas del cliente
     */
    public void setCallManager(CallManager callManager) {
        this.callManager = callManager;
    }

    /**
     * Maneja el mensaje de inicio de llamada del servidor.
     * 
     * @param message Mensaje completo "CALL_STARTED <callId> <participants>"
     */
    private void handleCallStartedMessage(String message) {
        try {
            String payload = message.substring("CALL_STARTED".length()).trim();
            String[] parts = payload.split(" ", 2);
            String callId = parts[0];
            String participants = parts.length > 1 ? parts[1] : "";
            
            handleCallStarted(callId, participants);
        } catch (Exception e) {
            System.err.println("Error procesando CALL_STARTED: " + e.getMessage());
        }
    }

    /**
     * Maneja el mensaje de finalización de llamada del servidor.
     * 
     * @param message Mensaje completo "CALL_ENDED <callId>"
     */
    private void handleCallEndedMessage(String message) {
        try {
            String[] parts = message.split(" ", 2);
            String callId = parts.length > 1 ? parts[1].trim() : null;
            handleCallEnded(callId);
        } catch (Exception e) {
            System.err.println("Error procesando CALL_ENDED: " + e.getMessage());
        }
    }

    /**
     * Parsea la lista de participantes en direcciones UDP.
     * 
     * @param participants Cadena con formato "user1:ip:port,user2:ip:port,..."
     * @return Lista de direcciones InetSocketAddress (excluyendo el usuario actual)
     */
    private List<InetSocketAddress> parseParticipants(String participants) {
        return Arrays.stream(participants.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::parseParticipant)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Parsea un participante individual en dirección UDP.
     * 
     * @param participant Cadena con formato "username:ip:port"
     * @return InetSocketAddress o null si el participante es el usuario actual o hay error
     */
    private InetSocketAddress parseParticipant(String participant) {
        try {
            String[] tokens = participant.split(":");
            if (tokens.length < 3) return null;
            
            String user = tokens[0];
            String ip = tokens[1];
            String portStr = tokens[2];
            
            if (user.equals(username)) return null; // No enviar a sí mismo
            
            int port = Integer.parseInt(portStr);
            return new InetSocketAddress(ip, port);
            
        } catch (NumberFormatException e) {
            System.err.println("Error parseando participante: " + participant);
            return null;
        }
    }
}