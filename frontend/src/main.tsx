import { createRoot } from "react-dom/client";
import { SavedProductsProvider } from "./components/context/SavedProductsContext";
import App from "./App";
import "./index.css";

createRoot(document.getElementById("root")!).render(
    <SavedProductsProvider>
        <App />
    </SavedProductsProvider>
);