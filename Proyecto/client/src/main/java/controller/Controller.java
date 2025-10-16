package controller;

import model.*;
import com.google.gson.Gson;
import java.io.FileReader;

/**
 * Controlador principal del cliente de chat.
 * Actua como intermediario entre la interfaz de usuario y el modelo del cliente.
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
     * Establece conexiÃ³n con el servidor de chat usando el nombre de usuario especificado.
     * 
     * @param username Nombre de usuario para la conexiÃ³n
     * @return Mensaje de estado del resultado de la conexiÃ³n
     */
    public String connectToServer(String username) {
        return chatClient.connect(username);
    }

    /**
     * EnvÃ­a un comando genÃ©rico al servidor de chat.
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
     * Verifica si el cliente estÃ¡ actualmente conectado al servidor.
     * 
     * @return true si estÃ¡ conectado, false en caso contrario
     */
    public boolean isConnected() {
        return chatClient.isConnected();
    }

    /**
     * Crea un nuevo grupo de chat con el nombre especificado.
     * El usuario actual se convierte automÃ¡ticamente en miembro del grupo.
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
     * Ambos usuarios deben estar conectados y tener informaciÃ³n UDP registrada.
     * 
     * @param username Nombre del usuario a llamar
     */
    public void callUser(String username) {
        chatClient.sendCommand("/call " + username);
    }

    /**
     * Inicia una llamada grupal con todos los miembros conectados de un grupo.
     * Solo los miembros con informaciÃ³n UDP registrada participarÃ¡n en la llamada.
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

    /**
     * Envía un mensaje de texto privado a un usuario específico usando TCP.
     * El mensaje se entrega de manera confiable al destinatario.
     *
     * @param username Nombre del usuario destinatario
     * @param message Contenido del mensaje a enviar
     */
    public void sendMessage(String username, String message) {
        chatClient.sendCommand("/msg " + username + " " + message);
    }

    /**
     * Envía un mensaje de texto a todos los miembros de un grupo usando TCP.
     * El mensaje se entrega de manera confiable a todos los miembros conectados.
     *
     * @param groupName Nombre del grupo destinatario
     * @param message Contenido del mensaje a enviar
     */
    public void sendGroupMessage(String groupName, String message) {
        chatClient.sendCommand("/msggroup " + groupName + " " + message);
    }

    /**
     * Inicia el envÃ­o de una nota de voz a un usuario especÃ­fico.
     * La nota de voz se transmite vÃ­a UDP para eficiencia.
     * 
     * @param username Nombre del usuario destinatario
     */
    public void sendVoiceNote(String username) {
        chatClient.startVoiceNoteToUser(username);
    }

    /**
     * Inicia el envÃ­o de una nota de voz a todos los miembros de un grupo.
     * La nota de voz se transmite vÃ­a UDP para eficiencia.
     * 
     * @param groupName Nombre del grupo destinatario
     */
    public void sendGroupVoiceNote(String groupName) {
        chatClient.startVoiceNoteToGroup(groupName);
    }

    /**
     * Detiene la grabación de nota de voz y la envía al destino configurado.
     */
    public void stopAndSendVoiceNote() {
        chatClient.stopAndSendVoiceNote();
    }

    /**
     * Solicita al servidor la lista de todos los usuarios actualmente conectados.
     * La respuesta se mostrará a través del manejador de mensajes del cliente.
     */
    public void listUsers() {
        chatClient.sendCommand("/listusers");
    }
}
