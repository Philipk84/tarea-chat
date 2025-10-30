package model;

import javax.sound.sampled.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class VoicePlayer {
    
    /**
     * Formato de audio para notas de voz: PCM 16000 Hz, 16 bits, mono.
     */
    public static final AudioFormat VOICE_FORMAT = new AudioFormat(44100, 16, 1, true, false);
    
    /**
     * Tamaño del buffer para captura de audio.
     */
    private static final int BUFFER_SIZE = 1024;

    private final AtomicBoolean playing = new AtomicBoolean(false);

    /**
     * Reproduce una nota de voz desde datos de audio.
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
                DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, VOICE_FORMAT);
                speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
                speakers.open(VOICE_FORMAT);
                speakers.start();

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
}