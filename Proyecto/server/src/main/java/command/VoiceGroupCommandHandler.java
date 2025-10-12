package command;

import interfaces.CommandHandler;
import model.*;
import java.net.*;
import java.util.Set;

/**
 * Comando /voicegroup <grupo>
 * Envía una nota de voz por UDP a todos los miembros del grupo.
 */
public class VoiceGroupCommandHandler implements CommandHandler {

    @Override
    public String getCommandName() {
        return "/voicegroup";
    }

    @Override
    public boolean execute(String[] args, String sender, ClientHandler clientHandler) {
        if (args.length < 2) {
            clientHandler.sendMessage("Uso correcto: /voicegroup <grupo>");
            return true;
        }

        String groupName = args[1].trim();
        Set<String> members = ChatServer.getGroupMembers(groupName);

        if (members == null || members.isEmpty()) {
            clientHandler.sendMessage("Error: El grupo '" + groupName + "' no existe o está vacío.");
            return true;
        }

        int senderPort = ChatServer.getUserUdpPort(sender);
        if (senderPort == -1) {
            clientHandler.sendMessage("Error: No has registrado tu puerto UDP (/udpport <puerto>).");
            return true;
        }

        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress senderAddress = clientHandler.getClientSocket().getInetAddress();

            clientHandler.sendMessage("🎤 Listo para enviar audio al grupo " + groupName + "...");

            byte[] buffer = new byte[4096];
            DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);

            // Recibir un único paquete de audio del emisor
            socket.receive(incoming);
            System.out.println("[VoiceGroup] Audio recibido desde " + sender);

            // Reenviar a todos los miembros del grupo (menos el emisor)
            for (String member : members) {
                if (!member.equals(sender)) {
                    String info = ChatServer.getUdpInfo(member);
                    if (info == null) continue;

                    String[] parts = info.split(":");
                    InetAddress targetAddress = InetAddress.getByName(parts[0]);
                    int targetPort = Integer.parseInt(parts[1]);

                    DatagramPacket packetToSend = new DatagramPacket(
                            incoming.getData(),
                            incoming.getLength(),
                            targetAddress,
                            targetPort
                    );
                    socket.send(packetToSend);
                }
            }

            socket.close();
            clientHandler.sendMessage("✅ Nota de voz enviada al grupo " + groupName + ".");

        } catch (Exception e) {
            clientHandler.sendMessage("Error al enviar nota de voz grupal: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}
