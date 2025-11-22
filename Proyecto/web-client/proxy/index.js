const net = require('net');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const userSockets = {};  // { username: net.Socket }
const userMessages = {}; // { username: [mensajes pendientes] }
// ─────────────────────────────────────────────────────────────
// Config
// ─────────────────────────────────────────────────────────────
const TCP_HOST = process.env.TCP_HOST || '127.0.0.1';
const TCP_PORT = Number(process.env.TCP_PORT || 5000);
const HTTP_PORT = Number(process.env.HTTP_PORT || 3001);

// Ruta de history del servidor Java (misma máquina)
const HISTORY_FILE = path.resolve(__dirname, '../../server/data/history.jsonl');
const VOICE_DIR = path.resolve(__dirname, '../../server/data/voice');

const app = express();
app.use(cors());
app.use(express.json());

// Servir archivos de audio WAV del historial
app.use('/voice', express.static(VOICE_DIR));

// ─────────────────────────────────────────────────────────────
// Conexión TCP persistente
// ─────────────────────────────────────────────────────────────
let socket = null;
let connected = false;
let recvBuffer = '';

// Conectar y auto-reconectar
function connectTCP() {
  socket = new net.Socket();

  socket.connect(TCP_PORT, TCP_HOST, () => {
    connected = true;
    console.log(`[TCP] Conectado a ${TCP_HOST}:${TCP_PORT}`);
  });

  socket.on('data', (chunk) => {
    // Acumula por si vienen trozos; el servidor suele responder por líneas
    recvBuffer += chunk.toString('utf8');
  });

  socket.on('close', () => {
    console.log('[TCP] Conexión cerrada. Reintentando en 1s...');
    connected = false;
    setTimeout(connectTCP, 1000);
  });

  socket.on('error', (err) => {
    console.error('[TCP] Error:', err.message);
  });
}
connectTCP();

// Utilidad: envía una línea y espera una respuesta corta
function sendCommandLine(line, timeoutMs = 1500) {
  return new Promise((resolve, reject) => {
    if (!connected) return reject(new Error('Socket no conectado'));

    // Limpia buffer de recepción previo para no mezclar
    recvBuffer = '';
    socket.write(line.trim() + '\n', 'utf8', (err) => {
      if (err) return reject(err);
    });

    const t = setTimeout(() => {
      // Devuelve lo que haya llegado (si llegó algo) o timeout
      if (recvBuffer.trim().length > 0) {
        clearTimeout(t);
        return resolve(recvBuffer.trim());
      }
      reject(new Error('Timeout esperando respuesta del servidor'));
    }, timeoutMs);

    // Polling simple del buffer (suficiente para comandos cortos)
    const poll = setInterval(() => {
      // Heurística: si llega un salto de línea o buffer creció lo suficiente
      if (recvBuffer.includes('\n') || recvBuffer.length > 512) {
        clearInterval(poll);
        clearTimeout(t);
        return resolve(recvBuffer.trim());
      }
    }, 50);
  });
}

// Utilidad: enviar comando al servidor usando el socket de un usuario concreto
function sendCommandFromUser(username, command, timeoutMs = 1500) {
  return new Promise((resolve, reject) => {
    const client = userSockets[username];
    if (!client) {
      return reject(new Error(`Usuario ${username} no conectado`));
    }

    let buffer = "";

    const onData = (chunk) => {
      buffer += chunk.toString("utf8");
      // asumimos respuestas por línea
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
// Endpoints HTTP
// ─────────────────────────────────────────────────────────────

// Registro de usuario (ajusta el comando que use tu ClientHandler: /name, /login, etc.)

app.post("/register", (req, res) => {
  const { username } = req.body || {};
  if (!username) return res.status(400).json({ error: "username requerido" });

  // Si ya existe socket activo, no crear otro
  if (userSockets[username]) {
    return res.status(200).json({ reply: "Usuario ya conectado" });
  }

  const client = new net.Socket();
  let responseData = "";

  client.connect(TCP_PORT, TCP_HOST, () => {
    client.write(username + "\n");
  });

  client.on("data", (data) => {
        const text = data.toString("utf8").trim();
        console.log(`[TCP -> ${username}] ${text}`);

        // Si el mensaje llega después del registro, guárdalo
        if (userMessages[username]) {
            userMessages[username].push(text);
        } else {
            // Durante el registro inicial, solo acumulamos la respuesta
            responseData += text;
        }
    });

  client.on("error", (err) => {
    console.error("TCP error:", err.message);
    delete userSockets[username];
  });

  client.on("close", () => {
    console.log(`🔌 Socket cerrado para ${username}`);
    delete userSockets[username];
  });

  // Espera breve para confirmar registro
  setTimeout(() => {
    userSockets[username] = client;
    res.status(200).json({ reply: responseData.trim() || "Usuario registrado" });
  }, 300);

  userSockets[username] = client;
  userMessages[username] = [];
});


// Enviar mensaje privado
// body: { sender, receiver, message }
app.post("/chat", (req, res) => {
  const { receiver, message, sender } = req.body || {};
  if (!receiver || !message || !sender) {
    return res.status(400).json({ error: "sender, receiver y message requeridos" });
  }

  const client = userSockets[sender];
  if (!client) return res.status(400).json({ error: "Usuario no conectado" });

  client.write(`/msg ${receiver} ${message}\n`);
  res.status(200).json({ reply: "Mensaje enviado" });
});

// Crear grupo
// body: { groupName, creator }
app.post("/group/create", async (req, res) => {
  try {
    const { groupName, creator } = req.body || {};
    if (!groupName || !creator) {
      return res
        .status(400)
        .json({ error: "groupName y creator son requeridos" });
    }

    const reply = await sendCommandFromUser(creator, `/creategroup ${groupName}`);
    const ok = /Grupo creado|OK/i.test(reply);

    return res.status(ok ? 200 : 409).json({ reply });
  } catch (e) {
    console.error("POST /group/create", e);
    return res.status(500).json({ error: e.message });
  }
});


// Unirse a grupo (si lo necesitas en el cliente)
// body: { groupName }
// body: { groupName, user }
app.post("/group/join", async (req, res) => {
  try {
    const { groupName, user } = req.body || {};
    if (!groupName || !user) {
      return res
        .status(400)
        .json({ error: "groupName y user son requeridos" });
    }

    const reply = await sendCommandFromUser(user, `/joingroup ${groupName}`);
    const ok = /Te has unido|Ya eres miembro|OK/i.test(reply);

    return res.status(ok ? 200 : 409).json({ reply });
  } catch (e) {
    console.error("POST /group/join", e);
    return res.status(500).json({ error: e.message });
  }
});


// Enviar mensaje a grupo
// body: { groupName, message }
// body: { groupName, sender, message }
app.post("/group/message", async (req, res) => {
  try {
    const { groupName, sender, message } = req.body || {};
    if (!groupName || !sender || !message) {
      return res
        .status(400)
        .json({ error: "groupName, sender y message son requeridos" });
    }

    const reply = await sendCommandFromUser(
      sender,
      `/msggroup ${groupName} ${message}`
    );
    const ok = /Mensaje enviado|OK/i.test(reply);

    return res.status(ok ? 200 : 409).json({ reply });
  } catch (e) {
    console.error("POST /group/message", e);
    return res.status(500).json({ error: e.message });
  }
});


// Historial (lee server/data/history.jsonl y filtra)
// GET /history?scope=private&user=U&peer=P
// GET /history?scope=group&group=G
app.get('/history', async (req, res) => {
  try {
    const { scope, user, peer, group } = req.query;

    if (!fs.existsSync(HISTORY_FILE)) {
      return res.status(200).json({ items: [] });
    }

    const lines = fs.readFileSync(HISTORY_FILE, 'utf8').split(/\r?\n/).filter(Boolean);
    const items = lines.map(l => {
      try { return JSON.parse(l); } catch { return null; }
    }).filter(Boolean);

    let filtered = items.filter(x => x.scope === 'private' || x.scope === 'group');

    if (scope === 'private') {
      if (!user || !peer) return res.status(400).json({ error: 'user y peer requeridos para scope=private' });
      filtered = filtered.filter(x =>
        x.scope === 'private' &&
        ((x.sender === user && x.recipient === peer) || (x.sender === peer && x.recipient === user))
      );
    } else if (scope === 'group') {
      if (!group) return res.status(400).json({ error: 'group requerido para scope=group' });
      filtered = filtered.filter(x => x.scope === 'group' && x.group === group);
    }

    // Ordena por timestamp asc
    filtered.sort((a, b) => (a.timestamp || '').localeCompare(b.timestamp || ''));
    return res.status(200).json({ items: filtered });
  } catch (e) {
    console.error('GET /history', e);
    return res.status(500).json({ error: e.message });
  }
});

app.get("/updates", (req, res) => {
  const { user } = req.query;
  if (!user || !userMessages[user]) {
    return res.status(400).json({ error: "Usuario no registrado" });
  }

  const messages = [...userMessages[user]];
  userMessages[user] = []; // limpia la cola
  res.json({ items: messages });
});

// Salud
app.get('/health', (_req, res) => {
  res.json({ tcpConnected: connected });
});

// Start HTTP
app.listen(HTTP_PORT, () => {
  console.log(`[HTTP] Proxy escuchando en http://localhost:${HTTP_PORT}`);
});
