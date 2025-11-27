import net from 'net';
import fs from 'fs';
import path from 'path';
import http from 'http';
import { fileURLToPath } from 'url';

// ─────────────────────────────────────────────────────────────
// Config
// ─────────────────────────────────────────────────────────────
const TCP_HOST = process.env.TCP_HOST || '0.0.0.0';
const TCP_PORT = Number(process.env.TCP_PORT || 6000);
const HTTP_PORT = Number(process.env.HTTP_PORT || 3001);

// IP del servidor principal (donde está el servidor Java)
// Dejar vacío si el proxy corre en el mismo dispositivo que el servidor
const MAIN_SERVER_IP = process.env.MAIN_SERVER_IP || '';

// Rutas de datos del servidor Java
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const HISTORY_FILE = path.resolve(__dirname, '../../../server/data/history.jsonl');
const VOICE_DIR = path.resolve(__dirname, '../../../server/data/voice');

// Estado global
const userSockets = {};   // { username: net.Socket }
const userMessages = {};  // { username: [mensajes pendientes] }

let globalSocket = null;
let connected = false;
let recvBuffer = '';

// ─────────────────────────────────────────────────────────────
// Conexión TCP persistente (global)
// ─────────────────────────────────────────────────────────────
function connectTCP() {
  globalSocket = new net.Socket();

  globalSocket.connect(TCP_PORT, TCP_HOST, () => {
    connected = true;
    console.log(`[TCP] Conectado a ${TCP_HOST}:${TCP_PORT}`);
  });

  globalSocket.on('data', (chunk) => {
    recvBuffer += chunk.toString('utf8');
  });

  globalSocket.on('close', () => {
    console.log('[TCP] Conexión cerrada. Reintentando en 1s...');
    connected = false;
    setTimeout(connectTCP, 1000);
  });

  globalSocket.on('error', (err) => {
    console.error('[TCP] Error:', err.message);
  });
}

// Iniciar conexión TCP al cargar el módulo
connectTCP();

// ─────────────────────────────────────────────────────────────
// Utilidades TCP
// ─────────────────────────────────────────────────────────────

/**
 * Envía un comando usando el socket de un usuario específico
 */
function sendCommandFromUser(username, command, timeoutMs = 1500) {
  return new Promise((resolve, reject) => {
    const client = userSockets[username];
    if (!client) {
      return reject(new Error(`Usuario ${username} no conectado`));
    }

    let buffer = "";

    const onData = (chunk) => {
      buffer += chunk.toString("utf8");
      if (buffer.includes("\n")) {
        cleanup();
        resolve(buffer.trim());
      }
    };

    const onError = (err) => {
      cleanup();
      reject(err);
    };

    const cleanup = () => {
      client.off("data", onData);
      client.off("error", onError);
      clearTimeout(timer);
    };

    const timer = setTimeout(() => {
      cleanup();
      if (buffer.trim()) {
        resolve(buffer.trim());
      } else {
        reject(new Error("Timeout esperando respuesta del servidor"));
      }
    }, timeoutMs);

    client.on("data", onData);
    client.on("error", onError);
    client.write(command.trim() + "\n");
  });
}

// ─────────────────────────────────────────────────────────────
// Servicios de Usuario
// ─────────────────────────────────────────────────────────────

/**
 * Registra un usuario creando una conexión TCP dedicada
 */
async function registerUser(username) {
  console.log(`[REGISTER] Solicitud de registro para: ${username}`);

  if (!username) {
    throw new Error("username requerido");
  }

  if (userSockets[username]) {
    console.log(`[REGISTER] Usuario ${username} ya está conectado`);
    return { reply: "Usuario ya conectado" };
  }

  return new Promise((resolve, reject) => {
    const client = new net.Socket();
    let responseData = "";

    console.log(`[REGISTER] Conectando a TCP ${TCP_HOST}:${TCP_PORT} para ${username}...`);

    client.connect(TCP_PORT, TCP_HOST, () => {
      console.log(`[REGISTER] ✓ Conectado, enviando nombre: ${username}`);
      client.write(username + "\n");
    });

    client.on("data", (data) => {
      const text = data.toString("utf8").trim();
      console.log(`[TCP -> ${username}] ${text}`);

      if (userMessages[username]) {
        userMessages[username].push(text);
      } else {
        responseData += text;
      }
    });

    client.on("error", (err) => {
      console.error("TCP error:", err.message);
      delete userSockets[username];
      reject(err);
    });

    client.on("close", () => {
      console.log(`🔌 Socket cerrado para ${username}`);
      delete userSockets[username];
    });

    // Espera breve para confirmar registro
    setTimeout(() => {
      userSockets[username] = client;
      userMessages[username] = [];
      console.log(`[REGISTER] ✓ Usuario ${username} registrado. Usuarios activos: ${Object.keys(userSockets).join(', ')}`);
      resolve({ reply: responseData.trim() || "Usuario registrado" });
    }, 300);
  });
}

/**
 * Obtiene mensajes pendientes de un usuario
 */
function getUpdates(username) {
  if (!username || !userMessages[username]) {
    throw new Error("Usuario no registrado");
  }

  const messages = [...userMessages[username]];
  userMessages[username] = [];
  return { items: messages };
}

// ─────────────────────────────────────────────────────────────
// Servicios de Chat
// ─────────────────────────────────────────────────────────────

/**
 * Envía un mensaje privado
 */
async function sendPrivateMessage(sender, receiver, message) {
  console.log(`[CHAT] Recibido: sender=${sender}, receiver=${receiver}, message=${message}`);
  console.log(`[CHAT] Usuarios conectados: ${Object.keys(userSockets).join(', ') || 'ninguno'}`);

  if (!receiver || !message || !sender) {
    throw new Error("sender, receiver y message requeridos");
  }

  const client = userSockets[sender];
  if (!client) {
    console.log(`[CHAT] ✗ Usuario ${sender} no encontrado en userSockets`);
    throw new Error("Usuario no conectado");
  }

  console.log(`[CHAT] ✓ Enviando mensaje de ${sender} a ${receiver}`);
  client.write(`/msg ${receiver} ${message}\n`);
  return;
}

// ─────────────────────────────────────────────────────────────
// Servicios de Grupos
// ─────────────────────────────────────────────────────────────

/**
 * Crea un grupo
 */
async function createGroup(groupName, creator) {
  console.log(`[GROUP CREATE] groupName=${groupName}, creator=${creator}`);
  console.log(`[GROUP CREATE] Usuarios conectados: ${Object.keys(userSockets).join(', ') || 'ninguno'}`);

  if (!groupName || !creator) {
    throw new Error("groupName y creator son requeridos");
  }

  const reply = await sendCommandFromUser(creator, `/creategroup ${groupName}`);
  console.log(`[GROUP CREATE] Respuesta: ${reply}`);
  const ok = /Grupo creado|OK/i.test(reply);

  return { reply, ok };
}

/**
 * Unirse a un grupo
 */
async function joinGroup(groupName, user) {
  if (!groupName || !user) {
    throw new Error("groupName y user son requeridos");
  }

  const reply = await sendCommandFromUser(user, `/joingroup ${groupName}`);
  const ok = /Te has unido|Ya eres miembro|OK/i.test(reply);

  return { reply, ok };
}

/**
 * Envía un mensaje a un grupo
 */
async function sendGroupMessage(groupName, sender, message) {
  if (!groupName || !sender || !message) {
    throw new Error("groupName, sender y message son requeridos");
  }

  const reply = await sendCommandFromUser(sender, `/msggroup ${groupName} ${message}`);
  const ok = /Mensaje enviado|OK/i.test(reply);

  return { reply, ok };
}

// ─────────────────────────────────────────────────────────────
// Servicios de Historial
// ─────────────────────────────────────────────────────────────

/**
 * Obtiene el historial de mensajes
 */
async function getHistory(scope, user, peer, group, queryString) {
  // Si hay servidor principal configurado, obtener historial de ahí
  if (MAIN_SERVER_IP) {
    const remoteUrl = `http://${MAIN_SERVER_IP}:${HTTP_PORT}/history?${queryString}`;
    console.log(`[HISTORY PROXY] Obteniendo historial de: ${remoteUrl}`);

    return new Promise((resolve, reject) => {
      http.get(remoteUrl, (proxyRes) => {
        let data = '';
        proxyRes.on('data', chunk => data += chunk);
        proxyRes.on('end', () => {
          try {
            resolve({ data: JSON.parse(data), statusCode: proxyRes.statusCode });
          } catch {
            resolve({ data, statusCode: proxyRes.statusCode });
          }
        });
      }).on('error', (err) => {
        console.error(`[HISTORY PROXY] Error: ${err.message}`);
        reject(new Error('Error obteniendo historial del servidor'));
      });
    });
  }

  // Leer historial local
  if (!fs.existsSync(HISTORY_FILE)) {
    return { data: { items: [] }, statusCode: 200 };
  }

  const lines = fs.readFileSync(HISTORY_FILE, 'utf8').split(/\r?\n/).filter(Boolean);
  const items = lines.map(l => {
    try { return JSON.parse(l); } catch { return null; }
  }).filter(Boolean);

  let filtered = items.filter(x => x.scope === 'private' || x.scope === 'group');

  if (scope === 'private') {
    if (!user || !peer) {
      throw new Error('user y peer requeridos para scope=private');
    }
    filtered = filtered.filter(x =>
      x.scope === 'private' &&
      ((x.sender === user && x.recipient === peer) || (x.sender === peer && x.recipient === user))
    );
  } else if (scope === 'group') {
    if (!group) {
      throw new Error('group requerido para scope=group');
    }
    filtered = filtered.filter(x => x.scope === 'group' && x.group === group);
  }

  filtered.sort((a, b) => (a.timestamp || '').localeCompare(b.timestamp || ''));
  return { data: { items: filtered }, statusCode: 200 };
}

// ─────────────────────────────────────────────────────────────
// Servicios de Audio/Voice
// ─────────────────────────────────────────────────────────────

/**
 * Obtiene un archivo de audio
 */
async function getVoiceFile(fileName) {
  const localPath = path.join(VOICE_DIR, fileName);

  console.log(`[VOICE] Solicitado: ${fileName}`);
  console.log(`[VOICE] Buscando en: ${localPath}`);

  // Primero intentar servir localmente
  if (fs.existsSync(localPath)) {
    console.log(`[VOICE] ✓ Archivo encontrado localmente`);
    return { type: 'local', path: localPath };
  }

  console.log(`[VOICE] ✗ Archivo no existe localmente`);

  // Si no existe localmente y hay un servidor principal configurado
  if (MAIN_SERVER_IP) {
    const remoteUrl = `http://${MAIN_SERVER_IP}:${HTTP_PORT}/voice/${fileName}`;
    console.log(`[VOICE PROXY] Obteniendo de servidor principal: ${remoteUrl}`);
    return { type: 'remote', url: remoteUrl };
  }

  console.log(`[VOICE] ✗ No hay servidor remoto configurado, archivo no encontrado`);
  return { type: 'notfound' };
}

// ─────────────────────────────────────────────────────────────
// Estado del sistema
// ─────────────────────────────────────────────────────────────

/**
 * Obtiene el estado de salud del sistema
 */
function getHealth() {
  return { tcpConnected: connected };
}

/**
 * Obtiene la configuración actual
 */
function getConfig() {
  return {
    TCP_HOST,
    TCP_PORT,
    HTTP_PORT,
    MAIN_SERVER_IP,
    VOICE_DIR,
    HISTORY_FILE
  };
}

// ─────────────────────────────────────────────────────────────
// Exports
// ─────────────────────────────────────────────────────────────

export {
  // Usuario
  registerUser,
  getUpdates,
  
  // Chat
  sendPrivateMessage,
  
  // Grupos
  createGroup,
  joinGroup,
  sendGroupMessage,
  
  // Historial
  getHistory,
  
  // Audio
  getVoiceFile,
  
  // Llamadas
  startCall,
  startGroupCall,
  endCall,
  registerUdpPort,
  
  // Sistema
  getHealth,
  getConfig
};
