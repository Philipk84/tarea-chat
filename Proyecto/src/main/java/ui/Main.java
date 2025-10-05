package ui;

import controller.Controller;
import java.util.Scanner;

public class Main {
    private Scanner sc;
    private Controller controller;

    public Main() {
        this.sc = new Scanner(System.in);
        this.controller = new Controller();
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.showMenu();
    }

    private void showMenu() {
        boolean exit = false;

        do {
            System.out.println(
                "============================="
            + "\n       SISTEMA DE CHAT       "
            + "\n============================="
            + "\nSeleccione un modo:"
            + "\n1. Iniciar servidor"
            + "\n2. Iniciar cliente"
            + "\n0. Salir");

            try{
                switch (sc.nextInt()) {
                    case 1 -> startServer();
                    case 2 -> ChatClient.startClient();
                    case 0 -> {
                        System.out.println("Saliendo...");
                        exit = true;
                    }
                    default -> System.out.println("Opción no válida.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Opción no válida.");
                sc.nextLine();
            }
        } while (!exit);
    }

    private void startServer() {
        System.out.println(controller.startServer());
    }
}
