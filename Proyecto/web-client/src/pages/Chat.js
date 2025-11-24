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

  voiceDelegate.subscribe((entry) => {
    appendHistoryItem(entry);
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
        return;
      }

      for (const it of items) {
        appendHistoryItem(it);
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

      // Título/etiqueta
      const label = document.createElement("div");
      label.style.fontWeight = "bold";
      if (item.scope === "group") {
        label.textContent = `[${item.group}] ${item.sender}: nota de voz`;
      } else {
        const target = item.sender === username ? item.recipient : username;
        label.textContent = `${item.sender} → ${target}: nota de voz`;
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
      row.appendChild(audio);

      messages.appendChild(row);
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
