// services/tariffApi.js
import api from './api';

export interface TariffDto {
    trade_id: string;
    hsDescription: string;
    productCode6: string;
    food_category: string;
    reporterName?: string;
    value: number;
}

const tariffApi = {
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

export default tariffApi;