package ui;

import controller.Controller;
import java.util.Scanner;
import java.util.InputMismatchException;

public class Main {
    private Scanner sc;
    private Controller controller;

    public Main() {
        this.sc = new Scanner(System.in);
        this.controller = new Controller();
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.login();
    }

    private void login() {
        System.out.println("Ingrese su nombre de usuario:");
        String username = sc.nextLine();
        showMenu(username);
    }

    private void showMenu(String username) {
        boolean exit = false;

        do {
            System.out.println(
                "============================="
            + "\n       SISTEMA DE CHAT       "
            + "\n============================="
            + "\nSeleccione un modo:"
            + "\n1. Crear Grupo"
            + "\n2. Enviar Mensaje Privado"
            + "\n3. Enviar Nota de Voz"
            + "\n4. Llamar"
            + "\n0. Salir");

            try{
                switch (sc.nextInt()) {
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
}