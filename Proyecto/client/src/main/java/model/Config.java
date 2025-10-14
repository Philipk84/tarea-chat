package model;

public class Config {
    private String host;
    private int port;
    private int voicePort;

    public Config(String host, int port) {
        this.host = host;
        this.port = port;
        this.voicePort = voicePort;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public int getVoicePort() { return voicePort; }
}
