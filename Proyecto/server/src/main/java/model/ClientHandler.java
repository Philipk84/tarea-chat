package model;

import java.io.*;
import java.net.Socket;
import command.*;

/**
 * Manejador de cliente que procesa conexiones TCP y ejecuta comandos
 * de seÃ±alizaciÃ³n para llamadas y gestiÃ³n de grupos.
 * 
 * Cada cliente conectado tiene su propio hilo de ejecuciÃ³n que procesa
 * los comandos de manera asÃ­ncrona.
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final CommandRegistry commandRegistry;
    private BufferedReader in;
    private PrintWriter out;
    private String name;
    private boolean active = true;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.commandRegistry = new CommandRegistry();
        initializeCommands();
    }

    /**
     * Inicializa todos los comandos disponibles en el sistema.
     */
    private void initializeCommands() {
        commandRegistry.registerHandler(new UdpPortCommandHandler());
        commandRegistry.registerHandler(new CallCommandHandler());
        commandRegistry.registerHandler(new CallGroupCommandHandler());
        commandRegistry.registerHandler(new EndCallCommandHandler());
        commandRegistry.registerHandler(new CreateGroupCommandHandler());
        commandRegistry.registerHandler(new JoinGroupCommandHandler());
        commandRegistry.registerHandler(new ListGroupsCommandHandler());
        commandRegistry.registerHandler(new MessageCommandHandler());
        commandRegistry.registerHandler(new MessageGroupCommandHandler());
        commandRegistry.registerHandler(new VoiceNoteCommandHandler());
        commandRegistry.registerHandler(new VoiceGroupCommandHandler());
        commandRegistry.registerHandler(new QuitCommandHandler());
    }

    /**
     * EnvÃ­a un mensaje al cliente a travÃ©s del socket TCP.
     * 
     * @param message El mensaje a enviar
     */
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    /**
     * Obtiene el nombre del usuario asociado a este cliente.
     * 
     * @return El nombre del usuario
     */
    public String getName() {
        return name;
    }

    /**
     * Obtiene el socket TCP del cliente.
     * 
     * @return El socket del cliente
     */
    public Socket getClientSocket() {
        return socket;
    }

    /**
     * Hilo principal que maneja la comunicación con el cliente.
     * Procesa el registro del usuario y ejecuta comandos de manera continua.
     */
    @Override
    public void run() {
        try {
            setupClientConnection();
            handleUserRegistration();
            processUserCommands();
        } catch (IOException e) {
            System.err.println("Error del cliente " + name + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    /**
     * Configura las conexiones de entrada y salida con el cliente.
     * 
     * @throws IOException Si hay problemas con los streams
     */
    private void setupClientConnection() throws IOException {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    /**
     * Maneja el proceso de registro del usuario.
     * 
     * @throws IOException Si hay problemas de comunicación
     */
    private void handleUserRegistration() throws IOException {
        name = in.readLine();
        
        if (name == null || name.trim().isEmpty()) {
            out.println("Error: Nombre inválido");
            active = false;
            return;
        }
        
        name = name.trim();
        ChatServer.registerUser(name, this);
        out.println("¡Bienvenido, " + name + "!");        
    }

    /**
     * Procesa los comandos enviados por el usuario de manera continua.
     * 
     * @throws IOException Si hay problemas de comunicación
     */
    private void processUserCommands() throws IOException {
        String line;
        while (active && (line = in.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }
            
            if (line.equals("/quit")) {
                commandRegistry.executeCommand(line, name, this);
                active = false;
                break;
            }
            
            if (!commandRegistry.executeCommand(line, name, this)) {
                out.println("Opción inválida.");
            }
        }
    }

    /**
     * Limpia los recursos al finalizar la conexión del cliente.
     */
    private void cleanup() {
        if (name != null) {
            ChatServer.removeUser(name);
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error cerrando socket: " + e.getMessage());
        }
    }
}
