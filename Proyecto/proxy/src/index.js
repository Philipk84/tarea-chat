import express from 'express';
import cors from 'cors';
import http from 'http';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import {
  registerUser,
  getUpdates,
  sendPrivateMessage,
  createGroup,
  joinGroup,
  sendGroupMessage,
  getHistory,
  getVoiceFile,
  getHealth,
  getConfig
} from './services/proxyService.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
app.use(cors());
app.use(express.json());

const HTTP_PORT = Number(process.env.HTTP_PORT || 3001);

// Servir archivos estáticos del build (SPA)
const DIST_DIR = path.resolve(__dirname, '../../web-client/dist');
app.use(express.static(DIST_DIR));

// ─────────────────────────────────────────────────────────────
// Rutas de Usuario
// ─────────────────────────────────────────────────────────────

app.post('/register', async (req, res) => {
  try {
    const { username } = req.body;
    const result = await registerUser(username);
    res.json(result);
  } catch (err) {
    console.error('[REGISTER ERROR]', err.message);
    res.status(500).json({ error: err.message });
  }
});

app.get('/updates', (req, res) => {
  try {
    const { user } = req.query;
    const result = getUpdates(user);
    res.json(result);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// ─────────────────────────────────────────────────────────────
// Rutas de Chat
// ─────────────────────────────────────────────────────────────

app.post('/chat', async (req, res) => {
  try {
    const { sender, receiver, message } = req.body;
    const result = await sendPrivateMessage(sender, receiver, message);
    res.json(result);
  } catch (err) {
    console.error('[CHAT ERROR]', err.message);
    res.status(400).json({ error: err.message });
  }
});

// ─────────────────────────────────────────────────────────────
// Rutas de Grupos
// ─────────────────────────────────────────────────────────────

app.post('/group/create', async (req, res) => {
  try {
    const { groupName, creator } = req.body;
    const result = await createGroup(groupName, creator);
    res.json(result);
  } catch (err) {
    console.error('[GROUP CREATE ERROR]', err.message);
    res.status(400).json({ error: err.message });
  }
});

app.post('/group/join', async (req, res) => {
  try {
    const { groupName, user } = req.body;
    const result = await joinGroup(groupName, user);
    res.json(result);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

app.post('/group/message', async (req, res) => {
  try {
    const { groupName, sender, message } = req.body;
    const result = await sendGroupMessage(groupName, sender, message);
    res.json(result);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// ─────────────────────────────────────────────────────────────
// Rutas de Historial
// ─────────────────────────────────────────────────────────────

app.get('/history', async (req, res) => {
  try {
    const { scope, user, peer, group } = req.query;
    const queryString = new URLSearchParams(req.query).toString();
    const result = await getHistory(scope, user, peer, group, queryString);
    res.status(result.statusCode || 200).json(result.data);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

// ─────────────────────────────────────────────────────────────
// Rutas de Audio/Voice
// ─────────────────────────────────────────────────────────────

app.get('/voice/:file', async (req, res) => {
  try {
    const fileName = req.params.file;
    const result = await getVoiceFile(fileName);

    if (result.type === 'local') {
      // Servir archivo local
      res.setHeader('Content-Type', 'audio/wav');
      fs.createReadStream(result.path).pipe(res);
    } else if (result.type === 'remote') {
      // Proxy al servidor principal
      http.get(result.url, (proxyRes) => {
        res.setHeader('Content-Type', proxyRes.headers['content-type'] || 'audio/wav');
        proxyRes.pipe(res);
      }).on('error', (err) => {
        console.error('[VOICE PROXY ERROR]', err.message);
        res.status(500).json({ error: 'Error obteniendo audio del servidor' });
      });
    } else {
      res.status(404).json({ error: 'Archivo de audio no encontrado' });
    }
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ─────────────────────────────────────────────────────────────
// Rutas de Sistema
// ─────────────────────────────────────────────────────────────

app.get('/health', (req, res) => {
  res.json(getHealth());
});

app.get('/config', (req, res) => {
  res.json(getConfig());
});

// ─────────────────────────────────────────────────────────────
// Iniciar servidor
// ─────────────────────────────────────────────────────────────

app.listen(HTTP_PORT, '0.0.0.0', () => {
  console.log(`╔═══════════════════════════════════════════════════════════╗`);
  console.log(`║   Chat Proxy Server - ES Modules                          ║`);
  console.log(`╠═══════════════════════════════════════════════════════════╣`);
  console.log(`║   HTTP Server:  http://0.0.0.0:${HTTP_PORT}               ║`);
  console.log(`╚═══════════════════════════════════════════════════════════╝`);
  
  const config = getConfig();
  console.log(`\n[CONFIG]`);
  console.log(`  TCP_HOST: ${config.TCP_HOST}`);
  console.log(`  TCP_PORT: ${config.TCP_PORT}`);
  console.log(`  MAIN_SERVER_IP: ${config.MAIN_SERVER_IP || '(local)'}`);
  console.log(`  VOICE_DIR: ${config.VOICE_DIR}`);
  console.log(`  HISTORY_FILE: ${config.HISTORY_FILE}`);
  console.log(`  DIST_DIR: ${DIST_DIR}\n`);
});

// ─────────────────────────────────────────────────────────────
// Catch-all: servir index.html para todas las rutas SPA
// ─────────────────────────────────────────────────────────────
app.use((req, res) => {
  const indexPath = path.join(DIST_DIR, 'index.html');
  if (fs.existsSync(indexPath)) {
    res.sendFile(indexPath);
  } else {
    res.status(404).json({ error: 'SPA not built. Run npm run build in web-client.' });
  }
});
