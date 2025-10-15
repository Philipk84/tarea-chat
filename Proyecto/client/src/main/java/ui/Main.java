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
        boolean exit = false;

        do {
            System.out.println(
                    "\n============================="
                + "\n     SISTEMA DE CHAT         "
                + "\n============================="
                + "\nUsuario: " + username
                + "\n-----------------------------"
                + "\nSelecciona una opción:"
                + "\n1. Interactuar con Grupos"
                + "\n2. Interactuar con Usuarios"
                + "\n0. Salir"
                + "\n=============================");

            try {
                int option = sc.nextInt();
                
                switch (option) {
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
        System.out.println(
                "\n============================="
            + "\n         MENU GRUPOS         "
            + "\n============================="
            + "\nSelecciona una opción:"
            + "\n   1. Listar Grupos"
            + "\n   2. Crear Grupo"
            + "\n   3. Unirse a Grupo"
            + "\n   4. Llamar a Grupo"
            + "\n   5. Finalizar Llamada"
            + "\n   6. Enviar Mensaje a Grupo"
            + "\n   7. Enviar Nota de Voz a Grupo"
            + "\n   0. Volver"
            + "\n=============================");

        try {
            int option = sc.nextInt();
            
            switch (option) {
                case 1 -> listGroups();
                case 2 -> createGroup();
                case 3 -> joinGroup();
                case 4 -> callGroup();
                case 5 -> endCall();
                case 6 -> sendMessageToGroup();
                case 7 -> sendVoiceToGroup();
                case 0 -> System.out.println("Volviendo al menú principal...");
                default -> System.out.println("Opción inválida.");
            }
        } catch (InputMismatchException e) {
            System.out.println("Opción inválida.");
            sc.nextLine();
        }
    }

    private void listGroups() {
        controller.listGroups();
    }

    private void createGroup() {
        System.out.println("Ingresa el nombre del grupo:");
        String groupName = sc.nextLine();
        controller.createGroup(groupName);
    }

    private void joinGroup() {
        listGroups();
        System.out.println("Ingresa el nombre del grupo al que unirse:");
        String groupName = sc.nextLine();
        controller.joinGroup(groupName);
    }

    private void callGroup() {
        listGroups();
        System.out.println("Ingresa el nombre del grupo a llamar:");
        String groupName = sc.nextLine();
        controller.callGroup(groupName);
    }

    private void sendMessageToGroup() {
        listGroups();
        System.out.println("Ingresa el nombre del grupo:");
        String groupName = sc.nextLine();
        System.out.println("Ingresa tu mensaje:");
        String message = sc.nextLine();
        
        if (!groupName.isEmpty() && !message.isEmpty()) {
            controller.sendGroupMessage(groupName, message);
            System.out.println("Mensaje enviado al grupo " + groupName);
        } else {
            System.out.println("Grupo o mensaje no puede estar vacío.");
        }
    }

    private void sendVoiceToGroup() {
        listGroups();
        System.out.println("Ingresa el nombre del grupo:");
        String groupName = sc.nextLine();
        
        if (!groupName.isEmpty()) {
            controller.sendGroupVoiceNote(groupName);
            System.out.println("Iniciando nota de voz para grupo " + groupName + "...");
            System.out.println("(La grabación se maneja automáticamente por UDP)");
        } else {
            System.out.println("El nombre del grupo no puede estar vacío.");
        }
    }

    private void showUserMenu() {
        System.out.println(
                "\n============================="
            + "\n        MENU USUARIOS        "
            + "\n============================="
            + "\nSelecciona una opción:"
            + "\n   1. Listar Usuarios"
            + "\n   2. Llamar a Usuario"
            + "\n   3. Finalizar Llamada"
            + "\n   4. Enviar Mensaje a Usuario"
            + "\n   5. Enviar Nota de Voz a Usuario"
            + "\n   0. Volver"
            + "\n=============================");

        try {
            int option = sc.nextInt();
            
            switch (option) {
                case 1 -> listUsers();
                case 2 -> callUser();
                case 3 -> endCall();
                case 4 -> sendMessageToUser();
                case 5 -> sendVoiceToUser();
                case 0 -> System.out.println("Volviendo al menú principal...");
                default -> System.out.println("Opción inválida.");
            }
        } catch (InputMismatchException e) {
            System.out.println("Opción inválida.");
            sc.nextLine();
        }
    }

    private void listUsers() {
        controller.listUsers();
    }

    private void callUser() {
        listUsers();
        System.out.println("Ingresa el nombre de usuario a llamar:");
        String targetUser = sc.nextLine();
        controller.callUser(targetUser);
    }

    private void sendMessageToUser() {
        listUsers();
        System.out.println("Ingresa el nombre del usuario:");
        String targetUser = sc.nextLine();
        System.out.println("Ingresa tu mensaje:");
        String message = sc.nextLine();
        
        if (!targetUser.isEmpty() && !message.isEmpty()) {
            controller.sendMessage(targetUser, message);
            System.out.println("Mensaje enviado a " + targetUser);
        } else {
            System.out.println("Usuario o mensaje no puede estar vacío.");
        }
    }

    private void sendVoiceToUser() {
        listUsers();
        System.out.println("Ingresa el nombre del usuario:");
        String targetUser = sc.nextLine();
        
        if (!targetUser.isEmpty()) {
            controller.sendVoiceNote(targetUser);
            System.out.println("Iniciando nota de voz para " + targetUser + "...");
            System.out.println("(La grabación se maneja automáticamente por UDP)");
        } else {
            System.out.println("El nombre de usuario no puede estar vacío.");
        }
    }


    // Metodo compartido para finalizar llamadas
    private void endCall() {
        controller.endCall();
    }
}
