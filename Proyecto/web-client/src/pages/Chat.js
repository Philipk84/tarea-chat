import {
  sendPrivateMessage,
  sendGroupMessage,
  createGroup,
  joinGroup,
  getUpdates
} from "../api/http.js";

function Chat() {
  const username = localStorage.getItem("chat_username");
  if (!username) {
    window.history.pushState({}, "", "/");
    window.location.reload();
    return document.createElement("div");
  }

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

  inputBar.appendChild(msgInput);
  inputBar.appendChild(sendBtn);

  main.appendChild(header);
  main.appendChild(messages);
  main.appendChild(inputBar);

  root.appendChild(sidebar);
  root.appendChild(main);

  // ----- ESTADO -----
  let currentChat = null; // { type: "user" | "group", id: string }

  // Seleccionar chat directo
  directInput.onchange = () => {
    const to = directInput.value.trim();
    if (!to) return;
    currentChat = { type: "user", id: to };
    chatTitle.textContent = "Chat con: " + to;
  };

  // Crear grupo
  createGroupBtn.onclick = async () => {
    const gname = groupNameInput.value.trim();
    if (!gname) return;

    try {
      await createGroup(gname);

      // Los miembros se unirán manualmente, uno por uno
      const membersRaw = groupMembersInput.value.trim();
      if (membersRaw) {
        const members = membersRaw
          .split(",")
          .map((m) => m.trim())
          .filter(Boolean);
        for (const member of members) {
          await joinGroup(gname);
        }
      }

      currentChat = { type: "group", id: gname };
      chatTitle.textContent = "Grupo: " + gname;
    } catch (e) {
      alert("Error creando grupo: " + e.message);
    }
  };

  // Unirse a grupo manualmente
  joinGroupBtn.onclick = async () => {
    const gname = groupNameInput.value.trim();
    if (!gname) return;
    try {
      await joinGroup(gname);
      currentChat = { type: "group", id: gname };
      chatTitle.textContent = "Grupo: " + gname;
    } catch (e) {
      alert("Error uniéndose al grupo: " + e.message);
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
