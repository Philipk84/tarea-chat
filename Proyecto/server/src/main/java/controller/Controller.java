package controller;

import model.*;
import com.google.gson.Gson;
import java.io.FileReader;

/**
 * Controlador principal del servidor de chat.
 * Actúa como intermediario entre la interfaz de usuario y el modelo del servidor.
 * Maneja la configuración y las operaciones del servidor de chat.
 */
public class Controller {
    private Gson gson;
    private Config config;
    private ChatServer chatServer;

    public Controller() {
        this.gson = new Gson();
        try {
            this.config = gson.fromJson(new FileReader("config.json"), Config.class);
            this.chatServer = new ChatServer(config);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing controller", e);
        }
    }

    /**
     * Inicia el servidor de chat.
     * 
     * @return Mensaje de estado del resultado de la operación
     */
    public String startServer() {
        return chatServer.startServer();
    }

    /**
     * Cierra el servidor de chat.
     * 
     * @return Mensaje de estado del resultado de la operación
     */
    public String closeServer() {
        return chatServer.closeServer();
    }
}
