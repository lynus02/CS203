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
import tariffApi from "../services/tariffApi";

// API helpers
const { runAudit: runAuditApi, suggestProducts, getTariffRatesBySize, calculateTariff, getCountries } = tariffApi;

interface CountryDto {
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
    reduction: number;
    conditions?: string;
}

interface TariffResult {
    product: Product;
    productValue: number;
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
}

export function CustomsDutyCalculator({ onResultsChange }: CustomsDutyCalculatorProps) {
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
    const [auditRunning, setAuditRunning] = useState(false);
    const [auditResult, setAuditResult] = useState<{
        status: "ok" | "modified" | "error";
        message: string;
        localHash?: string;
        onChainHash?: string;
    } | null>(null);

    const MAX_SUGGESTION_SIZE = 20;

    const runAudit = async () => {
        setAuditRunning(true);
        setAuditResult(null);
        try {
            const data = await runAuditApi();
            if (data.integrityOk) {
                setAuditResult({
                    status: "ok",
                    message: data.message || "Database integrity verified",
                    localHash: data.localHash ?? undefined,
                    onChainHash: data.onChainHash ?? undefined,
                });
            } else if (data.error) {
                setAuditResult({
                    status: "error",
                    message: data.message || "Audit failed",
                });
            } else {
                setAuditResult({
                    status: "modified",
                    message: data.message || "Database hash mismatch",
                });
            }
        } catch (err: any) {
            setAuditResult({ status: "error", message: err?.message || "Audit failed" });
        } finally {
            setAuditRunning(false);
        }
    };

    // Load countries from backend
    useEffect(() => {
        getCountries()
            .then((data: CountryDto[]) => {
                setCountries(
                    [...data].sort((a, b) => a.name.localeCompare(b.name, "en", { sensitivity: "base" }))
                );
            })
            .catch((err) => console.error("Failed to fetch countries:", err));
    }, []);

    // Initial product suggestions
    useEffect(() => {
        if (!productOpen) return;
        setLoadingSuggestions(true);

        getTariffRatesBySize(MAX_SUGGESTION_SIZE, destinationCountry)
            .then((data) =>
                setSuggestedProducts(
                    data.map((item: any) => ({
                        id: item.trade_id?.toString(),
                        name: item.hsDescription,
                        hsCode: item.productCode6,
                        category: item.food_category,
                        baseTariffRate: item.value,
                        reporterName: item.reporterName,
                    }))
                )
            )
            .finally(() => setLoadingSuggestions(false));
    }, [productOpen, destinationCountry]);

    // Search products as user types
    useEffect(() => {
        if (productSearch.length === 0) return;
        setLoadingSuggestions(true);

        suggestProducts(productSearch, destinationCountry, 0, MAX_SUGGESTION_SIZE)
            .then((data) => {
                const results = data.content || data;
                setSuggestedProducts(
                    results.map((item: any) => ({
                        id: item.trade_id?.toString(),
                        name: item.hsDescription,
                        hsCode: item.productCode6,
                        category: item.food_category,
                        baseTariffRate: item.value,
                        reporterName: item.reporterName,
                    }))
                );
            })
            .finally(() => setLoadingSuggestions(false));
    }, [productSearch, destinationCountry]);

    // ========== BACKEND TARIFF CALCULATION ========== //
    const handleCalculate = async () => {
        if (!selectedProduct || !productValue || !originCountry || !destinationCountry) return;

        try {
            const originCode = countries.find((c) => c.name === originCountry)?.code || originCountry;
            const destCode = countries.find((c) => c.name === destinationCountry)?.code || destinationCountry;

            const data = await calculateTariff(
                selectedProduct.hsCode,
                originCode,
                destCode,
                parseFloat(productValue)
            );

            const value = parseFloat(productValue);
            const finalRate = (data.tariffAmount / value) * 100;

            const tariffResult: TariffResult = {
                product: selectedProduct,
                productValue: value,
                originCountry,
                destinationCountry,
                importDate,
                baseTariffRate: selectedProduct.baseTariffRate,
                tradeAgreementReduction: selectedProduct.baseTariffRate - finalRate,
                finalTariffRate: finalRate,
                dutyAmount: data.tariffAmount,
                totalCost: value + data.tariffAmount,
                tradeAgreement: data.agreementType
                    ? {
                        name: data.agreementType,
                        countries: [originCountry, destinationCountry],
                        reduction: selectedProduct.baseTariffRate - finalRate,
                    }
                    : undefined,
            };

            setResult(tariffResult);
            onResultsChange?.(tariffResult);
        } catch (error) {
            console.error("Tariff calculation failed:", error);
        }
    };

    // Reset UI and states
    const clearState = () => {
        setProductValue("");
        setSelectedProduct(null);
        setProductSearch("");
        setOriginCountry("");
        setDestinationCountry("Singapore");
        setImportDate(new Date());
        setResult(null);
        onResultsChange?.(null);
    };

    return (
        <Card>
            <CardHeader>
                <div className="flex justify-between w-full items-start">
                    <div>
                        <CardTitle>Food Tariff Calculator</CardTitle>
                        <CardDescription>
                            Calculate import duties with trade agreements + food HS code lookup
                        </CardDescription>
                    </div>

                    <Button
                        onClick={runAudit}
                        disabled={auditRunning}
                        variant="outline"
                        className="text-primary border-primary hover:bg-primary/10"
                    >
                        {auditRunning ? "Running audit…" : "Run DB Audit"}
                    </Button>
                </div>

                {/* ✅ Audit status banner */}
                {auditResult && (
                    <div className="mt-3">
                        {auditResult.status === "ok" && (
                            <div className="flex gap-2 p-2 bg-green-600/20 border border-green-600 text-green-300 rounded">
                                <CheckCircle className="h-4 w-4" />
                                <span>{auditResult.message}</span>
                            </div>
                        )}

                        {auditResult.status === "modified" && (
                            <div className="flex gap-2 p-2 bg-yellow-600/20 border border-yellow-600 text-yellow-300 rounded">
                                <AlertTriangle className="h-4 w-4" />
                                <span>{auditResult.message}</span>
                            </div>
                        )}

                        {auditResult.status === "error" && (
                            <div className="flex gap-2 p-2 bg-red-600/20 border border-red-600 text-red-300 rounded">
                                <AlertTriangle className="h-4 w-4" />
                                <span>{auditResult.message}</span>
                            </div>
                        )}
                    </div>
                )}
            </CardHeader>

            <CardContent className="space-y-6">
                {/* Country selection */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-2">
                        <Label>Country of Origin</Label>
                        <Select value={originCountry} onValueChange={setOriginCountry}>
                            <SelectTrigger>
                                <SelectValue placeholder="Select origin" />
                            </SelectTrigger>
                            <SelectContent>
                                {countries.map((c) => (
                                    <SelectItem key={c.code} value={c.name}>
                                        <div className="flex items-center gap-2">
                                            <CountryFlag country={c.name} /> {c.name}
                                        </div>
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    <div className="space-y-2">
                        <Label>Destination Country</Label>
                        <Select value={destinationCountry} onValueChange={setDestinationCountry}>
                            <SelectTrigger>
                                <SelectValue placeholder="Select destination" />
                            </SelectTrigger>
                            <SelectContent>
                                {countries.map((c) => (
                                    <SelectItem key={c.code} value={c.name}>
                                        <div className="flex items-center gap-2">
                                            <CountryFlag country={c.name} /> {c.name}
                                        </div>
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                </div>

                {/* Product Search */}
                <div className="space-y-2">
                    <Label>Product Selection</Label>

                    <Popover open={productOpen} onOpenChange={setProductOpen}>
                        <PopoverTrigger asChild>
                            <Button variant="outline" role="combobox" className="w-full justify-between text-foreground">
                                {selectedProduct ? `${selectedProduct.name} (${selectedProduct.hsCode})` : "Search products..."}
                                <Search className="ml-2 h-4 w-4 text-primary opacity-70" />
                            </Button>
                        </PopoverTrigger>

                        <PopoverContent className="w-full p-0">
                            <Command>
                                <CommandInput placeholder="Search products..." value={productSearch} onValueChange={setProductSearch} />
                                <CommandList>
                                    <CommandEmpty>
                                        {loadingSuggestions ? "Loading..." : "No products found."}
                                    </CommandEmpty>
                                    <CommandGroup>
                                        {suggestedProducts.map((p) => (
                                            <CommandItem
                                                key={p.id}
                                                onSelect={() => {
                                                    setSelectedProduct(p);
                                                    if (p.reporterName) setDestinationCountry(p.reporterName);
                                                    setProductOpen(false);
                                                    setProductSearch("");
                                                }}
                                            >
                                                <div>
                                                    <div className="font-medium">{p.name}</div>
                                                    <div className="text-xs text-muted-foreground">
                                                        HS: {p.hsCode} • {p.category} • Rate: {p.baseTariffRate}%
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

                {/* Product value + date */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="space-y-2">
                        <Label>Product Value (USD)</Label>
                        <Input type="number" value={productValue} onChange={(e) => setProductValue(e.target.value)} />
                    </div>

                    <div className="space-y-2">
                        <Label>Import Date</Label>
                        <Popover>
                            <PopoverTrigger asChild>
                                <Button variant="outline" className="w-full justify-start text-foreground">
                                    <CalendarIcon className="mr-2 h-4 w-4 text-primary" />
                                    {importDate.toLocaleDateString()}
                                </Button>
                            </PopoverTrigger>
                            <PopoverContent className="p-0">
                                <Calendar mode="single" selected={importDate} onSelect={(d) => d && setImportDate(d)} />
                            </PopoverContent>
                        </Popover>
                    </div>
                </div>

                {/* Action Buttons */}
                <div className="flex gap-2">
                    <Button onClick={handleCalculate} className="flex-1 text-foreground">
                        Calculate Tariff
                    </Button>
                    <Button variant="outline" className="text-primary border-primary hover:bg-primary/10" onClick={clearState}>
                        Clear
                    </Button>
                </div>

                {/* Result Display */}
                {result && (
                    <div className="space-y-4 p-6 bg-card border rounded-lg">
                        <div className="flex items-center gap-2">
                            <CheckCircle className="h-5 w-5 text-green-600" />
                            <h3 className="text-lg font-medium">Tariff Calculation Results</h3>
                        </div>

                        {/* Product info */}
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                                <h4 className="font-medium mb-2">Product Details</h4>
                                <div className="text-sm space-y-1">
                                    <div><strong>Product:</strong> {result.product.name}</div>
                                    <div><strong>HS Code:</strong> {result.product.hsCode}</div>
                                    <div><strong>Category:</strong> {result.product.category}</div>
                                    <div><strong>Value:</strong> ${result.productValue.toLocaleString()}</div>
                                </div>
                            </div>
                            <div>
                                <h4 className="font-medium mb-2">Trade Information</h4>
                                <div className="text-sm space-y-1">
                                    <div><strong>Origin:</strong> {result.originCountry}</div>
                                    <div><strong>Destination:</strong> {result.destinationCountry}</div>
                                    <div><strong>Date:</strong> {result.importDate.toLocaleDateString()}</div>
                                </div>
                            </div>
                        </div>

                        {/* Tariff breakdown */}
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

                        {result.tradeAgreement && (
                            <div className="p-4 bg-green-50 rounded border border-green-200">
                                <div className="flex items-center gap-2 mb-2">
                                    <Info className="h-4 w-4 text-green-600" />
                                    <h4 className="font-medium text-green-700">Trade Agreement Applied</h4>
                                </div>
                                <div className="text-sm">
                                    <strong>{result.tradeAgreement.name}</strong>
                                    <br />
                                    Reduction: {result.tradeAgreement.reduction}%
                                </div>
                            </div>
                        )}

                        {/* Totals */}
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-4 border-t">
                            <div className="text-center p-4 bg-background rounded">
                                <div className="text-2xl font-bold text-primary">${result.dutyAmount.toFixed(2)}</div>
                                <div className="text-sm text-muted-foreground">Total Customs Duty</div>
                            </div>

                            <div className="text-center p-4 bg-background rounded">
                                <div className="text-2xl font-bold text-primary">${result.totalCost.toFixed(2)}</div>
                                <div className="text-sm text-muted-foreground">Total Cost (Product + Duty)</div>
                            </div>
                        </div>
                    </div>
                )}
            </CardContent>
        </Card>
    );
}