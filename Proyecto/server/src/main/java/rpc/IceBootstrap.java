package rpc;

import com.zeroc.Ice.*;
import com.zeroc.Ice.Exception;

import model.ChatServer;

public final class IceBootstrap {

    private static Communicator communicator;

    public static void start(ChatServer chatServer) {
        Thread t = new Thread(() -> {
            try {
                String[] args = {};
                communicator = Util.initialize(args);

                // Adaptador sobre WebSockets, por ejemplo puerto 10010
                ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                    "CallAdapter",
                    "ws -p 10010 -r /call"
                );

                CallI servant = new CallI(chatServer);
                adapter.add(servant, Util.stringToIdentity("Call"));
                adapter.activate();

                System.out.println("[ICE] Adaptador CallAdapter escuchando en ws://localhost:10010/call");
                communicator.waitForShutdown();

            } catch (Exception e) {
                System.err.println("[ICE] Error en adaptador: " + e.getMessage());
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
