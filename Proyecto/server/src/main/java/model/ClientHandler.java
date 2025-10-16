package model;

import java.io.*;
import java.net.Socket;
import java.util.Base64;
import command.*;

/**
 * Manejador de cliente que procesa conexiones TCP y ejecuta comandos
 * de señalización para llamadas, grupos y notas de voz.
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
        commandRegistry.registerHandler(new CallCommandHandler());
        commandRegistry.registerHandler(new CallGroupCommandHandler());
        commandRegistry.registerHandler(new CreateGroupCommandHandler());
        commandRegistry.registerHandler(new EndCallCommandHandler());
        commandRegistry.registerHandler(new JoinGroupCommandHandler());
        commandRegistry.registerHandler(new ListGroupsCommandHandler());
        commandRegistry.registerHandler(new ListUsersCommandHandler());
        commandRegistry.registerHandler(new MessageCommandHandler());
        commandRegistry.registerHandler(new MessageGroupCommandHandler());
        commandRegistry.registerHandler(new QuitCommandHandler());
        commandRegistry.registerHandler(new UdpPortCommandHandler());
        commandRegistry.registerHandler(new VoiceGroupCommandHandler());
        commandRegistry.registerHandler(new VoiceNoteCommandHandler());
    }

    /**
     * Envía un mensaje de texto al cliente.
     */
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    /**
     * Envía una nota de voz (archivo binario) al cliente destino.
     */
    public void sendVoiceNote(String sender, File audioFile) {
        try {
            OutputStream out = socket.getOutputStream();
            long fileSize = audioFile.length();

            // Enviar encabezado
            String header = "VOICE_NOTE_START " + sender + " " + fileSize + "\n";
            out.write(header.getBytes(StandardCharsets.UTF_8));

            // Enviar el archivo binario
            try (FileInputStream fis = new FileInputStream(audioFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            // Enviar finalización
            out.write("\nVOICE_NOTE_END\n".getBytes(StandardCharsets.UTF_8));
            out.flush();

        } catch (IOException e) {
            System.err.println("Error enviando nota de voz a " + name + ": " + e.getMessage());
        }
    }


    public String getName() {
        return name;
    }

    public Socket getClientSocket() {
        return socket;
    }

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

    private void setupClientConnection() throws IOException {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

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

    private void processUserCommands() throws IOException {
        String line;
        while (active && (line = in.readLine()) != null) {

            // Detección de inicio de nota de voz TCP
            if (line.startsWith("VOICE_NOTE_START")) {
                processVoiceNote(socket.getInputStream(), line);
                continue;
            }

            if (line.trim().isEmpty()) continue;

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
     * Procesa una nota de voz entrante (modo binario).
     * @param inputStream Flujo de entrada del socket
     * @param header Línea de encabezado "VOICE_NOTE_START <sender> <size>"
     */
    private void processVoiceNote(InputStream inputStream, String header) {
        try {
            String[] parts = header.split(" ");
            if (parts.length < 3) {
                sendMessage("Error: encabezado de nota de voz inválido");
                return;
            }

            String sender = parts[1];
            long fileSize = Long.parseLong(parts[2]);

            File receivedFile = new File("voice_from_" + sender + ".wav");
            try (FileOutputStream fos = new FileOutputStream(receivedFile)) {
                byte[] buffer = new byte[4096];
                long remaining = fileSize;
                while (remaining > 0) {
                    int bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (bytesRead == -1) break;
                    fos.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
            }

            System.out.println("Nota de voz recibida de " + sender + " (" + fileSize + " bytes)");
            sendMessage("Nota de voz recibida de " + sender + " (" + fileSize + " bytes)");

        } catch (Exception e) {
            System.err.println("Error procesando nota de voz: " + e.getMessage());
        }
    }

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
