package com.chatapp.model;

import com.chatapp.utils.JsonUtils;
import java.io.*;
import java.util.*;

public class ChatHistory {
    private static final String PATH = "data/chat_history.json";
    private Map<String, List<Message>> history = new HashMap<>();

    public ChatHistory() {
        load();
    }

    public void addMessage(String chat, Message msg) {
        history.computeIfAbsent(chat, k -> new ArrayList<>()).add(msg);
        save();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        try {
            File f = new File(PATH);
            if (!f.exists()) return;
            String json = new String(java.nio.file.Files.readAllBytes(f.toPath()));
            history = JsonUtils.fromJson(json, Map.class);
        } catch (Exception ignored) {}
    }

    private void save() {
        try {
            File dir = new File("data");
            if (!dir.exists()) dir.mkdir();
            FileWriter fw = new FileWriter(PATH);
            fw.write(JsonUtils.toJson(history));
            fw.close();
        } catch (IOException ignored) {}
    }
}
