package model;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {

    private static Client instance;

    private DatagramSocket socket;
    private DatagramPacket packet;  
    private InetAddress ipAddressServer;
    private int port;
    private byte[] nameData;
    private byte[] sendData;
    private DatagramPacket namePacket;
    private Thread receiveThread;
    private Thread audioThread;

    private Client(Config config, String username) {
        try {
            this.nameData = username.getBytes();
            
            this.ipAddressServer = InetAddress.getByName(config.getHost());
            this.port = config.getPort();

            this.socket = new DatagramSocket();
            
            this.namePacket = new DatagramPacket(nameData, nameData.length, ipAddressServer, port);
            socket.send(namePacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Client getInstance(Config config, String username) {
        if (instance == null) {
            instance = new Client(config, username);
        }
        return instance;
    }

    public void connectToChat() {
        receiveThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String msg = new String(packet.getData(), 0, packet.getLength());

                    // Solo procesa mensajes de texto
                    if (msg.startsWith("TEXT:")) {
                        String cleanMsg = msg.substring(5);
                        System.out.println("[MENSAJE] " + cleanMsg);
                    }

                } catch (Exception e) {
                    System.out.println("Chat cerrado: " + e.getMessage());
                    break;
                }
            }
        });
        receiveThread.start();
    }

    public void sendMessage(String message) {
        try {
            String fullMsg = "TEXT:" + message;
            byte[] data = fullMsg.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, ipAddressServer, port);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendVoiceNote(byte[] audioData) {
        try {
            byte[] header = "AUDIO:".getBytes();
            byte[] fullData = new byte[header.length + audioData.length];
            System.arraycopy(header, 0, fullData, 0, header.length);
            System.arraycopy(audioData, 0, fullData, header.length, audioData.length);

            DatagramPacket packet = new DatagramPacket(fullData, fullData.length, ipAddressServer, port);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


     public void connectToAudio() {
        audioThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    // Detectar cabecera
                    if (packet.getLength() > 6) {
                        String header = new String(buffer, 0, 6);

                        if (header.equals("AUDIO:")) {
                            byte[] audioData = new byte[packet.getLength() - 6];
                            System.arraycopy(buffer, 6, audioData, 0, audioData.length);

                            // Reproducir audio
                            AudioUtils.playAudio(audioData);
                        }
                    }

                } catch (Exception e) {
                    System.out.println("Audio cerrado: " + e.getMessage());
                    break;
                }
            }
        });
        audioThread.start();
    }
}