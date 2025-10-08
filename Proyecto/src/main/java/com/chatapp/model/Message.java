package com.chatapp.model;

public class Message {
    private String from;
    private String to;
    private String content;
    private boolean isGroup;

    public Message(String from, String to, String content, boolean isGroup) {
        this.from = from;
        this.to = to;
        this.content = content;
        this.isGroup = isGroup;
    }

    public String getFrom() { return from; }
    public String getTo() { return to; }
    public String getContent() { return content; }
    public boolean isGroup() { return isGroup; }
}
