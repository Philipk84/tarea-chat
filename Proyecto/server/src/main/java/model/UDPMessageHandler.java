package model;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;

/**
 * Manejador de mensajes UDP para el servidor.
 * Procesa paquetes UDP para notas de voz, llamadas y audio en tiempo real.
 * Usa la misma lógica que class/ClientHandler.java con DatagramSocket y DatagramPacket.
 */
public class UDPMessageHandler implements Runnable {
    private final DatagramPacket packet;
    private final DatagramSocket socket;
    private final Map<SocketAddress, String> udpClients;

    /**
     * Constructor que recibe el paquete UDP a procesar.
     * Usa la misma firma que class/ClientHandler.java
     * 
     * @param packet Paquete UDP recibido
     * @param socket Socket UDP del servidor
     * @param udpClients Mapa de direcciones UDP a nombres de usuario
     */
    public UDPMessageHandler(DatagramPacket packet, DatagramSocket socket, Map<SocketAddress, String> udpClients) {
        this.packet = packet;
        this.socket = socket;
        this.udpClients = udpClients;
    }

    @Override
    public void run() {
        try {
            String message = new String(packet.getData(), 0, packet.getLength());
            SocketAddress clientAddress = packet.getSocketAddress();

            // Procesar diferentes tipos de mensajes UDP
            if (message.startsWith("VOICE_NOTE:")) {
                handleVoiceNote(message, clientAddress);
            } else if (message.startsWith("VOICE_GROUP:")) {
                handleVoiceGroup(message, clientAddress);
            } else if (message.startsWith("CALL_AUDIO:")) {
                handleCallAudio(message, clientAddress);
            } else {
                // Mensaje de chat UDP general (como en class/ClientHandler.java)
                handleGeneralMessage(message, clientAddress);
            }
        } catch (Exception e) {
            System.err.println("Error procesando mensaje UDP: " + e.getMessage());
        }
    }

    /**
     * Maneja notas de voz dirigidas a un usuario específico.
     * Formato: VOICE_NOTE:targetUser:audioData
     */
    private void handleVoiceNote(String message, SocketAddress senderAddress) throws Exception {
        String[] parts = message.split(":", 3);
        if (parts.length < 3) return;

        String targetUser = parts[1];
        String audioDataStr = parts[2];

        // Obtener información UDP del usuario destino
        String targetUdpInfo = ChatServer.getUdpInfo(targetUser);
        if (targetUdpInfo == null) return;

        // Parsear dirección del destinatario
        String[] ipPort = targetUdpInfo.split(":");
        InetSocketAddress targetAddress = new InetSocketAddress(ipPort[0], Integer.parseInt(ipPort[1]));

        // Reenviar la nota de voz al destinatario
        String senderName = udpClients.get(senderAddress);
        String forwardMessage = "VOICE_FROM:" + (senderName != null ? senderName : "Unknown") + ":" + audioDataStr;
        
        DatagramPacket forwardPacket = new DatagramPacket(
            forwardMessage.getBytes(), forwardMessage.length(), targetAddress
        );
        socket.send(forwardPacket);

        System.out.println("Nota de voz reenviada de " + senderName + " a " + targetUser);
    }

    /**
     * Maneja notas de voz dirigidas a un grupo.
     * Formato: VOICE_GROUP:groupName:audioData
     */
    private void handleVoiceGroup(String message, SocketAddress senderAddress) throws Exception {
        String[] parts = message.split(":", 3);
        if (parts.length < 3) return;

        String groupName = parts[1];
        String audioDataStr = parts[2];
        String senderName = udpClients.get(senderAddress);
        
        // Obtener miembros del grupo
        Set<String> members = ChatServer.getGroupMembers(groupName);
        if (members.isEmpty()) return;

        String forwardMessage = "VOICE_GROUP_FROM:" + (senderName != null ? senderName : "Unknown") + 
                            ":" + groupName + ":" + audioDataStr;

        int sentCount = 0;
        // Enviar a todos los miembros del grupo excepto al remitente
        for (String member : members) {
            if (member.equals(senderName)) continue;

            String memberUdpInfo = ChatServer.getUdpInfo(member);
            if (memberUdpInfo != null) {
                String[] ipPort = memberUdpInfo.split(":");
                InetSocketAddress memberAddress = new InetSocketAddress(ipPort[0], Integer.parseInt(ipPort[1]));

                DatagramPacket forwardPacket = new DatagramPacket(
                    forwardMessage.getBytes(), forwardMessage.length(), memberAddress
                );
                socket.send(forwardPacket);
                sentCount++;
            }
        }

        System.out.println("Nota de voz grupal de " + senderName + " enviada a " + sentCount + " miembros del grupo [" + groupName + "]");
    }

    /**
     * Maneja audio de llamadas en tiempo real.
     * Simplemente reenvía el audio a todos los participantes de la llamada.
     */
    @SuppressWarnings("unused")
    private void handleCallAudio(String message, SocketAddress senderAddress) throws Exception {
        // Para llamadas, el audio se reenvía directamente entre participantes
        // El servidor actúa como relay usando la misma lógica de DatagramPacket
        byte[] audioData = packet.getData();
        int length = packet.getLength();

        // En una implementación real, aquí se obtendría la lista de participantes
        // de la llamada activa y se reenviaría el audio a cada uno
        String senderName = udpClients.get(senderAddress);
        System.out.println("Audio de llamada recibido de: " + (senderName != null ? senderName : senderAddress));
        
        // TODO: Implementar relay de audio a participantes de la llamada activa
        // Esto requeriría acceso al CallManager para obtener la lista de participantes
    }

    /**
     * Maneja mensajes UDP generales usando la misma lógica que class/ClientHandler.java
     */
    private void handleGeneralMessage(String message, SocketAddress clientAddress) throws Exception {
        // Si el cliente no está registrado, el primer mensaje es su nombre (como en class/ClientHandler.java)
        if (!udpClients.containsKey(clientAddress)) {
            udpClients.put(clientAddress, message.trim());
            String welcome = "Bienvenido " + message.trim() + " (UDP)";
            DatagramPacket welcomePacket = new DatagramPacket(
                welcome.getBytes(), welcome.length(),
                packet.getAddress(), packet.getPort()
            );
            socket.send(welcomePacket);
            System.out.println("Cliente UDP registrado: " + message.trim() + " desde " + clientAddress);
            return;
        }

        String sender = udpClients.get(clientAddress);
        String fullMessage = sender + " (UDP): " + message;

        // Reenviar el mensaje a todos los clientes UDP menos al remitente (como en class/ClientHandler.java)
        int sentCount = 0;
        for (Map.Entry<SocketAddress, String> entry : udpClients.entrySet()) {
            if (!entry.getKey().equals(clientAddress)) {
                DatagramPacket sendPacket = new DatagramPacket(
                    fullMessage.getBytes(), fullMessage.length(),
                    ((InetSocketAddress) entry.getKey()).getAddress(),
                    ((InetSocketAddress) entry.getKey()).getPort()
                );
                socket.send(sendPacket);
                sentCount++;
            }
        }

        System.out.println("Mensaje UDP de " + sender + " reenviado a " + sentCount + " clientes");
    }

    /**
     * Registra un cliente UDP en el mapa de clientes.
     * Útil para asociar direcciones UDP con nombres de usuario.
     * 
     * @param address Dirección del cliente UDP
     * @param username Nombre del usuario
     */
    public static void registerUdpClient(SocketAddress address, String username, Map<SocketAddress, String> udpClients) {
        udpClients.put(address, username);
        System.out.println("Cliente UDP registrado: " + username + " -> " + address);
    }

    /**
     * Desregistra un cliente UDP del mapa de clientes.
     * 
     * @param address Dirección del cliente UDP a desregistrar
     */
    public static void unregisterUdpClient(SocketAddress address, Map<SocketAddress, String> udpClients) {
        String username = udpClients.remove(address);
        if (username != null) {
            System.out.println("Cliente UDP desregistrado: " + username + " <- " + address);
        }
    }
}
