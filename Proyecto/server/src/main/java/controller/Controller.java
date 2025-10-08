package controller;

import model.*;
import com.google.gson.Gson;
import java.io.FileReader;

public class Controller {
    private Gson gson;
    private Config config;
    private Server server;

    public Controller() {
        this.gson = new Gson();
        try {
            this.config = gson.fromJson(new FileReader("config.json"), Config.class);
            this.server = Server.getInstance(config);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing controller", e);
        }
    }

    public String startServer () {
        return server.startServer();
    }

    public String closeServer () {
        return server.closeServer();
    }
}
