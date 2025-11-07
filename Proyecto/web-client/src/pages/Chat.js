import {
    sendPrivateMessage,
    sendGroupMessage,
    createGroup,
    addUsersToGroup
} from "../api/http.js";
import { connectWS, onWSMessage } from "../api/ws.js";

function Chat() {
    const username = localStorage.getItem("chat_username");
    if (!username) {
        window.history.pushState({}, "", "/");
        window.location.reload();
        const div = document.createElement("div");
        return div;
    }

    connectWS();

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
    groupTitle.textContent = "Grupo";

    const groupNameInput = document.createElement("input");
    groupNameInput.placeholder = "Nombre del grupo";

    const groupMembersInput = document.createElement("input");
    groupMembersInput.placeholder = "Miembros (comma separados)";

    const createGroupBtn = document.createElement("button");
    createGroupBtn.textContent = "Crear grupo";

    sidebar.appendChild(userInfo);
    sidebar.appendChild(directTitle);
    sidebar.appendChild(directInput);
    sidebar.appendChild(groupTitle);
    sidebar.appendChild(groupNameInput);
    sidebar.appendChild(groupMembersInput);
    sidebar.appendChild(createGroupBtn);

    // ----- MAIN -----
    const main = document.createElement("main");
    main.classList.add("chat-main");

    const header = document.createElement("div");
    header.classList.add("chat-header");

    const chatTitle = document.createElement("h2");
    chatTitle.textContent = "Selecciona destino o grupo";
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
        messages.innerHTML = "";
    };

    // Crear grupo
    createGroupBtn.onclick = async () => {
        const gname = groupNameInput.value.trim();
        if (!gname) return;

        try {
            await createGroup(gname);

            const membersRaw = groupMembersInput.value.trim();
            if (membersRaw) {
                const members = membersRaw.split(",").map((m) => m.trim()).filter(Boolean);
                if (members.length > 0) {
                    await addUsersToGroup(gname, members);
                }
            }

            currentChat = { type: "group", id: gname };
            chatTitle.textContent = "Grupo: " + gname;
            messages.innerHTML = "";
        } catch (e) {
            alert("Error creando grupo: " + e.message);
        }
    };

    // Enviar mensaje
    sendBtn.onclick = async () => {
        const text = msgInput.value.trim();
        if (!text || !currentChat) return;

        try {
            if (currentChat.type === "user") {
                await sendPrivateMessage(username, currentChat.id, text);
            } else {
                await sendGroupMessage(currentChat.id, username, text);
            }

            appendMessage({
                self: true,
                text: text,
                from: username,
                to: currentChat.type === "user" ? currentChat.id : null,
                group: currentChat.type === "group" ? currentChat.id : null
            });

            msgInput.value = "";
        } catch (e) {
            alert("Error enviando mensaje: " + e.message);
        }
    };

    msgInput.addEventListener("keydown", (e) => {
        if (e.key === "Enter") {
            sendBtn.click();
        }
    });

    // Mostrar mensaje en UI
    function appendMessage({ from, to, group, text, self }) {
        const div = document.createElement("div");
        div.classList.add("message");
        if (self) {
            div.classList.add("me");
            div.textContent = "(Yo) " + text;
        } else if (group) {
            div.textContent = "[" + group + "] " + from + ": " + text;
        } else {
            div.textContent = from + " → " + to + ": " + text;
        }
        messages.appendChild(div);
        messages.scrollTop = messages.scrollHeight;
    }

    // WS: escuchar mensajes push del server
    onWSMessage((msg) => {
        if (!msg || !msg.command || !msg.data) return;

        if (msg.command === "GET_MESSAGE") {
            const data = msg.data; // { sender, receiver, message }
            if (
                data.sender === username ||
                data.receiver === username ||
                (currentChat &&
                    currentChat.type === "user" &&
                    (currentChat.id === data.sender || currentChat.id === data.receiver))
            ) {
                appendMessage({
                    from: data.sender,
                    to: data.receiver,
                    text: data.message,
                    self: data.sender === username
                });
            }
        }

        if (msg.command === "GET_MSG_GROUP") {
            const data = msg.data; // { group, sender, message }
            if (
                currentChat &&
                currentChat.type === "group" &&
                currentChat.id === data.group
            ) {
                appendMessage({
                    from: data.sender,
                    group: data.group,
                    text: data.message,
                    self: data.sender === username
                });
            }
        }
    });

    return root;
}

export default Chat;
