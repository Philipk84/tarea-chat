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
    private CallManager CallManagerImpl;
    private String username;

    /**
     * Constructor que inicializa el manejador con el nombre de usuario.
     * 
     * @param username Nombre del usuario actual
     */
    public MessageHandlerImpl(String username) {
        this.username = username;
    }

    /**
     * Maneja mensajes recibidos del servidor.
     * Delegado a métodos específicos según el tipo de mensaje.
     * 
     * @param message Mensaje completo recibido del servidor
     */
    @Override
    public void handleMessage(String message) {
        System.out.println("[SERVER] " + message);

        if (message.startsWith("LLAMADA_INICIADA")) {
            handleCallStartedMessage(message);
        } else if (message.startsWith("LLAMADA_TERMINADA")) {
            handleCallEndedMessage(message);
        }
    }

    /**
     * Maneja el mensaje de inicio de llamada del servidor.
     * Delegado a CallManagerImpl para iniciar la llamada.
     * 
     * @param callId Identificador de la llamada
     * @param participants Cadena con formato "user1:ip:port,user2:ip:port,..."
     */
    @Override
    public void handleCallStarted(String callId, String participants) {
        if (CallManagerImpl == null) {
            System.err.println("CallManagerImpl no configurado");
            return;
        }

        try {
            List<InetSocketAddress> peers = parseParticipants(participants);
            if (peers.isEmpty()) {
                System.out.println("No hay pares para la llamada.");
                return;
            }

            System.out.println("Iniciando llamada (callId=" + callId + ") con pares: " + peers);
            CallManagerImpl.startCall(callId, peers);

        } catch (Exception e) {
            System.err.println("Error iniciando llamada: " + e.getMessage());
        }
    }

    /**
     * Maneja el mensaje de finalización de llamada del servidor.
     * Formato enviado por el servidor: "LLAMADA_TERMINADA: <callId> por <usuario>"
     */
    @Override
    public void handleCallEnded(String callId) {
        if (CallManagerImpl != null) {
            CallManagerImpl.endCall();
        }
        System.out.println("Llamada terminada: " + callId);
    }

    /**
     * Establece el gestor de llamadas para manejar eventos de llamadas.
     * 
     * @param CallManagerImpl Gestor de llamadas del cliente
     */
    public void setCallManagerImpl(CallManager CallManagerImpl) {
        this.CallManagerImpl = CallManagerImpl;
    }

    /**
     * Maneja el mensaje de inicio de llamada del servidor.
     * Formato enviado por el servidor: "LLAMADA_INICIADA: <callId> <participants>"
     * donde participants = "user1:ip:port,user2:ip:port,..."
     */
    private void handleCallStartedMessage(String message) {
        try {
            String prefix = "LLAMADA_INICIADA: ";
            String payload;
            if (message.startsWith(prefix)) {
                payload = message.substring(prefix.length()).trim();
            } else {
                payload = message.substring("LLAMADA_INICIADA".length()).replaceFirst("^:\\s*", "").trim();
            }

            String[] parts = payload.split(" ", 2);
            String callId = parts[0];
            String participants = parts.length > 1 ? parts[1] : "";
            
            handleCallStarted(callId, participants);
        } catch (Exception e) {
            System.err.println("Error procesando LLAMADA_INICIADA: " + e.getMessage());
        }
    }

    /**
     * Maneja el mensaje de finalización de llamada del servidor.
     * Formato enviado por el servidor: "LLAMADA_TERMINADA: <callId> por <usuario>"
     */
    private void handleCallEndedMessage(String message) {
        try {
            String callId = null;
            String prefix = "LLAMADA_TERMINADA: ";
            if (message.startsWith(prefix)) {
                String payload = message.substring(prefix.length());
                String[] tokens = payload.split(" ", 2);
                callId = tokens.length > 0 ? tokens[0].trim() : null;
            } else {
                String[] parts = message.split(" ", 3);
                if (parts.length > 1) callId = parts[1].trim();
            }
            handleCallEnded(callId);
        } catch (Exception e) {
            System.err.println("Error procesando LLAMADA_TERMINADA: " + e.getMessage());
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
     * Parsea un participante en una dirección UDP.
     * 
     * @param participant Cadena con formato "user:ip:port"
     * @return Dirección InetSocketAddress o null si es el usuario actual o error
     */
    private InetSocketAddress parseParticipant(String participant) {
        try {
            int firstColon = participant.indexOf(':');
            int lastColon = participant.lastIndexOf(':');
            if (firstColon <= 0 || lastColon <= firstColon) {
                return null;
            }

            String user = participant.substring(0, firstColon);
            String ip = participant.substring(firstColon + 1, lastColon);
            String portStr = participant.substring(lastColon + 1);

            if (user.equals(username)) return null;

            int port = Integer.parseInt(portStr);
            return new InetSocketAddress(ip, port);
            
        } catch (NumberFormatException e) {
            System.err.println("Error parseando participante: " + participant);
            return null;
        }
    }
}
