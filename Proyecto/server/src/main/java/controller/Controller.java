package controller;

import model.*;
import com.google.gson.Gson;
import java.io.FileReader;

/**
 * Controlador principal del servidor de chat.
 * ActÃºa como intermediario entre la interfaz de usuario y el modelo del servidor.
 * Maneja la configuraciÃ³n y las operaciones del servidor de chat.
 */
public class Controller {
    private Gson gson;
    private Config config;
    private ChatServer chatServer;

    public Controller() {
        this.gson = new Gson();
        try {
            this.config = gson.fromJson(new FileReader("Proyecto\\config.json"), Config.class);
            this.chatServer = new ChatServer(config);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing controller", e);
        }
    }

    /**
     * Inicia el servidor de chat.
     * 
     * @return Mensaje de estado del resultado de la operaciÃ³n
     */
    public String startServer() {
        return chatServer.startServer();
    }

    /**
     * Cierra el servidor de chat.
     * 
     * @return Mensaje de estado del resultado de la operaciÃ³n
     */
    public String closeServer() {
        return chatServer.closeServer();
    }

    /**
     * Verifica si el servidor de chat estÃ¡ en ejecuciÃ³n.
     * 
     * @return true si el servidor estÃ¡ en ejecuciÃ³n, false en caso contrario
     */
    public boolean isServerRunning() {
        return chatServer.isRunning();
    }
}
