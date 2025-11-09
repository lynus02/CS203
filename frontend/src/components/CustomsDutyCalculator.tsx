import { useState, useEffect } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select";
import { Button } from "./ui/button";
import { Badge } from "./ui/badge";
import { Calendar } from "./ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "./ui/popover";
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from "./ui/command";
import { CalendarIcon, Search, CheckCircle, AlertTriangle, Info } from "lucide-react";
import { CountryFlag } from "./ui/country-flags";
import { SaveProductDialog, SavedProductConfig } from "./SavedProducts";
// import { format } from "date-fns";

interface Product {
    id: string;
    name: string;
    hsCode: string;
    category: string;
    baseTariffRate: number;
}

interface TradeAgreement {
    countries: string[];
    name: string;
    reduction: number; // percentage reduction
    conditions?: string;
}

interface TariffResult {
    product: Product;
    originCountry: string;
    destinationCountry: string;
    importDate: Date;
    baseTariffRate: number;
    tradeAgreementReduction: number;
    finalTariffRate: number;
    dutyAmount: number;
    totalCost: number;
    tradeAgreement?: TradeAgreement;
}

interface CustomsDutyCalculatorProps {
    onResultsChange?: (results: TariffResult | null) => void;
    savedConfig?: SavedProductConfig;
}

export function CustomsDutyCalculator({ onResultsChange, savedConfig }: CustomsDutyCalculatorProps) {
    const [productValue, setProductValue] = useState("");
    const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
    const [productSearch, setProductSearch] = useState("");
    const [originCountry, setOriginCountry] = useState("");
    const [destinationCountry, setDestinationCountry] = useState("");
    const [importDate, setImportDate] = useState<Date>(new Date());
    const [result, setResult] = useState<TariffResult | null>(null);
    const [productOpen, setProductOpen] = useState(false);

    // Food product database with HS codes
    const products: Product[] = [
        { id: "1", name: "Fresh Beef Ribeye Steaks", hsCode: "0201.10.50", category: "Meat & Poultry", baseTariffRate: 26.4 },
        { id: "2", name: "Fresh Pork Tenderloin", hsCode: "0203.12.90", category: "Meat & Poultry", baseTariffRate: 1.4 },
        { id: "3", name: "Fresh Chicken Breast", hsCode: "0207.14.10", category: "Meat & Poultry", baseTariffRate: 17.6 },
        { id: "4", name: "Whole Milk Powder", hsCode: "0402.10.05", category: "Dairy Products", baseTariffRate: 13.8 },
        { id: "5", name: "Aged Cheddar Cheese", hsCode: "0406.90.54", category: "Dairy Products", baseTariffRate: 17.5 },
        { id: "6", name: "Fresh Atlantic Salmon", hsCode: "0302.12.00", category: "Seafood", baseTariffRate: 0 },
        { id: "7", name: "Frozen Shrimp", hsCode: "0306.17.00", category: "Seafood", baseTariffRate: 0 },
        { id: "8", name: "Fresh Bananas", hsCode: "0803.90.30", category: "Fruits & Vegetables", baseTariffRate: 0 },
        { id: "9", name: "Fresh Avocados", hsCode: "0804.40.00", category: "Fruits & Vegetables", baseTariffRate: 11.2 },
        { id: "10", name: "Arabica Coffee Beans", hsCode: "0901.21.00", category: "Coffee & Tea", baseTariffRate: 0 },
        { id: "11", name: "Black Tea Leaves", hsCode: "0902.30.00", category: "Coffee & Tea", baseTariffRate: 6.4 },
        { id: "12", name: "Extra Virgin Olive Oil", hsCode: "1509.10.20", category: "Oils & Fats", baseTariffRate: 5 },
        { id: "13", name: "Basmati Rice", hsCode: "1006.30.90", category: "Grains & Legumes", baseTariffRate: 2.1 },
        { id: "14", name: "Organic Quinoa", hsCode: "1008.50.90", category: "Grains & Legumes", baseTariffRate: 0.6 },
        { id: "15", name: "Dark Chocolate (70% Cocoa)", hsCode: "1806.32.70", category: "Confectionery", baseTariffRate: 5.1 },
        { id: "16", name: "Raw Cane Sugar", hsCode: "1701.14.20", category: "Sugar & Sweeteners", baseTariffRate: 1.4 },
        { id: "17", name: "Orange Juice Concentrate", hsCode: "2009.11.00", category: "Beverages", baseTariffRate: 7.9 },
        { id: "18", name: "Premium Red Wine", hsCode: "2204.21.30", category: "Beverages", baseTariffRate: 6.3 },
        { id: "19", name: "Canned Tuna in Oil", hsCode: "1604.14.30", category: "Preserved Foods", baseTariffRate: 12.5 },
        { id: "20", name: "Dried Dates", hsCode: "0804.10.80", category: "Dried Fruits & Nuts", baseTariffRate: 2.9 }
    ];

    const countries = [
        "United States", "Canada", "Mexico", "China", "Japan", "South Korea",
        "Germany", "France", "United Kingdom", "Italy", "Spain", "Australia",
        "Singapore", "Thailand", "Vietnam", "India", "Brazil", "Chile"
    ];

    // Trade agreements with tariff reductions
    const tradeAgreements: TradeAgreement[] = [
        {
            countries: ["United States", "Mexico", "Canada"],
            name: "USMCA (NAFTA)",
            reduction: 100, // 100% reduction (duty-free)
            conditions: "Must meet rules of origin"
        },
        {
            countries: ["United States", "Australia"],
            name: "US-Australia FTA",
            reduction: 100,
            conditions: "Qualifying goods only"
        },
        {
            countries: ["Australia", "Singapore"],
            name: "SAFTA",
            reduction: 50,
            conditions: "Preferential tariff treatment"
        },
        {
            countries: ["European Union", "Japan"],
            name: "EU-Japan EPA",
            reduction: 75,
            conditions: "Economic Partnership Agreement"
        },
        {
            countries: ["China", "Australia"],
            name: "ChAFTA",
            reduction: 30,
            conditions: "China-Australia Free Trade Agreement"
        }
    ];

    const filteredProducts = products.filter(product =>
        product.name.toLowerCase().includes(productSearch.toLowerCase()) ||
        product.hsCode.includes(productSearch)
    );

    const findApplicableTradeAgreement = (origin: string, destination: string): TradeAgreement | undefined => {
        return tradeAgreements.find(agreement =>
            (agreement.countries.includes(origin) && agreement.countries.includes(destination)) ||
            (agreement.countries.includes(destination) && agreement.countries.includes(origin))
        );
    };

    const calculateTariff = () => {
        if (!selectedProduct || !productValue || !originCountry || !destinationCountry) {
            return;
        }

        const value = parseFloat(productValue);
        let baseTariffRate = selectedProduct.baseTariffRate;
        let finalTariffRate = baseTariffRate;
        let tradeAgreementReduction = 0;

        // Check for applicable trade agreements
        const tradeAgreement = findApplicableTradeAgreement(originCountry, destinationCountry);

        if (tradeAgreement) {
            tradeAgreementReduction = (baseTariffRate * tradeAgreement.reduction) / 100;
            finalTariffRate = baseTariffRate - tradeAgreementReduction;
        }

        const dutyAmount = (value * finalTariffRate) / 100;
        const totalCost = value + dutyAmount;

        const calculationResult = {
            product: selectedProduct,
            originCountry,
            destinationCountry,
            importDate,
            baseTariffRate,
            tradeAgreementReduction,
            finalTariffRate,
            dutyAmount,
            totalCost,
            tradeAgreement,
            productValue: value
        };

        setResult(calculationResult);
        onResultsChange?.(calculationResult);
    };

    const clearCalculation = () => {
        setProductValue("");
        setSelectedProduct(null);
        setProductSearch("");
        setOriginCountry("");
        setDestinationCountry("");
        setImportDate(new Date());
        setResult(null);
        onResultsChange?.(null);
    };

    // Load saved configuration when provided
    useEffect(() => {
        if (savedConfig) {
            setSelectedProduct(savedConfig.product);
            setProductValue(savedConfig.productValue.toString());
            setOriginCountry(savedConfig.originCountry);
            setDestinationCountry(savedConfig.destinationCountry);
            setImportDate(new Date(savedConfig.importDate));

            // Trigger calculation after a brief delay to ensure state is updated
            setTimeout(() => {
                const value = savedConfig.productValue;
                const product = savedConfig.product;
                let baseTariffRate = product.baseTariffRate;
                let finalTariffRate = baseTariffRate;
                let tradeAgreementReduction = 0;

                const tradeAgreement = findApplicableTradeAgreement(savedConfig.originCountry, savedConfig.destinationCountry);

                if (tradeAgreement) {
                    tradeAgreementReduction = (baseTariffRate * tradeAgreement.reduction) / 100;
                    finalTariffRate = baseTariffRate - tradeAgreementReduction;
                }

                const dutyAmount = (value * finalTariffRate) / 100;
                const totalCost = value + dutyAmount;

                const calculationResult = {
                    product,
                    originCountry: savedConfig.originCountry,
                    destinationCountry: savedConfig.destinationCountry,
                    importDate: new Date(savedConfig.importDate),
                    baseTariffRate,
                    tradeAgreementReduction,
                    finalTariffRate,
                    dutyAmount,
                    totalCost,
                    tradeAgreement,
                    productValue: value
                };

                setResult(calculationResult);
                onResultsChange?.(calculationResult);
            }, 100);
        }
    }, [savedConfig]);

    const canSaveProduct = selectedProduct && productValue && originCountry && destinationCountry;

    return (
        <Card>
            <CardHeader>
                <CardTitle>Food Tariff Calculator</CardTitle>
                <CardDescription>
                    Calculate food import duties with trade agreement adjustments and comprehensive food product selection
                </CardDescription>
            </CardHeader>

            <CardContent className="space-y-6">
                {/* Country of Origin and Destination Country */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {/* Country of Origin */}
                    <div className="space-y-2">
                        <Label htmlFor="origin-country">Country of Origin</Label>
                        <Select value={originCountry} onValueChange={setOriginCountry}>
                            <SelectTrigger className="w-full">
                                <SelectValue placeholder="Select origin country">
                                    {originCountry && (
                                        <div className="flex items-center gap-2">
                                            <CountryFlag country={originCountry} />
                                            {originCountry}
                                        </div>
                                    )}
                                </SelectValue>
                            </SelectTrigger>
                            <SelectContent>
                                {countries.map((country) => (
                                    <SelectItem key={country} value={country}>
                                        <div className="flex items-center gap-2">
                                            <CountryFlag country={country} />
                                            {country}
                                        </div>
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    {/* Destination Country */}
                    <div className="space-y-2">
                        <Label htmlFor="destination-country">Destination Country</Label>
                        <Select value={destinationCountry} onValueChange={setDestinationCountry}>
                            <SelectTrigger className="w-full">
                                <SelectValue placeholder="Select destination">
                                    {destinationCountry && (
                                        <div className="flex items-center gap-2">
                                            <CountryFlag country={destinationCountry} />
                                            {destinationCountry}
                                        </div>
                                    )}
                                </SelectValue>
                            </SelectTrigger>
                            <SelectContent>
                                {countries.map((country) => (
                                    <SelectItem key={country} value={country}>
                                        <div className="flex items-center gap-2">
                                            <CountryFlag country={country} />
                                            {country}
                                        </div>
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                </div>

                {/* Product Selection */}
                <div className="space-y-2">
                    <Label>Product Selection</Label>
                    <Popover open={productOpen} onOpenChange={setProductOpen}>
                        <PopoverTrigger asChild>
                            <Button
                                variant="outline"
                                role="combobox"
                                aria-expanded={productOpen}
                                className="w-full justify-between"
                            >
                                {selectedProduct
                                    ? `${selectedProduct.name} (${selectedProduct.hsCode})`
                                    : "Search products by name or HS code..."}
                                <Search className="ml-2 h-4 w-4 shrink-0 opacity-50" />
                            </Button>
                        </PopoverTrigger>
                        <PopoverContent className="w-full p-0">
                            <Command>
                                <CommandInput
                                    placeholder="Search products..."
                                    value={productSearch}
                                    onValueChange={setProductSearch}
                                />
                                <CommandList>
                                    <CommandEmpty>No products found.</CommandEmpty>
                                    <CommandGroup>
                                        {filteredProducts.map((product) => (
                                            <CommandItem
                                                key={product.id}
                                                value={`${product.name} ${product.hsCode}`}
                                                onSelect={() => {
                                                    setSelectedProduct(product);
                                                    setProductOpen(false);
                                                    setProductSearch("");
                                                }}
                                            >
                                                <div className="flex flex-col">
                                                    <div className="font-medium">{product.name}</div>
                                                    <div className="text-sm text-muted-foreground">
                                                        HS: {product.hsCode} • {product.category} • Base Rate: {product.baseTariffRate}%
                                                    </div>
                                                </div>
                                            </CommandItem>
                                        ))}
                                    </CommandGroup>
                                </CommandList>
                            </Command>
                        </PopoverContent>
                    </Popover>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {/* Product Value */}
                    <div className="space-y-2">
                        <Label htmlFor="product-value">Product Value (USD)</Label>
                        <Input
                            id="product-value"
                            type="number"
                            placeholder="Enter product value"
                            value={productValue}
                            onChange={(e) => setProductValue(e.target.value)}
                        />
                    </div>

                    {/* Import Date */}
                    <div className="space-y-2">
                        <Label>Import Date</Label>
                        <Popover>
                            <PopoverTrigger asChild>
                                <Button
                                    variant="outline"
                                    className="w-full justify-start text-left font-normal"
                                >
                                    <CalendarIcon className="mr-2 h-4 w-4" />
                                    {importDate.toLocaleDateString()}
                                </Button>
                            </PopoverTrigger>
                            <PopoverContent className="w-auto p-0" align="start">
                                <Calendar
                                    mode="single"
                                    selected={importDate}
                                    onSelect={(date) => date && setImportDate(date)}
                                    initialFocus
                                />
                            </PopoverContent>
                        </Popover>
                    </div>
                </div>

                {/* Action Buttons */}
                <div className="flex gap-2">
                    <Button onClick={calculateTariff} className="flex-1">
                        Calculate Tariff
                    </Button>
                    <Button variant="outline" onClick={clearCalculation}>
                        Clear
                    </Button>
                </div>

                {/* Results Display */}
                {result && (
                    <div className="space-y-4 p-6 bg-muted rounded-lg">
                        <div className="flex items-center justify-between mb-4">
                            <div className="flex items-center gap-2">
                                <CheckCircle className="h-5 w-5 text-green-600" />
                                <h3 className="text-lg font-medium">Tariff Calculation Results</h3>
                            </div>
                            {canSaveProduct && selectedProduct && (
                                <SaveProductDialog
                                    product={selectedProduct}
                                    productValue={parseFloat(productValue)}
                                    originCountry={originCountry}
                                    destinationCountry={destinationCountry}
                                    importDate={importDate}
                                />
                            )}
                        </div>

                        {/* Product Information */}
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                            <div>
                                <h4 className="font-medium mb-2">Product Details</h4>
                                <div className="space-y-1 text-sm">
                                    <div><strong>Product:</strong> {result.product.name}</div>
                                    <div><strong>HS Code:</strong> {result.product.hsCode}</div>
                                    <div><strong>Category:</strong> {result.product.category}</div>
                                    <div><strong>Value:</strong> ${parseFloat(productValue).toLocaleString()}</div>
                                </div>
                            </div>
                            <div>
                                <h4 className="font-medium mb-2">Trade Information</h4>
                                <div className="space-y-1 text-sm">
                                    <div><strong>Origin:</strong> {result.originCountry}</div>
                                    <div><strong>Destination:</strong> {result.destinationCountry}</div>
                                    <div><strong>Import Date:</strong> {result.importDate.toLocaleDateString()}</div>
                                </div>
                            </div>
                        </div>

                        {/* Tariff Breakdown */}
                        <div className="space-y-3">
                            <h4 className="font-medium">Tariff Breakdown</h4>
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                <div className="p-3 bg-background rounded border">
                                    <div className="text-sm text-muted-foreground">Base Tariff Rate</div>
                                    <div className="text-xl font-medium">{result.baseTariffRate}%</div>
                                </div>
                                <div className="p-3 bg-background rounded border">
                                    <div className="text-sm text-muted-foreground">Trade Agreement Reduction</div>
                                    <div className="text-xl font-medium text-green-600">
                                        -{result.tradeAgreementReduction.toFixed(2)}%
                                    </div>
                                </div>
                                <div className="p-3 bg-background rounded border">
                                    <div className="text-sm text-muted-foreground">Final Tariff Rate</div>
                                    <div className="text-xl font-medium text-primary">
                                        {result.finalTariffRate.toFixed(2)}%
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Trade Agreement Information */}
                        {result.tradeAgreement && (
                            <div className="p-4 bg-green-50 dark:bg-green-950/20 rounded border border-green-200 dark:border-green-800">
                                <div className="flex items-center gap-2 mb-2">
                                    <Info className="h-4 w-4 text-green-600" />
                                    <h4 className="font-medium text-green-800 dark:text-green-200">
                                        Trade Agreement Applied
                                    </h4>
                                </div>
                                <div className="text-sm text-green-700 dark:text-green-300">
                                    <div><strong>{result.tradeAgreement.name}</strong></div>
                                    <div>Reduction: {result.tradeAgreement.reduction}%</div>
                                    {result.tradeAgreement.conditions && (
                                        <div>Conditions: {result.tradeAgreement.conditions}</div>
                                    )}
                                </div>
                            </div>
                        )}

                        {/* Final Results */}
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-4 border-t">
                            <div className="text-center p-4 bg-background rounded">
                                <div className="text-2xl font-bold text-primary">
                                    ${result.dutyAmount.toFixed(2)}
                                </div>
                                <div className="text-sm text-muted-foreground">Total Customs Duty</div>
                            </div>
                            <div className="text-center p-4 bg-background rounded">
                                <div className="text-2xl font-bold text-primary">
                                    ${result.totalCost.toFixed(2)}
                                </div>
                                <div className="text-sm text-muted-foreground">Total Cost (Product + Duty)</div>
                            </div>
                        </div>

                        {/* Savings Information */}
                        {result.tradeAgreement && result.tradeAgreementReduction > 0 && (
                            <div className="p-3 bg-blue-50 dark:bg-blue-950/20 rounded border border-blue-200 dark:border-blue-800">
                                <div className="text-sm text-blue-700 dark:text-blue-300">
                                    <strong>You saved ${((parseFloat(productValue) * result.tradeAgreementReduction) / 100).toFixed(2)}</strong> in customs duties thanks to the {result.tradeAgreement.name} trade agreement.
                                </div>
                            </div>
                        )}
                    </div>
                )}
            </CardContent>
        </Card>
    );
}