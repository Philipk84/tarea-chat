package com.chatapp.model;

import java.util.ArrayList;
import java.util.List;

public class ChatGroup {
    private String name;
    private List<String> members;

    public ChatGroup(String name, List<String> members) {
        this.name = name;
        this.members = new ArrayList<>(members);
    }

    public String getName() { return name; }
    public List<String> getMembers() { return members; }

    public void addMember(String user) {
        if (!members.contains(user)) members.add(user);
    }
}
