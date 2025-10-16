package service;

import interfaces.AudioService;
import model.CallAudio;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * Implementación del servicio de audio del cliente.
 * Maneja la captura, envío, recepción y reproducción de audio (UDP y TCP).
 */
public class AudioServiceImpl implements AudioService {
    private DatagramSocket udpSocket;
    private final ExecutorService audioThreads = Executors.newCachedThreadPool();

    private CallAudio.CallSender sender;
    private CallAudio.CallReceiver receiver;
    private Future<?> senderFuture;
    private Future<?> receiverFuture;

    private volatile boolean active = false;

    /**
     * Constructor por defecto que inicializa el servicio de audio.
     */
    public AudioServiceImpl() {
        // Constructor vacío - la configuración se hace vía setUdpSocket
    }

    @Override
    public void setUdpSocket(DatagramSocket udpSocket) {
        this.udpSocket = udpSocket;
    }

    // ===========================
    // 🎙️ AUDIO EN TIEMPO REAL (UDP)
    // ===========================
    @Override
    public void startSending(List<InetSocketAddress> peers) {
        if (udpSocket == null) {
            throw new IllegalStateException("UDP socket no configurado");
        }

        stopSending(); // Detener envío anterior si existe
        
        sender = new CallAudio.CallSender(udpSocket, peers);
        senderFuture = audioThreads.submit(sender);
        active = true;
    }

    @Override
    public void startReceiving() {
        if (udpSocket == null) {
            throw new IllegalStateException("UDP socket no configurado");
        }

        stopReceiving(); // Detener recepción anterior si existe
        
        receiver = new CallAudio.CallReceiver(udpSocket);
        receiverFuture = audioThreads.submit(receiver);
        active = true;
    }

    @Override
    public void stopAudio() {
        stopSending();
        stopReceiving();
        active = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    private void stopSending() {
        try {
            if (sender != null) sender.stop();
            if (senderFuture != null) senderFuture.cancel(true);
        } catch (Exception ignored) {}
        sender = null;
        senderFuture = null;
    }

    private void stopReceiving() {
        try {
            if (receiver != null) receiver.stop();
            if (receiverFuture != null) receiverFuture.cancel(true);
        } catch (Exception ignored) {}
        receiver = null;
        receiverFuture = null;
    }

    /**
     * Cierra el servicio de audio y libera todos los recursos.
     */
    public void shutdown() {
        stopAudio();
        audioThreads.shutdownNow();
    }

    // ===========================
    // 🎧 NOTAS DE VOZ (TCP)
    // ===========================

    /**
     * Envía una nota de voz por TCP al servidor.
     *
     * @param tcpSocket Socket TCP ya conectado al servidor.
     * @param audioFile Archivo de audio (.wav o .dat) a enviar.
     * @param target Nombre del usuario o grupo destino.
     */
    public void sendVoiceNoteTCP(Socket tcpSocket, File audioFile, String target) {
        audioThreads.submit(() -> {
            try {
                DataOutputStream out = new DataOutputStream(tcpSocket.getOutputStream());
                byte[] audioBytes = readAllBytes(audioFile);

                // Enviamos un encabezado indicando que es una nota de voz
                out.writeUTF("VOICE_NOTE");
                out.writeUTF(target);
                out.writeInt(audioBytes.length);
                out.write(audioBytes);
                out.flush();

                System.out.println("✅ Nota de voz enviada por TCP a " + target + " (" + audioBytes.length + " bytes)");
            } catch (IOException e) {
                System.err.println("❌ Error enviando nota de voz TCP: " + e.getMessage());
            }
        });
    }

    /**
     * Recibe una nota de voz del servidor y la guarda localmente.
     *
     * @param inStream flujo de entrada desde el socket TCP
     */
    public void receiveVoiceNoteTCP(DataInputStream inStream) {
        audioThreads.submit(() -> {
            try {
                String sender = inStream.readUTF();
                int size = inStream.readInt();
                byte[] audioData = new byte[size];
                inStream.readFully(audioData);

                File received = new File("voice_from_" + sender + ".wav");
                try (FileOutputStream fos = new FileOutputStream(received)) {
                    fos.write(audioData);
                }

                System.out.println("📥 Nota de voz recibida de " + sender + " (" + size + " bytes)");

                // Aquí puedes agregar reproducción automática si lo deseas
                // playAudio(received);

            } catch (IOException e) {
                System.err.println("❌ Error recibiendo nota de voz TCP: " + e.getMessage());
            }
        });
    }

    /**
     * Lee todos los bytes de un archivo.
     */
    private byte[] readAllBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return fis.readAllBytes();
        }
    }
}
