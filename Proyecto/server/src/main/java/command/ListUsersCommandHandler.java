package command;

import java.util.Set;

import interfaces.CommandHandler;
import model.ChatServer;
import model.ClientHandler;

public class ListUsersCommandHandler implements CommandHandler {

    /**
     * Verifica si este manejador puede procesar el comando dado.
     * 
     * @param command El comando a verificar
     * @return true si el comando es exactamente "/listusers"
     */
    @Override
    public boolean canHandle(String command) {
        return command.equals("/listusers");
    }

    /**
     * Ejecuta el comando para listar todos los usuarios conectados.
     * 
     * @param command El comando completo (debe ser "/listusers")
     * @param userName El nombre del usuario que solicita la lista
     * @param clientHandler El manejador del cliente
     */
    @Override
    public void execute(String command, String userName, ClientHandler clientHandler) {        
        Set<String> users = ChatServer.getUsers();
        
        if (users.isEmpty()) {
            clientHandler.sendMessage("No hay usuarios conectados.");
            return;
        }

        StringBuilder response = new StringBuilder("Usuarios conectados (");        
        response.append(users.size()).append("):");
        for (String user : users) {
            response.append("\n * ").append(user);
        }

        clientHandler.sendMessage(response.toString().trim());
    }
}
