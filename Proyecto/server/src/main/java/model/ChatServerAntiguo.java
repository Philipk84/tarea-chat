//package model;
//
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.io.IOException;
//import java.net.*;
//import java.util.*;
//import java.util.concurrent.*;
//
//import interfaces.ServerService;
//import interfaces.UserManager;
//import interfaces.GroupManager;
//import service.UserManagerImpl;
//import service.CallManagerImpl;
//import service.GroupManagerImpl;
//
///**
// * Servidor principal del sistema de chat.
// * Coordina gestión de usuarios, grupos, llamadas y mensajes UDP/TCP.
// */
//public class ChatServerAntiguo implements ServerService {
//
//    private static ChatServer instance;
//    private static final int THREAD_POOL_SIZE = 10;
//
//    private final Config config;
//    private final UserManager userManager;
//    private final GroupManager groupManager;
//    private final CallManagerImpl callManager;
//
//    private ServerSocket serverSocket;
//    private DatagramSocket udpSocket;
//    private ExecutorService threadPool;
//    private boolean running = false;
//
//    private static final Map<String, Integer> udpPorts = new ConcurrentHashMap<>();
//    private final Map<SocketAddress, String> udpClients = new ConcurrentHashMap<>();
//
//    public ChatServer(Config config) {
//        this.config = config;
//        this.userManager = new UserManagerImpl();
//        this.groupManager = new GroupManagerImpl();
//        this.callManager = new CallManagerImpl();
//    }
//
//    @Override
//    public String startServer() {
//        if (running) return "Servidor ya está en ejecución.";
//        instance = this;
//
//        try {
//            serverSocket = new ServerSocket(config.getPort());
//            udpSocket = new DatagramSocket(config.getPort() + 1);
//            threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
//            running = true;
//
//            // --- TCP Listener ---
//            Thread tcpThread = new Thread(() -> {
//                System.out.println("🟢 Servidor TCP escuchando en puerto " + config.getPort());
//                while (running) {
//                    try {
//                        Socket clientSocket = serverSocket.accept();
//                        ClientHandler handler = new ClientHandler(clientSocket);
//                        threadPool.submit(handler);
//                    } catch (IOException e) {
//                        if (running) e.printStackTrace();
//                    }
//                }
//            });
//            tcpThread.setDaemon(true);
//            tcpThread.start();
//
//            // --- VOICE Listener ---
//            Thread voiceThread = new Thread(() -> {
//                int voicePort = config.getVoicePort();
//                try (ServerSocket voiceServerSocket = new ServerSocket(voicePort)) {
//                    System.out.println("🎧 Servidor de voz escuchando en puerto " + voicePort);
//                    while (running) {
//                        try {
//                            Socket clientVoiceSocket = voiceServerSocket.accept();
//                            threadPool.submit(() -> handleVoiceConnection(clientVoiceSocket));
//                        } catch (IOException e) {
//                            if (running) e.printStackTrace();
//                        }
//                    }
//                } catch (IOException e) {
//                    System.err.println("❌ Error en servidor de voz: " + e.getMessage());
//                }
//            });
//            voiceThread.setDaemon(true);
//            voiceThread.start();
//
//            // --- UDP Listener ---
//            Thread udpThread = new Thread(() -> {
//                System.out.println("🔵 Servidor UDP escuchando en puerto " + (config.getPort() + 1));
//                byte[] buffer = new byte[4096];
//                while (running) {
//                    try {
//                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
//                        udpSocket.receive(packet);
//                        threadPool.submit(new UDPMessageHandler(packet, udpSocket, udpClients));
//                    } catch (Exception e) {
//                        if (running) e.printStackTrace();
//                    }
//                }
//            });
//            udpThread.setDaemon(true);
//            udpThread.start();
//
//            return "Servidor iniciado correctamente (TCP:" + config.getPort() +
//                    ", UDP:" + (config.getPort() + 1) +
//                    ", VOICE:" + config.getVoicePort() + ")";
//        } catch (IOException e) {
//            e.printStackTrace();
//            return "Error al iniciar servidor: " + e.getMessage();
//        }
//    }
//
//    @Override
//    public String closeServer() {
//        running = false;
//        try {
//            if (serverSocket != null) serverSocket.close();
//            if (udpSocket != null) udpSocket.close();
//            if (threadPool != null) threadPool.shutdown();
//            return "Servidor detenido correctamente.";
//        } catch (IOException e) {
//            return "Error cerrando servidor: " + e.getMessage();
//        }
//    }
//
//    private void handleVoiceConnection(Socket clientSocket) {
//        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
//             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {
//            while (true) {
//                Object obj = in.readObject();
//                if (obj instanceof VoiceNote note) {
//                    System.out.println("🎤 Nota de voz recibida de " + note.getFromUser() + " → " + note.getTarget());
//                    forwardVoiceNote(note);
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("❌ Error manejando conexión de voz: " + e.getMessage());
//        }
//    }
//
//    @Override
//    public boolean isRunning() {
//        return running;
//    }
//
//    // ======================
//    // MÉTODOS ESTÁTICOS
//    // ======================
//    public static synchronized ChatServer getInstance() {
//        return instance;
//    }
//
//    public static synchronized UserManager getUserManager() {
//        return instance.userManager;
//    }
//
//    public static synchronized GroupManager getGroupManager() {
//        return instance.groupManager;
//    }
//
//    public static synchronized CallManagerImpl getCallManager() {
//        return instance.callManager;
//    }
//
//    // ===========================================
//    // MANEJO DE USUARIOS
//    // ===========================================
//    public static synchronized void registerUser(String username, ClientHandler handler) {
//        instance.userManager.registerUser(username, handler);
//    }
//
//    public static synchronized void removeUser(String username) {
//        instance.userManager.removeUser(username);
//    }
//
//    public static synchronized void registerUdpInfo(String username, String ipPort) {
//        instance.userManager.registerUdpInfo(username, ipPort);
//        System.out.println("[UDP] Registrado " + username + " → " + ipPort);
//    }
//
//    public static synchronized String getUdpInfo(String username) {
//        return instance.userManager.getUdpInfo(username);
//    }
//
//    public static synchronized ClientHandler getClientHandler(String username) {
//        return instance.userManager.getClientHandler(username);
//    }
//
//    // ===========================================
//    // MANEJO DE GRUPOS
//    // ===========================================
//    public static synchronized void createGroup(String groupName, String creator) {
//        instance.groupManager.createGroup(groupName, creator);
//        System.out.println("[Grupo] Creado grupo: " + groupName + " por " + creator);
//    }
//
//    public static synchronized void joinGroup(String groupName, String username) {
//        instance.groupManager.joinGroup(groupName, username);
//        System.out.println("[Grupo] " + username + " se unió a " + groupName);
//    }
//
//    public static synchronized Set<String> getGroupMembers(String groupName) {
//        return instance.groupManager.getGroupMembers(groupName);
//    }
//
//    public static synchronized Set<String> getGroups() {
//        return instance.groupManager.getGroups();
//    }
//
//    // ===========================================
//    // MANEJO DE LLAMADAS
//    // ===========================================
//    public static synchronized String startIndividualCall(String from, String to) {
//        if (!instance.userManager.isUserOnline(to) || instance.userManager.getUdpInfo(to) == null)
//            return null;
//
//        Set<String> participants = new HashSet<>(Arrays.asList(from, to));
//        String callId = instance.callManager.createCall(participants);
//        notifyCallStarted(callId);
//        return callId;
//    }
//
//    public static synchronized String startGroupCall(String from, String groupName) {
//        Set<String> members = instance.groupManager.getGroupMembers(groupName);
//        if (members.isEmpty()) return null;
//
//        Set<String> participants = new HashSet<>();
//        for (String user : members) {
//            if (instance.userManager.isUserOnline(user) && instance.userManager.getUdpInfo(user) != null) {
//                participants.add(user);
//            }
//        }
//        participants.add(from);
//        if (participants.size() < 2) return null;
//
//        String callId = instance.callManager.createCall(participants);
//        notifyCallStarted(callId);
//        return callId;
//    }
//
//    private static void notifyCallStarted(String callId) {
//        Set<String> participants = instance.callManager.getParticipants(callId);
//        Map<String, String> peerMap = new HashMap<>();
//
//        for (String user : participants) {
//            String ipPort = instance.userManager.getUdpInfo(user);
//            if (ipPort != null) peerMap.put(user, ipPort);
//        }
//
//        for (String user : participants) {
//            ClientHandler ch = getClientHandler(user);
//            if (ch != null) {
//                StringBuilder sb = new StringBuilder("LLAMADA_INICIADA: ").append(callId).append(" ");
//                boolean first = true;
//                for (Map.Entry<String, String> e : peerMap.entrySet()) {
//                    if (!first) sb.append(",");
//                    sb.append(e.getKey()).append(":").append(e.getValue());
//                    first = false;
//                }
//                ch.sendMessage(sb.toString());
//            }
//        }
//    }
//
//    public static synchronized void endCall(String callId, String requester) {
//        Set<String> participants = instance.callManager.getParticipants(callId);
//        if (participants == null) return;
//
//        for (String user : participants) {
//            ClientHandler ch = getClientHandler(user);
//            if (ch != null) {
//                ch.sendMessage("LLAMADA_TERMINADA: " + callId + " por " + requester);
//            }
//        }
//        instance.callManager.endCall(callId);
//    }
//
//    public static void registerUdpPort(String username, InetAddress address, int port) {
//        udpPorts.put(username, port);
//        System.out.println("[UDP] Registrado " + username + " en puerto " + port);
//    }
//
//    public static int getUserUdpPort(String username) {
//        return udpPorts.getOrDefault(username, -1);
//    }
//
//    // ===========================
//    // 🎤 NOTAS DE VOZ
//    // ===========================
//    public static void handleVoiceNote(String from, String to, byte[] audioData, boolean isGroup) {
//        VoiceNote note = new VoiceNote(from, to, audioData, isGroup);
//        instance.forwardVoiceNote(note);
//    }
//
//    public static synchronized void forwardVoiceNote(VoiceNote note) {
//        System.out.println("🎤 Reenviando nota de voz de " + note.getFromUser() +
//                (note.isGroup() ? " para grupo " + note.getTarget() : " para usuario " + note.getTarget()));
//
//        if (note.isGroup()) {
//            Set<String> members = instance.groupManager.getGroupMembers(note.getTarget());
//            for (String member : members) {
//                if (!member.equals(note.getFromUser())) {
//                    ClientHandler ch = getClientHandler(member);
//                    if (ch != null) {
//                        ch.sendVoiceNote(note.getFromUser(), note.getAudioData());
//                    }
//                }
//            }
//        } else {
//            ClientHandler recipient = getClientHandler(note.getTarget());
//            if (recipient != null) {
//                recipient.sendVoiceNote(note.getFromUser(), note.getAudioData());
//            } else {
//                System.out.println("⚠️ Usuario destino no encontrado o desconectado: " + note.getTarget());
//            }
//        }
//    }
//}
