package model;

import interfaces.ServerService;
import interfaces.UserManager;
import interfaces.GroupManager;
import service.UserManagerImpl;
import service.GroupManagerImpl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Servidor principal del sistema de chat que coordina todas las operaciones
 * de comunicación, gestión de usuarios, grupos y llamadas.
 * 
 * Implementa el patrón Dependency Inversion y utiliza inyección de dependencias
 * para mantener bajo acoplamiento entre componentes.
 */
public class ChatServer implements ServerService {
    private final Config config;
    private final UserManager userManager;
    private final GroupManager groupManager;
    public final CallManager callManager;

    private ServerSocket serverSocket;
    private boolean running = false;

    public ChatServer(Config config) {
        this(config, new UserManagerImpl(), new GroupManagerImpl());
    }

    public ChatServer(Config config, UserManager userManager, GroupManager groupManager) {
        this.config = config;
        this.userManager = userManager;
        this.groupManager = groupManager;
        this.callManager = new CallManager();
    }

    public String startServer() {
        if (running) {
            return "El servidor ya está ejecutándose en el puerto " + config.getPort();
        }
        
        instance = this;
        
        try {
            serverSocket = new ServerSocket(config.getPort());
            running = true;
            
            Thread serverThread = new Thread(() -> {
                System.out.println("Servidor iniciado en puerto " + config.getPort());
                
                while (running) {
                    try {
                        Socket socket = serverSocket.accept();
                        ClientHandler handler = new ClientHandler(socket);
                        new Thread(handler).start();
                    } catch (IOException e) {
                        if (running) {
                            System.err.println("Error aceptando conexión del cliente: " + e.getMessage());
                        }
                    }
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();
            
            return "Servidor iniciado exitosamente en puerto " + config.getPort();
        } catch (IOException e) {
            running = false;
            return "Error iniciando servidor: " + e.getMessage();
        }
    }

    public String closeServer() {
        if (!running) {
            return "El servidor no está ejecutándose";
        }
        
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            return "Servidor cerrado exitosamente";
        } catch (IOException e) {
            return "Error cerrando servidor: " + e.getMessage();
        }
    }

    /**
     * Verifica si el servidor está actualmente en ejecución.
     * 
     * @return true si el servidor está ejecutándose, false en caso contrario
     */
    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Registra un nuevo usuario en el sistema.
     * 
     * @param name Nombre del usuario
     * @param handler Manejador del cliente
     */
    public static synchronized void registerUser(String name, ClientHandler handler) {
        instance.userManager.registerUser(name, handler);
    }

    /**
     * Remueve un usuario del sistema.
     * 
     * @param name Nombre del usuario a remover
     */
    public static synchronized void removeUser(String name) {
        instance.userManager.removeUser(name);
    }

    /**
     * Registra la información UDP de un usuario para llamadas de audio.
     * 
     * @param name Nombre del usuario
     * @param ipPort Dirección IP y puerto UDP en formato "ip:puerto"
     */
    public static synchronized void registerUdpInfo(String name, String ipPort) {
        instance.userManager.registerUdpInfo(name, ipPort);
    }

    /**
     * Obtiene la información UDP de un usuario.
     * 
     * @param name Nombre del usuario
     * @return Información UDP en formato "ip:puerto" o null si no existe
     */
    public static synchronized String getUdpInfo(String name) {
        return instance.userManager.getUdpInfo(name);
    }

    /**
     * Crea un nuevo grupo con el usuario especificado como creador.
     * 
     * @param groupName Nombre del grupo a crear
     * @param creator Usuario que crea el grupo
     */
    public static synchronized void createGroup(String groupName, String creator) {
        instance.groupManager.createGroup(groupName, creator);
    }

    /**
     * Permite a un usuario unirse a un grupo existente.
     * 
     * @param groupName Nombre del grupo
     * @param user Usuario que se une al grupo
     */
    public static synchronized void joinGroup(String groupName, String user) {
        instance.groupManager.joinGroup(groupName, user);
    }

    /**
     * Obtiene la lista de miembros de un grupo específico.
     * 
     * @param groupName Nombre del grupo
     * @return Conjunto de nombres de usuarios miembros del grupo
     */
    public static synchronized Set<String> getGroupMembers(String groupName) {
        return instance.groupManager.getGroupMembers(groupName);
    }

    /**
     * Obtiene la lista de todos los grupos disponibles.
     * 
     * @return Conjunto de nombres de grupos
     */
    public static synchronized Set<String> getGroups() {
        return instance.groupManager.getGroups();
    }

    private static ChatServer instance;

    /**
     * Inicia una llamada individual entre dos usuarios.
     * 
     * @param from Usuario que inicia la llamada
     * @param to Usuario destinatario de la llamada
     * @return ID de la llamada creada o null si no se pudo crear
     */
    public static synchronized String startIndividualCall(String from, String to) {
        if (!instance.userManager.isUserOnline(to) || instance.userManager.getUdpInfo(to) == null) return null;
        Set<String> participants = new HashSet<>();
        participants.add(from);
        participants.add(to);
        String callId = instance.callManager.createCall(participants);
        notifyCallStarted(callId);
        return callId;
    }

    /**
     * Inicia una llamada grupal con todos los miembros conectados de un grupo.
     * 
     * @param from Usuario que inicia la llamada grupal
     * @param groupName Nombre del grupo a llamar
     * @return ID de la llamada creada o null si no se pudo crear
     */
    public static synchronized String startGroupCall(String from, String groupName) {
        Set<String> members = instance.groupManager.getGroupMembers(groupName);
        if (members.isEmpty()) return null;
        Set<String> participants = new HashSet<>();
        for (String u : members) {
            if (instance.userManager.isUserOnline(u) && instance.userManager.getUdpInfo(u) != null) {
                participants.add(u);
            }
        }
        participants.add(from);
        if (participants.size() < 2) return null;
        String callId = instance.callManager.createCall(participants);
        notifyCallStarted(callId);
        return callId;
    }

    /**
     * Notifica a todos los participantes que una llamada ha comenzado.
     * 
     * @param callId ID de la llamada iniciada
     */
    private static void notifyCallStarted(String callId) {
        Set<String> participants = instance.callManager.getParticipants(callId);
        Map<String, String> peerMap = new HashMap<>();
        for (String u : participants) {
            String ipPort = instance.userManager.getUdpInfo(u);
            if (ipPort != null) peerMap.put(u, ipPort);
        }
        
        for (String u : participants) {
            ClientHandler ch = getClientHandler(u);
            if (ch != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("CALL_STARTED ").append(callId).append(" ");
                boolean first = true;
                for (Map.Entry<String, String> e : peerMap.entrySet()) {
                    if (!first) sb.append(",");
                    sb.append(e.getKey()).append(":").append(e.getValue());
                    first = false;
                }
                ch.sendMessage(sb.toString());
            }
        }
    }

    /**
     * Termina una llamada activa y notifica a todos los participantes.
     * 
     * @param callId ID de la llamada a terminar
     * @param requester Usuario que solicita terminar la llamada
     */
    public static synchronized void endCall(String callId, String requester) {
        Set<String> participants = instance.callManager.getParticipants(callId);
        if (participants == null) return;
        
        for (String u : participants) {
            ClientHandler ch = getClientHandler(u);
            if (ch != null) ch.sendMessage("CALL_ENDED " + callId);
        }
        instance.callManager.endCall(callId);
        System.out.println("Llamada terminada: " + callId + " por " + requester);
    }

    /**
     * Obtiene el manejador de cliente para un usuario específico.
     * 
     * @param username Nombre del usuario
     * @return Manejador del cliente o null si no existe
     */
    private static ClientHandler getClientHandler(String username) {
        if (instance.userManager instanceof UserManagerImpl) {
            return ((UserManagerImpl) instance.userManager).getClientHandler(username);
        }
        return null;
    }

    /**
     * Obtiene el gestor de llamadas del servidor.
     * 
     * @return Gestor de llamadas o null si no hay instancia activa
     */
    public static CallManager getCallManager() {
        return instance != null ? instance.callManager : null;
    }
}
