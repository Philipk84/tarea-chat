let ws = null;
let listeners = [];

export function connectWS() {
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
        return;
    }

    const serverHost = window.location.hostname || 'localhost';
    ws = new WebSocket(`ws://${serverHost}:3002`);

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
