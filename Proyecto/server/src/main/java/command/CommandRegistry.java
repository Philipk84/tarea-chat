package command;

import interfaces.CommandHandler;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro centralizado de comandos que permite agregar y ejecutar comandos
 * siguiendo el patrón Command y el principio Open/Closed.
 */
public class CommandRegistry {
    private final List<CommandHandler> handlers = new ArrayList<>();

    /**
     * Registra un nuevo manejador de comando.
     * 
     * @param handler El manejador de comando a registrar
     * @throws IllegalArgumentException si el handler es null
     */
    public void registerHandler(CommandHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("El handler no puede ser null");
        }
        handlers.add(handler);
    }

    /**
     * Ejecuta el comando apropiado basado en el input del usuario.
     * 
     * @param command El comando completo ingresado por el usuario
     * @param userName El nombre del usuario que ejecuta el comando
     * @param clientHandler El manejador del cliente
     * @return true si el comando fue procesado, false si no se encontró un manejador
     */
    public boolean executeCommand(String command, String userName, Object clientHandler) {
        for (CommandHandler handler : handlers) {
            if (handler.canHandle(command)) {
                handler.execute(command, userName, clientHandler);
                return true;
            }
        }
        return false;
    }

    /**
     * Obtiene la lista de comandos disponibles registrados.
     * 
     * @return Lista de manejadores de comandos registrados
     */
    public List<CommandHandler> getRegisteredHandlers() {
        return new ArrayList<>(handlers);
    }
}