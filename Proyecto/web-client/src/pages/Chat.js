import {
  sendPrivateMessage,
  sendGroupMessage,
  createGroup,
  joinGroup,
  getUpdates,
  getPrivateHistory,
  getGroupHistory,
} from "../api/http.js";

import voiceDelegate from "../services/voiceDelegate.js";
import callManager from "../services/callManager.js";
import { createRecorder } from "../services/recorder.js";

function Chat() {
  const username = localStorage.getItem("chat_username");
  if (!username) {
    window.history.pushState({}, "", "/");
    window.location.reload();
    return document.createElement("div");
  }

  let recBtn = null;
  let voiceInitErrorShown = false;
  
  // Almacenar mensajes de voz pendientes por conversación
  const pendingVoiceMessages = {}; // { "user:id" o "group:id": [entries] }
  
  // Inicializar callManager
  callManager.init(username);

  // Función auxiliar para obtener clave de chat
  function getChatKey(chat) {
    if (!chat) return null;
    return chat.type === "user" ? `user:${chat.id}` : `group:${chat.id}`;
  }

  // Función auxiliar para verificar si un entry pertenece al chat actual
  function isEntryForCurrentChat(entry) {
    if (!currentChat) return false;
    
    if (currentChat.type === "user") {
      // Para chat privado: verificar si involucra al usuario actual y al otro
      const isPrivate = entry.scope === "private";
      const involvesSelf = entry.sender === username || entry.recipient === username;
      const involvesOther = entry.sender === currentChat.id || entry.recipient === currentChat.id;
      return isPrivate && involvesSelf && involvesOther;
    } else {
      // Para grupo: verificar nombre del grupo
      return entry.scope === "group" && entry.group === currentChat.id;
    }
  }

  /** 
  voiceDelegate.subscribe((entry) => {
    console.log("[Voice] Notificación recibida:", entry);
    
    // Verificar si el entry es para el chat actual
    if (isEntryForCurrentChat(entry)) {
      // Mostrar inmediatamente
      appendHistoryItem(entry);
      messages.scrollTop = messages.scrollHeight;
    } else {
      // Guardar en pendientes para cuando se abra ese chat
      let chatKey;
      if (entry.scope === "private") {
        // Determinar el otro usuario
        const otherUser = entry.sender === username ? entry.recipient : entry.sender;
        chatKey = `user:${otherUser}`;
      } else {
        chatKey = `group:${entry.group}`;
      }
      
      if (!pendingVoiceMessages[chatKey]) {
        pendingVoiceMessages[chatKey] = [];
      }
      pendingVoiceMessages[chatKey].push(entry);
      console.log(`[Voice] Mensaje guardado para ${chatKey}`);
    }
  });
  */

    voiceDelegate.subscribe((entry) => {
    // Los eventos de llamada (call_chunk, call_event) los maneja callManager
    // Aquí solo procesamos VoiceEntry (notas de voz)
    console.log("[Voice] Notificación recibida:", entry);
    
    // Filtrar solo VoiceEntry (tienen audioFile y type="voice_note" o "voice_group")
    if (!entry.audioFile || (entry.type !== "voice_note" && entry.type !== "voice_group")) {
      console.log("[Voice] Ignorando notificación que no es VoiceEntry");
      return;
    }
    
    if (isEntryForCurrentChat(entry)) {
      appendHistoryItem(entry);
      messages.scrollTop = messages.scrollHeight;
    } else {
      let chatKey;
      if (entry.scope === "private") {
        const otherUser = entry.sender === username ? entry.recipient : entry.sender;
        chatKey = `user:${otherUser}`;
      } else {
        chatKey = `group:${entry.group}`;
      }
      
      if (!pendingVoiceMessages[chatKey]) {
        pendingVoiceMessages[chatKey] = [];
      }
      pendingVoiceMessages[chatKey].push(entry);
      console.log(`[Voice] Mensaje guardado para ${chatKey}`);
    }
  });


  // ----- ESTRUCTURA GENERAL -----
  const root = document.createElement("div");
  root.classList.add("chat-app");

  // ----- SIDEBAR -----
  const sidebar = document.createElement("aside");
  sidebar.classList.add("sidebar");

  const userInfo = document.createElement("div");
  userInfo.classList.add("user-info");
  userInfo.textContent = "Conectado como: " + username;

  const directTitle = document.createElement("h2");
  directTitle.textContent = "Chat directo";

  const directInput = document.createElement("input");
  directInput.placeholder = "Usuario destino";

  const groupTitle = document.createElement("h2");
  groupTitle.textContent = "Grupos";

  const groupNameInput = document.createElement("input");
  groupNameInput.placeholder = "Nombre del grupo";

  const groupMembersInput = document.createElement("input");
  groupMembersInput.placeholder = "Miembros (coma separados)";

  const createGroupBtn = document.createElement("button");
  createGroupBtn.textContent = "Crear grupo";

  const joinGroupBtn = document.createElement("button");
  joinGroupBtn.textContent = "Unirse a grupo";

  sidebar.appendChild(userInfo);
  sidebar.appendChild(directTitle);
  sidebar.appendChild(directInput);
  sidebar.appendChild(groupTitle);
  sidebar.appendChild(groupNameInput);
  sidebar.appendChild(groupMembersInput);
  sidebar.appendChild(createGroupBtn);
  sidebar.appendChild(joinGroupBtn);

  // ----- MAIN -----
  const main = document.createElement("main");
  main.classList.add("chat-main");

  const header = document.createElement("div");
  header.classList.add("chat-header");

  const chatTitle = document.createElement("h2");
  chatTitle.textContent = "Conversación global";
  header.appendChild(chatTitle);

  // ----- BOTONES PARA LLAMADAS -----
  const callBtn = document.createElement("button");
  callBtn.textContent = "📞 Llamar";
  callBtn.onclick = makeCallToCurrentUser;

  const hangBtn = document.createElement("button");
  hangBtn.textContent = "⛔ Colgar";
  hangBtn.onclick = hangUpCall;

  header.appendChild(callBtn);
  header.appendChild(hangBtn);

  // Modal para llamadas entrantes
  const callModal = document.createElement("div");
  callModal.style.display = "none";
  callModal.style.position = "fixed";
  callModal.style.top = "50%";
  callModal.style.left = "50%";
  callModal.style.transform = "translate(-50%, -50%)";
  callModal.style.background = "white";
  callModal.style.padding = "20px";
  callModal.style.borderRadius = "8px";
  callModal.style.boxShadow = "0 4px 6px rgba(0,0,0,0.3)";
  callModal.style.zIndex = "1000";

  const callModalText = document.createElement("p");
  callModal.appendChild(callModalText);

  const acceptCallBtn = document.createElement("button");
  acceptCallBtn.textContent = "✅ Aceptar";
  acceptCallBtn.style.marginRight = "10px";
  callModal.appendChild(acceptCallBtn);

  const rejectCallBtn = document.createElement("button");
  rejectCallBtn.textContent = "❌ Rechazar";
  callModal.appendChild(rejectCallBtn);

  const messages = document.createElement("div");
  messages.classList.add("messages");

  const inputBar = document.createElement("div");
  inputBar.classList.add("input-bar");

  const msgInput = document.createElement("input");
  msgInput.type = "text";
  msgInput.placeholder = "Escribe un mensaje...";

  const sendBtn = document.createElement("button");
  sendBtn.textContent = "Enviar";

  let currentRecorder = null;

  recBtn = document.createElement("button");
  recBtn.textContent = "🎤";
  recBtn.disabled = true;
  recBtn.title = "Conectando con el servicio de voz...";

  voiceDelegate.onStatusChange((status, detail) => {
    if (!recBtn) return;

    if (status === "connected") {
      recBtn.disabled = false;
      recBtn.title = "Pulsa para grabar una nota de voz";
    } else if (status === "connecting") {
      recBtn.disabled = true;
      recBtn.title = "Conectando con el servicio de voz...";
    } else if (status === "error") {
      recBtn.disabled = true;
      recBtn.title = "Reintentando conexión con el servicio de voz...";

      if (!voiceInitErrorShown) {
        const message = detail?.message || "No se pudo conectar con el servicio de notas de voz.";
        alert(message + " Revisa que el servidor esté activo.");
        voiceInitErrorShown = true;
      }
    }
  });

  voiceDelegate.init(username);

  recBtn.onclick = async () => {
    if (!currentChat) {
      alert("Selecciona primero un usuario o grupo");
      return;
    }

    if (voiceDelegate.getStatus() !== "connected") {
      alert("El servicio de voz todavía se está conectando. Inténtalo en unos segundos.");
      return;
    }

    await voiceDelegate.ensureReady();

    // Si ya está grabando, paramos y enviamos
    if (currentRecorder) {
      try {
        await currentRecorder.stop();
      } catch (err) {
        console.error("Error enviando nota de voz:", err);
        alert(err.message || "No se pudo enviar la nota de voz");
      } finally {
        currentRecorder = null;
        recBtn.textContent = "🎤";
      }
      return;
    }

    // Crear grabador con el destino actual
    try {
      currentRecorder = createRecorder(username, currentChat);
      await currentRecorder.start();
      recBtn.textContent = "⏹";
      console.log("[Voice] Grabación iniciada");
    } catch (err) {
      console.error("Error iniciando grabación:", err);
      currentRecorder = null;
      alert(err.message || "No se pudo iniciar la grabación");
    }
  };

  inputBar.appendChild(recBtn);

  inputBar.appendChild(msgInput);
  inputBar.appendChild(sendBtn);

  main.appendChild(header);
  main.appendChild(messages);
  main.appendChild(inputBar);

  root.appendChild(sidebar);
  root.appendChild(main);
  root.appendChild(callModal);

  // ----- ESTADO -----
  let currentChat = null; // { type: "user" | "group", id: string }

  function openDirectChat() {
  const to = directInput.value.trim();
  if (!to) return;
  if (to === username) {
    alert("No puedes chatear contigo mismo 😉");
    return;
  }
  currentChat = { type: "user", id: to };
  chatTitle.textContent = "Chat con: " + to;
  loadHistory();
}

// Cambiar cuando se pierda el foco
directInput.addEventListener("change", openDirectChat);

// Cambiar al presionar Enter
directInput.addEventListener("keydown", (e) => {
  if (e.key === "Enter") {
    e.preventDefault();
    openDirectChat();
    msgInput.focus();
  }
});


  // Crear grupo
createGroupBtn.onclick = async () => {
  const gname = groupNameInput.value.trim();
  if (!gname) return;

  try {
    // 1) Crear grupo como el usuario actual
    await createGroup(gname, username);

    // 2) Asegurar que el creador esté dentro del grupo
    await joinGroup(gname, username);

    // 3) Seleccionar el grupo como chat actual
    currentChat = { type: "group", id: gname };
    chatTitle.textContent = "Grupo: " + gname;

    // 4) Cargar historial de ese grupo
    await loadHistory();
  } catch (e) {
    alert("Error creando grupo: " + (e.message || e));
  }
};


// Unirse a grupo manualmente
joinGroupBtn.onclick = async () => {
  const gname = groupNameInput.value.trim();
  if (!gname) return;

  try {
    await joinGroup(gname, username);

    currentChat = { type: "group", id: gname };
    chatTitle.textContent = "Grupo: " + gname;
    await loadHistory();
  } catch (e) {
    alert("Error uniéndose al grupo: " + (e.message || e));
  }
};


  // ----- ENVÍO DE MENSAJES -----
  sendBtn.onclick = async () => {
    const text = msgInput.value.trim();
    if (!text) return;

    try {
      if (!currentChat) {
        alert("Selecciona un usuario o grupo primero");
        return;
      }

      if (currentChat.type === "user") {
        await sendPrivateMessage(username, currentChat.id, text);
      } else {
        await sendGroupMessage(currentChat.id, username, text);
      }

      appendMessage({
        self: true,
        from: username,
        to: currentChat.type === "user" ? currentChat.id : null,
        group: currentChat.type === "group" ? currentChat.id : null,
        text
      });

      msgInput.value = "";
    } catch (e) {
      alert("Error enviando mensaje: " + e.message);
    }
  };

  // ----- LLAMADAS CON ICE -----
  async function makeCallToCurrentUser() {
    if (!currentChat) {
      alert("Selecciona primero un usuario o grupo");
      return;
    }
    
    try {
      if (currentChat.type === "user") {
        await callManager.startPrivateCall(currentChat.id);
        alert(`Llamando a ${currentChat.id}...`);
      } else if (currentChat.type === "group") {
        await callManager.startGroupCall(currentChat.id);
        alert(`Llamando al grupo ${currentChat.id}...`);
      }
    } catch (e) {
      alert("Error iniciando llamada: " + (e.message || e));
    }
  }

  async function hangUpCall() {
    console.log("[Chat] hangUpCall ejecutado - stack:", new Error().stack);
    try {
      await callManager.endCall();
      alert("Llamada finalizada");
    } catch (e) {
      alert("Error terminando llamada: " + (e.message || e));
    }
  }
  
  // Manejar llamadas entrantes
  callManager.onIncomingCall((call) => {
    let callerInfo;
    if (call.type === "private") {
      callerInfo = `${call.caller} te está llamando`;
    } else {
      callerInfo = `Llamada grupal entrante en ${call.group}`;
    }
    
    callModalText.textContent = callerInfo;
    callModal.style.display = "block";
    
    acceptCallBtn.onclick = async () => {
      try {
        await callManager.acceptCall(call.callId);
        callModal.style.display = "none";
        alert("Llamada aceptada");
      } catch (e) {
        alert("Error aceptando llamada: " + (e.message || e));
        callModal.style.display = "none";
      }
    };
    
    rejectCallBtn.onclick = async () => {
      try {
        await callManager.rejectCall(call.callId);
        callModal.style.display = "none";
      } catch (e) {
        alert("Error rechazando llamada: " + (e.message || e));
        callModal.style.display = "none";
      }
    };
  });
  
  // Manejar eventos de llamada para notificaciones en UI
  callManager.onCallEvent((event) => {
    console.log("[Call Event]", event);
    
    const eventMessages = {
      call_started: "Llamada iniciada",
      call_accepted: `${event.caller} aceptó la llamada`,
      call_rejected: `${event.caller} rechazó la llamada`,
      call_ended: "Llamada finalizada",
    };
    
    const message = eventMessages[event.type];
    if (message) {
      const div = document.createElement("div");
      div.classList.add("message", "system");
      div.textContent = `🔔 ${message}`;
      div.style.textAlign = "center";
      div.style.fontStyle = "italic";
      div.style.color = "#666";
      messages.appendChild(div);
      messages.scrollTop = messages.scrollHeight;
    }
  });


  msgInput.addEventListener("keydown", (e) => {
    if (e.key === "Enter") sendBtn.click();
  });

  // ----- MOSTRAR MENSAJES -----
  function appendMessage({ from, to, group, text, self }) {
    const div = document.createElement("div");
    div.classList.add("message");

    if (self) {
      div.classList.add("me");
      div.textContent = `(Yo) ${text}`;
    } else if (group) {
      div.innerHTML = `<strong>[${group}] ${from}:</strong> ${text}`;
    } else if (to) {
      div.innerHTML = `<strong>${from} → ${to}:</strong> ${text}`;
    } else {
      div.textContent = text;
    }

    messages.appendChild(div);
    messages.scrollTop = messages.scrollHeight;
  }

  // ----- HISTORIAL -----
  async function loadHistory() {
    // Limpia mensajes actuales
    messages.innerHTML = "";

    if (!currentChat) return;

    try {
      let items = [];
      if (currentChat.type === "user") {
        const res = await getPrivateHistory(username, currentChat.id);
        items = res.items || [];
      } else if (currentChat.type === "group") {
        const res = await getGroupHistory(currentChat.id);
        items = res.items || [];
      }

      if (!items.length) {
        const empty = document.createElement("div");
        empty.classList.add("message");
        empty.textContent = "Sin historial";
        messages.appendChild(empty);
      } else {
        for (const it of items) {
          appendHistoryItem(it);
        }
      }
      
      // Cargar mensajes de voz pendientes para este chat
      const chatKey = getChatKey(currentChat);
      if (chatKey && pendingVoiceMessages[chatKey]) {
        console.log(`[Voice] Cargando ${pendingVoiceMessages[chatKey].length} mensajes pendientes para ${chatKey}`);
        for (const entry of pendingVoiceMessages[chatKey]) {
          appendHistoryItem(entry);
        }
        // Limpiar pendientes ya mostrados
        delete pendingVoiceMessages[chatKey];
      }
      
      messages.scrollTop = messages.scrollHeight;
    } catch (e) {
      const err = document.createElement("div");
      err.classList.add("message");
      err.textContent = "Error cargando historial: " + (e.message || e);
      messages.appendChild(err);
    }
  }

  function appendHistoryItem(item) {
    // item tiene campos: type, scope, sender, recipient|group, message|audioFile
    if (item.type === "text") {
      if (item.scope === "group") {
        appendMessage({ from: item.sender, group: item.group, text: item.message });
      } else {
        const other = item.sender === username ? item.recipient : item.sender;
        appendMessage({ from: item.sender, to: other, text: item.message });
      }
      return;
    }

    if (item.type === "voice_note" || item.type === "voice_group") {
      const row = document.createElement("div");
      row.classList.add("message");
      
      // Agregar clase para identificar si es mensaje propio
      if (item.sender === username) {
        row.classList.add("me");
      }

      // Título/etiqueta
      const label = document.createElement("div");
      label.style.fontWeight = "bold";
      if (item.scope === "group") {
        label.textContent = `[${item.group}] ${item.sender}: nota de voz`;
      } else {
        const target = item.sender === username ? item.recipient : item.sender;
        if (item.sender === username) {
          label.textContent = `(Yo) → ${target}: nota de voz`;
        } else {
          label.textContent = `${item.sender} → Tú: nota de voz`;
        }
      }
      row.appendChild(label);

      // Audio
      const audio = document.createElement("audio");
      audio.controls = true;

      // audioFile es una ruta relativa tipo 'server/data/voice/xxx.wav'
      const audioFile = (item.audioFile || "").toString();
      const fileName = audioFile.split(/[\\/]/).pop();
      audio.src = `/voice/${fileName}`;
      audio.style.display = "block";
      audio.style.marginTop = "4px";
      
      // Agregar timestamp si existe
      if (item.timestamp) {
        const time = document.createElement("div");
        time.style.fontSize = "0.8em";
        time.style.color = "#666";
        time.style.marginTop = "2px";
        const date = new Date(item.timestamp);
        time.textContent = date.toLocaleTimeString();
        row.appendChild(time);
      }
      
      row.appendChild(audio);

      messages.appendChild(row);
      console.log("[Voice] Audio agregado a la UI:", fileName);
      return;
    }

    // Ignorar otros tipos (llamadas, etc.)
  }

  // ----- POLLING AUTOMÁTICO DE NUEVOS MENSAJES -----
  async function pollUpdates() {
    try {
      const { items } = await getUpdates(username);
      if (!items || !Array.isArray(items)) return;

      for (const line of items) {
        appendIncoming(line);
      }
    } catch (e) {
      console.error("Error obteniendo updates:", e.message);
    }
  }


  function appendIncoming(line) {
    const div = document.createElement("div");
    div.classList.add("message");

    // Mejorar formato si tiene "usuario: mensaje"
    const parts = line.split(":");
    if (parts.length > 1) {
      const [meta, ...rest] = parts;
      div.innerHTML = `<strong>${meta}:</strong> ${rest.join(":").trim()}`;
    } else {
      div.textContent = line;
    }

    messages.appendChild(div);
    messages.scrollTop = messages.scrollHeight;
  }

  // Ejecutar cada 1.5 s
  setInterval(pollUpdates, 1500);

  return root;
}

export default Chat;