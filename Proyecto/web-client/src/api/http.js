const API_BASE = "http://localhost:3001";

async function postJSON(path, body) {
    const res = await fetch(API_BASE + path, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(body)
    });

    const text = await res.text();
    let data;
    try {
        data = JSON.parse(text);
    } catch (e) {
        data = text;
    }

    if (!res.ok) {
        const msg = typeof data === "string" ? data : (data.error || "Error en la petición");
        throw new Error(msg);
    }

    return data;
}

export function registerUser(username) {
    return postJSON("/register", {
        username: username,
        clientIp: "127.0.0.1"
    });
}

export function sendPrivateMessage(sender, receiver, message) {
    return postJSON("/chat", { sender, receiver, message });
}

export function createGroup(groupName) {
    return postJSON("/group/create", { groupName });
}

export function addUsersToGroup(groupName, members) {
    return postJSON("/group/add", { groupName, members });
}

export function sendGroupMessage(groupName, sender, message) {
    return postJSON("/group/message", { groupName, sender, message });
}
