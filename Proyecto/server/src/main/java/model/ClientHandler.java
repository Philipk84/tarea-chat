package model;

import java.io.*;
import java.net.Socket;
import command.*;
import service.HistoryService;

/**
 * Manejador de cliente que procesa conexiones TCP y ejecuta comandos
 * de señalización para llamadas, grupos y notas de voz.
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final CommandRegistry commandRegistry;
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
    }

    /**
     * Envía un mensaje de texto al cliente.
     */
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
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
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    private void handleUserRegistration() throws IOException {
        name = readLineFromInputStream(socket.getInputStream());
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
        while (active && (line = readLineFromInputStream(socket.getInputStream())) != null) {

            if (line.trim().isEmpty()) continue;

            // Detección de inicio de nota de voz TCP
            if (line.startsWith("VOICE_NOTE_START") || line.startsWith("VOICE_NOTE_GROUP_START")) {
                processVoiceNote(socket.getInputStream(), line);
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
     * Procesa una nota de voz entrante (modo binario).
     * @param inputStream Flujo de entrada del socket
     * @param header Línea de encabezado "VOICE_NOTE_START <sender> <size>" o "VOICE_NOTE_GROUP_START <sender> <group> <size>"
     */
    private void processVoiceNote(InputStream inputStream, String header) {
        try {
            String[] parts = header.split(" ");
            if (parts.length < 3) {
                sendMessage("Error: encabezado de nota de voz inválido");
                return;
            }

            if (header.startsWith("VOICE_NOTE_GROUP_START")) {
                // Cliente -> Servidor: VOICE_NOTE_GROUP_START <grupo> <tamaño>
                String groupName = parts[1];
                long size = Long.parseLong(parts[2]);

                java.util.Set<String> members = ChatServer.getGroupMembers(groupName);
                if (members == null || members.isEmpty()) {
                    sendMessage("Error: grupo '" + groupName + "' no existe o está vacío");
                    skipBytes(inputStream, size);
                    skipLine();
                    return;
                }

                java.util.List<ClientHandler> recipients = new java.util.ArrayList<>();
                for (String m : members) {
                    if (!m.equals(name)) {
                        ClientHandler ch = ChatServer.getClientHandler(m);
                        if (ch != null) recipients.add(ch);
                    }
                }

                // Enviar encabezado a todos: VOICE_NOTE_GROUP_START <remitente> <grupo> <tamaño>
                String outHeader = "VOICE_NOTE_GROUP_START " + name + " " + groupName + " " + size + "\n";
                byte[] outHeaderBytes = outHeader.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                for (ClientHandler ch : recipients) {
                    ch.socket.getOutputStream().write(outHeaderBytes);
                }

                // Reenviar bytes y capturarlos para guardar en historial
                byte[] captured = teeForwardBytesToRecipients(inputStream, size, recipients);

                // Leer y reenviar fin
                String end = readLineFromInputStream(inputStream);
                if (end == null || !end.equals("VOICE_NOTE_GROUP_END")) {
                    System.err.println("Advertencia: VOICE_NOTE_GROUP_END no detectado correctamente");
                }
                byte[] endBytes = "VOICE_NOTE_GROUP_END\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                for (ClientHandler ch : recipients) {
                    ch.socket.getOutputStream().write(endBytes);
                    ch.socket.getOutputStream().flush();
                }

                // Guardar y registrar en historial
                try {
                    HistoryService.SavedAudio saved = HistoryService.saveVoiceBytes(captured);
                    HistoryService.logVoiceGroup(name, groupName, saved.relativePath(), saved.sizeBytes());
                } catch (IOException ioe) {
                    System.err.println("No se pudo guardar nota de voz grupal: " + ioe.getMessage());
                }
                sendMessage("Nota de voz grupal enviada a '" + groupName + "'.");

            } else if (header.startsWith("VOICE_NOTE_START")) {
                // Cliente -> Servidor: VOICE_NOTE_START <destino> <tamaño>
                String targetUser = parts[1];
                long size = Long.parseLong(parts[2]);

                ClientHandler target = ChatServer.getClientHandler(targetUser);
                if (target == null) {
                    sendMessage("Error: Usuario '" + targetUser + "' no está conectado");
                    skipBytes(inputStream, size);
                    skipLine();
                    return;
                }

                // Encabezado para destinatario: VOICE_NOTE_START <remitente> <tamaño>
                String outHeader = "VOICE_NOTE_START " + name + " " + size + "\n";
                OutputStream out = target.socket.getOutputStream();
                out.write(outHeader.getBytes(java.nio.charset.StandardCharsets.UTF_8));

                // Reenviar bytes y capturarlos para guardar en historial
                byte[] captured = pipeAndCaptureBytes(inputStream, out, size);

                // Leer end del emisor y reenviar
                String end = readLineFromInputStream(inputStream);
                if (end == null || !end.equals("VOICE_NOTE_END")) {
                    System.err.println("Advertencia: VOICE_NOTE_END no detectado correctamente");
                }
                out.write("VOICE_NOTE_END\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.flush();

                // Guardar y registrar en historial
                try {
                    HistoryService.SavedAudio saved = HistoryService.saveVoiceBytes(captured);
                    HistoryService.logVoiceNote(name, targetUser, saved.relativePath(), saved.sizeBytes());
                } catch (IOException ioe) {
                    System.err.println("No se pudo guardar nota de voz: " + ioe.getMessage());
                }
                sendMessage("Nota de voz enviada a " + targetUser);
            } else {
                sendMessage("Error: encabezado de nota de voz no reconocido");
            }

        } catch (Exception e) {
            System.err.println("Error procesando nota de voz: " + e.getMessage());
        }
    }

    

    // Variante que reenvía y además captura los bytes en memoria para guardarlos
    private byte[] pipeAndCaptureBytes(InputStream in, OutputStream out, long size) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream((int)Math.min(size, 1024 * 1024));
        byte[] buffer = new byte[4096];
        long remaining = size;
        while (remaining > 0) {
            int n = in.read(buffer, 0, (int)Math.min(buffer.length, remaining));
            if (n == -1) break;
            out.write(buffer, 0, n);
            baos.write(buffer, 0, n);
            remaining -= n;
        }
        return baos.toByteArray();
    }

    private byte[] teeForwardBytesToRecipients(InputStream in, long size, java.util.List<ClientHandler> recipients) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream((int)Math.min(size, 1024 * 1024));
        byte[] buffer = new byte[4096];
        long remaining = size;
        while (remaining > 0) {
            int n = in.read(buffer, 0, (int)Math.min(buffer.length, remaining));
            if (n == -1) break;
            for (ClientHandler ch : recipients) {
                ch.socket.getOutputStream().write(buffer, 0, n);
            }
            baos.write(buffer, 0, n);
            remaining -= n;
        }
        return baos.toByteArray();
    }

    private void skipBytes(InputStream in, long size) throws IOException {
        long remaining = size;
        byte[] buffer = new byte[4096];
        while (remaining > 0) {
            int n = in.read(buffer, 0, (int)Math.min(buffer.length, remaining));
            if (n == -1) break;
            remaining -= n;
        }
    }

    private void skipLine() {
        // Intentar consumir una línea residual si existe
        try {
            readLineFromInputStream(socket.getInputStream());
        } catch (IOException ignored) {}
    }

    private String readLineFromInputStream(InputStream inputStream) throws IOException {
        // Leer bytes hasta \n y construir String (UTF-8)
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        int b;
        while ((b = inputStream.read()) != -1) {
            if (b == '\n') break;
            if (b != '\r') baos.write(b);
        }
        if (baos.size() == 0 && b == -1) return null;
        return baos.toString(java.nio.charset.StandardCharsets.UTF_8);
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
