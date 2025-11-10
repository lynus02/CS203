import { createRoot } from "react-dom/client";
import { SavedProductsProvider } from "./components/context/SavedProductsContext";
import { ThemeProvider } from "./components/context/ThemeContext";
import App from "./App";
import "./index.css";

createRoot(document.getElementById("root")!).render(
    <ThemeProvider children={
        <SavedProductsProvider children={
            <App />
        } />
    } />
);