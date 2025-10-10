package command;

import interfaces.CommandHandler;
import model.ClientHandler;
import model.ChatServer;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador del comando /voicegroup que notifica a todos los miembros
 * de un grupo que estÃ¡n por recibir una nota de voz vÃ­a UDP.
 */
public class VoiceGroupCommandHandler implements CommandHandler {

    /**
     * Verifica si este manejador puede procesar el comando dado.
     * 
     * @param command El comando a verificar
     * @return true si el comando inicia con "/voicegroup "
     */
    @Override
    public boolean canHandle(String command) {
        return command.startsWith("/voicegroup ");
    }

    /**
     * Ejecuta el comando de nota de voz grupal.
     * Formato: /voicegroup <grupo>
     * 
     * @param command El comando completo
     * @param userName El nombre del usuario que envÃ­a la nota
     * @param clientHandler El manejador del cliente
     */
    @Override
    public void execute(String command, String userName, ClientHandler clientHandler) {
        String[] parts = command.split(" ", 2);
        
        if (parts.length < 2) {
            clientHandler.sendMessage("Error: Formato correcto -> /voicegroup <grupo>");
            return;
        }
        
        String groupName = parts[1].trim();
        
        if (groupName.isEmpty()) {
            clientHandler.sendMessage("Error: Debes especificar un nombre de grupo vÃ¡lido");
            return;
        }
        
        // Obtener miembros del grupo
        Set<String> members = ChatServer.getGroupMembers(groupName);
        
        if (members == null || members.isEmpty()) {
            clientHandler.sendMessage("Error: El grupo '" + groupName + "' no existe o no tiene miembros");
            return;
        }
        
        // Verificar que el usuario es miembro del grupo
        if (!members.contains(userName)) {
            clientHandler.sendMessage("Error: No eres miembro del grupo '" + groupName + "'");
            return;
        }
        
        // Obtener informaciÃ³n UDP del remitente
        String senderUdpInfo = ChatServer.getUdpInfo(userName);
        
        if (senderUdpInfo == null) {
            clientHandler.sendMessage("Error: Tu informaciÃ³n UDP no estÃ¡ registrada");
            return;
        }
        
        // Recopilar informaciÃ³n UDP de los miembros disponibles
        Map<String, String> membersUdpInfo = new HashMap<>();
        for (String member : members) {
            if (!member.equals(userName)) {
                String udpInfo = ChatServer.getUdpInfo(member);
                if (udpInfo != null) {
                    membersUdpInfo.put(member, udpInfo);
                }
            }
        }
        
        if (membersUdpInfo.isEmpty()) {
            clientHandler.sendMessage("Error: No hay miembros disponibles con UDP en el grupo");
            return;
        }
        
        // Notificar a cada miembro del grupo
        for (Map.Entry<String, String> entry : membersUdpInfo.entrySet()) {
            String member = entry.getKey();
            ClientHandler memberHandler = ChatServer.getClientHandler(member);
            if (memberHandler != null) {
                memberHandler.sendMessage("VOICE_NOTE_GROUP_INCOMING from " + userName + " in " + groupName + " " + senderUdpInfo);
            }
        }
        
        // Enviar lista de destinatarios al remitente
        StringBuilder targets = new StringBuilder();
        targets.append("VOICE_NOTE_GROUP_TARGETS ").append(groupName).append(" ");
        boolean first = true;
        for (Map.Entry<String, String> entry : membersUdpInfo.entrySet()) {
            if (!first) targets.append(",");
            targets.append(entry.getKey()).append(":").append(entry.getValue());
            first = false;
        }
        
        clientHandler.sendMessage(targets.toString());
    }
}
