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
                + "\n1. Grupos"
                + "\n2. Usuarios"
                + "\n3. Mensajes"
                + "\n4. Notas de Voz"
                + "\n5. Ayuda"
                + "\n0. Salir"
                + "\n=============================");

            try {
                int option = sc.nextInt();
                sc.nextLine(); // consume newline
                
                switch (option) {
                    case 1 -> showGroupMenu();
                    case 2 -> showUserMenu();
                    case 3 -> showMessageMenu();
                    case 4 -> showVoiceMenu();
                    case 5 -> showHelp();
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
            + "\n         MENÚ GRUPOS         "
            + "\n============================="
            + "\nSelecciona una opción:"
            + "\n   1. Listar Grupos"
            + "\n   2. Crear Grupo"
            + "\n   3. Unirse a Grupo"
            + "\n   4. Llamar a Grupo"
            + "\n   0. Volver"
            + "\n=============================");

        try {
            int option = sc.nextInt();
            sc.nextLine(); // consume newline
            
            switch (option) {
                case 1 -> listGroups();
                case 2 -> createGroup();
                case 3 -> joinGroup();
                case 4 -> callGroup();
                case 0 -> System.out.println("Volviendo al menú principal...");
                default -> System.out.println("Opción inválida.");
            }
        } catch (InputMismatchException e) {
            System.out.println("Opción inválida.");
            sc.nextLine();
        }
    }

    private void showUserMenu() {
        System.out.println(
                "\n============================="
            + "\n        MENÚ USUARIOS        "
            + "\n============================="
            + "\nSelecciona una opción:"
            + "\n   1. Llamar a Usuario"
            + "\n   2. Terminar Llamada"
            + "\n   0. Volver"
            + "\n=============================");

        try {
            int option = sc.nextInt();
            sc.nextLine(); // consume newline
            
            switch (option) {
                case 1 -> callUser();
                case 2 -> endCall();
                case 0 -> System.out.println("Volviendo al menú principal...");
                default -> System.out.println("Opción inválida.");
            }
        } catch (InputMismatchException e) {
            System.out.println("Opción inválida.");
            sc.nextLine();
        }
    }

    private void showMessageMenu() {
        System.out.println(
                "\n============================="
            + "\n        MENÚ MENSAJES        "
            + "\n============================="
            + "\nSelecciona una opción:"
            + "\n   1. Enviar Mensaje a Usuario"
            + "\n   2. Enviar Mensaje a Grupo"
            + "\n   0. Volver"
            + "\n=============================");

        try {
            int option = sc.nextInt();
            sc.nextLine(); // consume newline
            
            switch (option) {
                case 1 -> sendMessageToUser();
                case 2 -> sendMessageToGroup();
                case 0 -> System.out.println("Volviendo al menú principal...");
                default -> System.out.println("Opción inválida.");
            }
        } catch (InputMismatchException e) {
            System.out.println("Opción inválida.");
            sc.nextLine();
        }
    }

    private void showVoiceMenu() {
        System.out.println(
                "\n============================="
            + "\n      MENÚ NOTAS DE VOZ      "
            + "\n============================="
            + "\nSelecciona una opción:"
            + "\n   1. Enviar Nota de Voz a Usuario"
            + "\n   2. Enviar Nota de Voz a Grupo"
            + "\n   0. Volver"
            + "\n=============================");

        try {
            int option = sc.nextInt();
            sc.nextLine(); // consume newline
            
            switch (option) {
                case 1 -> sendVoiceToUser();
                case 2 -> sendVoiceToGroup();
                case 0 -> System.out.println("Volviendo al menú principal...");
                default -> System.out.println("Opción inválida.");
            }
        } catch (InputMismatchException e) {
            System.out.println("Opción inválida.");
            sc.nextLine();
        }
    }

    private void showHelp() {
        System.out.println(
                "\n============================="
            + "\n         AYUDA - COMANDOS    "
            + "\n============================="
            + "\n📝 GRUPOS:"
            + "\n   /creategroup <nombre>    - Crear grupo"
            + "\n   /joingroup <nombre>      - Unirse a grupo"
            + "\n   /listgroups              - Listar grupos"
            + "\n"
            + "\n💬 MENSAJES (TCP - confiable):"
            + "\n   /msg <usuario> <mensaje> - Mensaje privado"
            + "\n   /msggroup <grupo> <mensaje> - Mensaje grupal"
            + "\n"
            + "\n🎤 NOTAS DE VOZ (UDP - eficiente):"
            + "\n   /voice <usuario>         - Nota de voz privada"
            + "\n   /voicegroup <grupo>      - Nota de voz grupal"
            + "\n"
            + "\n📞 LLAMADAS (TCP + UDP):"
            + "\n   /call <usuario>          - Llamar usuario"
            + "\n   /callgroup <grupo>       - Llamar grupo"
            + "\n   /endcall                 - Terminar llamada"
            + "\n"
            + "\n🔧 OTROS:"
            + "\n   /quit                    - Desconectar"
            + "\n=============================");
        
        System.out.println("\nPresiona ENTER para continuar...");
        sc.nextLine();
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

    private void sendMessageToUser() {
        System.out.println("Ingresa el nombre del usuario:");
        String targetUser = sc.nextLine();
        System.out.println("Ingresa tu mensaje:");
        String message = sc.nextLine();
        
        if (!targetUser.isEmpty() && !message.isEmpty()) {
            controller.sendMessage(targetUser, message);
            System.out.println("✅ Mensaje enviado a " + targetUser);
        } else {
            System.out.println("❌ Usuario o mensaje no puede estar vacío.");
        }
    }

    private void sendMessageToGroup() {
        System.out.println("Ingresa el nombre del grupo:");
        String groupName = sc.nextLine();
        System.out.println("Ingresa tu mensaje:");
        String message = sc.nextLine();
        
        if (!groupName.isEmpty() && !message.isEmpty()) {
            controller.sendGroupMessage(groupName, message);
            System.out.println("✅ Mensaje enviado al grupo " + groupName);
        } else {
            System.out.println("❌ Grupo o mensaje no puede estar vacío.");
        }
    }

    private void sendVoiceToUser() {
        System.out.println("Ingresa el nombre del usuario:");
        String targetUser = sc.nextLine();
        
        if (!targetUser.isEmpty()) {
            controller.sendVoiceNote(targetUser);
            System.out.println("🎤 Iniciando nota de voz para " + targetUser + "...");
            System.out.println("(La grabación se maneja automáticamente por UDP)");
        } else {
            System.out.println("❌ El nombre de usuario no puede estar vacío.");
        }
    }

    private void sendVoiceToGroup() {
        System.out.println("Ingresa el nombre del grupo:");
        String groupName = sc.nextLine();
        
        if (!groupName.isEmpty()) {
            controller.sendGroupVoiceNote(groupName);
            System.out.println("🎤 Iniciando nota de voz para grupo " + groupName + "...");
            System.out.println("(La grabación se maneja automáticamente por UDP)");
        } else {
            System.out.println("❌ El nombre del grupo no puede estar vacío.");
        }
    }
}
