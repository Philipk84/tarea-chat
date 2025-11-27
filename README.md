# Proyecto de Chat | Compunet 2025 - 2

## Integrantes
- Alejandro Vargas Sánchez (A00404840) 
- Sebastián Romero Leon (A00404670)
- Felipe Calderon Arias (A00404998)
  
## Ejecución del proyecto

### Solo la primera vez

1. Hacer el build del backend Java desde la carpeta `Proyecto`:

```bash
cd Proyecto
./gradlew clean build 
```

2. Instalar dependencias del proxy:

```bash
cd proxy
npm install
```

3. Instalar dependencias del cliente web:

```bash
cd ../web-client
npm install
```

### Ejecución de la aplicación

#### Opción 1: Comandos separados (3 terminales)

**Terminal 1 - Servidor Java:**
```bash
# Ejecutar desde IDE (clase Main.java en server/src/main/java/ui/)
```

**Terminal 2 - Proxy HTTP:**
```bash
cd Proyecto/proxy
npm start
```

**Terminal 3 - Cliente Web (desarrollo):**
```bash
cd Proyecto/web-client
npm start
```

#### Opción 2: Proxy + Cliente juntos (2 terminales)

**Terminal 1 - Servidor Java:**
```bash
# Ejecutar desde IDE (clase Main.java en server/src/main/java/ui/)
```

**Terminal 2 - Proxy + Webpack:**
```bash
cd Proyecto/web-client
npm run dev
```

Este comando ejecuta `concurrently` para iniciar el proxy y webpack-dev-server simultáneamente.

### Estructura del proyecto

```
Proyecto/
├── server/                 # Backend Java
│   ├── src/main/java/      # Código fuente
│   └── data/               # Datos persistentes
│       ├── history.jsonl   # Historial de mensajes
│       └── voice/          # Archivos de audio WAV
├── proxy/                  # Proxy HTTP/TCP (ES Modules)
│   ├── package.json        # "type": "module"
│   └── src/
│       ├── index.js        # Express routes
│       └── services/
│           └── proxyService.js  # Lógica TCP/usuarios
├── web-client/             # Frontend (Webpack + Vanilla JS)
│   ├── src/                # Código fuente
│   ├── dist/               # Build de producción
│   └── package.json        # Scripts npm
└── config.json             # Configuración de puertos
```

### Scripts disponibles

#### Desde `Proyecto/proxy`:
| Comando | Descripción |
|---------|-------------|
| `npm start` | Inicia el proxy en puerto 3001 |
| `npm run dev` | Inicia con hot-reload (--watch) |

#### Desde `Proyecto/web-client`:
| Comando | Descripción |
|---------|-------------|
| `npm start` | Webpack dev server (puerto 8080) |
| `npm run build` | Genera build en `dist/` |
| `npm run proxy` | Inicia solo el proxy |
| `npm run dev` | Proxy + Webpack juntos |

### Configuración multi-dispositivo

Para ejecutar la aplicación en múltiples dispositivos (ej: servidor en PC1, cliente en PC2):

**En el dispositivo servidor (PC1):**
```bash
# Ejecutar servidor Java normalmente
# Ejecutar proxy normalmente
cd Proyecto/proxy
npm start
```

**En el dispositivo cliente (PC2):**

1. Configurar las IPs en `web-client/src/config.js`:
```javascript
const ICE_SERVER_IP = '192.168.1.90';  // IP del PC1
```

2. Configurar el proxy para conectar al servidor remoto:
```bash
cd Proyecto/proxy
TCP_HOST=192.168.1.90 MAIN_SERVER_IP=192.168.1.90 npm start
```

Variables de entorno del proxy:
| Variable | Default | Descripción |
|----------|---------|-------------|
| `TCP_HOST` | `0.0.0.0` | IP del servidor Java (TCP) |
| `TCP_PORT` | `6000` | Puerto TCP del servidor |
| `HTTP_PORT` | `3001` | Puerto HTTP del proxy |
| `MAIN_SERVER_IP` | (vacío) | IP para obtener audio/historial remoto |

## 2. Descripción del flujo de comunicación entre cliente, proxy y backend

### Arquitectura general

```
┌─────────────────┐     HTTP/WS      ┌─────────────────┐      TCP       ┌─────────────────┐
│   Web Client    │ ──────────────── │     Proxy       │ ────────────── │  Java Server    │
│   (Webpack)     │    :8080→:3001   │   (Express)     │     :6000      │   (Netty/Ice)   │
│                 │                  │                 │                │                 │
│  ┌───────────┐  │                  │  ┌───────────┐  │                │  ┌───────────┐  │
│  │  UI/SPA   │  │                  │  │  Routes   │  │                │  │  Handlers │  │
│  └───────────┘  │                  │  └───────────┘  │                │  └───────────┘  │
│        │        │                  │        │        │                │        │        │
│        ▼        │                  │        ▼        │                │        ▼        │
│  ┌───────────┐  │     Ice/WS       │  ┌───────────┐  │                │  ┌───────────┐  │
│  │ Ice Client│  │ ─────────────────┼──┼───────────┼──┼────────────────│─▶│  CallI    │  │
│  └───────────┘  │    :10010        │  │  Service  │  │                │  └───────────┘  │
└─────────────────┘                  │  └───────────┘  │                └─────────────────┘
                                     └─────────────────┘
```

### Conexiones activas y puertos

| Conexión | Protocolo | Puerto | Descripción |
|----------|-----------|--------|-------------|
| Cliente → Proxy | HTTP | 8080 → 3001 | API REST (webpack proxy) |
| Cliente → Java | Ice/WebSocket | 10010 | Audio bidireccional |
| Proxy → Java | TCP | 6000 | Comandos de texto |
| Proxy (archivos) | HTTP | 3001 | `/voice/*` archivos WAV |

Puertos por defecto:
- Backend TCP (mensajes de texto): `6000` (desde `Proyecto/config.json`)
- Backend Ice ZeroC (audio/llamadas): `10010` (WebSocket bidireccional)
- Proxy HTTP: `3001`
- Cliente (webpack): `8080` (proxy de `/api` y `/voice` hacia `3001`)

---

### Estructura del Proxy (ES Modules)

El proxy está estructurado como módulo ES con separación de responsabilidades:

```
proxy/
├── package.json              # "type": "module"
└── src/
    ├── index.js              # Express app + rutas
    └── services/
        └── proxyService.js   # Lógica TCP + estado
```

**`src/index.js`** - Rutas HTTP:
- Importa funciones del servicio
- Define endpoints Express
- Sirve archivos estáticos del SPA

**`src/services/proxyService.js`** - Lógica de negocio:
- Conexión TCP persistente al servidor Java
- Gestión de sockets por usuario (`userSockets`)
- Cola de mensajes por usuario (`userMessages`)
- Funciones exportadas para cada operación

---

### Patrón "pull" (HTTP → TCP → HTTP)

#### Cliente (HTTP) → Proxy

Endpoints de la API:
| Método | Ruta | Body | Descripción |
|--------|------|------|-------------|
| POST | `/register` | `{ username }` | Registrar usuario |
| POST | `/chat` | `{ sender, receiver, message }` | Mensaje privado |
| POST | `/group/create` | `{ groupName, creator }` | Crear grupo |
| POST | `/group/join` | `{ groupName, user }` | Unirse a grupo |
| POST | `/group/message` | `{ groupName, sender, message }` | Mensaje a grupo |
| GET | `/history` | `?scope=private&user=U&peer=P` | Historial privado |
| GET | `/history` | `?scope=group&group=G` | Historial de grupo |
| GET | `/updates` | `?user=U` | Polling de mensajes |
| GET | `/voice/:file` | - | Obtener archivo de audio |
| GET | `/health` | - | Estado del proxy |
| GET | `/config` | - | Configuración actual |

#### Proxy (traducción) → Backend Java (TCP, texto plano por línea)

| Endpoint HTTP | Comando TCP |
|---------------|-------------|
| `POST /register` | Abre socket usuario, envía: `<username>\n` |
| `POST /chat` | Socket del sender: `/msg <receiver> <message>\n` |
| `POST /group/create` | Socket del creator: `/creategroup <groupName>\n` |
| `POST /group/join` | Socket del user: `/joingroup <groupName>\n` |
| `POST /group/message` | Socket del sender: `/msggroup <groupName> <message>\n` |
| `GET /history` | Lee `server/data/history.jsonl` y filtra |
| `GET /updates` | Retorna y limpia cola `userMessages[user]` |
| `GET /voice/:file` | Sirve archivo o proxy a `MAIN_SERVER_IP` |

#### Backend Java → Proxy → Cliente (HTTP JSON)

- El backend responde líneas de texto (no JSON). Ejemplos:
  - Registro: `¡Bienvenido, <user>!`
  - Privado OK: `Mensaje enviado a <destino>`
  - Grupo OK: `Mensaje enviado al grupo '<grupo>' (enviado a N miembros)`
  - Errores: `Error: Usuario 'X' no está conectado`, `Error: ...`
- El proxy encapsula como `{ reply: "..." }` o, en historial/updates, `{ items: [...] }`.
- Códigos HTTP:
  - 200: éxito (o respuesta textual que la UI interpreta)
  - 400: parámetros faltantes/invalidos
  - 409: conflicto (p. ej., ya eres miembro, grupo duplicado) detectado por regex en la respuesta
  - 500: timeout TCP o error interno

---

### Patrón "push" simulado (Backend → Proxy → Cliente)

1. El backend envía al socket del usuario receptor líneas como:
   - Privado: `MENSAJE_PRIVADO de <sender>: <texto>`
   - Grupo: `MENSAJE_GRUPO [<grupo>] de <sender>: <texto>`
2. El proxy las guarda en `userMessages[usuario]`.
3. El navegador consulta cada 1.5 s `GET /updates?user=U` y recibe `{ items: ["MENSAJE_PRIVADO de ...", ...] }`.
4. La UI pinta cada línea.
   
---

### Historial (persistencia y consulta)

- Archivo: `Proyecto/server/data/history.jsonl` (una línea JSON por evento: textos privados/grupo, notas de voz, llamadas).
- El proxy:
  - Carga y filtra por `scope=private&user=U&peer=P` o `scope=group&group=G`
  - Ordena por `timestamp`
  - Sirve audio con `/voice/<nombre.wav>`

Ejemplos de objetos en historial:
- Privado: `{"type":"text","scope":"private","sender":"ana","recipient":"bob","message":"Hola","timestamp":"..."}`
- Grupo: `{"type":"text","scope":"group","sender":"ana","group":"devs","message":"Hola","timestamp":"..."}`
- Voz privada: `{"type":"voice_note","scope":"private","sender":"ana","recipient":"bob","audioFile":"server/data/voice/xxx.wav","sizeBytes":12345,"timestamp":"..."}`
- Voz grupo: `{"type":"voice_group","scope":"group","sender":"ana","group":"devs","audioFile":"server/data/voice/yyy.wav","sizeBytes":23456,"timestamp":"..."}`

---

### Ejemplos de extremo a extremo

**Mensaje privado (pull + ack):**
```
UI → POST /chat { sender:"ana", receiver:"bob", message:"Hola!" }
Proxy → TCP: /msg bob Hola!
Backend → socket ana: Mensaje enviado a bob
Backend → socket bob: MENSAJE_PRIVADO de ana: Hola!
UI de bob → GET /updates?user=bob → muestra el mensaje
```

**Mensaje a grupo:**
```
UI → POST /group/create { groupName:"compunet", creator:"ana" }
UI → POST /group/join { groupName:"compunet", user:"bob" }
UI → POST /group/message { groupName:"compunet", sender:"ana", message:"Hola equipo" }
Backend difunde: MENSAJE_GRUPO [compunet] de ana: Hola equipo
Cada miembro lo obtiene en GET /updates
```

**Historial:**
```
UI → GET /history?scope=private&user=ana&peer=bob
Proxy → lee y filtra history.jsonl
Respuesta: { items: [...] }
UI renderiza texto y audios con <audio src="/voice/archivo.wav">
```

---

## 3. Sistema de Audio: Notas de Voz y Llamadas en Tiempo Real (Ice ZeroC)

### Arquitectura general

El sistema de audio utiliza **Ice ZeroC** (versión 3.7.10 para JavaScript) como middleware RPC bidireccional sobre WebSocket. Esto permite:
- **Callbacks del servidor al cliente** (notificaciones push de audio recibido)
- **Streaming de audio en tiempo real** durante llamadas
- **Serialización eficiente** de arrays de bytes (audio PCM16)

**Componentes principales:**

1. **Backend Java (Ice Server)**:
   - `IceBootstrap.java`: Inicializa Ice Communicator y escucha en `ws://0.0.0.0:10010/call`
   - `CallI.java`: Implementa el servant Ice que maneja llamadas y notas de voz
   - `CallManagerImpl.java`: Gestiona el estado de las llamadas activas (participantes por callId)
   - `Services.ice`: Define la interfaz Slice (IDL) compartida entre Java y JavaScript

2. **Frontend JavaScript (Ice Client)**:
   - `voiceDelegate.js`: Cliente Ice que se conecta al servidor y maneja callbacks
   - `callManager.js`: Gestiona el AudioContext, captura de micrófono y reproducción
   - `audioRecorder.js`: Maneja la grabación de notas de voz

3. **Interfaz Slice (Services.ice)**:
```c
module Chat {
    // Entrada de nota de voz
    struct VoiceEntry {
        string type;        // "voice_note" | "voice_group"
        string scope;       // "private" | "group"
        string sender;
        string recipient;
        string group;
        string audioFile;
    };

    // Chunk de audio en llamada
    struct CallChunk {
        string callId;
        string fromUser;
        sequence<byte> audio;  // PCM16 little-endian
    };

    // Evento de llamada
    struct CallEvent {
        string type;      // "call_incoming" | "call_started" | "call_accepted" | "call_ended"
        string callId;
        string caller;
        string callee;
        string group;
        string scope;     // "private" | "group"
    };

    // Observer para callbacks del servidor al cliente
    interface VoiceObserver {
        void onVoice(VoiceEntry entry);           // Notificación de nota de voz
        void onCallChunk(CallChunk chunk);        // Chunk de audio en llamada
        void onCallEvent(CallEvent event);        // Evento de estado de llamada
    };

    // Interfaz principal del servidor
    interface Call {
        // Suscripción para recibir callbacks
        void subscribe(string username, VoiceObserver* obs);
        void unsubscribe(string username, VoiceObserver* obs);

        // Notas de voz
        void sendVoiceNoteToUser(string fromUser, string toUser, sequence<byte> audio);
        void sendVoiceNoteToGroup(string fromUser, string groupName, sequence<byte> audio);

        // Gestión de llamadas
        string startCall(string caller, string callee);
        string startGroupCall(string caller, string groupName);
        void acceptCall(string callId, string user);
        void rejectCall(string callId, string user);
        void endCall(string callId, string user);

        // Streaming de audio en llamada
        void sendCallChunk(string callId, string fromUser, sequence<byte> audio);
    };
};
```

---

### Flujo de Notas de Voz

#### 1. Grabación y Envío (Cliente → Servidor)

**Cliente (`audioRecorder.js` + `voiceDelegate.js`):**
```
1. Usuario presiona botón de grabación
2. navigator.mediaDevices.getUserMedia({ audio: true })
3. MediaRecorder graba audio → Blob
4. AudioContext decodifica → Float32Array
5. Conversión a PCM16 little-endian (Int16)
6. voiceDelegate.sendVoiceToUser(fromUser, toUser, pcm16Bytes)
   → Ice: CallPrx.sendVoiceNoteToUser()
```

**Servidor (`CallI.java`):**
```java
@Override
public void sendVoiceNoteToUser(String fromUser, String toUser, byte[] audio, Current current) {
    // 1. Guardar audio como archivo WAV
    HistoryService.SavedAudio saved = HistoryService.saveVoiceBytes(audio);
    // → Genera archivo en server/data/voice/note_<timestamp>_<uuid>.wav
    
    // 2. Registrar en historial (history.jsonl)
    HistoryService.logVoiceNote(fromUser, toUser, saved.relativePath(), saved.sizeBytes());
    
    // 3. Crear VoiceEntry para notificación
    VoiceEntry entry = new VoiceEntry();
    entry.type = "voice_note";
    entry.scope = "private";
    entry.sender = fromUser;
    entry.recipient = toUser;
    entry.audioFile = saved.relativePath();
    
    // 4. Notificar a ambos usuarios via callbacks Ice
    notifyUser(fromUser, entry);   // Para que emisor vea su nota
    notifyUser(toUser, entry);     // Para que receptor la reciba
}

private void notifyUser(String username, VoiceEntry entry) {
    VoiceObserverPrx obs = observers.get(username);
    if (obs != null) {
        new Thread(() -> {
            try {
                // Verificar conexión activa
                obs.ice_getConnection();
                
                // Invocar callback en el cliente
                obs.onVoice(entry);
                System.out.println("[ICE] ✓ Notificación enviada a: " + username);
            } catch (Exception e) {
                System.err.println("[ICE] Error notificando a " + username);
                observers.remove(username);
            }
        }, "ICE-Notify-" + username).start();
    }
}
```

#### 2. Recepción y Reproducción (Servidor → Cliente)

**Cliente (`voiceDelegate.js` callback):**
```javascript
// Al inicializar, se crea un servant con callback
servant.onVoice = (entry, current) => {
    console.log("[VoiceDelegate] Nota de voz recibida:", entry);
    // entry = { type, scope, sender, recipient, group, audioFile }
    
    // Propagar a todos los listeners (Chat.js)
    this.callbacks.forEach(cb => cb(entry));
};
```

**UI (`Chat.js`):**
```javascript
voiceDelegate.subscribe((data) => {
    if (data.audioFile) {
        // Renderizar reproductor de audio
        const audioUrl = `/voice/${data.audioFile.split('/').pop()}`;
        // <audio controls src={audioUrl}></audio>
    }
});
```

**Trazabilidad completa:**
```
Cliente (Ale)                           Servidor Java                      Cliente (Fel)
    │                                        │                                   │
    ├─ [Graba audio] ─────────────────────>  │                                   │
    │  MediaRecorder → Blob                  │                                   │
    │  AudioContext → Float32                │                                   │
    │  Conversión → PCM16                    │                                   │
    │                                        │                                   │
    ├─ sendVoiceToUser(Ale, Fel, bytes) ──>  │                                   │
    │  Ice.CallPrx.sendVoiceNoteToUser()     │                                   │
    │                                        │                                   │
    │                                   [CallI.java]                             │
    │                                   saveVoiceBytes()                         │
    │                                   ↓ WAV en disk                            │
    │                                   ↓ Log en history.jsonl                   │
    │                                        │                                   │
    │                                   notifyUser(Ale)                          │
    │ <─────────────────────────────── obs.onVoice(entry)                        │
    │  [Callback Ice recibido]               │                                   │
    │  UI muestra nota enviada               │                                   │
    │                                        │                                   │
    │                                   notifyUser(Fel) ───────────────────────> │
    │                                        │      obs.onVoice(entry) ────────> │
    │                                        │                     [Callback Ice]│
    │                                        │                     UI muestra    │
    │                                        │                     reproductor   │
```

---

### Flujo de Llamadas en Tiempo Real

#### 1. Inicio de Llamada

**Cliente que llama (Ale):**
```javascript
// callManager.js
async startPrivateCall(callee) {
    const callId = await voiceDelegate.startCall(this.username, callee);
    // Ice: CallPrx.startCall("Ale", "Fel") → retorna UUID
    
    this.currentCall = {
        callId,
        type: "private",
        participants: [this.username, callee],
        isIncoming: false
    };
}
```

**Servidor:**
```java
@Override
public String startCall(String caller, String callee, Current current) {
    String callId = UUID.randomUUID().toString();
    
    // Registrar participantes en CallManager
    Set<String> participants = ConcurrentHashMap.newKeySet();
    participants.add(caller);
    participants.add(callee);
    
    CallManagerImpl callManager = ChatServer.getCallManagerImpl();
    callManager.createCall(callId, participants);
    
    // Notificar eventos
    notifyCallEvent(callee, "call_incoming", callId, caller, callee, "", "private");
    notifyCallEvent(caller, "call_started", callId, caller, callee, "", "private");
    
    return callId;
}
```

**Cliente que recibe (Fel):**
```javascript
// voiceDelegate.js callback
servant.onCallEvent = (event, current) => {
    // event = { type: "call_incoming", callId, caller, callee, ... }
    this.callbacks.forEach(cb => cb(event));
};

// callManager.js
_handleCallEvent(event) {
    if (event.type === "call_incoming") {
        this.currentCall = {
            callId: event.callId,
            caller: event.caller,
            isIncoming: true
        };
        
        // Notificar a UI para mostrar modal de llamada entrante
        this.onIncomingCallListeners.forEach(listener => listener(this.currentCall));
    }
}
```

#### 2. Aceptar Llamada y Captura de Audio

**Cliente (Fel) acepta:**
```javascript
async acceptCall(callId) {
    await voiceDelegate.acceptCall(callId, this.username);
    // Ice: CallPrx.acceptCall(callId, "Fel")
    
    // Iniciar captura de micrófono
    await this._startAudioCapture(callId);
}

async _startAudioCapture(callId) {
    // 1. Crear AudioContext a 44100 Hz
    this.audioContext = new AudioContext({ sampleRate: 44100 });
    
    // 2. Obtener stream del micrófono
    this.mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    
    // 3. Crear procesador de audio (chunks de 2048 samples)
    const source = this.audioContext.createMediaStreamSource(this.mediaStream);
    this.audioProcessor = this.audioContext.createScriptProcessor(2048, 1, 1);
    
    // 4. Procesar cada chunk
    this.audioProcessor.onaudioprocess = (e) => {
        const float32 = e.inputBuffer.getChannelData(0);  // Float32Array
        const pcm16 = this._float32ToPCM16(float32);      // Int16 → Uint8Array
        
        // 5. Enviar chunk al servidor
        this._sendCallChunk(callId, pcm16);
    };
    
    source.connect(this.audioProcessor);
    this.audioProcessor.connect(this.audioContext.destination);
}
```

**Servidor reenvía chunks:**
```java
@Override
public void sendCallChunk(String callId, String fromUser, byte[] audio, Current current) {
    // 1. Obtener participantes de la llamada
    CallManagerImpl callManager = ChatServer.getCallManagerImpl();
    Set<String> participants = callManager.getParticipants(callId);
    
    if (participants == null || participants.isEmpty()) {
        return; // Llamada terminada o no existe
    }
    
    // 2. Crear chunk para reenviar
    CallChunk chunk = new CallChunk();
    chunk.callId = callId;
    chunk.fromUser = fromUser;
    chunk.audio = audio;
    
    // 3. Reenviar a todos EXCEPTO al emisor
    for (String user : participants) {
        if (user.equals(fromUser)) continue;
        
        VoiceObserverPrx obs = observers.get(user);
        if (obs != null) {
            new Thread(() -> {
                try {
                    obs.ice_getConnection();
                    obs.onCallChunk(chunk);  // Callback al cliente
                    System.out.println("[ICE] ✓ Chunk enviado a " + user);
                } catch (Exception e) {
                    System.err.println("[ICE] Error enviando chunk a " + user);
                    observers.remove(user);
                }
            }, "ICE-CallChunk-" + user).start();
        }
    }
}
```

#### 3. Reproducción de Audio Remoto

**Cliente receptor del chunk:**
```javascript
// voiceDelegate.js callback
servant.onCallChunk = (chunk, current) => {
    // chunk = { callId, fromUser, audio: Uint8Array }
    this.callbacks.forEach(cb => cb(chunk));
};

// callManager.js
_handleCallChunk(chunk) {
    if (!this.currentCall || this.currentCall.callId !== chunk.callId) {
        return; // No estamos en esta llamada
    }
    
    this._playCallChunk(chunk.fromUser, chunk.audio);
}

_playCallChunk(fromUser, audioData) {
    // 1. Crear/obtener AudioContext para este usuario
    if (!this.remoteAudioSources.has(fromUser)) {
        const context = new AudioContext({ sampleRate: 44100 });
        const gainNode = context.createGain();
        gainNode.connect(context.destination);
        this.remoteAudioSources.set(fromUser, { context, gainNode });
    }
    
    const { context, gainNode } = this.remoteAudioSources.get(fromUser);
    
    // 2. Convertir PCM16 → Float32
    const float32 = this._pcm16ToFloat32(audioData);
    
    // 3. Crear buffer y reproducir
    const audioBuffer = context.createBuffer(1, float32.length, 44100);
    audioBuffer.getChannelData(0).set(float32);
    
    const source = context.createBufferSource();
    source.buffer = audioBuffer;
    source.connect(gainNode);
    source.start(0);  // Reproducción inmediata
}
```

#### 4. Trazabilidad Completa de una Llamada

```
Ale (caller)                        Servidor Java                      Fel (callee)
    │                                      │                                  │
    ├─ startPrivateCall("Fel") ─────────>  │                                  │
    │  Ice: startCall("Ale", "Fel")        │                                  │
    │                                      │                                  │
    │                                [CallI.java]                             │
    │                                UUID.randomUUID()                        │
    │                                callId = "0fcb2a97..."                   │
    │                                CallManager.createCall(callId, [Ale,Fel])│
    │                                      │                                  │
    │                                      ├─notifyCallEvent(Fel, incoming)─> │
    │                                      │               onCallEvent() ───> │
    │                                      │                [Modal: Llamada]  │
    │                                      │                [Entrante de Ale] │
    │                                      │                                  │
    │ <─── notifyCallEvent(Ale, started) ──┤                                  │
    │  onCallEvent()                       │                                  │
    │  [Iniciando llamada...]              │                                  │
    │  _startAudioCapture()                │                                  │
    │  ↓ AudioContext                      │                                  │
    │  ↓ getUserMedia()                    │                                  │
    │  ↓ ScriptProcessor                   │                                  │
    │                                      │                                  │
    │                                      │               [Fel acepta] <──── │
    │                                      │<─── acceptCall(callId, "Fel") ───│
    │                                      │                                  │
    │                                [CallI.acceptCall]                       │
    │                                participants.add("Fel")                  │
    │                                      │                                  │
    │ <────── notifyCallEvent(accepted) ───┼─────────────────────────────────>│
    │  onCallEvent()                       │                 onCallEvent()    │
    │  [Fel aceptó]                        │              _startAudioCapture()│
    │                                      │                ↓ AudioContext    │
    │                                      │                ↓ getUserMedia()  │
    │                                      │                                  │
    │ [STREAMING BIDIRECCIONAL DE AUDIO - Cada ~46ms]                         │
    │                                      │                                  │
    ├─ onaudioprocess ────────────────────>│                                  │
    │   Float32[2048] → PCM16[4096 bytes]  │                                  │
    │   sendCallChunk(callId, "Ale", bytes)│                                  │
    │                                      │                                  │
    │                                [sendCallChunk]                          │
    │                                getParticipants(callId)                  │
    │                                → [Ale, Fel]                             │
    │                              reenviar a Fel ──────────────────────────> │
    │                              obs.onCallChunk(chunk) ──────────────────> │
    │                                      │                onCallChunk()     │
    │                                      │                _playCallChunk()  │
    │                                      │                PCM16 → Float32   │
    │                                      │                AudioBuffer       │
    │                                      │                Reproducir        │
    │                                      │                                  │
    │ [Simultaneamente Fel envía audio] <──────────────────────────────────── │
    │ <────────────────────────────────────┤ <─ sendCallChunk(callId, "Fel") ─┤
    │  onCallChunk()                       │  reenviar a Ale                  │
    │  _playCallChunk()                    │                                  │
    │     Reproducir audio de Fel          │                                  │
    │                                      │                                  │
    │ [Ale termina llamada]                │                                  │
    ├─ endCall() ─────────────────────────>│                                  │
    │  Ice: endCall(callId, "Ale")         │                                  │
    │  _stopAudioCapture()                 │                                  │
    │                                      │                                  │
    │                                [CallI.endCall]                          │
    │                                activeCalls.remove(callId)               │
    │                                CallManager.endCall(callId)              │
    │                                      │                                  │
    │ <──── notifyCallEvent(ended) ────────┼─────────────────────────────────>│
    │  [Llamada terminada]                 │                 onCallEvent()    │
    │                                      │               _stopAudioCapture()│
    │                                      │               [Llamada terminada]│
```

---

### Gestión de Estado de Llamadas

**CallManagerImpl.java** mantiene el estado de todas las llamadas activas:

```java
// Map: callId → Set<participantes>
private final Map<String, Set<String>> activeCalls = new ConcurrentHashMap<>();

public void createCall(String callId, Set<String> participants) {
    activeCalls.put(callId, participants);
    System.out.println("[CallManager] Llamada registrada: " + callId);
    System.out.println("[CallManager]   - Participantes: " + participants);
}

public Set<String> getParticipants(String callId) {
    return activeCalls.get(callId);  // Usado por sendCallChunk
}

public void endCall(String callId) {
    activeCalls.remove(callId);
    System.out.println("[CallManager] Llamada eliminada: " + callId);
}
```

**Sincronización con CallI.java:**
- `CallI.startCall()` → `CallManager.createCall(callId, participants)`
- `CallI.sendCallChunk()` → `CallManager.getParticipants(callId)` → reenvío
- `CallI.endCall()` → `CallManager.endCall(callId)`

---

### Formato de Audio

**PCM16 Little-Endian:**
- Sample rate: 44100 Hz (estándar CD)
- Channels: 1 (mono)
- Bit depth: 16 bits por sample
- Endianness: Little-endian (LSB primero)
- Chunk size: 2048 samples = 4096 bytes
- Frecuencia de chunks: ~46ms (2048 / 44100 = 0.0464 segundos)

**Conversión Float32 → PCM16 (cliente):**
```javascript
_float32ToPCM16(float32) {
    const buffer = new ArrayBuffer(float32.length * 2);
    const view = new DataView(buffer);
    for (let i = 0; i < float32.length; i++) {
        let s = Math.max(-1, Math.min(1, float32[i]));  // Clamp [-1, 1]
        view.setInt16(i * 2, s * 0x7fff, true);         // true = little-endian
    }
    return new Uint8Array(buffer);
}
```

**Conversión PCM16 → Float32 (cliente):**
```javascript
_pcm16ToFloat32(uint8Array) {
    const view = new DataView(uint8Array.buffer, uint8Array.byteOffset);
    const float32 = new Float32Array(uint8Array.length / 2);
    for (let i = 0; i < float32.length; i++) {
        const int16 = view.getInt16(i * 2, true);  // true = little-endian
        float32[i] = int16 / 0x7fff;               // Normalizar a [-1, 1]
    }
    return float32;
}
```

---

### Persistencia de Audio

**Notas de voz** (HistoryService.java):
```java
public static SavedAudio saveVoiceBytes(byte[] pcm16Data) throws IOException {
    String filename = "note_" + System.currentTimeMillis() + "_" + UUID.randomUUID() + ".wav";
    Path voiceDir = Paths.get("server/data/voice");
    Files.createDirectories(voiceDir);
    
    Path filePath = voiceDir.resolve(filename);
    
    // Construir header WAV (RIFF)
    ByteArrayOutputStream wavStream = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(wavStream);
    
    int sampleRate = 44100;
    int channels = 1;
    int bitsPerSample = 16;
    int byteRate = sampleRate * channels * bitsPerSample / 8;
    
    // Escribir header RIFF/WAV
    dos.writeBytes("RIFF");
    dos.writeInt(Integer.reverseBytes(36 + pcm16Data.length));
    dos.writeBytes("WAVE");
    dos.writeBytes("fmt ");
    dos.writeInt(Integer.reverseBytes(16));  // fmt chunk size
    dos.writeShort(Short.reverseBytes((short) 1));  // PCM
    dos.writeShort(Short.reverseBytes((short) channels));
    dos.writeInt(Integer.reverseBytes(sampleRate));
    dos.writeInt(Integer.reverseBytes(byteRate));
    dos.writeShort(Short.reverseBytes((short) (channels * bitsPerSample / 8)));
    dos.writeShort(Short.reverseBytes((short) bitsPerSample));
    dos.writeBytes("data");
    dos.writeInt(Integer.reverseBytes(pcm16Data.length));
    dos.write(pcm16Data);
    
    Files.write(filePath, wavStream.toByteArray());
    
    return new SavedAudio(filePath.toString(), pcm16Data.length);
}
```

**Historial (history.jsonl):**
```json
{"type":"voice_note","scope":"private","sender":"Ale","recipient":"Fel","audioFile":"server/data/voice/note_1732234567890_abc123.wav","sizeBytes":88200,"timestamp":"2025-11-26T10:30:45.123Z"}
{"type":"voice_group","scope":"group","sender":"Ale","group":"compunet","audioFile":"server/data/voice/note_1732234578901_def456.wav","sizeBytes":132300,"timestamp":"2025-11-26T10:31:15.234Z"}
{"type":"call","callId":"0fcb2a97-aaad-456d-a9e8-322d8ca98ea5","participants":["Ale","Fel"],"status":"started","timestamp":"2025-11-26T10:35:00.000Z"}
{"type":"call","callId":"0fcb2a97-aaad-456d-a9e8-322d8ca98ea5","status":"ended","endedBy":"Ale","timestamp":"2025-11-26T10:37:30.000Z"}
```

---

### Contratos y delimitación

- **TCP entre proxy/usuarios y backend**: texto plano por líneas terminadas en `\n` (sin JSON) - solo para mensajes de texto.
- **Ice ZeroC**: Binario sobre WebSocket en `ws://<host>:10010/call` - para audio (notas de voz y llamadas).
- **HTTP Polling**: El cliente consulta `GET /updates` cada 1.5s para recibir mensajes de texto.
- Comandos TCP soportados: `/msg`, `/msggroup`, `/creategroup`, `/joingroup`, `/quit` (audio NO usa estos comandos).
- Cada usuario mantiene:
  - Un socket TCP (vía proxy) para mensajes de texto
  - Una conexión Ice bidireccional para audio con callbacks (VoiceObserver)
