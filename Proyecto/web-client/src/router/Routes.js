import Home from "../pages/Home.js";
import Chat from "../pages/Chat.js";
import { Router } from "./Router.js";

const urls = {
    "/": Home,
    "/chat": Chat,
};

export const routes = Router(urls);
