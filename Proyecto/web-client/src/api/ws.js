import { config } from "../config.js";

let ws = null;
let listeners = [];

export function connectWS() {
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
        return;
    }

    const serverHost = config.wsHost || window.location.hostname || 'localhost';
    console.log("[WS] Conectando a servidor WebSocket en:", serverHost, ":", config.wsPort);
    ws = new WebSocket(`ws://${serverHost}:${config.wsPort}`);

    ws.onopen = () => {
        console.log("[WS] Conectado al proxy");
    };

    ws.onclose = () => {
        console.log("[WS] Desconectado del proxy");
    };

    ws.onerror = (err) => {
        console.error("[WS] Error:", err);
    };

    ws.onmessage = (event) => {
        const text = event.data.toString();
        let msg;
        try {
            msg = JSON.parse(text);
        } catch (e) {
            console.warn("WS mensaje no JSON:", text);
            return;
        }

        for (let i = 0; i < listeners.length; i++) {
            listeners[i](msg);
        }
    };
}

export function onWSMessage(callback) {
    listeners.push(callback);
}
