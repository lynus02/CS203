const API_BASE_URL = '/api';

export class ApiService {
    private static async request<T> (
        endpoint: string,
        options: RequestInit = {}
    ): Promise<T> {
        const url = `${API_BASE_URL}${endpoint}`;

        const config: RequestInit = {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options,
        };

        const response = await fetch(url, config);

        if (!response.ok) {
            throw new Error('HTTP error! Status: ' + response.status);
        }
        return response.json();
    }

    static get<T> (endpoint: string): Promise<T> {
        return this.request<T>(endpoint, { method: 'GET' });
    }

    static post<T> (endpoint: string, body?: any): Promise<T> {
        return this.request<T>(endpoint, {
            method: 'POST',
            body: body ? JSON.stringify(body) : undefined,
        });
    }

    static put<T> (endpoint: string, body?: any): Promise<T> {
        return this.request<T>(endpoint, {
            method: 'PUT',
            body: JSON.stringify(body),
        });
    }

    static delete<T> (endpoint: string): Promise<T> {
        return this.request<T>(endpoint, { method: 'DELETE' });
    }
}