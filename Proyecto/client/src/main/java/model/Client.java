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

    public void connectToChat () {
        receiveThread = new Thread(() -> {
            byte[] buffer = new byte[256];
            while (true) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(receivePacket);
                    String msg = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    System.out.println(msg);
                } catch (Exception e) {
                    break;
                }
            }
        });
        receiveThread.start();
    }

    public void sendMessage(String message) {
        try {
            this.sendData = message.getBytes();
            this.packet = new DatagramPacket(sendData, sendData.length, ipAddressServer, port);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendVoiceNote(byte[] audioData){
        try{
            DatagramPacket packet = new DatagramPacket(audioData, audioData.length, ipAddressServer, port);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}