package com.chatapp.client;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Cliente que se conecta al servidor por TCP (señalización) y usa UDP para audio.
 *
 * Comandos a usar desde la consola:
 * - /udpport <port>   -> registrar puerto UDP local
 * - /call <user>      -> llamar a usuario
 * - /callgroup <grp>  -> llamar grupo
 * - /endcall [callId] -> terminar llamada
 * - /creargroup <grp>
 * - /joingroup <grp>
 * - /listgroups
 * - /quit
 *
 * Cuando reciba "CALL_STARTED <callId> user:ip:port,..." inicia audio (sender+receiver).
 */
public class ChatClient {
    private final String serverHost;
    private final int serverPort;
    private final String username;

    private Socket tcpSocket;
    private BufferedReader tcpIn;
    private PrintWriter tcpOut;

    private DatagramSocket udpSocket;
    private final ExecutorService callThreads = Executors.newCachedThreadPool();

    // active call state
    @SuppressWarnings("unused")
    private volatile String activeCallId = null;
    private CallAudio.CallSender sender = null;
    private CallAudio.CallReceiver receiver = null;
    private Future<?> senderFuture = null;
    private Future<?> receiverFuture = null;

    public ChatClient(String host, int port, String username) {
        this.serverHost = host;
        this.serverPort = port;
        this.username = username;
    }

    public void start() {
        try {
            tcpSocket = new Socket(serverHost, serverPort);
            tcpIn = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            tcpOut = new PrintWriter(tcpSocket.getOutputStream(), true);

            // read server prompt for name, send username
            tcpIn.readLine(); // "Ingrese su nombre:" (ignored)
            tcpOut.println(username);
            System.out.println(tcpIn.readLine()); // welcome

            // open UDP socket on ephemeral port
            udpSocket = new DatagramSocket(); // system picks a free port
            int udpPort = udpSocket.getLocalPort();
            // register UDP port with server
            tcpOut.println("/udpport " + udpPort);
            System.out.println("Puerto UDP local: " + udpPort + " (registrado en servidor)");

            // start thread to listen server messages (signaling)
            Thread sinalThread = new Thread(this::listenServer);
            sinalThread.setDaemon(true);
            sinalThread.start();

            // read user input and send commands to server
            try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while ((line = console.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    // local handling for endcall
                    if (line.startsWith("/endcall")) {
                        tcpOut.println(line); // server will notify peers
                        stopActiveCall();
                        continue;
                    } else if (line.equals("/quit")) {
                        tcpOut.println(line);
                        break;
                    } else {
                        tcpOut.println(line);
                    }
                }
            }

            cleanup();

        } catch (IOException e) {
            System.err.println("Error cliente: " + e.getMessage());
            cleanup();
        }
    }


    private void listenServer() {
        String line;
        try {
            while ((line = tcpIn.readLine()) != null) {
                System.out.println("[SERVER] " + line);

                if (line.startsWith("CALL_STARTED")) {
                    // format: CALL_STARTED <callId> user:ip:port,user2:ip:port,...
                    handleCallStarted(line);
                } else if (line.startsWith("CALL_ENDED")) {
                    // CALL_ENDED <callId>
                    String[] p = line.split(" ", 2);
                    String callId = p.length > 1 ? p[1].trim() : null;
                    System.out.println("Llamada terminada: " + callId);
                    stopActiveCall();
                }
            }
        } catch (IOException e) {
            System.out.println("Conexión con servidor cerrada.");
        }
    }

    private void handleCallStarted(String line) {
        try {
            String payload = line.substring("CALL_STARTED".length()).trim();
            String[] parts = payload.split(" ", 2);
            String callId = parts[0];
            String peersList = parts.length > 1 ? parts[1] : "";

            // parse peers into InetSocketAddress list (exclude self)
            List<InetSocketAddress> peers = Arrays.stream(peersList.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> {
                        // format: username:ip:port
                        String[] tok = s.split(":");
                        if (tok.length < 3) return null;
                        String user = tok[0];
                        String ip = tok[1];
                        String portStr = tok[2];
                        try {
                            int port = Integer.parseInt(portStr);
                            if (user.equals(username)) return null; // don't send to self
                            return new InetSocketAddress(ip, port);
                        } catch (NumberFormatException ex) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (peers.isEmpty()) {
                System.out.println("No hay peers para la llamada.");
                return;
            }

            System.out.println("Iniciando llamada (callId=" + callId + ") con peers: " + peers);
            startCall(callId, peers);

        } catch (Exception e) {
            System.err.println("Error iniciando llamada: " + e.getMessage());
        }
    }

    private void startCall(String callId, List<InetSocketAddress> peers) throws SocketException {
        stopActiveCall(); // stop any previous call

        activeCallId = callId;

        // Sender uses same UDP socket and sends audio frames to all peers
        sender = new CallAudio.CallSender(udpSocket, peers);
        senderFuture = callThreads.submit(sender);

        // Receiver listens on udpSocket and plays audio
        receiver = new CallAudio.CallReceiver(udpSocket);
        receiverFuture = callThreads.submit(receiver);

        System.out.println("Llamada activa: " + callId + ". Escribe /endcall para finalizar.");
    }

    private void stopActiveCall() {
        try {
            if (sender != null) sender.stop();
            if (receiver != null) receiver.stop();
            if (senderFuture != null) senderFuture.cancel(true);
            if (receiverFuture != null) receiverFuture.cancel(true);
        } catch (Exception ignored) {}
        sender = null;
        receiver = null;
        senderFuture = null;
        receiverFuture = null;
        activeCallId = null;
    }

    private void cleanup() {
        stopActiveCall();
        callThreads.shutdownNow();
        if (udpSocket != null && !udpSocket.isClosed()) udpSocket.close();
        try {
            if (tcpSocket != null && !tcpSocket.isClosed()) tcpSocket.close();
        } catch (IOException ignored) {}
    }
}
