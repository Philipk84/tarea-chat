package model;

import javax.sound.sampled.*;
import java.net.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Clase para manejar notas de voz usando DatagramSocket y DatagramPacket.
 * Usa la misma lógica de audio que class/MicCapture.java y class/WavPlayback.java
 * pero adaptada para envío UDP.
 */
public class VoiceNote {
    
    /**
     * Formato de audio para notas de voz: PCM 16000 Hz, 16 bits, mono.
     * Similar a class/MicCapture.java pero optimizado para red.
     */
    public static final AudioFormat VOICE_FORMAT = new AudioFormat(16000.0f, 16, 1, true, false);
    
    /**
     * Tamaño del buffer para captura de audio (como en class/MicCapture.java).
     */
    private static final int BUFFER_SIZE = 1024;

    /**
     * Grabador de notas de voz que captura audio del micrófono.
     * Usa la misma lógica que class/MicCapture.java.
     */
    public static class VoiceRecorder {
        private final AtomicBoolean recording = new AtomicBoolean(false);
        private ByteArrayOutputStream audioBuffer;
        private TargetDataLine microphone;

        /**
         * Inicia la grabación de una nota de voz.
         * 
         * @return true si la grabación inició correctamente
         */
        public boolean startRecording() {
            if (recording.get()) {
                return false;
            }

            try {
                // Configurar línea de captura como en class/MicCapture.java
                DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, VOICE_FORMAT);
                microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
                microphone.open(VOICE_FORMAT);
                microphone.start();

                audioBuffer = new ByteArrayOutputStream();
                recording.set(true);

                // Hilo de grabación (como en class/MicCapture.java)
                Thread recordingThread = new Thread(() -> {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    
                    while (recording.get()) {
                        int bytesRead = microphone.read(buffer, 0, buffer.length);
                        if (bytesRead > 0) {
                            audioBuffer.write(buffer, 0, bytesRead);
                        }
                    }
                });
                recordingThread.start();

                return true;
            } catch (LineUnavailableException e) {
                System.err.println("Error iniciando grabación: " + e.getMessage());
                return false;
            }
        }

        /**
         * Detiene la grabación y retorna los datos de audio.
         * 
         * @return Array de bytes con el audio grabado
         */
        public byte[] stopRecording() {
            if (!recording.get()) {
                return new byte[0];
            }

            recording.set(false);

            if (microphone != null) {
                microphone.stop();
                microphone.close();
            }

            return audioBuffer != null ? audioBuffer.toByteArray() : new byte[0];
        }

        /**
         * Verifica si está grabando actualmente.
         * 
         * @return true si está grabando
         */
        public boolean isRecording() {
            return recording.get();
        }
    }

    /**
     * Reproductor de notas de voz que reproduce audio desde datos.
     * Usa la misma lógica que class/WavPlayback.java pero para datos en memoria.
     */
    public static class VoicePlayer {
        private final AtomicBoolean playing = new AtomicBoolean(false);

        /**
         * Reproduce una nota de voz desde datos de audio.
         * Usa la misma lógica que class/WavPlayback.java.
         * 
         * @param audioData Datos de audio a reproducir
         */
        public void playVoiceNote(byte[] audioData) {
            if (playing.get() || audioData == null || audioData.length == 0) {
                return;
            }

            playing.set(true);

            Thread playbackThread = new Thread(() -> {
                SourceDataLine speakers = null;
                try {
                    // Configurar línea de reproducción como en class/WavPlayback.java
                    DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, VOICE_FORMAT);
                    speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
                    speakers.open(VOICE_FORMAT);
                    speakers.start();

                    // Reproducir en bloques pequeños como en class/WavPlayback.java
                    int offset = 0;
                    while (offset < audioData.length && playing.get()) {
                        int length = Math.min(BUFFER_SIZE, audioData.length - offset);
                        speakers.write(audioData, offset, length);
                        offset += length;
                    }

                } catch (LineUnavailableException e) {
                    System.err.println("Error reproduciendo nota de voz: " + e.getMessage());
                } finally {
                    playing.set(false);
                    if (speakers != null) {
                        speakers.drain();
                        speakers.stop();
                        speakers.close();
                    }
                }
            });
            playbackThread.start();
        }

        /**
         * Detiene la reproducción actual.
         */
        public void stopPlayback() {
            playing.set(false);
        }

        /**
         * Verifica si está reproduciendo actualmente.
         * 
         * @return true si está reproduciendo
         */
        public boolean isPlaying() {
            return playing.get();
        }
    }

    /**
     * Enviador de notas de voz que envía audio vía UDP.
     * Usa DatagramSocket y DatagramPacket como en class/UDPclient.java.
     */
    public static class VoiceSender {
        private final DatagramSocket socket;

        public VoiceSender(DatagramSocket socket) {
            this.socket = socket;
        }

        /**
         * Envía una nota de voz a un usuario específico vía UDP.
         * Usa DatagramPacket como en class/UDPclient.java.
         * 
         * @param audioData Datos de audio a enviar
         * @param target Usuario destinatario
         * @param serverAddress Dirección del servidor
         * @param serverPort Puerto UDP del servidor
         */
        public void sendVoiceNote(byte[] audioData, String target, InetAddress serverAddress, int serverPort) {
            try {
                // Formato: VOICE_NOTE:target:audioData
                String header = "VOICE_NOTE:" + target + ":";
                byte[] headerBytes = header.getBytes();
                
                // Combinar header y audio
                byte[] fullMessage = new byte[headerBytes.length + audioData.length];
                System.arraycopy(headerBytes, 0, fullMessage, 0, headerBytes.length);
                System.arraycopy(audioData, 0, fullMessage, headerBytes.length, audioData.length);

                // Enviar como DatagramPacket (como en class/UDPclient.java)
                DatagramPacket packet = new DatagramPacket(
                    fullMessage, fullMessage.length, serverAddress, serverPort
                );
                socket.send(packet);

            } catch (IOException e) {
                System.err.println("Error enviando nota de voz: " + e.getMessage());
            }
        }

        /**
         * Envía una nota de voz a un grupo vía UDP.
         * 
         * @param audioData Datos de audio a enviar
         * @param groupName Nombre del grupo destinatario
         * @param serverAddress Dirección del servidor
         * @param serverPort Puerto UDP del servidor
         */
        public void sendVoiceNoteToGroup(byte[] audioData, String groupName, InetAddress serverAddress, int serverPort) {
            try {
                // Formato: VOICE_GROUP:groupName:audioData
                String header = "VOICE_GROUP:" + groupName + ":";
                byte[] headerBytes = header.getBytes();
                
                // Combinar header y audio
                byte[] fullMessage = new byte[headerBytes.length + audioData.length];
                System.arraycopy(headerBytes, 0, fullMessage, 0, headerBytes.length);
                System.arraycopy(audioData, 0, fullMessage, headerBytes.length, audioData.length);

                // Enviar como DatagramPacket (como en class/UDPclient.java)
                DatagramPacket packet = new DatagramPacket(
                    fullMessage, fullMessage.length, serverAddress, serverPort
                );
                socket.send(packet);

            } catch (IOException e) {
                System.err.println("Error enviando nota de voz al grupo: " + e.getMessage());
            }
        }
    }
}