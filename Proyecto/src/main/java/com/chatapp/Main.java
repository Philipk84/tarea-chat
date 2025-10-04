package com.chatapp;

import com.chatapp.server.ChatServer;
import com.chatapp.client.ChatClient;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("============================");
        System.out.println("   SISTEMA DE CHAT");
        System.out.println("============================");
        System.out.println("Seleccione un modo:");
        System.out.println("1. Iniciar servidor");
        System.out.println("2. Iniciar cliente");
        System.out.println("3. Salir");
        System.out.print("> ");

        int opcion = sc.nextInt();
        sc.nextLine(); // limpiar buffer

        switch (opcion) {
            case 1 -> ChatServer.startServer();
            case 2 -> ChatClient.startClient();
            case 3 -> {
                System.out.println("Saliendo...");
                System.exit(0);
            }
            default -> System.out.println("Opción no válida.");
        }
    }
}
