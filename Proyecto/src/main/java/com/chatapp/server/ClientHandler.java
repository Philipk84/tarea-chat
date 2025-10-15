package com.chatapp.server;

import java.io.*;
import java.net.Socket;
import java.util.Set;

/**
 * Hilo que maneja a un cliente conectado.
 * Interpreta comandos, interactúa con el servidor y gestiona chats.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String nombre;
    private boolean activo = true;
    private String grupoActual = null;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void enviarMensaje(String mensaje) {
        out.println(mensaje);
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public void run() {
        try {
            inicializarFlujos();
            registrarUsuario();

            String linea;
            while (activo && (linea = in.readLine()) != null) {
                procesarComando(linea.trim());
            }

        } catch (IOException e) {
            System.err.println("Error cliente " + nombre + ": " + e.getMessage());
        } finally {
            ChatServer.removerUsuario(nombre);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    // -------------------- MÉTODOS PRINCIPALES --------------------

    private void inicializarFlujos() throws IOException {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    private void registrarUsuario() throws IOException {
        out.println("Ingrese su nombre:");
        nombre = in.readLine();
        ChatServer.registrarUsuario(nombre, this);
        out.println("Bienvenido, " + nombre + "!");
        mostrarMenu();
    }

    private void procesarComando(String linea) throws IOException {
        if (linea.startsWith("/udpport")) {
            procesarComandoUdpPort(linea);
        } else if (linea.startsWith("/callgroup")) {
            procesarComandoCallGroup(linea);
        } else if (linea.startsWith("/call")) {
            procesarComandoCall(linea);
        } else if (linea.startsWith("/endcall")) {
            procesarComandoEndCall(linea);
        } else if (linea.equals("/GroupChat")) {
            chatGrupal();
        } else if (linea.startsWith("/joingroup")) {
            procesarComandoJoinGroup(linea);
        } else if (linea.equals("/listgroups")) {
            listarGrupos();
        } else if (linea.equals("/help")) {
            mostrarMenu();
        } else if (linea.equals("/quit")) {
            salir();
        } else if (linea.equals("/msg")) {
            chatDirecto();
        } else {
            out.println("Comando no reconocido. Escriba /help para ver los disponibles.");
        }
    }

    // -------------------- COMANDOS --------------------

    private void procesarComandoUdpPort(String linea) {
        String[] parts = linea.split(" ", 2);
        if (parts.length != 2) {
            out.println("Uso: /udpport <port>");
            return;
        }
        String clientIp = socket.getInetAddress().getHostAddress();
        String ipPort = clientIp + ":" + parts[1].trim();
        ChatServer.registrarUdpInfo(nombre, ipPort);
        out.println("UDP registrado: " + ipPort);
    }

    private void procesarComandoCall(String linea) {
        String[] parts = linea.split(" ", 2);
        if (parts.length < 2) {
            out.println("Uso: /call <username>");
            return;
        }
        String target = parts[1].trim();
        String callId = ChatServer.iniciarLlamadaIndividual(nombre, target);
        if (callId == null)
            out.println("No se pudo iniciar llamada (usuario no disponible o sin UDP).");
        else
            out.println("Llamada iniciada: " + callId);
    }

    private void procesarComandoCallGroup(String linea) {
        String[] parts = linea.split(" ", 2);
        if (parts.length < 2) {
            out.println("Uso: /callgroup <groupName>");
            return;
        }
        String group = parts[1].trim();
        String callId = ChatServer.iniciarLlamadaGrupal(nombre, group);
        if (callId == null)
            out.println("No se pudo iniciar llamada grupal (pocos miembros online/UDP).");
        else
            out.println("Llamada grupal iniciada: " + callId);
    }

    private void procesarComandoEndCall(String linea) {
        String[] parts = linea.split(" ", 2);
        String callId = (parts.length == 2)
                ? parts[1].trim()
                : ChatServer.callManager != null
                    ? ChatServer.callManager.getCallOfUser(nombre)
                    : null;

        if (callId == null)
            out.println("No estás en ninguna llamada.");
        else
            ChatServer.terminarLlamada(callId, nombre);
    }

    private void procesarComandoJoinGroup(String linea) {
        String[] parts = linea.split(" ", 2);
        if (parts.length != 2) {
            out.println("Uso: /joingroup <groupName>");
            return;
        }
        ChatServer.unirseAGrupo(parts[1].trim(), nombre);
    }

    private void listarGrupos() {
        Set<String> grupos = ChatServer.obtenerGrupos();
        out.println("Grupos disponibles: " + grupos);
    }

    private void salir() {
        activo = false;
        out.println("Adiós, " + nombre + "!");
    }

    // -------------------- FUNCIONES DE CHAT --------------------

    private void chatDirecto() throws IOException {
        out.println("Ingrese el nombre del usuario al que desea escribir:");
        String destino = in.readLine();
        out.println("Escriba su mensaje (o 'salir' para volver al menú):");

        String mensaje;
        while (!(mensaje = in.readLine()).equalsIgnoreCase("salir")) {
            ChatServer.enviarMensajeDirecto(destino, nombre + ": " + mensaje);
        }
        mostrarMenu();
    }

    private void chatGrupal() throws IOException {
        out.println("Grupos disponibles: " + ChatServer.obtenerGruposChat());
        out.println("1. Crear grupo");
        out.println("2. Unirse a grupo existente");
        String opcion = in.readLine();

        out.println("Ingrese el nombre del grupo:");
        String nombreGrupo = in.readLine();

        if (opcion.equals("1")) {
            ChatServer.crearGrupo(nombreGrupo, this);
        } else if (opcion.equals("2")) {
            ChatServer.unirseAGrupo(nombreGrupo, this);
        } else {
            out.println("Opción no válida.");
            return;
        }

        grupoActual = nombreGrupo;
        out.println("Estás en el chat del grupo '" + grupoActual + "'. Escribe 'salir' para volver al menú.");

        String mensajeGrupo;
        while (!(mensajeGrupo = in.readLine()).equalsIgnoreCase("salir")) {
            ChatServer.enviarMensajeAGrupo(grupoActual, mensajeGrupo, this);
        }

        grupoActual = null;
        mostrarMenu();
    }

    // -------------------- UTILIDADES --------------------

    private void mostrarMenu() {
        out.println("===============================================");
        out.println("Comandos disponibles:");
        out.println("/udpport <port>        -> registrar puerto UDP local");
        out.println("/call <user>           -> iniciar llamada privada");
        out.println("/callgroup <group>     -> iniciar llamada grupal");
        out.println("/GroupChat             -> gestionar chats grupales");
        out.println("/listgroups            -> listar grupos existentes");
        out.println("/joingroup <nombre>    -> unirse a un grupo");
        out.println("/msg                   -> enviar mensaje directo");
        out.println("/endcall [callId]      -> terminar llamada");
        out.println("/help                  -> mostrar comandos");
        out.println("/quit                  -> desconectarse");
        out.println("===============================================");
    }
}
