import { createContext, useState, useContext, ReactNode } from 'react';
import { Product } from '../ProductSelector'; // ← Importing from your file

interface SavedProductsContextType {
    savedProducts: Product[];
    fetchSavedProducts: (userId: number) => Promise<void>;
    addSavedProduct: (product: Product) => Promise<void>;
    removeSavedProduct: (productId: string) => Promise<void>;
    isLoading: boolean;
}

const SavedProductsContext = createContext<SavedProductsContextType | undefined>(undefined);

interface SavedProductsProviderProps {
    children: ReactNode;
}

export const SavedProductsProvider = ({ children }: SavedProductsProviderProps) => {
    const [savedProducts, setSavedProducts] = useState<Product[]>([]);
    const [isLoading, setIsLoading] = useState(false);

    const fetchSavedProducts = async (userId: number) => {
        setIsLoading(true);
        try {
            const response = await fetch(`/api/users/${userId}/saved-products`, {
                headers: {
                    'Content-Type': 'application/json',
                    // Add authorization header if needed
                    // 'Authorization': `Bearer ${token}`
                }
            });

            if (!response.ok) {
                throw new Error('Failed to fetch saved products');
            }

            const data: Product[] = await response.json();
            setSavedProducts(data);
        } catch (error) {
            console.error('Error fetching saved products:', error);
            setSavedProducts([]);
        } finally {
            setIsLoading(false);
        }
    };

    const addSavedProduct = async (product: Product) => {
        try {
            const response = await fetch('/api/saved-products', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    // Add authorization header if needed
                },
                body: JSON.stringify(product)
            });

            if (!response.ok) {
                throw new Error('Failed to save product');
            }

            const savedProduct: Product = await response.json();
            setSavedProducts([...savedProducts, savedProduct]);
        } catch (error) {
            console.error('Error saving product:', error);
            throw error;
        }
    };

    const removeSavedProduct = async (productId: string) => {
        try {
            const response = await fetch(`/api/saved-products/${productId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                    // Add authorization header if needed
                }
            });

            if (!response.ok) {
                throw new Error('Failed to remove product');
            }

            setSavedProducts(savedProducts.filter(p => p.id !== productId));
        } catch (error) {
            console.error('Error removing product:', error);
            throw error;
        }
    };

    return (
        <SavedProductsContext.Provider
            value={{
                savedProducts,
                fetchSavedProducts,
                addSavedProduct,
                removeSavedProduct,
                isLoading
            }}
        >
            {children}
        </SavedProductsContext.Provider>
    );
};

export const useSavedProducts = () => {
    const context = useContext(SavedProductsContext);
    if (context === undefined) {
        throw new Error('useSavedProducts must be used within a SavedProductsProvider');
    }
    return context;
};