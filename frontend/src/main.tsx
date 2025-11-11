import { createRoot } from "react-dom/client";
import { ThemeProvider } from "./components/context/ThemeContext";
import App from "./App";
import "./index.css";
import "./styles/globals.css";

createRoot(document.getElementById("root")!).render(
    <ThemeProvider children={<App />} />
);
