import { registerUser } from "../api/http.js";
//import { connectWS } from "../api/ws.js";
import Chat from "./Chat.js"; // 👈 IMPORTANTE: importamos el componente de chat

function Home() {
    const container = document.createElement("div");
    container.classList.add("centered");

    const title = document.createElement("h1");
    title.textContent = "Chat TCP";

    const input = document.createElement("input");
    input.type = "text";
    input.placeholder = "Nombre de usuario";

    const btn = document.createElement("button");
    btn.textContent = "Entrar";

    const error = document.createElement("div");
    error.classList.add("error");

    btn.onclick = async () => {
        const username = input.value.trim();
        if (!username) {
            error.textContent = "Ingresa un nombre";
            return;
        }

        try {
            // 1) Registramos en el backend
            await registerUser(username);

            // 2) Guardamos el usuario localmente
            localStorage.setItem("chat_username", username);

            // 3) Conectamos WebSocket
            //connectWS();

            // 4) Opcional: actualizamos la URL para que se vea /chat
            window.history.pushState({}, "", "/chat");

            // 5) Renderizamos la pantalla de chat en el mismo SPA
            const app = document.getElementById("app");
            app.innerHTML = "";
            app.appendChild(Chat());

        } catch (e) {
            console.error(e);
            error.textContent = e.message || "No se pudo registrar";
        }
    };

    container.appendChild(title);
    container.appendChild(input);
    container.appendChild(btn);
    container.appendChild(error);

    return container;
}

export default Home;
