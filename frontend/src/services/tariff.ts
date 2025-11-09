// services/tariffApi.js
import api from './api.ts';

export const runAudit = async (): Promise<any> => {
    try {
        const response = await api.post('/api/audit/check');
        return response.data;
    } catch (err: any) {
        const payload = err?.response?.data;
        const message = payload?.message || err?.message || 'Audit request failed';
        throw { message, status: err?.response?.status, payload };
    }
};

// Suggest products based on search and country
export const suggestProducts = async (query: string, country?: string, page = 0, size = 20) => {
    const params: any = { q: query, page, size };
    if (country) params.country = country;
    const response = await api.get('/tariffs/suggest', { params });
    return response.data;
};

// Get product suggestions by country and size
export const getTariffRatesBySize = async (size: number, country?: string) => {
    let url = `/tariffs/size=${size}`;
    if (country) url += `?country=${encodeURIComponent(country)}`;
    const response = await api.get(url);
    return response.data;
};

// Calculate tariff for a product
export const calculateTariff = async (productCode: string, exportCountryCode: string, desCountryCode: string, customsValue: number) => {
    const payload = { productCode, exportCountryCode, desCountryCode, customsValue };
    const response = await api.post('/tariffs/calculate', payload);
    return response.data;
};

// Example: get all products (if needed)
export const getAllProducts = async () => {
    const response = await api.get('/products');
    return response.data;
};