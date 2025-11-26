// Proyecto/web-client/src/api/http.js
import axios from "axios";
import { config } from "../config.js";

const API_BASE = config.httpBaseUrl || "/api";

async function post(path, body) {
  const url = API_BASE + path;
  const res = await axios.post(url, body, {
    headers: { "Content-Type": "application/json" }
  });
  return res.data;
}

async function get(path, params) {
  const url = API_BASE + path;
  const res = await axios.get(url, { params });
  return res.data;
}

export function registerUser(username) {
  return post("/register", { username });
}

export function sendPrivateMessage(sender, receiver, message) {
  // el backend no necesita sender aquí; lo conserva el servidor Java
  return post("/chat", { sender, receiver, message });
}

// Crear grupo (incluye quién lo crea)
export function createGroup(groupName, creator) {
  return post("/group/create", { groupName, creator });
}

// Unirse a grupo (incluye quién se une)
export function joinGroup(groupName, user) {
  return post("/group/join", { groupName, user });
}

// Mensaje a grupo (incluye quién envía)
export function sendGroupMessage(groupName, sender, message) {
  return post("/group/message", { groupName, sender, message });
}

export function getPrivateHistory(user, peer) {
  return get("/history", { scope: "private", user, peer });
}

export function getGroupHistory(group) {
  return get("/history", { scope: "group", group });
}

export function getUpdates(user) {
  return get("/updates", { user });
}

export function startCall(caller, callee) {
  return post("/call/start", { caller, callee });
}

export function startGroupCall(caller, groupName) {
  return post("/call/group", { caller, groupName });
}

export function endCall(user, callId) {
  return post("/call/end", { user, callId });
}

// (Opcional) registrar puerto UDP en el server, si estás usando /udpport
export function registerUdpPort(user, port) {
  return post("/udp/register", { user, port });
}