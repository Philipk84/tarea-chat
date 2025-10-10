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
            showMainMenu();
        } else {
            System.out.println("Falló la conexión al servidor. Saliendo...");
        }
    }

    private void showMainMenu() {
        sc.nextLine();
        boolean exit = false;

        do {
            System.out.println(
                    "============================="
                + "\n        SISTEMA CHAT         "
                + "\n============================="
                + "\nSelecciona una opción:"
                + "\n1. Menu de Grupos"
                + "\n2. Menu de Usuarios"
                + "\n0. Salir");

            try {
                switch (sc.nextInt()) {
                    case 1 -> showGroupMenu();
                    case 2 -> showUserMenu();
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

    private void showGroupMenu() {
        sc.nextLine();

        System.out.println(
                "============================="
            + "\n         MENU GRUPO          "
            + "\n============================="
            + "\nSelecciona una opción:"
            + "\n   1. Listar Grupos"
            + "\n   2. Crear Grupo"
            + "\n   3. Unirse a Grupo"
            + "\n   4. Enviar Mensaje a Grupo"
            + "\n   5. Llamar Grupo"
            + "\n   6. Terminar Llamada"
            + "\n   0. Salir");

        try {
            switch (sc.nextInt()) {
                case 1 -> listGroups();
                case 2 -> joinGroup();
                case 3 -> createGroup();
                case 4 -> callGroup();
                case 5 -> endCall();
                case 0 -> System.out.println("Saliendo...");

                default -> System.out.println("Opción inválida. Saliendo...");
            }
        } catch (InputMismatchException e) {
            System.out.println("Opción inválida. Saliendo...");
        }
    }

    private void showUserMenu() {
        sc.nextLine();

        System.out.println(
                "============================="
            + "\n         MENU USUARIO        "
            + "\n============================="
            + "\nSelecciona una opción:"
            + "\n   1. Listar Usuarios"
            + "\n   2. Llamar Usuario"
            + "\n   0. Salir");

        try {
            switch (sc.nextInt()) {
                case 1 -> listUsers();
                case 2 -> callUser();
                case 0 -> System.out.println("Saliendo...");

                default -> System.out.println("Opción inválida. Saliendo...");
            }
        } catch (InputMismatchException e) {
            System.out.println("Opción inválida. Saliendo...");
        }
    }

    private Object listUsers() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listUsers'");
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
}