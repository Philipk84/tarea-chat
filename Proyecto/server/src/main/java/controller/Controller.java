package controller;

import model.*;
import com.google.gson.Gson;
import java.io.FileReader;

/**
 * Controlador principal del servidor de chat.
 * ActÃºa como intermediario entre la interfaz de usuario y el modelo del servidor.
 * Maneja la configuracion y las operaciones del servidor de chat.
 */
public class Controller {
    private final ChatServer chatServer;

    public Controller() {
        Gson gson = new Gson();
        try {
            Config config = gson.fromJson(new FileReader("Proyecto\\config.json"), Config.class);
            this.chatServer = new ChatServer(config);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing controller", e);
        }
    }

    /**
     * Inicia el servidor de chat.
     * 
     * @return Mensaje de estado del resultado de la operacion
     */
    public String startServer() {
        return chatServer.startServer();
    }

    /**
     * Cierra el servidor de chat.
     * 
     * @return Mensaje de estado del resultado de la operacion
     */
    public String closeServer() {
        return chatServer.closeServer();
    }

    /**
     * Verifica si el servidor de chat está en ejecución.
     * 
     * @return true si el servidor está en ejecución, false en caso contrario
     */
    public boolean isServerRunning() {
        return chatServer.isRunning();
    }
}
