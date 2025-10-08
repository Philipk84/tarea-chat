package controller;

import model.*;
import com.google.gson.Gson;
import java.io.FileReader;

/**
 * Controlador principal del cliente de chat.
 * Actúa como intermediario entre la interfaz de usuario y el modelo del cliente.
 * Maneja la configuración, conexión al servidor y ejecución de comandos de chat.
 * Proporciona una API simplificada para todas las operaciones del cliente.
 */
public class Controller {
    private Gson gson;
    private Config config;
    private ChatClient chatClient;

    public Controller() {
        this.gson = new Gson();
        try {
            this.config = gson.fromJson(new FileReader("Proyecto\\config.json"), Config.class);
            this.chatClient = new ChatClient(config);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing controller", e);
        }
    }

    /**
     * Establece conexión con el servidor de chat usando el nombre de usuario especificado.
     * 
     * @param username Nombre de usuario para la conexión
     * @return Mensaje de estado del resultado de la conexión
     */
    public String connectToServer(String username) {
        return chatClient.connect(username);
    }

    /**
     * Envía un comando genérico al servidor de chat.
     * 
     * @param command Comando completo a enviar al servidor
     */
    public void sendCommand(String command) {
        chatClient.sendCommand(command);
    }

    /**
     * Desconecta el cliente del servidor de chat.
     * Cierra todas las conexiones activas y libera recursos.
     */
    public void disconnect() {
        chatClient.disconnect();
    }

    /**
     * Verifica si el cliente está actualmente conectado al servidor.
     * 
     * @return true si está conectado, false en caso contrario
     */
    public boolean isConnected() {
        return chatClient.isConnected();
    }

    /**
     * Crea un nuevo grupo de chat con el nombre especificado.
     * El usuario actual se convierte automáticamente en miembro del grupo.
     * 
     * @param groupName Nombre del grupo a crear
     */
    public void createGroup(String groupName) {
        chatClient.sendCommand("/creategroup " + groupName);
    }

    /**
     * Se une a un grupo de chat existente.
     * 
     * @param groupName Nombre del grupo al que unirse
     */
    public void joinGroup(String groupName) {
        chatClient.sendCommand("/joingroup " + groupName);
    }

    /**
     * Solicita al servidor la lista de todos los grupos disponibles.
     * La respuesta se mostrará a través del manejador de mensajes del cliente.
     */
    public void listGroups() {
        chatClient.sendCommand("/listgroups");
    }

    /**
     * Inicia una llamada individual con otro usuario.
     * Ambos usuarios deben estar conectados y tener información UDP registrada.
     * 
     * @param username Nombre del usuario a llamar
     */
    public void callUser(String username) {
        chatClient.sendCommand("/call " + username);
    }

    /**
     * Inicia una llamada grupal con todos los miembros conectados de un grupo.
     * Solo los miembros con información UDP registrada participarán en la llamada.
     * 
     * @param groupName Nombre del grupo a llamar
     */
    public void callGroup(String groupName) {
        chatClient.sendCommand("/callgroup " + groupName);
    }

    /**
     * Termina la llamada actual en la que participa el usuario.
     * Notifica a todos los participantes que la llamada ha finalizado.
     */
    public void endCall() {
        chatClient.sendCommand("/endcall");
    }
}
