package model;

import java.io.*;
import java.net.Socket;
import command.*;

/**
 * Manejador de cliente que procesa conexiones TCP (texto)
 * y mantiene un canal de voz independiente.
 *
 * Este handler maneja:
 *  🔹 Comandos de texto por TCP
 *  🔹 Reenvío de notas de voz (binarias)
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final CommandRegistry commandRegistry;
    private BufferedReader in;
    private PrintWriter out;

    // Flujos binarios para notas de voz
    private DataInputStream dataIn;
    private DataOutputStream dataOut;

    private String name;
    private boolean active = true;

    // Canal de voz separado (para futuras llamadas)
    private VoiceChannelHandler voiceChannel;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.commandRegistry = new CommandRegistry();
        initializeCommands();
    }

    /** Inicializa los comandos disponibles en el sistema. */
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

    /** Envía un mensaje de texto al cliente. */
    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    /** Envía una nota de voz binaria al cliente. */
    public synchronized void sendVoiceNote(String fromUser, byte[] audioData) {
        try {
            if (dataOut == null) return;
            dataOut.writeUTF("VOICE_NOTE_FROM:" + fromUser);
            dataOut.writeInt(audioData.length);
            dataOut.write(audioData);
            dataOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getName() { return name; }
    public Socket getClientSocket() { return socket; }

    @Override
    public void run() {
        try {
            setupClientConnection();
            handleUserRegistration();
            startVoiceChannel();

            // 🔹 Escucha comandos de texto y voz
            listenForMessages();

        } catch (IOException e) {
            System.err.println("Error con cliente " + name + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    /** Configura los streams de texto y binarios. */
    private void setupClientConnection() throws IOException {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        dataIn = new DataInputStream(socket.getInputStream());
        dataOut = new DataOutputStream(socket.getOutputStream());
    }

    /** Maneja el registro del usuario. */
    private void handleUserRegistration() throws IOException {
        out.println("Ingresa tu nombre:");
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

    /** Escucha comandos del cliente y posibles notas de voz. */
    private void listenForMessages() throws IOException {
        String line;
        while (active && (line = in.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            // Comando de salida
            if (line.equals("/quit")) {
                commandRegistry.executeCommand(line, name, this);
                active = false;
                break;
            }

            // 🔊 Nota de voz (binaria)
            if (line.startsWith("VOICE_NOTE:")) {
                String[] parts = line.split(":", 4);
                String from = parts[1];
                String to = parts[2];
                boolean isGroup = Boolean.parseBoolean(parts[3]);

                int length = dataIn.readInt();
                byte[] audioData = new byte[length];
                dataIn.readFully(audioData);

                ChatServer.handleVoiceNote(from, to, audioData, isGroup);
                continue;
            }

            // Otros comandos
            if (!commandRegistry.executeCommand(line, name, this)) {
                out.println("Comando no reconocido.");
            }
        }
    }

    /** Inicia el canal de voz en un puerto fijo (5555). */
    private void startVoiceChannel() {
        int voicePort = 5001; // 🔹 Puerto fijo
        voiceChannel = new VoiceChannelHandler(name, voicePort);
        voiceChannel.start();
        System.out.println("🎧 Canal de voz del servidor iniciado en puerto " + voicePort);
    }


    /** Limpia los recursos al finalizar. */
    private void cleanup() {
        active = false;
        ChatServer.removeUser(name);
        try {
            if (socket != null && !socket.isClosed()) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
            if (dataIn != null) dataIn.close();
            if (dataOut != null) dataOut.close();
            if (voiceChannel != null) voiceChannel.shutdown();
        } catch (IOException e) {
            System.err.println("Error cerrando recursos de " + name + ": " + e.getMessage());
        }
    }
}
