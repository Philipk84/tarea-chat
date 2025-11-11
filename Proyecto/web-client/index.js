import { routes } from "./src/router/Routes.js";
import "./styles.css";

const app = document.getElementById("app");
app.innerHTML = "";



app.appendChild(routes);