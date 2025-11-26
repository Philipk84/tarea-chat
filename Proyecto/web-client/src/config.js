// ============================================================
// CONFIGURACIÓN DE CONEXIÓN AL SERVIDOR
// ============================================================
//
// INSTRUCCIONES:
//
// 1. Si el cliente está en el MISMO dispositivo que el servidor:
//    - Dejar todas las variables VACÍAS ('')
//    - El sistema detectará automáticamente 'localhost'
//
// 2. Si el cliente está en un DISPOSITIVO DIFERENTE (Opción 3):
//    - Poner la IP del servidor en SERVER_IP
//    - Ejemplo: const SERVER_IP = '192.168.1.90';
//
// ============================================================

const SERVER_IP = ''; // Dejar vacío para mismo dispositivo, o poner IP (ej: '192.168.1.90')

// ============================================================
// NO MODIFICAR ABAJO (configuración automática)
// ============================================================

export const config = {
  // Servidor HTTP (API REST)
  httpBaseUrl: SERVER_IP ? `http://${SERVER_IP}:3001` : '',
  
  // Servidor WebSocket (notificaciones en tiempo real)
  wsHost: SERVER_IP || '',
  wsPort: 3002,
  
  // Servidor Ice (llamadas y audio)
  iceHost: SERVER_IP || '',
  icePort: 10010,
};

// Log de configuración para debugging
console.log('[CONFIG] Configuración de red:', config);
