// services/savedProductsService.ts
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

const LOCAL_STORAGE_KEY = 'foodTariffSavedProducts';

const savedProductsService = {
    // Helper: get JWT token
    getAuthHeaders() {
        const token = localStorage.getItem('token');
        return token ? { Authorization: `Bearer ${token}` } : {};
    },
    // API METHODS
    async saveProduct(userId: string, productData: SaveProductRequest): Promise<SavedProductConfig> {
        try {
            const response = await api.post(`/api/users/${userId}/saved-products`, productData, {
                headers: this.getAuthHeaders()
            });
            return response.data;
        } catch (err: any) {
            const message = err?.response?.data?.message || err.message || 'Failed to save product';
            throw { message, status: err?.response?.status };
        }
    },

    async getSavedProducts(userId: String): Promise<SavedProductConfig[]> {
        try {
            const response = await api.get(`/api/users/${userId}/saved-products`, {
                headers: this.getAuthHeaders()
            });
            return response.data;
        } catch (err: any) {
            const message = err?.response?.data?.message || err.message || 'Failed to get saved products';
            throw { message, status: err?.response?.status };
        }
    },

    async deleteSavedProduct(userId: string, productId: string): Promise<void> {
        try {
            await api.delete(`/api/users/${userId}/saved-products/${productId}`, {
                headers: this.getAuthHeaders()
            });
        } catch (err: any) {
            const message = err?.response?.data?.message || err.message || 'Failed to delete product';
            throw { message, status: err?.response?.status };
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
    async smartSave(UserId: string, productData: SaveProductRequest): Promise<SavedProductConfig> {
        const newConfig: SavedProductConfig = {
            ...productData,
            id: this.generateId(),
            savedAt: new Date().toISOString(),
            _syncStatus: 'local'
        };
        // save to localstorage
        this.saveToLocalStorage(newConfig);

        if (this.isAuthenticated()) {
            try {
                const syncedConfig = await this.saveProduct(productData);
                this.updateLocalStorageWithSynced(syncedConfig);
                return syncedConfig;
            } catch (error) {
                console.warn('Backend save failed, keeping local only:', error);
                this.updateLocalStorageWithSyncStatus(newConfig.id, 'local');
            }
        }

        return newConfig;
    },

    async smartGet(userId: string, productData: SaveProductRequest): Promise<SavedProductConfig[]> {
        if (!this.isAuthenticated()) return this.getFromLocalStorage();

        try {
            const backendProducts = await this.getSavedProducts(userId);
            const localProducts = this.getFromLocalStorage();
            return this.mergeProducts(localProducts, backendProducts);
        } catch (error) {
            console.warn('Backend fetch failed, using local data:', error);
            return this.getFromLocalStorage();
        }
    },

    async smartDelete(userId: string, productId: string): Promise<void> {
        this.deleteFromLocalStorage(productId);

        if (this.isAuthenticated()) {
            try {
                await this.deleteSavedProduct(userId, productId);
            } catch (error) {
                console.warn('Backend delete failed, but local delete succeeded:', error);
            }
        }
    },

    async syncLocalToBackend(userId: string): Promise<void> {
        if (!this.isAuthenticated()) return;

        const localProducts = this.getFromLocalStorage();
        const unsyncedProducts = localProducts.filter(p => p._syncStatus === 'local');

        for (const product of unsyncedProducts) {
            try {
                const { name, product: prod, productValue, originCountry, destinationCountry, importDate } = product;
                const synced = await this.saveProduct(userId,{
                    name, product: prod, productValue, originCountry, destinationCountry, importDate: importDate ||''
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
