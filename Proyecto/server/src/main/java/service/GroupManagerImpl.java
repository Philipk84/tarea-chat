package service;

import interfaces.GroupManager;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import model.ChatGroup;

/**
 * Implementación concreta del gestor de grupos para el sistema de chat.
 * Administra la creación, membresía y operaciones relacionadas con grupos de chat.
 */
public class GroupManagerImpl implements GroupManager {
    /**
     * Mapa de grupos: nombre del grupo -> conjunto de nombres de usuario miembros
     */
    private final Map<String, Set<String>> groups = new ConcurrentHashMap<>();

    /**
     * Crea un nuevo grupo de chat con el usuario especificado como creador.
     * Si el grupo ya existe, simplemente añade al creador como miembro.
     * 
     * @param groupName Nombre del grupo a crear
     * @param creator Nombre del usuario que crea el grupo
     */
    @Override
    public void createGroup(String groupName, String creator) {
        groups.putIfAbsent(groupName, ConcurrentHashMap.newKeySet());
        groups.get(groupName).add(creator);
        System.out.println("Grupo creado: " + groupName + " por " + creator);
    }

    /**
     * Añade un usuario a un grupo existente o crea el grupo si no existe.
     * 
     * @param groupName Nombre del grupo al que unirse
     * @param user Nombre del usuario que se une al grupo
     */
    @Override
    public void joinGroup(String groupName, String user) {
        groups.putIfAbsent(groupName, ConcurrentHashMap.newKeySet());
        groups.get(groupName).add(user);
        System.out.println(user + " se unió al grupo " + groupName);
    }

    /**
     * Obtiene el conjunto de miembros de un grupo específico.
     * 
     * @param groupName Nombre del grupo
     * @return Conjunto de nombres de usuario miembros del grupo o conjunto vacío si no existe
     */
    @Override
    public Set<String> getGroupMembers(String groupName) {
        return groups.getOrDefault(groupName, Collections.emptySet());
    }

    /**
     * Obtiene el conjunto de todos los grupos disponibles en el sistema.
     * 
     * @return Conjunto de nombres de grupos existentes
     */
    @Override
    public Set<String> getGroups() {
        return groups.keySet();
    }

}