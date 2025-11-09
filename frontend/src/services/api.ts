// services/api.ts
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';
console.log("All Vite env vars:", import.meta.env);
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
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        console.log('Making API request:', config.method?.toUpperCase(), config.url);
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response interceptor
api.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        // Log helpful details
        console.error('API Error:', {
            status: error.response?.status,
            data: error.response?.data,
            message: error.message
        });

        // If unauthorized, clear local token and notify app so it can prompt for login
        const status = error.response?.status;
        if (status === 401) {
            try {
                localStorage.removeItem('token');
            } catch (e) {
                console.warn('Failed to remove token from localStorage', e);
            }
            try {
                window.dispatchEvent(new Event('auth:unauthorized'));
            } catch (e) {
                console.warn('Failed to dispatch auth:unauthorized event', e);
            }
        }

        return Promise.reject(error);
    }
);

export default api;