// services/savedProductsService.ts
import api from './api';

import api from './api';

// Interfaces
export interface SavedProductConfig {
    id: string;
    name: string;
    product: {
        id: string;
        name: string;
        hsCode: string;
        category: string;
        baseTariffRate: number;
    };
    productValue: number;
    originCountry: string;
    destinationCountry: string;
    importDate: string;
    savedAt: string;
    _syncStatus?: 'local' | 'synced' | 'pending';
}

export interface SaveProductRequest {
    name: string;
    product: {
        id: string;
        name: string;
        hsCode: string;
        category: string;
        baseTariffRate: number;
    };
    productValue: number;
    originCountry: string;
    destinationCountry: string;
    importDate: string;
}

// ✅ Declare constants OUTSIDE the object
const LOCAL_STORAGE_KEY = "foodTariffSavedProducts";

const savedProductsService = {
    // API METHODS
    async saveProduct(productData: SaveProductRequest): Promise<SavedProductConfig> {
        try {
            const response = await api.post('/api/saved-products', productData);
            return response.data;
        } catch (err: any) {
            const payload = err?.response?.data;
            const message = payload?.message || err?.message || 'Save product failed';
            throw { message, status: err?.response?.status, payload };
        }
    },

    async getSavedProducts(): Promise<SavedProductConfig[]> {
        try {
            const response = await api.get('/api/saved-products');
            return response.data;
        } catch (err: any) {
            const payload = err?.response?.data;
            const message = payload?.message || err?.message || 'Get saved products failed';
            throw { message, status: err?.response?.status, payload };
        }
    },

    async deleteSavedProduct(productId: string): Promise<void> {
        try {
            await api.delete(`/api/saved-products/${productId}`);
        } catch (err: any) {
            const payload = err?.response?.data;
            const message = payload?.message || err?.message || 'Delete product failed';
            throw { message, status: err?.response?.status, payload };
        }
    },

    // LOCAL STORAGE HELPERS
    isAuthenticated(): boolean {
        return !!localStorage.getItem('token');
    },

    generateId(): string {
        return `saved-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    },

    // SMART SAVE
    async smartSave(productData: SaveProductRequest): Promise<SavedProductConfig> {
        const newConfig: SavedProductConfig = {
            ...productData,
            id: this.generateId(),
            savedAt: new Date().toISOString(),
            _syncStatus: 'local'
        };

        this.saveToLocalStorage(newConfig);

        if (this.isAuthenticated()) {
            try {
                const syncedConfig = await this.saveProduct(productData);
                this.updateLocalStorageWithSynced(syncedConfig);
                return syncedConfig;
            } catch (error) {
                console.warn('Backend save failed, keeping local only:', error);
                newConfig._syncStatus = 'local';
                this.updateLocalStorageWithSyncStatus(newConfig.id, 'local');
            }
        }

        return newConfig;
    },

    async smartGet(): Promise<SavedProductConfig[]> {
        if (!this.isAuthenticated()) {
            return this.getFromLocalStorage();
        }

        try {
            const backendProducts = await this.getSavedProducts();
            const localProducts = this.getFromLocalStorage();
            return this.mergeProducts(localProducts, backendProducts);
        } catch (error) {
            console.warn('Backend fetch failed, using local data:', error);
            return this.getFromLocalStorage();
        }
    },

    async smartDelete(productId: string): Promise<void> {
        this.deleteFromLocalStorage(productId);

        if (this.isAuthenticated()) {
            try {
                await this.deleteSavedProduct(productId);
            } catch (error) {
                console.warn('Backend delete failed, but local delete succeeded:', error);
            }
        }
    },

    async syncLocalToBackend(): Promise<void> {
        if (!this.isAuthenticated()) return;

        const localProducts = this.getFromLocalStorage();
        const unsyncedProducts = localProducts.filter(p => p._syncStatus === 'local');

        for (const product of unsyncedProducts) {
            try {
                const { name, product: prod, productValue, originCountry, destinationCountry, importDate } = product;
                const synced = await this.saveProduct({
                    name, product: prod, productValue, originCountry, destinationCountry, importDate
                });
                this.updateLocalStorageWithSynced(synced);
            } catch (error) {
                console.warn(`Failed to sync product ${product.id}:`, error);
            }
        }
    },

    // LOCAL STORAGE IMPLEMENTATION
    saveToLocalStorage(config: SavedProductConfig): void {
        const existing = this.getFromLocalStorage();
        const updated = [...existing.filter(p => p.id !== config.id), config];
        localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(updated));
    },

    getFromLocalStorage(): SavedProductConfig[] {
        const stored = localStorage.getItem(LOCAL_STORAGE_KEY);
        return stored ? JSON.parse(stored) : [];
    },

    deleteFromLocalStorage(productId: string): void {
        const existing = this.getFromLocalStorage();
        const updated = existing.filter(p => p.id !== productId);
        localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(updated));
    },

    updateLocalStorageWithSynced(syncedConfig: SavedProductConfig): void {
        const existing = this.getFromLocalStorage();
        const updated = existing.map(item =>
            item.id === syncedConfig.id ? { ...syncedConfig, _syncStatus: 'synced' } : item
        );
        localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(updated));
    },

    updateLocalStorageWithSyncStatus(productId: string, status: 'local' | 'synced'): void {
        const existing = this.getFromLocalStorage();
        const updated = existing.map(item =>
            item.id === productId ? { ...item, _syncStatus: status } : item
        );
        localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(updated));
    },

    mergeProducts(local: SavedProductConfig[], backend: SavedProductConfig[]): SavedProductConfig[] {
        const merged = [...backend];
        local.forEach(localItem => {
            const existsInBackend = backend.some(backendItem => backendItem.id === localItem.id);
            if (!existsInBackend && localItem._syncStatus === 'local') {
                merged.push(localItem);
            }
        });
        return merged.sort((a, b) => new Date(b.savedAt).getTime() - new Date(a.savedAt).getTime());
    }
};

export default savedProductsService;
