import  Home  from "../pages/Home.js";
import { Router } from "./router.js";

const urls = {
    "/": Home ,

}


export const routes = Router(urls);