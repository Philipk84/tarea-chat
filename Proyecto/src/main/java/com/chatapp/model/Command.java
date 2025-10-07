package com.chatapp.model;

/**
 * Representa comandos que el cliente puede enviar al servidor.
 */
public class Command {
    private String action;
    private String target;
    private String content;

    public Command(String action, String target, String content) {
        this.action = action;
        this.target = target;
        this.content = content;
    }

    public String getAction() { return action; }
    public String getTarget() { return target; }
    public String getContent() { return content; }
}
