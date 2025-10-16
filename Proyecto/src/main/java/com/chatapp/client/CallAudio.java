package com.chatapp.client;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utilities to send audio via UDP (CallSender) and receive/play audio via UDP (CallReceiver).
 *
 * Audio format: PCM 16000 Hz, 16 bits, mono.
 */
public class CallAudio {

    public static final AudioFormat AUDIO_FORMAT = new AudioFormat(16000.0f, 16, 1, true, false);

    public static class CallSender implements Runnable {
        private final DatagramSocket socket;
        private final List<InetSocketAddress> peers;
        private final AtomicBoolean running = new AtomicBoolean(true);

        public CallSender(DatagramSocket socket, List<InetSocketAddress> peers) {
            this.socket = socket;
            this.peers = peers;
        }

        public void stop() {
            running.set(false);
        }

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
                    if (read <= 0) continue;
                    DatagramPacket packet = new DatagramPacket(buffer, read);
                    // send to each peer
                    for (InetSocketAddress peer : peers) {
                        try {
                            packet.setSocketAddress(peer);
                            socket.send(packet);
                        } catch (SocketException se) {
                            // Likely closed/ending; exit loop
                            running.set(false);
                            break;
                        }
                    }
                }
            } catch (LineUnavailableException e) {
                System.err.println("CallSender error: audio line unavailable - " + e.getMessage());
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
    }

    public static class CallReceiver implements Runnable {
        private final DatagramSocket socket;
        private final AtomicBoolean running = new AtomicBoolean(true);

        public CallReceiver(DatagramSocket socket) {
            this.socket = socket;
        }

        public void stop() {
            // Do NOT close the shared UDP socket here because the sender uses the same socket.
            // Just signal the loop to stop. The run() method uses SO_TIMEOUT to exit promptly.
            running.set(false);
        }

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

                // Use a timeout so we can check the running flag and exit gracefully
                try {
                    socket.setSoTimeout(300);
                } catch (SocketException ignored) {}

                while (running.get()) {
                    try {
                        socket.receive(packet);
                        speakers.write(packet.getData(), 0, packet.getLength());
                    } catch (SocketTimeoutException ste) {
                        // Periodic wake-up to check running flag
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
