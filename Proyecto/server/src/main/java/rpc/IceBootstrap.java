package rpc;

import Chat.Call;
import com.zeroc.Ice.*;
import com.zeroc.Ice.Exception;

import model.ChatServer;

public final class IceBootstrap {

    private static Communicator communicator;

    public static void start(ChatServer chatServer) {
        Thread t = new Thread(() -> {
            try {
                // Inicializar con propiedades para habilitar callbacks bidireccionales
                InitializationData initData = new InitializationData();
                initData.properties = Util.createProperties();
                
                // Habilitar ACM (Active Connection Management) para mantener conexiones vivas
                initData.properties.setProperty("Ice.ACM.Client.Timeout", "0");
                initData.properties.setProperty("Ice.ACM.Client.Heartbeat", "3");
                
                communicator = Util.initialize(initData);

                // Crear adapter con soporte WebSocket bidireccional
                ObjectAdapter adapter =
                        communicator.createObjectAdapterWithEndpoints(
                                "CallAdapter",
                                "ws -p 10010 -r /call"
                        );

                Call servant = new CallI(chatServer);
                adapter.add(servant, Util.stringToIdentity("Call"));

                adapter.activate();

                System.out.println("[ICE] CallAdapter escuchando en ws://0.0.0.0:10010/call");
                System.out.println("[ICE] Soporte bidireccional habilitado para callbacks");

                communicator.waitForShutdown();
            } catch (Exception e) {
                System.err.println("[ICE] Error en CallAdapter: " + e.getMessage());
                e.printStackTrace();
            }
        }, "Ice-CallAdapter");

        t.setDaemon(true);
        t.start();
    }

    public static void stop() {
        if (communicator != null) {
            communicator.destroy();
            communicator = null;
        }
    }
}
