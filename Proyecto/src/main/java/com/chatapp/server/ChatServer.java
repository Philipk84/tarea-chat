package com.chatapp.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Servidor principal. Registra usuarios, puertos UDP y crea llamadas.
 */
public class ChatServer {
    private static final int PORT = 5000;

    // username -> ClientHandler
    private static final Map<String, ClientHandler> usuarios = new HashMap<>();

    // username -> InetSocketAddress (IP + UDP port) reported by client
    private static final Map<String, String> udpInfo = new HashMap<>();

    // groups: groupName -> set of usernames
    private static final Map<String, Set<String>> grupos = new HashMap<>();

    private static final Map<String, Set<ClientHandler>> gruposChat = new HashMap<>();

    // call manager
    public static final CallManager callManager = new CallManager();

    public static void iniciarServidor() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado en el puerto " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Error iniciando el servidor: " + e.getMessage());
        }
    }

    public static synchronized void registrarUsuario(String nombre, ClientHandler handler) {
        usuarios.put(nombre, handler);
        System.out.println("Usuario registrado: " + nombre);
    }

    public static synchronized void enviarMensajeDirecto(String destinatario, String mensaje) {
        ClientHandler handler = usuarios.get(destinatario);
        if (handler != null) handler.enviarMensaje(mensaje);
    }

    public static synchronized void crearGrupo(String nombreGrupo, ClientHandler creador) {
        gruposChat.putIfAbsent(nombreGrupo, new HashSet<>());
        gruposChat.get(nombreGrupo).add(creador);
        creador.enviarMensaje("Grupo '" + nombreGrupo + "' creado y unido.");
    }

    public static synchronized void unirseAGrupo(String nombreGrupo, ClientHandler cliente) {
        if (!gruposChat.containsKey(nombreGrupo)) {
            cliente.enviarMensaje("El grupo no existe.");
            return;
        }
        gruposChat.get(nombreGrupo).add(cliente);
        cliente.enviarMensaje("Te has unido al grupo '" + nombreGrupo + "'.");
    }

    public static synchronized void enviarMensajeAGrupo(String nombreGrupo, String mensaje, ClientHandler remitente) {
        Set<ClientHandler> miembros = gruposChat.get(nombreGrupo);
        if (miembros != null) {
            for (ClientHandler miembro : miembros) {
                if(remitente != miembro){
                    miembro.enviarMensaje("[Grupos: " + nombreGrupo + "] " + remitente.getNombre() + ": " + mensaje);
                } 
            }
        }
    }

    public static synchronized Set<String> obtenerGruposChat() {
        return gruposChat.keySet();
    }
    
    public static synchronized void removerUsuario(String nombre) {
        usuarios.remove(nombre);
        udpInfo.remove(nombre);
        System.out.println("Usuario removido: " + nombre);
    }

    public static synchronized void registrarUdpInfo(String nombre, String ipPort) {
        udpInfo.put(nombre, ipPort);
        System.out.println("UDP registrado: " + nombre + " -> " + ipPort);
    }

    public static synchronized String obtenerUdpInfo(String nombre) {
        return udpInfo.get(nombre);
    }

    /* -------------------- Grupos -------------------- */
    public static synchronized void crearGrupo(String nombreGrupo, String creador) {
        grupos.putIfAbsent(nombreGrupo, new HashSet<>());
        grupos.get(nombreGrupo).add(creador);
        System.out.println("Grupo creado: '" + nombreGrupo + "' por " + creador);
    }

    public static synchronized void unirseAGrupo(String nombreGrupo, String usuario) {
        grupos.putIfAbsent(nombreGrupo, new HashSet<>());
        grupos.get(nombreGrupo).add(usuario);
        System.out.println(usuario + " se unió al grupo " + nombreGrupo);
    }

    public static synchronized Set<String> obtenerMiembrosGrupo(String nombreGrupo) {
        return grupos.getOrDefault(nombreGrupo, Collections.emptySet());
    }

    public static synchronized Set<String> obtenerGrupos() {
        return grupos.keySet();
    }

    /* -------------------- Llamadas (señalización) -------------------- */

    public static synchronized String iniciarLlamadaIndividual(String from, String to) {
        if (!usuarios.containsKey(to) || !udpInfo.containsKey(to)) return null;
        Set<String> participants = new HashSet<>();
        participants.add(from);
        participants.add(to);
        String callId = callManager.createCall(participants);
        // Notify both participants with peers list
        notifyCallStarted(callId);
        return callId;
    }

    public static synchronized String iniciarLlamadaGrupal(String from, String groupName) {
        Set<String> members = obtenerMiembrosGrupo(groupName);
        if (members.isEmpty()) return null;
        Set<String> participants = new HashSet<>();
        for (String u : members) {
            if (usuarios.containsKey(u) && udpInfo.containsKey(u)) {
                participants.add(u);
            }
        }
        // ensure caller is included
        participants.add(from);
        if (participants.size() < 2) return null;
        String callId = callManager.createCall(participants);
        notifyCallStarted(callId);
        return callId;
    }

    private static void notifyCallStarted(String callId) {
        Set<String> participants = callManager.getParticipants(callId);
        // build map username->udpInfo, only include those with valid udpInfo
        Map<String, String> peerMap = new HashMap<>();
        for (String u : participants) {
            String ipPort = udpInfo.get(u);
            if (ipPort != null) peerMap.put(u, ipPort);
        }
        // send to each participant the list of peers (including themselves if desired)
        for (String u : participants) {
            ClientHandler ch = usuarios.get(u);
            if (ch != null) {
                // format: CALL_STARTED <callId> user1:ip:port,user2:ip:port,...
                StringBuilder sb = new StringBuilder();
                sb.append("CALL_STARTED ").append(callId).append(" ");
                boolean first = true;
                for (Map.Entry<String, String> e : peerMap.entrySet()) {
                    if (!first) sb.append(",");
                    sb.append(e.getKey()).append(":").append(e.getValue());
                    first = false;
                }
                ch.enviarMensaje(sb.toString());
            }
        }
    }

    public static synchronized void terminarLlamada(String callId, String requester) {
        Set<String> participants = callManager.getParticipants(callId);
        if (participants == null) return;
        // notify participants
        for (String u : participants) {
            ClientHandler ch = usuarios.get(u);
            if (ch != null) ch.enviarMensaje("CALL_ENDED " + callId);
        }
        callManager.endCall(callId);
        System.out.println("Llamada terminada: " + callId + " por " + requester);
    }
}
