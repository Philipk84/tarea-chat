package com.chatapp.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {

    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 5000;

    public static void startClient() {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
             Scanner sc = new Scanner(System.in)) {

            System.out.print("Ingrese su nombre: ");
            String name = sc.nextLine();
            output.writeObject(name);
            output.flush();

            Thread listener = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = (String) input.readObject()) != null) {
                        System.out.println(msg);
                    }
                } catch (Exception e) {
                    System.out.println("Conexión cerrada.");
                }
            });
            listener.start();

            while (true) {
                String inputLine = sc.nextLine();
                output.writeObject(inputLine);
                output.flush();
            }

        } catch (IOException e) {
            System.err.println("Error de conexión con el servidor.");
        }
    }
}
