package ui;

import controller.Controller;
import java.util.Scanner;
import java.util.InputMismatchException;

public class Main {
    private Scanner sc;
    private Controller controller;
    private String username;

    public Main() {
        this.sc = new Scanner(System.in);
        this.controller = new Controller();
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.login();
    }

    private void login() {
        System.out.println("Ingresa tu nombre de usuario:");
        username = sc.nextLine();
        
        String result = controller.connectToServer(username);
        System.out.println(result);
        
        if (controller.isConnected()) {
            showMenu();
        } else {
            System.out.println("Falló la conexión al servidor. Saliendo...");
        }
    }

    private void showMenu() {
        boolean exit = false;

        do {
            System.out.println(
                "============================="
            + "\n     CLIENTE SISTEMA CHAT    "
            + "\n============================="
            + "\nSelecciona una opción:"
            + "\n1. Crear Grupo"
            + "\n2. Unirse a Grupo"
            + "\n3. Listar Grupos"
            + "\n4. Llamar Usuario"
            + "\n5. Llamar Grupo"
            + "\n6. Terminar Llamada"
            + "\n7. Enviar Comando Personalizado"
            + "\n0. Salir");

            try{
                switch (sc.nextInt()) {
                    case 1 -> createGroup();
                    case 2 -> joinGroup();
                    case 3 -> listGroups();
                    case 4 -> callUser();
                    case 5 -> callGroup();
                    case 6 -> endCall();
                    case 7 -> sendCustomCommand();
                    case 0 -> {
                        System.out.println("Desconectando...");
                        controller.disconnect();
                        exit = true;
                    }
                    default -> System.out.println("Opción inválida.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Opción inválida.");
                sc.nextLine();
            }
        } while (!exit);
    }

    private void createGroup() {
        sc.nextLine(); // consume newline
        System.out.println("Ingresa el nombre del grupo:");
        String groupName = sc.nextLine();
        controller.createGroup(groupName);
    }

    private void joinGroup() {
        sc.nextLine(); // consume newline
        System.out.println("Ingresa el nombre del grupo al que unirse:");
        String groupName = sc.nextLine();
        controller.joinGroup(groupName);
    }

    private void listGroups() {
        controller.listGroups();
    }

    private void callUser() {
        sc.nextLine(); // consume newline
        System.out.println("Ingresa el nombre de usuario a llamar:");
        String targetUser = sc.nextLine();
        controller.callUser(targetUser);
    }

    private void callGroup() {
        sc.nextLine(); // consume newline
        System.out.println("Ingresa el nombre del grupo a llamar:");
        String groupName = sc.nextLine();
        controller.callGroup(groupName);
    }

    private void endCall() {
        controller.endCall();
    }

    private void sendCustomCommand() {
        sc.nextLine(); // consume newline
        System.out.println("Ingresa comando (ej: /help, /udpport 12345):");
        String command = sc.nextLine();
        controller.sendCommand(command);
    }
}