package service;

import interfaces.MessageHandler;
import interfaces.NetworkService;
import javax.sound.sampled.*;
import java.io.*;
import java.net.*;

/**
 * Implementación del servicio de comunicación de red del cliente.
 * Maneja la conexión TCP con el servidor y el envío de comandos.
 * Además, maneja un canal UDP independiente para transmisión de audio.
 */
public class NetworkServiceImpl implements NetworkService {
    private final String serverHost;
    private final int serverPort;

    // === TCP ===
    private Socket tcpSocket;
    private BufferedReader tcpIn;
    private PrintWriter tcpOut;
    private volatile boolean connected = false;

    private MessageHandler messageHandler;
    private Thread listenerThread;

    // === UDP: audio en tiempo real ===
    private DatagramSocket audioSocket;
    private Thread audioSendThread;
    private Thread audioReceiveThread;
    private volatile boolean audioActive = false;

    // Configuración de audio
    private static final int AUDIO_PORT = 5555;
    private static final int BUFFER_SIZE = 4096;
    private static final AudioFormat AUDIO_FORMAT =
            new AudioFormat(16000.0f, 16, 1, true, false);

    private static final boolean DEBUG = false;

    public NetworkServiceImpl(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    // === TCP CHAT ===
    @Override
    public String connect(String username) {
        try {
            tcpSocket = new Socket(serverHost, serverPort);
            tcpSocket.setSoTimeout(3000);

            tcpIn = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            tcpOut = new PrintWriter(tcpSocket.getOutputStream(), true);

            String prompt = tcpIn.readLine();
            if (prompt != null && prompt.toLowerCase().contains("name")) {
                tcpOut.println(username);
            } else {
                tcpOut.println(username);
            }

            tcpSocket.setSoTimeout(0);
            String welcomeMessage = tcpIn.readLine();

            connected = true;
            startMessageListener();

            return welcomeMessage != null ? welcomeMessage : "Conectado al servidor.";

        } catch (SocketTimeoutException e) {
            cleanup();
            return "Timeout esperando respuesta del servidor.";
        } catch (IOException e) {
            cleanup();
            return "Error conectando al servidor: " + e.getMessage();
        }
    }

    @Override
    public void sendCommand(String command) {
        if (!connected || tcpOut == null) {
            System.out.println("⚠️ No conectado al servidor.");
            return;
        }

        tcpOut.println(command);
        if ("/quit".equalsIgnoreCase(command)) {
            disconnect();
        }
    }

    @Override
    public void disconnect() {
        if (!connected) return;
        connected = false;

        stopAudio(); // detener hilos de audio UDP

        try {
            if (tcpSocket != null && !tcpSocket.isClosed()) tcpSocket.close();
        } catch (IOException e) {
            System.err.println("Error cerrando socket TCP: " + e.getMessage());
        }

        if (listenerThread != null) listenerThread.interrupt();

        cleanup();
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    // === GETTERS ===
    public Socket getSocket() {
        return tcpSocket;
    }

    // === ESCUCHA DE MENSAJES TCP (texto) ===
    private void startMessageListener() {
        listenerThread = new Thread(this::listenServer);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void listenServer() {
        try {
            String line;
            while (connected && (line = tcpIn.readLine()) != null) {
                if (messageHandler != null) {
                    messageHandler.handleMessage(line);
                } else {
                    System.out.println("[SERVER] " + line);
                }
            }
        } catch (IOException e) {
            if (connected) {
                System.out.println("❌ Error en la conexión con el servidor: " + e.getMessage());
            }
        } finally {
            cleanup();
            connected = false;
        }
    }

    // === AUDIO UDP ===
    public void startAudio() {
        if (audioActive) return;

        try {
            audioSocket = new DatagramSocket();
            audioActive = true;

            audioSendThread = new Thread(this::sendAudioLoop, "AudioSendThread");
            audioReceiveThread = new Thread(this::receiveAudioLoop, "AudioReceiveThread");

            audioSendThread.setDaemon(true);
            audioReceiveThread.setDaemon(true);

            audioSendThread.start();
            audioReceiveThread.start();

            System.out.println("🎤 Canal de audio iniciado (UDP en puerto " + AUDIO_PORT + ").");

        } catch (SocketException e) {
            System.err.println("No se pudo iniciar el canal de audio: " + e.getMessage());
        }
    }

    public void stopAudio() {
        audioActive = false;
        if (audioSocket != null && !audioSocket.isClosed()) {
            audioSocket.close();
        }
        System.out.println("🔇 Canal de audio detenido.");
    }

    private void sendAudioLoop() {
        try {
            TargetDataLine mic = AudioSystem.getTargetDataLine(AUDIO_FORMAT);
            mic.open(AUDIO_FORMAT);
            mic.start();

            byte[] buffer = new byte[BUFFER_SIZE];
            InetAddress address = InetAddress.getByName(serverHost);

            while (audioActive) {
                int bytesRead = mic.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    DatagramPacket packet = new DatagramPacket(buffer, bytesRead, address, AUDIO_PORT);
                    audioSocket.send(packet);
                }
            }

            mic.stop();
            mic.close();

        } catch (Exception e) {
            if (audioActive) System.err.println("Error enviando audio: " + e.getMessage());
        }
    }

    private void receiveAudioLoop() {
        try {
            SourceDataLine speakers = AudioSystem.getSourceDataLine(AUDIO_FORMAT);
            speakers.open(AUDIO_FORMAT);
            speakers.start();

            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (audioActive) {
                audioSocket.receive(packet);
                speakers.write(packet.getData(), 0, packet.getLength());
            }

            speakers.stop();
            speakers.close();

        } catch (Exception e) {
            if (audioActive) System.err.println("Error recibiendo audio: " + e.getMessage());
        }
    }

    // === LIMPIEZA ===
    private void cleanup() {
        try {
            if (tcpIn != null) tcpIn.close();
            if (tcpOut != null) tcpOut.close();
            if (tcpSocket != null && !tcpSocket.isClosed()) tcpSocket.close();
        } catch (IOException e) {
            System.err.println("Error cerrando recursos de red: " + e.getMessage());
        }
    }

    private void log(String msg) {
        if (DEBUG) System.out.println("[NetworkService] " + msg);
    }
}
