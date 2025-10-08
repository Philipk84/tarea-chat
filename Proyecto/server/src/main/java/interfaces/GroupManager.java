package interfaces;

import java.util.Set;

/**
 * Interface para manejo de grupos
 */
public interface GroupManager {
    void createGroup(String groupName, String creator);
    void joinGroup(String groupName, String user);
    Set<String> getGroupMembers(String groupName);
    Set<String> getGroups();
}