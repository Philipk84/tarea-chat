package model;

import interfaces.ServerService;
import interfaces.UserManager;
import interfaces.GroupManager;
import service.UserManagerImpl;
import service.CallManagerImpl;
import service.GroupManagerImpl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import service.HistoryService;

/**
 * Servidor principal del sistema de chat que coordina todas las operaciones
 * de comunicaciÃ³n, gestiÃ³n de usuarios, grupos, llamadas, mensajes y audios.
 */
public class ChatServer implements ServerService {
    private static ChatServer instance;
    private static final int THREAD_POOL_SIZE = 10; // Como en class/Server.java
    
    private final Config config;
    private final UserManager userManager;
    private final GroupManager groupManager;
    public final CallManagerImpl CallManagerImpl;

    private ServerSocket serverSocket;
    private DatagramSocket udpSocket; // Socket UDP para audio y notas de voz (como class/UDPserver.java)
    private ExecutorService threadPool;
    private Map<SocketAddress, String> udpClients; // Mapa de clientes UDP (como class/UDPserver.java)
    private boolean running = false;

    public ChatServer(Config config) {
        this.config = config;
        this.userManager = new UserManagerImpl();
        this.groupManager = new GroupManagerImpl();
        this.CallManagerImpl = new CallManagerImpl();
        this.udpClients = new ConcurrentHashMap<>(); // Inicializar mapa UDP como en class/UDPserver.java
    }

    /**
     * Inicia el servidor de chat y comienza a aceptar conexiones de clientes.
     * Usa ExecutorService con ThreadPool fijo para manejar clientes de manera eficiente.
     * PatrÃ³n similar a class/Server.java
     * 
     * @return Mensaje de estado del resultado de la operaciÃ³n
     */
    @Override
    public String startServer() {
        if (running) {
            return "El servidor ya estÃ¡ ejecutÃ¡ndose en el puerto " + config.getPort();
        }
        
        instance = this;
        
        try {
            serverSocket = new ServerSocket(config.getPort());
            udpSocket = new DatagramSocket(config.getPort() + 1, InetAddress.getByName(config.getHost())); // Puerto UDP+1 como en class/UDPserver.java
            threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE); // Como class/Server.java
            running = true;
            
            // Hilo para conexiones TCP (como class/Server.java)
            Thread serverThread = new Thread(() -> {
                System.out.println("Servidor TCP escuchando en puerto " + config.getPort() + "...");
                
                while (running) {
                    try {
                        Socket socket = serverSocket.accept();
                        ClientHandler handler = new ClientHandler(socket);
                        threadPool.submit(handler); // Usar threadPool en lugar de new Thread()
                    } catch (IOException e) {
                        if (running) {
                            System.err.println("Error aceptando conexión TCP del cliente: " + e.getMessage());
                        }
                    }
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();
            
            // Hilo para mensajes UDP (como class/UDPserver.java)
            Thread udpThread = new Thread(() -> {
                System.out.println("Servidor UDP escuchando en puerto " + (config.getPort() + 1) + "...");
                byte[] receiveData = new byte[4096]; // Buffer más grande para audio
                
                while (running && !udpSocket.isClosed()) {
                    try {
                        DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
                        udpSocket.receive(packet);
                        // Usar UDPMessageHandler como ClientHandler en class/UDPserver.java
                        threadPool.submit(new UDPMessageHandler(packet, udpSocket, udpClients));
                    } catch (Exception e) {
                        if (running && !udpSocket.isClosed()) {
                            System.err.println("Error procesando mensaje UDP: " + e.getMessage());
                        }
                    }
                }
            });
            udpThread.setDaemon(true);
            udpThread.start();
            
            return "Servidor iniciado exitosamente - TCP:" + config.getPort() + " UDP:" + (config.getPort() + 1);
        } catch (IOException e) {
            running = false;
            return "Error iniciando servidor: " + e.getMessage();
        }
    }

    /**
     * Cierra el servidor de chat y libera los recursos asociados.
     * Cierra el threadPool de manera ordenada como en class/Server.java
     * 
     * @return Mensaje de estado del resultado de la operaciÃ³n
     */
    @Override
    public String closeServer() {
        if (!running) {
            return "El servidor no estÃ¡ ejecutÃ¡ndose";
        }
        
        running = false;
        try {
            if (threadPool != null) {
                threadPool.shutdown(); // Cerrar threadPool como en class/Server.java
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close(); // Cerrar socket UDP como en class/UDPserver.java
            }
            return "Servidor cerrado exitosamente";
        } catch (IOException e) {
            return "Error cerrando servidor: " + e.getMessage();
        }
    }

    /**
     * Verifica si el servidor estÃ¡ actualmente en ejecuciÃ³n.
     * 
     * @return true si el servidor estÃ¡ ejecutÃ¡ndose, false en caso contrario
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
     * Registra la informaciÃ³n UDP de un usuario para llamadas de audio.
     * 
     * @param name Nombre del usuario
     * @param ipPort DirecciÃ³n IP y puerto UDP en formato "ip:puerto"
     */
    public static synchronized void registerUdpInfo(String name, String ipPort) {
        instance.userManager.registerUdpInfo(name, ipPort);
    }

    /**
     * Registra un mapeo directo de dirección UDP -> usuario para facilitar
     * la identificación del emisor en mensajes UDP.
     *
     * @param address Dirección UDP del cliente (ip:puerto)
     * @param username Nombre de usuario
     */
    public static synchronized void registerUdpClientAddress(SocketAddress address, String username) {
        if (instance != null && instance.udpClients != null && address != null && username != null) {
            instance.udpClients.put(address, username);
        }
    }

    /**
     * Obtiene la informaciÃ³n UDP de un usuario.
     * 
     * @param name Nombre del usuario
     * @return InformaciÃ³n UDP en formato "ip:puerto" o null si no existe
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
     * Obtiene la lista de miembros de un grupo especÃ­fico.
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
        String callId = instance.CallManagerImpl.createCall(participants);
        notifyCallStarted(callId);
        try {
            HistoryService.logCallStarted(callId, participants);
        } catch (Exception ignored) {}
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
        String callId = instance.CallManagerImpl.createCall(participants);
        notifyCallStarted(callId);
        try {
            HistoryService.logCallStarted(callId, participants);
        } catch (Exception ignored) {}
        return callId;
    }

    /**
     * Notifica a todos los participantes que una llamada ha comenzado.
     * 
     * @param callId ID de la llamada iniciada
     */
    private static void notifyCallStarted(String callId) {
        Set<String> participants = instance.CallManagerImpl.getParticipants(callId);
        Map<String, String> peerMap = new HashMap<>();
        for (String u : participants) {
            String ipPort = instance.userManager.getUdpInfo(u);
            if (ipPort != null) peerMap.put(u, ipPort);
        }
        
        for (String u : participants) {
            ClientHandler ch = getClientHandler(u);
            if (ch != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("LLAMADA_INICIADA: ").append(callId).append(" ");
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
        Set<String> participants = instance.CallManagerImpl.getParticipants(callId);
        if (participants == null) return;
        
        for (String u : participants) {
            ClientHandler ch = getClientHandler(u);
            if (ch != null) ch.sendMessage("LLAMADA_TERMINADA: " + callId + " por " + requester);
        }
        instance.CallManagerImpl.endCall(callId);
        try {
            HistoryService.logCallEnded(callId, participants, requester);
        } catch (Exception ignored) {}
    }

    /**
     * Obtiene el manejador de cliente para un usuario especÃ­fico.
     * 
     * @param username Nombre del usuario
     * @return Manejador del cliente o null si no existe
     */
    public static ClientHandler getClientHandler(String username) {
        if (instance == null || instance.userManager == null) {
            return null;
        }
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
    public static CallManagerImpl getCallManagerImpl() {
        return instance != null ? instance.CallManagerImpl : null;
    }

    /**
     * Obtiene la lista de todos los usuarios disponibles.
     * 
     * @return Conjunto de nombres de usuarios
     */
    public static synchronized Set<String> getUsers() {
        return instance.userManager.getUsers();
    }
}
