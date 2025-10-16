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
                microphone = openMicrophoneWithFallback();
                if (microphone == null) {
                    System.err.println("CallSender error: no se pudo abrir el micrófono en ningún mixer.");
                    return;
                }

                int bufferSize = 512; // bytes
                byte[] buffer = new byte[bufferSize];

                while (running.get()) {
                    int read = microphone.read(buffer, 0, buffer.length);
                    if (read <= 0) continue;
                    DatagramPacket packet = new DatagramPacket(buffer, read);
                    // enviar a cada peer
                    for (InetSocketAddress peer : peers) {
                        try {
                            packet.setSocketAddress(peer);
                            socket.send(packet);
                        } catch (SocketException se) {
                            // probablemente socket cerrado al detenerse; salir
                            running.set(false);
                            break;
                        }
                    }
                }
            } catch (LineUnavailableException e) {
                System.err.println("CallSender error: línea de audio no disponible - " + e.getMessage());
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("CallSender error: " + e.getMessage());
                }
            } finally {
                if (microphone != null) {
                    microphone.stop();
                    microphone.close();
                }
            }
        }

        /**
         * Intenta abrir el micrófono en el mixer por defecto y, si falla,
         * recorre todos los mixers disponibles hasta encontrar uno compatible.
         */
        private TargetDataLine openMicrophoneWithFallback() throws LineUnavailableException {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
            // 1) Intentar con el mixer por defecto
            try {
                TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(AUDIO_FORMAT);
                line.start();
                System.out.println("[Audio] Mic abierto en mixer por defecto.");
                return line;
            } catch (Exception ex) {
                System.err.println("[Audio] No se pudo abrir mic en mixer por defecto: " + ex.getMessage());
            }

            // 2) Intentar con mixers alternativos
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            for (Mixer.Info mi : mixers) {
                try {
                    Mixer m = AudioSystem.getMixer(mi);
                    if (m.isLineSupported(info)) {
                        TargetDataLine line = (TargetDataLine) m.getLine(info);
                        line.open(AUDIO_FORMAT);
                        line.start();
                        System.out.println("[Audio] Mic abierto en mixer: " + mi.getName());
                        return line;
                    }
                } catch (Exception ignored) {
                    // probar siguiente mixer
                }
            }
            return null;
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
            // No cerrar el socket UDP compartido aquí; solo detener el bucle.
            running.set(false);
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

                // usar timeout para poder salir al detenerse
                try {
                    socket.setSoTimeout(300);
                } catch (SocketException ignored) {}

                while (running.get()) {
                    try {
                        socket.receive(packet);
                        speakers.write(packet.getData(), 0, packet.getLength());
                    } catch (SocketTimeoutException ste) {
                        // despertar periódico para comprobar bandera
                        continue;
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
