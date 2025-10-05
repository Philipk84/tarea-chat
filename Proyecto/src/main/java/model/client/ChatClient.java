package model.client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {

    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 5000;

    public static void startClient() {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            Scanner scanner = new Scanner(System.in)) {

            System.out.print("Ingrese su nombre: ");
            String name = scanner.nextLine();
            output.writeObject(name);
            output.flush();

            // Hilo para escuchar mensajes
            Thread listener = new Thread(() -> {
                try {
                    Message msg;
                    while ((msg = (Message) input.readObject()) != null) {
                        System.out.println("[" + msg.getSender() + "]: " + msg.getContent());
                    }
                } catch (Exception e) {
                    System.out.println("Conexión cerrada.");
                }
            });
            listener.start();

            // Bucle de envío de mensajes
            while (true) {
                String content = scanner.nextLine();
                Message msg = new Message(name, "ALL", content);
                output.writeObject(msg);
                output.flush();
            }

        } catch (IOException e) {
            System.err.println("Error de conexión con el servidor.");
        }
    }
}
