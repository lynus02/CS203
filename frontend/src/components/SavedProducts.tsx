import { useState, useEffect } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card";
import { Button } from "./ui/button";
import { Badge } from "./ui/badge";
import { CountryFlag } from "./ui/country-flags";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "./ui/dialog";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { Bookmark, Trash2, Download, Calendar, DollarSign, Package } from "lucide-react";
import { toast } from "sonner";
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "./ui/alert-dialog";
import savedProductsService from "../services/savedProductsService";

interface Product {
    id: string;
    name: string;
    hsCode: string;
    category: string;
    baseTariffRate: number;
}

export interface SavedProductConfig {
    id: string;
    name: string; // User-defined name for this saved configuration
    product: Product;
    productValue: number;
    originCountry: string;
    destinationCountry: string;
    importDate: string;
    savedAt: string;
}

interface SavedProductsProps {
    onLoadProduct?: (config: SavedProductConfig) => void;
}

export function SavedProducts({ onLoadProduct }: SavedProductsProps) {
    const [savedProducts, setSavedProducts] = useState<SavedProductConfig[]>([]);
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [productToDelete, setProductToDelete] = useState<string | null>(null);

    // Reload saved products from localStorage into state
    const reloadSaved = () => {
        const stored = localStorage.getItem("foodTariffSavedProducts");
        if (stored) {
            try {
                const parsed = JSON.parse(stored) as SavedProductConfig[];
                setSavedProducts(parsed);
                console.log('reloadSaved: loaded', parsed.length, 'saved products');
                return;
            } catch (e) {
                console.error('Failed to parse saved products', e);
            }
        }
        // If no stored items or parse failed, ensure empty list
        setSavedProducts([]);
        console.log('reloadSaved: no saved products found');
    };

    // Load saved products from localStorage on mount
    useEffect(() => {
        reloadSaved();
        console.log('SavedProducts component mounted');
    }, []);

    // Listen for global saved-products changes so external save actions (from other components)
    // can notify this component to reload the list.
    useEffect(() => {
        const handler = () => reloadSaved();
        window.addEventListener('savedProductsChanged', handler as EventListener);
        return () => window.removeEventListener('savedProductsChanged', handler as EventListener);
    }, []);

    const handleDelete = (id: string) => {
        setProductToDelete(id);
        setDeleteDialogOpen(true);
    };

    const confirmDelete = () => {
        if (productToDelete) {
            const updated = savedProducts.filter(p => p.id !== productToDelete);
            setSavedProducts(updated);
            localStorage.setItem("foodTariffSavedProducts", JSON.stringify(updated));
            toast.success("Product configuration deleted");
            setDeleteDialogOpen(false);
            setProductToDelete(null);
        }
    };

    const handleLoad = (config: SavedProductConfig) => {
        onLoadProduct?.(config);
        toast.success("Product configuration loaded");
    };

    return (
        <>
            <Card>
                <CardHeader>
                    <div className="flex items-center justify-between">
                        <div>
                            <CardTitle className="flex items-center gap-2">
                                <Bookmark className="h-5 w-5" />
                                Saved Products
                            </CardTitle>
                            <CardDescription>
                                Manage your saved product configurations for quick access
                            </CardDescription>
                        </div>
                    </div>
                </CardHeader>
                <CardContent>
                    {savedProducts.length === 0 ? (
                        <div className="text-center py-8 text-muted-foreground">
                            <Bookmark className="h-12 w-12 mx-auto mb-3 opacity-20" />
                            <p>No saved products yet</p>
                            <p className="text-sm mt-1">
                                Calculate a product and save it for quick access later
                            </p>
                        </div>
                    ) : (
                        <div className="space-y-3">
                            {savedProducts.map((config) => (
                                <div
                                    key={config.id}
                                    className="p-4 border rounded-lg bg-card hover:bg-accent/50 transition-colors"
                                >
                                    <div className="flex items-start justify-between gap-4">
                                        <div className="flex-1 space-y-2">
                                            <div className="flex items-center gap-2">
                                                <Package className="h-4 w-4 text-primary" />
                                                <h4 className="font-medium">{config.name}</h4>
                                            </div>

                                            <div className="grid grid-cols-1 md:grid-cols-2 gap-2 text-sm">
                                                <div>
                                                    <span className="text-muted-foreground">Product:</span>{" "}
                                                    <span className="font-medium">{config.product.name}</span>
                                                </div>
                                                <div>
                                                    <span className="text-muted-foreground">HS Code:</span>{" "}
                                                    <span className="font-medium">{config.product.hsCode}</span>
                                                </div>
                                                <div className="flex items-center gap-1">
                                                    <span className="text-muted-foreground">Value:</span>{" "}
                                                    <DollarSign />
                                                    <span className="font-xl">{config.productValue.toLocaleString()}</span>
                                                </div>
                                                <div>
                                                    <Badge variant="secondary">{config.product.category}</Badge>
                                                </div>
                                            </div>

                                            <div className="flex flex-wrap items-center gap-3 text-sm">
                                                <div className="flex items-center gap-2">
                                                    <span className="text-muted-foreground">From: </span>
                                                    <span className="font-medium ml-1">{config.originCountry}</span>
                                                    <CountryFlag country={config.originCountry} />
                                                </div>
                                                <div className="flex items-center gap-1">
                                                    <span className="text-muted-foreground">To: </span>
                                                    <span className="font-medium ml-1">{config.destinationCountry}</span>
                                                    <CountryFlag country={config.destinationCountry} />
                                                </div>
                                                <div className="flex items-center gap-2">
                                                    <Calendar className="h-3 w-3" />
                                                    <span className="text-muted-foreground">Import:</span>
                                                    <span className="ml-1">{new Date(config.importDate).toLocaleDateString()}</span>
                                                </div>
                                            </div>

                                            <div className="text-xs text-muted-foreground">
                                                Saved: {new Date(config.savedAt).toLocaleString()}
                                            </div>
                                        </div>

                                        <div className="flex flex-col gap-2">
                                            {/* Load button removed per request - kept delete button only */}
                                            <Button
                                                size="sm"
                                                variant="outline"
                                                onClick={() => handleDelete(config.id)}
                                            >
                                                <Trash2 className="h-4 w-4" />
                                            </Button>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </CardContent>
            </Card>

            {/* Delete Confirmation Dialog */}
            <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Delete Saved Product?</AlertDialogTitle>
                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently delete this saved product configuration.
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setProductToDelete(null)}>Cancel</AlertDialogCancel>
                        <AlertDialogAction onClick={confirmDelete}>Delete</AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </>
    );
}

interface SaveProductDialogProps {
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
    importDate: Date;
    userId: string;
    onSave?: () => void;
}

export function SaveProductDialog({
                                      product,
                                      productValue,
                                      originCountry,
                                      destinationCountry,
                                      importDate,
                                      userId,
                                      onSave
                                  }: SaveProductDialogProps) {
    const [open, setOpen] = useState(false);
    const [configName, setConfigName] = useState("");
    const [loading, setLoading] = useState(false);

    // handle saving product configuration to backend
    const handleSave = async() => {
        if (!configName.trim()) {
            toast.error("Please enter a name for this configuration");
            return;
        }

        const productData: SavedProductConfig = {
            id: `saved-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
            name: configName,
            product,
            productValue,
            originCountry,
            destinationCountry,
            importDate: importDate.toISOString(),
            savedAt: new Date().toISOString()
        };

        // Load existing saved products
        try {
            setLoading(true);

            // call backend with userId
            const saved = await savedProductsService.saveProduct(userId, productData);
            toast.success("Product configuration saved to your account!");
            setConfigName("");
            setOpen(false);
            onSave?.();
            console.log('SaveProductDialog: saved to backend', saved);
            window.dispatchEvent(
                new CustomEvent('savedProductsChanged', {detail: saved})
            );
        } catch (error: any) {
            console.error('Error saving product configuration', error);
            toast.error(`Failed to save product: ${error.message || 'Unknown error'}`);
        } finally {
            setLoading(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
                <Button variant="outline">
                    <Bookmark className="h-4 w-4 mr-2" />
                    Save Product
                </Button>
            </DialogTrigger>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Save Product</DialogTitle>
                    <DialogDescription>
                        Give this configuration a name to save it for quick access later
                    </DialogDescription>
                </DialogHeader>

                <div className="space-y-4 py-4">
                    <div className="space-y-2">
                        <Label htmlFor="config-name">Product Name</Label>
                        <Input
                            id="config-name"
                            placeholder="e.g., Weekly Beef Import to USA"
                            value={configName}
                            onChange={(e) => setConfigName(e.target.value)}
                        />
                    </div>

                    <div className="p-3 bg-muted rounded-lg space-y-2 text-sm">
                        <div><strong>Product:</strong> {product.name}</div>
                        <div><strong>HS Code:</strong> {product.hsCode}</div>
                        <div><strong>Value:</strong> ${productValue.toLocaleString()}</div>
                        <div className="flex items-center gap-2">
                            <CountryFlag country={originCountry} />
                            <strong>Origin:</strong> {originCountry}
                        </div>
                        <div className="flex items-center gap-2">
                            <CountryFlag country={destinationCountry} />
                            <strong>Destination:</strong> {destinationCountry}
                        </div>
                    </div>
                </div>

                <DialogFooter>
                    <Button variant="outline" onClick={() => setOpen(false)}>
                        Cancel
                    </Button>
                    <Button onClick={handleSave} disable={loading}>
                        {loading ? "Saving..." : "Save Product"}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
