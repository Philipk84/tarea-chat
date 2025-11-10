// Proyecto/web-client/src/api/http.js
import axios from "axios";
const API_BASE = "/api";

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

export function createGroup(groupName) {
  return post("/group/create", { groupName });
}

export function joinGroup(groupName) {
  return post("/group/join", { groupName });
}

export function sendGroupMessage(groupName, sender, message) {
  return post("/group/message", { groupName, message });
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