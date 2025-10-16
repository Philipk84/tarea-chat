package com.chatapp;

import com.chatapp.client.ChatClient;
import com.chatapp.server.ChatServer;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("============================");
        System.out.println("   SISTEMA DE CHAT");
        System.out.println("============================");
        System.out.println("Seleccione un modo:");
        System.out.println("1. Iniciar servidor");
        System.out.println("2. Iniciar cliente");
        System.out.println("3. Salir");
        System.out.print("> ");

        String opcion = scanner.nextLine();

        switch (opcion) {
            case "1":
                ChatServer.iniciarServidor();
                break;
            case "2":
                iniciarCliente();
                break;
            case "3":
                System.out.println("Cerrando aplicación...");
                System.exit(0);
                break;
            default:
                System.out.println("Opción inválida.");
                break;
        }
        scanner.close();
    }

    public static void iniciarCliente() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Iniciando cliente...");
        System.out.print("Ingrese su nombre de usuario: ");
        String username = scanner.nextLine();
        
        ChatClient client = new ChatClient("172.20.10.3", 5000, username);
        client.start();
        scanner.close();
    }
}
