package com.chatapp.model;

import java.util.List;

/**
 * Representa un chat entre dos usuarios.
 */
public class DirectChat {
    private String user1;
    private String user2;
    private List<Message> messages;

    public DirectChat(String user1, String user2, List<Message> messages) {
        this.user1 = user1;
        this.user2 = user2;
        this.messages = messages;
    }

    public String getChatName() {
        return user1 + "_" + user2;
    }

    public List<Message> getMessages() {
        return messages;
    }
}
