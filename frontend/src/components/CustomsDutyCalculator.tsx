import { useState, useEffect } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select";
import { Button } from "./ui/button";
import { Calendar } from "./ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "./ui/popover";
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from "./ui/command";
import { CalendarIcon, Search, CheckCircle, AlertTriangle, Info } from "lucide-react";
import { CountryFlag } from "./ui/country-flags";
import { suggestProducts, getTariffRatesBySize } from "../services/tariff";
import api from "../services/api";
import { getCountries } from "../services/tariff";

interface CountryDto{
    code: string;
    name: string;
}

interface Product {
    id: string;
    name: string;
    hsCode: string;
    category: string;
    baseTariffRate: number;
    reporterName?: string;
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

export function CustomsDutyCalculator({ onResultsChange }: { onResultsChange?: (data: any) => void }) {
    const [productValue, setProductValue] = useState("");
    const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
    const [productSearch, setProductSearch] = useState("");
    const [originCountry, setOriginCountry] = useState("");
    const [destinationCountry, setDestinationCountry] = useState("Singapore");
    const [importDate, setImportDate] = useState<Date>(new Date());
    const [result, setResult] = useState<TariffResult | null>(null);
    const [productOpen, setProductOpen] = useState(false);
    const [suggestedProducts, setSuggestedProducts] = useState<Product[]>([]);
    const [loadingSuggestions, setLoadingSuggestions] = useState(false);
    const [countries, setCountries] = useState<CountryDto[]>([]);

    // const staticCountries = [
    //     { name: 'Albania', code: 'C008' },
    //     { name: 'Argentina', code: 'C032' },
    //     { name: 'Belize', code: 'C084' },
    //     { name: 'Botswana', code: 'C072' },
    //     { name: 'Brazil', code: 'C076' },
    //     { name: 'Cambodia', code: 'C116' },
    //     { name: 'Canada', code: 'C124' },
    //     { name: 'Chile', code: 'C152' },
    //     { name: 'China', code: 'C156' },
    //     { name: 'Chinese Taipei', code: 'C158' },
    //     { name: 'Colombia', code: 'C170' },
    //     { name: 'Dominican Republic', code: 'C214' },
    //     { name: 'Ecuador', code: 'C218' },
    //     { name: 'El Salvador', code: 'C222' },
    //     { name: 'Eswatini', code: 'C748' },
    //     { name: 'Honduras', code: 'C340' },
    //     { name: 'Hong Kong, China', code: 'C344' },
    //     { name: 'Jordan', code: 'C400' },
    //     { name: 'Korea, Republic of', code: 'C410' },
    //     { name: 'Kuwait, the State of', code: 'C414' },
    //     { name: 'Lesotho', code: 'C426' },
    //     { name: 'Macao, China', code: 'C446' },
    //     { name: 'Malaysia', code: 'C458' },
    //     { name: 'Mauritius', code: 'C480' },
    //     { name: 'Mexico', code: 'C484' },
    //     { name: 'Montenegro', code: 'C893' },
    //     { name: 'Myanmar', code: 'C104' },
    //     { name: 'Namibia', code: 'C516' },
    //     { name: 'New Zealand', code: 'C554' },
    //     { name: 'North Macedonia', code: 'C807' },
    //     { name: 'Saudi Arabia, Kingdom of', code: 'C682' },
    //     { name: 'Seychelles', code: 'C690' },
    //     { name: 'Singapore', code: 'C702' },
    //     { name: 'South Africa', code: 'C710' },
    //     { name: 'Switzerland', code: 'C756' },
    //     { name: 'Ukraine', code: 'C804' },
    //     { name: 'United Kingdom', code: 'C826' },
    //     { name: 'United States of America', code: 'C840' }
    // ];



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

    const MAX_SUGGESTION_SIZE = 20;

    // Fetch countries from backend
    useEffect(() => {
        getCountries()
            .then((data: CountryDto[]) => setCountries(data))
            .catch(err => {
                console.error("Failed to fetch countries", err);
            });
    }, []);

    useEffect(() => {
        if (productOpen) {
            setLoadingSuggestions(true);
            getTariffRatesBySize(MAX_SUGGESTION_SIZE, destinationCountry)
                .then(data => {
                    setSuggestedProducts(data.map((rate: any) => ({
                        id: rate.trade_id?.toString(),
                        name: rate.hsDescription,
                        hsCode: rate.productCode6,
                        category: rate.food_category,
                        baseTariffRate: rate.value,
                        reporterName: rate.reporterName
                    })));
                })
                .finally(() => setLoadingSuggestions(false));
        }
    }, [productOpen, destinationCountry]);

    useEffect(() => {
        if (productSearch.length > 0) {
            setLoadingSuggestions(true);
            suggestProducts(productSearch, destinationCountry, 0, 20)
                .then(data => {
                    const rates = data.content || data; // handle both array and paged response
                    setSuggestedProducts(rates.map((rate: any) => ({
                        id: rate.trade_id?.toString(),
                        name: rate.hsDescription,
                        hsCode: rate.productCode6,
                        category: rate.food_category,
                        baseTariffRate: rate.value,
                        reporterName: rate.reporterName
                    })));
                })
                .finally(() => setLoadingSuggestions(false));
        }
    }, [productSearch, destinationCountry]);

    const findApplicableTradeAgreement = (origin: string, destination: string): TradeAgreement | undefined => {
        return tradeAgreements.find(agreement =>
            (agreement.countries.includes(origin) && agreement.countries.includes(destination)) ||
            (agreement.countries.includes(destination) && agreement.countries.includes(origin))
        );
    };

    const calculateTariff = async () => {
        // console.log("selectedProduct:", selectedProduct);
        // console.log("productValue:", productValue);
        // console.log("originCountry:", originCountry);
        // console.log("destinationCountry:", destinationCountry);
        if (!selectedProduct || !productValue || !originCountry || !destinationCountry) {
            return;
        }

        try {
            // call backend to calculate tariff
            const response = await api.post('/tariffs/calculate',{
                productCode: selectedProduct.hsCode,
                exportCountryCode: originCountry,
                desCountryCode: destinationCountry,
                customsValue: parseFloat(productValue)
            });

            const data = response.data;

            const value = parseFloat(productValue);
            const baseTariffRate = selectedProduct.baseTariffRate;
            const finalTariffRate = (data.tariffAmount / value) * 100;
            const tradeAgreementReduction = baseTariffRate - finalTariffRate;

            // needed for frountend UI
            const tariffResult: TariffResult = {
                product: selectedProduct,
                productValue:value,
                originCountry,
                destinationCountry,
                importDate,
                baseTariffRate,
                tradeAgreementReduction,
                finalTariffRate,
                dutyAmount: data.tariffAmount,
                totalCost: value + data.tariffAmount,
                tradeAgreement: data.agreementType
                    ? {
                        name: data.agreementType,
                        countries: [originCountry, destinationCountry],
                        reduction: tradeAgreementReduction
                    }
                    : undefined
            };

            setResult(tariffResult); // set result for ui display
            onResultsChange?.(tariffResult);
        } catch (err) {
            console.error("Tariff calculation failed:", err);
            if (err.response) {
                console.error("Response data:", err.response.data);
                console.error("Status:", err.response.status);
            } else if (err.request) {
                console.error("No response received:", err.request);
            } else {
                console.error("Error message:", err.message);
            }
            alert("Failed to calculate tariff. Check console for details.");
        }
    };

    const clearCalculation = () => {
        setProductValue("");
        setSelectedProduct(null);
        setProductSearch("");
        setOriginCountry("");
        setDestinationCountry("");
        setImportDate(new Date());
        setResult(null);
    };

    return (
        <Card>
            <CardHeader>
                <CardTitle>Food Tariff Calculator</CardTitle>
                <CardDescription>
                    Calculate food import duties with trade agreement adjustments and comprehensive food product selection
                </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {/* Country of Origin */}
                    <div className="space-y-2">
                        <Label htmlFor="origin-country">Country of Origin</Label>
                        <Select
                            value={originCountry}
                            onValueChange={setOriginCountry}
                            name="origin-country"
                        >
                            <SelectTrigger>
                                <SelectValue placeholder="Select origin country">
                                    {originCountry ? (
                                        <div className="flex items-center gap-2">
                                            <CountryFlag country={originCountry} />
                                            {countries.find((c) => c.code === originCountry)?.name || originCountry}
                                        </div>
                                    ) : (
                                        "Select origin country"
                                    )}
                                </SelectValue>
                            </SelectTrigger>
                            <SelectContent>
                                {countries.map((country) => (
                                    <SelectItem key={country.code} value={country.code}>
                                        <div className="flex items-center gap-2">
                                            <CountryFlag country={country.code} />
                                            {country.name}
                                        </div>
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    {/* Destination Country */}
                    <div className="space-y-2">
                        <Label htmlFor="destination-country">Destination Country</Label>
                        <Select
                            value={destinationCountry}
                            onValueChange={setDestinationCountry}
                            name="destination-country"
                        >
                            <SelectTrigger>
                                <SelectValue placeholder="Select destination country">
                                    {destinationCountry ? (
                                        <div className="flex items-center gap-2">
                                            <CountryFlag country={destinationCountry} />
                                            {countries.find((c) => c.code === destinationCountry)?.name || destinationCountry}
                                        </div>
                                    ) : (
                                        "Select destination country"
                                    )}
                                </SelectValue>
                            </SelectTrigger>
                            <SelectContent>
                                {countries.map((country) => (
                                    <SelectItem key={country.code} value={country.code}>
                                        <div className="flex items-center gap-2">
                                            <CountryFlag country={country.code} />
                                            {country.name}
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
                                className="w-full flex justify-between items-center"
                                style={{ minWidth: 0 }}
                            >
                <span className="truncate block" style={{ maxWidth: "70%" }}>
                  {selectedProduct
                      ? `${selectedProduct.name} (${selectedProduct.hsCode})`
                      : "Search products by name or HS code..."}
                </span>
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
                                    <CommandEmpty>
                                        {loadingSuggestions ? (
                                            <div className="py-4 text-center text-muted-foreground">
                                                Loading products...
                                            </div>
                                        ) : (
                                            "No products found."
                                        )}
                                    </CommandEmpty>
                                    <CommandGroup>
                                        {suggestedProducts.map((product) => (
                                            <CommandItem
                                                key={product.id}
                                                value={`${product.name} ${product.hsCode}`}
                                                onSelect={() => {
                                                    setSelectedProduct(product);
                                                    if (product.reporterName) {
                                                        setDestinationCountry(product.reporterName);
                                                    }
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
                        <div className="flex items-center gap-2 mb-4">
                            <CheckCircle className="h-5 w-5 text-green-600" />
                            <h3 className="text-lg font-medium">Tariff Calculation Results</h3>
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