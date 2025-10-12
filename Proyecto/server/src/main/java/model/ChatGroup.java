package model;

import java.util.HashSet;
import java.util.Set;

/**
 * Representa un grupo de chat dentro del servidor.
 * Contiene el nombre del grupo y los miembros asociados.
 */
public class ChatGroup {
    private final String name;
    private final Set<String> members = new HashSet<>();

    public ChatGroup(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Set<String> getMembers() {
        return members;
    }

    public void addMember(String username) {
        members.add(username);
    }

    public void removeMember(String username) {
        members.remove(username);
    }

    public boolean hasMember(String username) {
        return members.contains(username);
    }
}
