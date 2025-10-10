package com.chatapp.server;

import java.io.*;
import java.net.Socket;
import java.util.Set;

/**
 * Hilo por cliente que procesa comandos de señalización
 * y delega en ChatServer para llamadas/grupos.
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
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Ingrese su nombre:");
            nombre = in.readLine();
            ChatServer.registrarUsuario(nombre, this);
            out.println("Bienvenido, " + nombre + "!");

            mostrarMenu();

            String linea;
            while (activo && (linea = in.readLine()) != null) {
                // comandos:
                // /udpport <puerto>
                // /call <usuario>
                // /callgroup <grupo>
                // /endcall <callId>
                // /creargroup <nombre>
                // /joingroup <nombre>
                // /listgroups
                if (linea.startsWith("/udpport")) {
                    String[] parts = linea.split(" ", 2);
                    if (parts.length == 2) {
                        String clientIp = socket.getInetAddress().getHostAddress();
                        String ipPort = clientIp + ":" + parts[1].trim();
                        ChatServer.registrarUdpInfo(nombre, ipPort);
                        out.println("UDP registrado: " + ipPort);
                    } else out.println("Uso: /udpport <port>");
                } else if (linea.startsWith("/callgroup")) {
                    String[] parts = linea.split(" ", 2);
                    if (parts.length < 2) {
                        out.println("Uso: /callgroup <groupName>");
                        continue;
                    }
                    String group = parts[1].trim();
                    String callId = ChatServer.iniciarLlamadaGrupal(nombre, group);
                    if (callId == null) out.println("No se pudo iniciar llamada grupal (pocos miembros online/udp).");
                    else out.println("Llamada grupal iniciada: " + callId);
                } else if (linea.startsWith("/call")) {
                    String[] parts = linea.split(" ", 2);
                    if (parts.length < 2) {
                        out.println("Uso: /call <username>");
                        continue;
                    }
                    String target = parts[1].trim();
                    String callId = ChatServer.iniciarLlamadaIndividual(nombre, target);
                    if (callId == null) out.println("No se pudo iniciar llamada (usuario no disponible o sin UDP).");
                    else out.println("Llamada iniciada: " + callId);
                } else if (linea.startsWith("/endcall")) {
                    String[] parts = linea.split(" ", 2);
                    String callId = parts.length == 2 ? parts[1].trim() : ChatServer.callManager != null ? ChatServer.callManager.getCallOfUser(nombre) : null;
                    if (callId == null) {
                        out.println("No estás en ninguna llamada.");
                    } else {
                        ChatServer.terminarLlamada(callId, nombre);
                    }
                } else if (linea.startsWith("/GroupChat")) {
                    chatGrupal();
                } else if (linea.startsWith("/joingroup")) {
                    String[] p = linea.split(" ", 2);
                    if (p.length == 2) {
                        ChatServer.unirseAGrupo(p[1].trim(), nombre);
                    } else out.println("Uso: /joingroup <groupName>");
                } else if (linea.equals("/listgroups")) {
                
                    Set<String> g = ChatServer.obtenerGrupos();
                    out.println("Grupos: " + g);
                } else if (linea.equals("/help")) {
                    mostrarMenu();
                }else if (linea.equals("/quit")) {
                    activo = false;
                    out.println("Adiós!");
                } else if (linea.equals("/msg")) {
                    chatDirecto();
                }else {
                    out.println("Comando no reconocido.");
                }
            }

        } catch (IOException e) {
            System.err.println("Error cliente " + nombre + ": " + e.getMessage());
        } finally {
            ChatServer.removerUsuario(nombre);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void mostrarMenu() {
        out.println("Comandos disponibles:");
        out.println("/udpport <port>          -> registrar puerto UDP local");
        out.println("/call <user>             -> iniciar llamada privada");
        out.println("/callgroup <group>       -> iniciar llamada grupal");
        out.println("/GroupChat       -> Ver opciones de grupos");
        out.println("/listgroups              -> listar grupos");
        out.println("/msg       -> Mensaje directo");
        out.println("/endcall [callId]        -> terminar llamada");
        out.println("/help                    -> mostrar comandos");
        out.println("/quit                    -> desconectarse");
    }

    private void chatDirecto() throws IOException{
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
        String opcionGrupo = in.readLine();
        out.println("Ingrese el nombre del grupo:");
        String nombreGrupo = in.readLine();

        if (opcionGrupo.equals("1")) {
            ChatServer.crearGrupo(nombreGrupo, this);
        } else if (opcionGrupo.equals("2")) {
            ChatServer.unirseAGrupo(nombreGrupo, this);
        }

        grupoActual = nombreGrupo;
        out.println("Estás en el chat del grupo '" + grupoActual + "'. Escribe 'salir' para volver al menú.");

        String msgGrupo;
        while (!(msgGrupo = in.readLine()).equalsIgnoreCase("salir")) {
            ChatServer.enviarMensajeAGrupo(grupoActual, msgGrupo, this);
        }

        grupoActual = null;
        mostrarMenu();
    }

}
