package command;

import interfaces.CommandHandler;
import model.ClientHandler;

/**
 * Manejador del comando /help que muestra todos los comandos disponibles
 * y su descripción de uso.
 */
public class HelpCommandHandler implements CommandHandler {

    /**
     * Verifica si este manejador puede procesar el comando dado.
     * 
     * @param command El comando a verificar
     * @return true si el comando es exactamente "/help"
     */
    @Override
    public boolean canHandle(String command) {
        return command.equals("/help");
    }

    /**
     * Ejecuta el comando para mostrar la ayuda con todos los comandos disponibles.
     * 
     * @param command El comando completo (debe ser "/help")
     * @param userName El nombre del usuario que solicita ayuda
     * @param clientHandler El manejador del cliente
     */
    @Override
    public void execute(String command, String userName, Object clientHandler) {
        if (!(clientHandler instanceof ClientHandler)) return;
        
        ClientHandler handler = (ClientHandler) clientHandler;
        showMenu(handler);
    }

    /**
     * Muestra el menú de comandos disponibles al usuario.
     * 
     * @param handler El manejador del cliente para enviar los mensajes
     */
    private void showMenu(ClientHandler handler) {
        handler.sendMessage("=== COMANDOS DISPONIBLES ===");
        handler.sendMessage("");
        handler.sendMessage("CONFIGURACIÓN:");
        handler.sendMessage("/udpport <puerto>         -> registrar puerto UDP local para audio");
        handler.sendMessage("");
        handler.sendMessage("LLAMADAS:");
        handler.sendMessage("/call <usuario>           -> iniciar llamada privada");
        handler.sendMessage("/callgroup <grupo>        -> iniciar llamada grupal");
        handler.sendMessage("/endcall [callId]         -> terminar llamada actual o específica");
        handler.sendMessage("");
        handler.sendMessage("GRUPOS:");
        handler.sendMessage("/creategroup <nombre>     -> crear nuevo grupo");
        handler.sendMessage("/joingroup <nombre>       -> unirse a grupo existente");
        handler.sendMessage("/listgroups               -> listar todos los grupos");
        handler.sendMessage("");
        handler.sendMessage("SISTEMA:");
        handler.sendMessage("/help                     -> mostrar esta ayuda");
        handler.sendMessage("/quit                     -> desconectar del servidor");
        handler.sendMessage("");
        handler.sendMessage("=== FIN DE COMANDOS ===");
    }
}