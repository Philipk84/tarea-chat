package model;

import java.net.*;
import java.util.Map;

public class ClientHandler implements Runnable {
    private DatagramPacket packet;
    private DatagramSocket socket;
    private Map<SocketAddress, String> clients;

    public ClientHandler(DatagramPacket packet, DatagramSocket socket, Map<SocketAddress, String> clients) {
        this.packet = packet;
        this.socket = socket;
        this.clients = clients;
    }

    @Override
    public void run() {
        try {
            SocketAddress clientAddress = packet.getSocketAddress();
            byte[] data = packet.getData();
            int length = packet.getLength();
            String message = new String(data, 0, length);

            if (!clients.containsKey(clientAddress)) {
                handleClientRegistration(clientAddress, message);
            } else {
                // Aquí detectamos el tipo de mensaje según la etiqueta
                if (message.startsWith("TEXT:")) {
                    handleMessageBroadcast(clientAddress, message.substring(5)); // Quitamos el prefijo TEXT:
                } else if (message.startsWith("AUDIO:")) {
                    handleAudioBroadcast(clientAddress, data, length);
                } else {
                    System.out.println("⚠️ Mensaje desconocido recibido de " + clientAddress);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleClientRegistration(SocketAddress clientAddress, String clientName) {
        clients.put(clientAddress, clientName);
        String welcomeMessage = "Bienvenido " + clientName;

        try {
            DatagramPacket welcomePacket = new DatagramPacket(
                    welcomeMessage.getBytes(), welcomeMessage.length(),
                    packet.getAddress(), packet.getPort()
            );
            socket.send(welcomePacket);
            System.out.println("Cliente registrado: " + clientName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Manejo de texto
    private void handleMessageBroadcast(SocketAddress senderAddress, String message) {
        String senderName = clients.get(senderAddress);
        String fullMessage = "TEXT:" + senderName + ": " + message;
        broadcastMessage(fullMessage, senderAddress);
        System.out.println("Mensaje: " + fullMessage);
    }

    private void broadcastMessage(String message, SocketAddress senderAddress) {
        for (Map.Entry<SocketAddress, String> entry : clients.entrySet()) {
            SocketAddress clientAddress = entry.getKey();

            if (!clientAddress.equals(senderAddress)) {
                InetSocketAddress inetAddress = (InetSocketAddress) clientAddress;
                sendPacketToClient(message, inetAddress.getAddress(), inetAddress.getPort());
            }
        }
    }

    // Envio mensaje de manera individual a cliente
    private void sendPacketToClient(String message, InetAddress address, int port) {
        try {
            byte[] messageBytes = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                messageBytes, messageBytes.length, address, port
            );
            socket.send(sendPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Manejo de audio
    private void handleAudioBroadcast(SocketAddress senderAddress, byte[] data, int length) {
        String senderName = clients.get(senderAddress);
        System.out.println("Nota de voz recibida de " + senderName + " (" + length + " bytes)");

        broadcastAudio(data, length, senderAddress);
    }

    private void broadcastAudio(byte[] data, int length, SocketAddress senderAddress) {
    for (Map.Entry<SocketAddress, String> entry : clients.entrySet()) {
        SocketAddress clientAddress = entry.getKey();

        if (!clientAddress.equals(senderAddress)) {
            try {
                InetSocketAddress inetAddress = (InetSocketAddress) clientAddress;
                sendAudioToClient(data, length, inetAddress.getAddress(), inetAddress.getPort());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

    // Envio individual de audio a cliente
    private void sendAudioToClient(byte[] audioData, int length, InetAddress address, int port) {
    try {
        DatagramPacket audioPacket = new DatagramPacket(
            audioData, length, address, port
        );
        socket.send(audioPacket);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
    
}
