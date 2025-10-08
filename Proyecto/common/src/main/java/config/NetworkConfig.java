package config;

/**
 * Clase de configuración de red compartida entre cliente y servidor.
 * Encapsula la información de conexión necesaria para establecer comunicación TCP/UDP.
 * Esta clase es inmutable y thread-safe, garantizando la integridad de la configuración.
 */
public class NetworkConfig {
    private final String host;
    private final int port;

    /**
     * Construye una nueva configuración de red con validación de parámetros.
     * 
     * @param host Dirección IP o nombre del host (no puede ser null o vacío)
     * @param port Número de puerto (debe estar entre 1 y 65535)
     * @throws IllegalArgumentException Si el host es null/vacío o el puerto está fuera del rango válido
     */
    public NetworkConfig(String host, int port) {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("El host no puede ser nulo o vacío");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("El puerto debe estar entre 1 y 65535");
        }
        
        this.host = host;
        this.port = port;
    }

    /**
     * Obtiene la dirección del host configurada.
     * 
     * @return Dirección del host como String
     */
    public String getHost() {
        return host;
    }

    /**
     * Obtiene el puerto configurado.
     * 
     * @return Número de puerto como entero
     */
    public int getPort() {
        return port;
    }

    /**
     * Genera una representación en cadena de la configuración de red.
     * 
     * @return Cadena con formato "NetworkConfig{host='<host>', port=<port>}"
     */
    @Override
    public String toString() {
        return "NetworkConfig{host='" + host + "', port=" + port + "}";
    }

    /**
     * Compara esta configuración de red con otro objeto para determinar igualdad.
     * Dos configuraciones son iguales si tienen el mismo host y puerto.
     * 
     * @param o Objeto a comparar con esta configuración
     * @return true si los objetos son iguales, false en caso contrario
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkConfig that = (NetworkConfig) o;
        return port == that.port && host.equals(that.host);
    }

    /**
     * Genera un código hash para esta configuración de red.
     * El hash se basa en el host y el puerto para mantener consistencia con equals().
     * 
     * @return Código hash como entero
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(host, port);
    }
}