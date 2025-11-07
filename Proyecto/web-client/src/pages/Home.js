import { registerUser } from "../api/http.js";
import { connectWS } from "../api/ws.js";

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
            await registerUser(username);
            localStorage.setItem("chat_username", username);
            connectWS();
            window.history.pushState({}, "", "/chat");
            window.location.reload();
        } catch (e) {
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
