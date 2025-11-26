// ============================================================
// CONFIGURACIÓN DE CONEXIÓN AL SERVIDOR
// ============================================================
//
// INSTRUCCIONES:
//
// 1. Si el cliente está en el MISMO dispositivo que el servidor Java:
//    - Dejar todas las variables VACÍAS ('')
//    - El sistema detectará automáticamente 'localhost'
//
// 2. Si el cliente está en un DISPOSITIVO DIFERENTE:
//    - ICE_SERVER_IP: IP del servidor Java (para llamadas/audio Ice)
//    - HTTP_BASE_URL: Dejar VACÍO para usar el proxy local
//
//    El proxy local de cada dispositivo se encarga de:
//    - Registrar usuarios y mantener conexión TCP
//    - Reenviar peticiones de audio/historial al servidor principal
//
// ============================================================

// IP del servidor Java (donde corre Ice y el servidor principal)
// Device 1 (servidor): dejar vacío
// Device 2 (cliente): poner IP de Device 1, ej: '192.168.1.90'
const ICE_SERVER_IP = '';

// ============================================================
// NO MODIFICAR ABAJO (configuración automática)
// ============================================================

export const config = {
  // Servidor HTTP (API REST) - SIEMPRE usar proxy local (vacío = relativo)
  // El proxy local reenvía historial/audio al servidor principal si es necesario
  httpBaseUrl: '',
  
  // Servidor WebSocket (notificaciones en tiempo real) - usar proxy local
  wsHost: '',
  wsPort: 3002,
  
  // Servidor Ice (llamadas y audio) - apuntar al servidor Java
  iceHost: ICE_SERVER_IP || '',
  icePort: 10010,
};

// Log de configuración para debugging
console.log('[CONFIG] Configuración de red:', config);
console.log('[CONFIG] ICE_SERVER_IP:', ICE_SERVER_IP || '(auto-detect)');
