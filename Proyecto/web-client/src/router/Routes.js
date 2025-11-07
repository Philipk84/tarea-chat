import Home from "../pages/Home.js";
import Chat from "../pages/Chat.js";
import { Router } from "./router.js";

const urls = {
    "/": Home,
    "/chat": Chat,
};

export const routes = Router(urls);
