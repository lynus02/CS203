// services/tariffApi.js
import api from './api';

export interface TariffDto {
    trade_id: string;
    productId: number;
    hsDescription: string;
    productCode6: string;
    food_category: string;
    reporterName?: string;
    value: number;
}

const tariffApi = {
    // Run Audit
    async runAudit(): Promise<any> {
        try {
            const response = await api.post('/api/audit/check');
            return response.data;
        } catch (err: any) {
            const payload = err?.response?.data;
            const message =
                payload?.message || err?.message || 'Audit request failed';

            throw { message, status: err?.response?.status, payload };
        }
    },

    // GET /tariffs/suggest?q=xxx&country=xxx&page=0&size=20
    suggestProducts(query: string, country?: string, page = 0, size = 20) {
        return api.get('/tariffs/suggest',
            { params: { q: query, country, page, size } })
            .then((res) => res.data);
    },

    // GET /tariffs/rates/{size}?country=SG
    getTariffRatesBySize(size: number, country?: string) {
        return api.get(`/tariffs/rates/${size}`,
            { params: { country } })
            .then((res) => res.data);
    },

    // GET /tariffs/countries
    getCountries() {
        return api.get('/tariffs/countries')
            .then((res) => res.data);
    },

    // POST /tariffs/calculate
    calculateTariff(productCode: string, exportCountryCode: string, desCountryCode: string, customsValue: number) {
        return api.post('/tariffs/calculate',
            { productCode, exportCountryCode, desCountryCode, customsValue })
            .then((res) => res.data);
    }
}

// ===============================
// Saved Products API Integration
// ===============================

// GET /users/saved-products
async function getSavedProducts() {
    return api.get("/users/saved-products").then(res => res.data);
}

// POST /users/saved-products
async function saveProductConfig(payload: any) {
    return api.post("/users/saved-products", payload).then(res => res.data);
}

// DELETE /users/saved-products/{id}
async function deleteSavedProduct(id: number | string) {
    return api.delete(`/users/saved-products/${id}`).then(res => res.data);
}

export default {
    ...tariffApi,
    getSavedProducts,
    saveProductConfig,
    deleteSavedProduct
};