import { useTheme } from "../context/ThemeContext";
import { Moon, Sun } from "lucide-react";

const ThemeToggle = () => {
    const { theme, toggleTheme } = useTheme();

    return (
        <button
            onClick={toggleTheme}
            className="inline-flex h-16 items-center justify-between border-2 border-gray-400 bg-gray-200"
            style={{
                width: '500px',
                paddingLeft: '30px',
                paddingRight: '400px',
                borderRadius: '9999px'
            }}
        >
            {/* Left - Sun */}
            <div
                className="flex h-20 w-20 items-center justify-center transition-all duration-300"
                style={{
                    backgroundColor: theme === 'light' ? '#e5e7eb' : 'transparent',
                    boxShadow: theme === 'light' ? 'inset 0 2px 6px rgba(0,0,0,0.15)' : 'none',
                    borderRadius: '50%'
                }}
            >
                <Sun className={`h-7 w-7 transition-colors duration-300 ${theme === 'light' ? 'text-gray-700' : 'text-gray-400'}`} />
            </div>

            {/* Right - Moon */}
            <div
                className="flex h-12 w-12 items-center justify-center transition-all duration-300"
                style={{
                    backgroundColor: theme === 'dark' ? '#475569' : 'transparent',
                    boxShadow: theme === 'dark' ? 'inset 0 2px 6px rgba(0,0,0,0.3)' : 'none',
                    borderRadius: '50%'
                }}
            >
                <Moon className={`h-7 w-7 transition-colors duration-300 ${theme === 'dark' ? 'text-white' : 'text-gray-400'}`} />
            </div>
        </button>
    );
};

export default ThemeToggle;