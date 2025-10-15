package service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio para registrar el historial de mensajes (texto y audios) en JSON.
 * Escribe en formato NDJSON (una entrada JSON por línea) para simplificar la concurrencia.
 */
public class HistoryService {
    private static final String HISTORY_DIR = "Proyecto" + File.separator + "server" + File.separator + "data";
    private static final String HISTORY_FILE = HISTORY_DIR + File.separator + "history.jsonl"; // NDJSON (JSON Lines)
    private static final String VOICE_DIR = HISTORY_DIR + File.separator + "voice";

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private static final Object lock = new Object();

    private static void ensureDirs() throws IOException {
        Files.createDirectories(Paths.get(HISTORY_DIR));
        Files.createDirectories(Paths.get(VOICE_DIR));
        // Asegurar existencia del archivo
        Path historyPath = Paths.get(HISTORY_FILE);
        if (!Files.exists(historyPath)) {
            Files.createFile(historyPath);
        }
    }

    private static void appendLine(String jsonLine) {
        synchronized (lock) {
            try {
                ensureDirs();
                try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(HISTORY_FILE), StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND)) {
                    bw.write(jsonLine);
                    bw.newLine();
                }
            } catch (IOException e) {
                System.err.println("[HistoryService] Error escribiendo historial: " + e.getMessage());
            }
        }
    }

    private static String isoNow() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }

    public static void logTextPrivate(String sender, String recipient, String text) {
        Map<String, Object> entry = baseEntry("text", "private", sender);
        entry.put("recipient", recipient);
        entry.put("message", text);
        appendLine(gson.toJson(entry));
    }

    public static void logTextGroup(String sender, String groupName, String text) {
        Map<String, Object> entry = baseEntry("text", "group", sender);
        entry.put("group", groupName);
        entry.put("message", text);
        appendLine(gson.toJson(entry));
    }

    public static void logVoiceNote(String sender, String recipient, String relativeFilePath, long sizeBytes) {
        Map<String, Object> entry = baseEntry("voice_note", "private", sender);
        entry.put("recipient", recipient);
        entry.put("audioFile", relativeFilePath);
        entry.put("sizeBytes", sizeBytes);
        appendLine(gson.toJson(entry));
    }

    public static void logVoiceGroup(String sender, String groupName, String relativeFilePath, long sizeBytes) {
        Map<String, Object> entry = baseEntry("voice_group", "group", sender);
        entry.put("group", groupName);
        entry.put("audioFile", relativeFilePath);
        entry.put("sizeBytes", sizeBytes);
        appendLine(gson.toJson(entry));
    }

    public static void logCallStarted(String callId, Iterable<String> participants) {
        Map<String, Object> entry = baseEntry("call_started", "call", "server");
        entry.put("callId", callId);
        entry.put("participants", participants);
        appendLine(gson.toJson(entry));
    }

    public static void logCallEnded(String callId, Iterable<String> participants, String requester) {
        Map<String, Object> entry = baseEntry("call_ended", "call", "server");
        entry.put("callId", callId);
        entry.put("participants", participants);
        entry.put("endedBy", requester);
        appendLine(gson.toJson(entry));
    }

    private static Map<String, Object> baseEntry(String type, String scope, String sender) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("id", newId());
        entry.put("timestamp", isoNow());
        entry.put("type", type);
        entry.put("scope", scope);
        entry.put("sender", sender);
        return entry;
    }

    /**
     * Guarda bytes de audio en un archivo bajo data/voice y devuelve la ruta relativa para el JSON.
     */
    public static SavedAudio saveVoiceBytes(byte[] data) throws IOException {
        ensureDirs();
        String fileName = "voice-" + System.currentTimeMillis() + "-" + UUID.randomUUID() + ".bin";
        Path filePath = Paths.get(VOICE_DIR, fileName);
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(data);
        }
        long size = Files.size(filePath);
        // Devolver ruta relativa desde Proyecto (para que sea portable en logs)
        String relative = "server" + File.separator + "data" + File.separator + "voice" + File.separator + fileName;
        return new SavedAudio(relative, size);
    }

    public record SavedAudio(String relativePath, long sizeBytes) {}
}
