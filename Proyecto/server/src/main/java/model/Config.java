package model;

/**
 * Clase de configuración para almacenar parámetros de conexión del servidor.
 * Contiene la información necesaria para establecer conexiones de red.
 */
public class Config {
    private String host;
    private int port;
    private int voicePort;

    /**
     * Constructor para crear una configuración con host y puerto específicos.
     * 
     * @param host Dirección IP o nombre del host
     * @param port Número de puerto para la conexión
     */
    public Config(String host, int port) {
        this.host = host;
        this.port = port;
        this.voicePort = voicePort;
    }

    /**
     * Obtiene la dirección del host configurada.
     * 
     * @return Dirección del host
     */
    public String getHost() {
        return host;
    }
    
    /**
     * Obtiene el puerto configurado.
     * 
     * @return Número de puerto
     */
    public int getPort() {
        return port;
    }

    /**
     * Establece una nueva dirección de host.
     * 
     * @param host Nueva dirección del host
     */
    public void setHost(String host) {
        this.host = host;
    }
    
    /**
     * Establece un nuevo puerto.
     * 
     * @param port Nuevo número de puerto
     */
    public void setPort(int port) {
        this.port = port;
    }

    public int getVoicePort() {
        return voicePort;
    }

    public void setVoicePort(int voicePort) {
        this.voicePort = voicePort;
    }
}