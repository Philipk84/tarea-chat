package service;

import interfaces.CallManager;
import interfaces.MessageHandler;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ImplementaciÃƒÂ³n del manejador de mensajes del cliente.
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

    @Override
    public void handleMessage(String message) {
        System.out.println("[SERVER] " + message);

        // Señalización de llamadas (coincidir con el servidor)
        if (message.startsWith("LLAMADA_INICIADA")) {
            handleCallStartedMessage(message);
        } else if (message.startsWith("LLAMADA_TERMINADA")) {
            handleCallEndedMessage(message);
        } else if (message.startsWith("MENSAJE_PRIVADO")) {
            handlePrivateMessage(message);
        } else if (message.startsWith("MENSAJE_GRUPO")) {
            handleGroupMessage(message);
        } else if (message.startsWith("VOICE_NOTE_INCOMING")) {
            handleVoiceNoteIncoming(message);
        } else if (message.startsWith("VOICE_NOTE_TARGET")) {
            handleVoiceNoteTarget(message);
        } else if (message.startsWith("VOICE_NOTE_GROUP_INCOMING")) {
            handleVoiceNoteGroupIncoming(message);
        } else if (message.startsWith("VOICE_NOTE_GROUP_TARGETS")) {
            handleVoiceNoteGroupTargets(message);
        } else if (message.startsWith("VOICE_FROM:")) {
            handleReceivedVoiceNote(message);
        } else if (message.startsWith("VOICE_GROUP_FROM:")) {
            handleReceivedGroupVoiceNote(message);
        }
        // Otros tipos de mensajes se muestran directamente
    }

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
                // Respaldo por si no hay espacio tras los dos puntos
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
                // payload esperado: "<callId> por <usuario>" o solo "<callId>"
                String[] tokens = payload.split(" ", 2);
                callId = tokens.length > 0 ? tokens[0].trim() : null;
            } else {
                // Respaldo: intentar tomar el segundo token como callId
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
     * Parsea un participante individual en direcciÃƒÂ³n UDP.
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

    /**
     * Maneja mensajes privados recibidos de otros usuarios.
     * 
     * @param message Mensaje completo del servidor
     */
    private void handlePrivateMessage(String message) {
        // El mensaje ya viene formateado desde el servidor
        // Solo se muestra en la salida estándar
    }

    /**
     * Maneja mensajes de grupo recibidos.
     * 
     * @param message Mensaje completo del servidor
     */
    private void handleGroupMessage(String message) {
        // El mensaje ya viene formateado desde el servidor
        // Solo se muestra en la salida estándar
    }

    /**
     * Maneja la notificación de nota de voz entrante de un usuario.
     * 
     * @param message Mensaje con formato "VOICE_NOTE_INCOMING from <user> <ip:port>"
     */
    private void handleVoiceNoteIncoming(String message) {
        try {
            String payload = message.substring("VOICE_NOTE_INCOMING from ".length()).trim();
            String[] parts = payload.split(" ", 2);
            String sender = parts[0];
            @SuppressWarnings("unused")
            String senderUdpInfo = parts.length > 1 ? parts[1] : "";
            
            System.out.println("⏹️ Recibiendo nota de voz de " + sender + "...");
            // El AudioService manejará la recepción real del audio vía UDP
            // En una implementación completa, aquí se activaría la reproducción
            
        } catch (Exception e) {
            System.err.println("Error procesando nota de voz entrante: " + e.getMessage());
        }
    }

    /**
     * Maneja la informaciÃ³n del destinatario para enviar una nota de voz.
     * 
     * @param message Mensaje con formato "VOICE_NOTE_TARGET <user> <ip:port>"
     */
    private void handleVoiceNoteTarget(String message) {
        try {
            String payload = message.substring("VOICE_NOTE_TARGET ".length()).trim();
            String[] parts = payload.split(" ", 2);
            String targetUser = parts[0];
            String targetUdpInfo = parts.length > 1 ? parts[1] : "";
            
            System.out.println("🎤 Iniciando grabación de nota de voz para " + targetUser + "...");
            System.out.println("📡 Destino UDP: " + targetUdpInfo);
            System.out.println("Presiona ENTER cuando termines de grabar.");
            // El cliente deberá iniciar la grabación y envío por UDP
            
        } catch (Exception e) {
            System.err.println("Error procesando destino de nota de voz: " + e.getMessage());
        }
    }

    /**
     * Maneja la notificaciÃ³n de nota de voz grupal entrante.
     * 
     * @param message Mensaje con formato "VOICE_NOTE_GROUP_INCOMING from <user> in <group> <ip:port>"
     */
    private void handleVoiceNoteGroupIncoming(String message) {
        try {
            String payload = message.substring("VOICE_NOTE_GROUP_INCOMING from ".length()).trim();
            String[] parts = payload.split(" in ", 2);
            String sender = parts[0];
            
            if (parts.length > 1) {
                String[] groupAndUdp = parts[1].split(" ", 2);
                String groupName = groupAndUdp[0];
                
                System.out.println("\n Recibiendo nota de voz de " + sender + " en grupo [" + groupName + "]...");
            }
            
        } catch (Exception e) {
            System.err.println("Error procesando nota de voz grupal entrante: " + e.getMessage());
        }
    }

    /**
     * Maneja la lista de destinatarios para enviar una nota de voz grupal.
     * 
     * @param message Mensaje con formato "VOICE_NOTE_GROUP_TARGETS <group> <user1:ip:port,user2:ip:port,...>"
     */
    private void handleVoiceNoteGroupTargets(String message) {
        try {
            String payload = message.substring("VOICE_NOTE_GROUP_TARGETS ".length()).trim();
            String[] parts = payload.split(" ", 2);
            String groupName = parts[0];
            
            System.out.println("🎤 Iniciando grabación de nota de voz para grupo [" + groupName + "]...");
            System.out.println("Presiona ENTER cuando termines de grabar.");
            // El cliente deberá iniciar la grabación y envío por UDP a múltiples destinatarios
            
        } catch (Exception e) {
            System.err.println("Error procesando destinos de nota de voz grupal: " + e.getMessage());
        }
    }

    /**
     * Maneja la recepción de una nota de voz vía UDP.
     * Formato del mensaje: "VOICE_FROM:sender:audioData"
     * 
     * @param message Mensaje UDP recibido con la nota de voz
     */
    private void handleReceivedVoiceNote(String message) {
        try {
            String[] parts = message.split(":", 3);
            if (parts.length < 3) return;

            String sender = parts[1];
            String audioDataStr = parts[2];
            
            System.out.println("Reproduciendo nota de voz de " + sender + "...");
            
            // En una implementación completa, aquí se convertiría audioDataStr a bytes
            // y se reproduciría usando AudioServiceImpl.playReceivedVoiceNote()
            @SuppressWarnings("unused")
            byte[] audioData = audioDataStr.getBytes(); // Conversión simplificada
            
            // Reproducir la nota de voz (esto requeriría acceso al AudioService)
            System.out.println("[AUDIO] Nota de voz de " + sender + " (simulada)");
            
        } catch (Exception e) {
            System.err.println("Error procesando nota de voz recibida: " + e.getMessage());
        }
    }

    /**
     * Maneja la recepción de una nota de voz grupal vía UDP.
     * Formato del mensaje: "VOICE_GROUP_FROM:sender:group:audioData"
     * 
     * @param message Mensaje UDP recibido con la nota de voz grupal
     */
    private void handleReceivedGroupVoiceNote(String message) {
        try {
            String[] parts = message.split(":", 4);
            if (parts.length < 4) return;

            String sender = parts[1];
            String groupName = parts[2];
            String audioDataStr = parts[3];
            
            System.out.println("Reproduciendo nota de voz de " + sender + " en grupo [" + groupName + "]...");
            
            // En una implementación completa, aquí se convertiría audioDataStr a bytes
            // y se reproduciría usando AudioServiceImpl.playReceivedVoiceNote()
            @SuppressWarnings("unused")
            byte[] audioData = audioDataStr.getBytes(); // Conversión simplificada
            
            // Reproducir la nota de voz grupal
            System.out.println("[GRUPO " + groupName + "] Nota de voz de " + sender + " (simulada)");
            
        } catch (Exception e) {
            System.err.println("Error procesando nota de voz grupal recibida: " + e.getMessage());
        }
    }
}
