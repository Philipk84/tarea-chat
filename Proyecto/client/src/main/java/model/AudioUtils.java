package model;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayOutputStream;

public class AudioUtils {
    // Formato estándar de audio: 44100 Hz, 16 bits, mono, signed, big endian, sacado del proyecto del profesor
    private static final AudioFormat FORMAT = new AudioFormat(44100, 16, 1, true, true);

    /**
     * Graba audio desde el micrófono durante 'seconds' segundos
     * y devuelve los datos en un arreglo de bytes.
     */
    public static byte[] recordAudio(int seconds) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); // Almacena bytes en memoria del audio (Contenedor del audio grabado)
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT); // Información sobre la línea de datos
            TargetDataLine mic = (TargetDataLine) AudioSystem.getLine(info); // Línea de entrada de audio (micrófono)
            mic.open(FORMAT);
            mic.start();

            System.out.println("Grabando audio durante " + seconds + " segundos...");
            byte[] buffer = new byte[1024];
            long endTime = System.currentTimeMillis() + (seconds * 1000);

            // Leer datos del micrófono y escribirlos en el ByteArrayOutputStream
            while (System.currentTimeMillis() < endTime) {
                int bytesRead = mic.read(buffer, 0, buffer.length);
                out.write(buffer, 0, bytesRead);
            }

            mic.stop();
            mic.close();
            System.out.println("Grabación finalizada.");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    /**
     * Reproduce audio desde un arreglo de bytes.
     */
    public static void playAudio(byte[] audioData) {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT); // Información sobre la línea de datos
            SourceDataLine speaker = (SourceDataLine) AudioSystem.getLine(info); // Línea de salida de audio (altavoz)
            speaker.open(FORMAT);
            speaker.start();

            // Reproducir los datos de audio
            System.out.println("Reproduciendo nota de voz...");
            speaker.write(audioData, 0, audioData.length);
            speaker.drain();
            speaker.stop();
            speaker.close();
            System.out.println("Reproducción finalizada.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
