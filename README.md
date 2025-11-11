## Integrantes
- Alejandro Vargas
- Sebastián Romero
- Felipe Calderon
  
## Ejecución del proyecto

### Solo la primera vez

Se hace el build de backend en Java desde la carpeta Proyecto, donde está gradlew:

  ```bash
  cd Proyecto
  ```

  ```bash
  ./gradlew clean build 
  ```
Luego hay que moverse dentro de la carpeta web-client y se instalan todas las dependencias:
  ```bash
  cd web-client
  ```
  ```bash
  npm install
  ```

### Ejecución de la aplicación

1. Se inicia el sevidor Java, desde la clase Main.js

2. Se inicia el proxy, para esto hay que moverse a la carpeta de web-client

  ```bash
  cd Proyecto/web-client
  ```
Y luego se ejecuta

  ```bash
  node proxy/index.js
  ```

3. Iniciar el cliente web, se crea otra terminal y se vuelve a posicionar en web-client:

  ```bash
  cd Proyecto/web-client
  ```
Y se inicia

## 2. Descripción del flujo de comunicación entre cliente, proxy y backend

### Conexiones activas y puertos

- Cliente → Proxy (HTTP): la UI hace fetch/XHR a `http://localhost:3001` mediante rutas `/api/*` (el devServer en `:8080` reescribe hacia `:3001`).
- Proxy ↔ Backend Java (TCP texto, línea a línea):
  - Conexión TCP persistente a `127.0.0.1:5000` para comandos “globales” (crear grupo, unirse, mensaje a grupo).
  - Conexión TCP por usuario registrado: al hacer `POST /register` se abre un `net.Socket` dedicado; se usa para enviar sus comandos (p. ej. `/msg`) y recibir mensajes entrantes.
- Archivos de audio: el proxy sirve estático en `/voice/*` los WAV que el backend guarda en `server/data/voice`.

Puertos por defecto:
- Backend TCP: `5000` (desde `Proyecto/config.json`)
- Proxy HTTP: `3001`
- Cliente: `8080` (proxy de `/api` y `/voice` hacia `3001`)

---

### Patrón “pull” (HTTP → TCP → HTTP)

#### Cliente (HTTP) → Proxy

Endpoints reales de la pagina web:
- Registrar usuario → `POST /api/register` `{ username }`
- Enviar privado → `POST /api/chat` `{ sender, receiver, message }`
- Crear grupo → `POST /api/group/create` `{ groupName }`
- Unirse a grupo → `POST /api/group/join` `{ groupName }`
- Mensaje a grupo → `POST /api/group/message` `{ groupName, message }`
- Historial privado → `GET /api/history?scope=private&user=U&peer=P`
- Historial de grupo → `GET /api/history?scope=group&group=G`
- Polling de eventos → `GET /api/updates?user=U`
- Salud proxy → `GET /api/health`

#### Proxy (traducción) → Backend Java (TCP, texto plano por línea)

- `POST /register`              → abre socket usuario y envía: `<username>\n`
- `POST /chat`                  → usa socket del sender: `/msg <receiver> <message>\n`
- `POST /group/create`          → socket persistente: `/creategroup <groupName>\n`
- `POST /group/join`            → socket persistente: `/joingroup <groupName>\n`
- `POST /group/message`         → socket persistente: `/msggroup <groupName> <message>\n`
- `GET /history`                → no consulta al backend; lee `server/data/history.jsonl` y filtra
- `GET /updates`                → retorna y limpia cola `userMessages[user]` acumulada del socket de ese usuario

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

### Patrón “push” simulado (Backend → Proxy → Cliente)

1. El backend envía al socket del usuario receptor líneas como:
   - Privado: `MENSAJE_PRIVADO de <sender>: <texto>`
   - Grupo: `MENSAJE_GRUPO [<grupo>] de <sender>: <texto>`
2. El proxy las guarda en `userMessages[usuario]`.
3. El navegador consulta cada 1.5 s `GET /api/updates?user=U` y recibe `{ items: ["MENSAJE_PRIVADO de ...", ...] }`.
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

- Privado (pull + ack):
  - UI → `POST /api/chat` `{ sender:"ana", receiver:"bob", message:"Hola!" }`
  - Proxy → TCP: `/msg bob Hola!`
  - Backend → socket ana: `Mensaje enviado a bob`
  - Backend → socket bob: `MENSAJE_PRIVADO de ana: Hola!`
  - UI de bob → `GET /api/updates?user=bob` → muestra el mensaje

- Grupo (mensaje + recepción por polling):
  - UI → `POST /api/group/create` `{ groupName:"compunet" }`
  - UI → `POST /api/group/join` `{ groupName:"compunet" }` (cada miembro)
  - UI → `POST /api/group/message` `{ groupName:"compunet", message:"Hola equipo" }`
  - Backend difunde a miembros: `MENSAJE_GRUPO [compunet] de ana: Hola equipo`
  - Cada miembro lo obtiene en `/api/updates`

- Historial:
  - UI → `GET /api/history?scope=private&user=ana&peer=bob`
  - Proxy → lee y filtra `history.jsonl`, devuelve `{ items:[...] }`
  - UI renderiza texto y audios (con `<audio src="/voice/archivo.wav">`)

---

### Contratos y delimitación

- TCP entre proxy/usuarios y backend: texto plano por líneas terminadas en `\n` (sin JSON).
- Comandos soportados (entre otros): `/msg`, `/msggroup`, `/creategroup`, `/joingroup`, `/udpport`, `/call`, `/callgroup`, `/quit`.
- Cada usuario mantiene su propio socket; el proxy enruta por ese socket los comandos que requieren identidad del emisor.
