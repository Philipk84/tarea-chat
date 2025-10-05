package model.server;

import model.Message;
import java.io.*;
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
            String message = new String(packet.getData(), 0, packet.getLength());
            SocketAddress clientAddress = packet.getSocketAddress();

            if (!clients.containsKey(clientAddress)) {
                handleClientRegistration(clientAddress, message);
            } else {
                handleMessageBroadcast(clientAddress, message);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMessageBroadcast(SocketAddress senderAddress, String message) {
        String senderName = clients.get(senderAddress);
        String fullMessage = senderName + ": " + message;
        broadcastMessage(fullMessage, senderAddress);
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
}
