package controller;

import model.*;

public class Controller {
    private Gson gson;
    private Config config;
    private ChatServer server;

    public Controller() {
        this.gson = new Gson();
        this.config = gson.fromJson(new FileReader("config.json"), Config.class);
        this.server = ChatServer.getInstance(config);
    }

    public String startServer () {
        server.startServer();
        return "Servidor Iniciado en " + config.getHost() + " : " + config.getPort();
    }
}
