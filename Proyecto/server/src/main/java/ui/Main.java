package ui;

import controller.Controller;
import java.util.Scanner;
import java.util.InputMismatchException;

/**
 * Clase principal de la interfaz de usuario para el servidor de chat.
 * Proporciona un menú de consola para controlar el servidor de chat.
 */
public class Main {
    private final Scanner sc;
    private final Controller controller;

    public Main() {
        this.sc = new Scanner(System.in);
        this.controller = new Controller();
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.showMenu();
    }

    /**
     * Muestra el menú principal y maneja las opciones del usuario.
     * Continúa ejecutándose hasta que el usuario selecciona salir.
     */
    private void showMenu() {
        boolean exit = false;

        do {
            System.out.println(
            "\n============================="
            + "\n     SERVIDOR SISTEMA CHAT    "
            + "\n============================="
            + "\nSelecciona una opción:"
            + "\n1. Iniciar servidor"
            + "\n2. Cerrar servidor"
            + "\n0. Salir");

            try{
                switch (sc.nextInt()) {
                    case 1 -> startServer();
                    case 2 -> closeServer();
                    case 0 -> {
                        System.out.println("Saliendo...");
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

    /**
     * Inicia el servidor de chat a través del controlador.
     */
    private void startServer() {
        System.out.println("\n" + controller.startServer());
    }

    /**
     * Cierra el servidor de chat a través del controlador.
     */
    private void closeServer() {
        if (!controller.isServerRunning()) System.out.println("\n El servidor no está en ejecución.");
        else System.out.println("\n" + controller.closeServer());
    }
}
