package model;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utilidades para el manejo de audio en llamadas UDP.
 * Proporciona clases para enviar audio (CallSender) y recibir/reproducir audio (CallReceiver).
 * Utiliza formato de audio PCM 16000 Hz, 16 bits, mono para la comunicación.
 */
public class CallAudio {

    /**
     * Formato de audio estándar: PCM 16000 Hz, 16 bits, mono.
     */
    public static final AudioFormat AUDIO_FORMAT = new AudioFormat(16000.0f, 16, 1, true, false);

    /**
     * Clase que maneja la captura y envío de audio desde el micrófono.
     * Captura audio del micrófono y lo envía vía UDP a todos los participantes.
     */
    public static class CallSender implements Runnable {
        private final DatagramSocket socket;
        private final List<InetSocketAddress> peers;
        private final AtomicBoolean running = new AtomicBoolean(true);

        /**
         * Constructor que inicializa el emisor de audio.
         * 
         * @param socket Socket UDP para envío de audio
         * @param peers Lista de destinatarios del audio
         */
        public CallSender(DatagramSocket socket, List<InetSocketAddress> peers) {
            this.socket = socket;
            this.peers = peers;
        }

        /**
         * Detiene la captura y envío de audio.
         */
        public void stop() {
            running.set(false);
        }

        /**
         * Ejecuta el bucle principal de captura y envío de audio.
         */
        @Override
        public void run() {
            TargetDataLine microphone = null;
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
                microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(AUDIO_FORMAT);
                microphone.start();

                int bufferSize = 512; // bytes
                byte[] buffer = new byte[bufferSize];

                while (running.get()) {
                    int read = microphone.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        DatagramPacket packet = new DatagramPacket(buffer, read);
                        // send to each peer
                        for (InetSocketAddress peer : peers) {
                            packet.setSocketAddress(peer);
                            socket.send(packet);
                        }
                    }
                }
            } catch (LineUnavailableException | IOException e) {
                System.err.println("CallSender error: " + e.getMessage());
            } finally {
                if (microphone != null) {
                    microphone.stop();
                    microphone.close();
                }
            }
        }
    }

    /**
     * Clase que maneja la recepción y reproducción de audio.
     * Recibe paquetes de audio vía UDP y los reproduce en los altavoces.
     */
    public static class CallReceiver implements Runnable {
        private final DatagramSocket socket;
        private final AtomicBoolean running = new AtomicBoolean(true);

        /**
         * Constructor que inicializa el receptor de audio.
         * 
         * @param socket Socket UDP para recepción de audio
         */
        public CallReceiver(DatagramSocket socket) {
            this.socket = socket;
        }

        /**
         * Detiene la recepción y reproducción de audio.
         */
        public void stop() {
            running.set(false);
            socket.close();
        }

        /**
         * Ejecuta el bucle principal de recepción y reproducción de audio.
         */
        @Override
        public void run() {
            SourceDataLine speakers = null;
            try {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);
                speakers = (SourceDataLine) AudioSystem.getLine(info);
                speakers.open(AUDIO_FORMAT);
                speakers.start();

                byte[] buffer = new byte[512];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                while (running.get()) {
                    try {
                        socket.receive(packet);
                        speakers.write(packet.getData(), 0, packet.getLength());
                    } catch (SocketException se) {
                        // socket closed
                        break;
                    }
                }
            } catch (LineUnavailableException | IOException e) {
                System.err.println("CallReceiver error: " + e.getMessage());
            } finally {
                if (speakers != null) {
                    speakers.drain();
                    speakers.stop();
                    speakers.close();
                }
            }
        }
    }
}
