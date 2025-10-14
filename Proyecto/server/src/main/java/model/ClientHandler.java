package model;

import java.io.*;
import java.net.Socket;
import command.*;

/**
 * Manejador de cliente que procesa conexiones TCP (texto)
 * y mantiene un canal de voz independiente.
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final CommandRegistry commandRegistry;
    private BufferedReader in;
    private PrintWriter out;

    private String name;
    private boolean active = true;

    // Canal de voz separado
    private VoiceChannelHandler voiceChannel;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.commandRegistry = new CommandRegistry();
        initializeCommands();
    }

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

    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    @Override
    public void run() {
        try {
            setupClientConnection();
            handleUserRegistration();
            startVoiceChannel(); // 🔹 canal de voz

            listenForMessages();

        } catch (IOException e) {
            System.err.println("Error con cliente " + name + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void setupClientConnection() throws IOException {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    private void handleUserRegistration() throws IOException {
        out.println("Ingresa tu nombre:");
        name = in.readLine();
        if (name == null || name.trim().isEmpty()) {
            out.println("Nombre inválido");
            active = false;
            return;
        }
        name = name.trim();
        ChatServer.registerUser(name, this);
        out.println("¡Bienvenido, " + name + "!");
    }

    private void listenForMessages() throws IOException {
        String line;
        while (active && (line = in.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            if (line.equals("/quit")) {
                commandRegistry.executeCommand(line, name, this);
                active = false;
                break;
            }

            if (!commandRegistry.executeCommand(line, name, this)) {
                out.println("Comando no reconocido.");
            }
        }
    }

    /** Inicia el canal de voz con un socket ya conectado */
    private void startVoiceChannel() {
        try {
            // 🔹 Para pruebas locales usamos localhost y puerto fijo 5002
            Socket voiceSocket = new Socket("localhost", 5002);
            voiceChannel = new VoiceChannelHandler(voiceSocket, name);
            new Thread(voiceChannel).start();
            System.out.println("🎧 Canal de voz iniciado para " + name);
        } catch (IOException e) {
            System.err.println("❌ No se pudo iniciar el canal de voz de " + name);
        }
    }

    public void sendVoiceNote(VoiceNote note) {
        if (voiceChannel != null) voiceChannel.sendVoice(note);
    }

    private void cleanup() {
        active = false;
        ChatServer.removeUser(name);
        try {
            if (socket != null && !socket.isClosed()) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
            if (voiceChannel != null) voiceChannel.shutdown();
        } catch (IOException ignored) {}
    }

    public String getName() { return name; }
    public Socket getSocket() { return socket; }
}
