// services/api.ts
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';
console.log("API Base URL:", import.meta.env.VITE_API_URL);

const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: 10000,
});

// Request interceptor
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');

        // With Spring Security endpoints
        const publicEndpoints = [
            "/auth/login",
            "/auth/refresh",
            "/users",
            "/tariffs",
            "/products",
            "/", // just in case
        ];

        const isPublic = publicEndpoints.some((p) =>
            config.url === p || config.url?.startsWith(`${p}/`)
        );

        if (!isPublic && token) {
            config.headers["Authorization"] = `Bearer ${token}`;
        } else {
            delete config.headers["Authorization"]; // ensures public endpoints are truly public
        }

        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response interceptor
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            localStorage.removeItem("token");
        }
        return Promise.reject(error);
    }
);

export default api;