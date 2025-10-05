package controller;

import model.*;
import com.google.gson.Gson;
import java.io.FileReader;

public class Controller {
    private Gson gson;
    private Config config;
    private Client client;

    public Controller() {
        this.gson = new Gson();
        try {
            this.config = gson.fromJson(new FileReader("config.json"), Config.class);
            this.client = Client.getInstance(config, "defaultUser");
        } catch (Exception e) {
            throw new RuntimeException("Error initializing controller", e);
        }
    }
}
