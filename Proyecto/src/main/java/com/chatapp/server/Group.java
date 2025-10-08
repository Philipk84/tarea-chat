package com.chatapp.server;

import java.util.ArrayList;
import java.util.List;

public class Group {
    private String name;
    private final List<ClientHandler> members = new ArrayList<>();

    public Group(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public synchronized void addMember(ClientHandler client) {
        if (!members.contains(client)) {
            members.add(client);
        }
    }

    public synchronized void removeMember(ClientHandler client) {
        members.remove(client);
    }

    public synchronized List<ClientHandler> getMembers() {
        return new ArrayList<>(members);
    }

    public synchronized void broadcast(String message, String sender) {
        for (ClientHandler member : members) {
            member.sendMessage("[" + name + "] " + sender + ": " + message);
        }
    }
}
