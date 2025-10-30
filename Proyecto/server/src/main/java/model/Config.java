package model;

/**
 * Clase de configuración para almacenar parámetros de conexión del servidor.
 * Contiene la información necesaria para establecer conexiones de red.
 */
public record Config(String host, int port) {}