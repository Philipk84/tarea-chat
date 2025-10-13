package interfaces;

import java.util.Set;

/**
 * Interface para manejo de grupos
 */
public interface GroupManager {
    /**
     * Crea un nuevo grupo con el nombre y creador especificados.
     * 
     * @param groupName Nombre del grupo a crear
     * @param creator Nombre del usuario que crea el grupo
     */
    void createGroup(String groupName, String creator);
    
    /**
     * Permite a un usuario unirse a un grupo existente.
     * 
     * @param groupName Nombre del grupo al que se desea unir
     * @param user Nombre del usuario que desea unirse al grupo
     */
    void joinGroup(String groupName, String user);

    /**
     * Obtiene los miembros de un grupo específico.
     * 
     * @param groupName Nombre del grupo
     * @return Conjunto de nombres de usuario de los miembros del grupo
     */
    Set<String> getGroupMembers(String groupName);

    /**
     * Obtiene todos los grupos existentes.
     * 
     * @return Conjunto de nombres de todos los grupos
     */
    Set<String> getGroups();

}