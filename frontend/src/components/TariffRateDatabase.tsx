import { useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { Button } from "./ui/button";
import { Badge } from "./ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "./ui/table";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select";
import { Search, ExternalLink, Filter, TrendingUp, AlertCircle } from "lucide-react";
import { CountryFlag } from "./ui/country-flags";

interface TariffRate {
  hsCode: string;
  description: string;
  country: string;
  rate: number;
  lastUpdated: string;
  category: string;
  tradeAgreements?: string[];
}

export function TariffRateDatabase() {
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedCountry, setSelectedCountry] = useState("all");
  const [selectedCategory, setSelectedCategory] = useState("all");

  // Enhanced food tariff data with categories and trade agreements
  const tariffRates: TariffRate[] = [
    {
      hsCode: "0201.10.50",
      description: "Fresh beef carcasses and half-carcasses",
      country: "United States",
      rate: 26.4,
      type: "Ad Valorem",
      lastUpdated: "2024-01-15",
      category: "Meat & Poultry",
      tradeAgreements: ["USMCA"]
    },
    {
      hsCode: "0201.10.50",
      description: "Fresh beef carcasses and half-carcasses",
      country: "Canada",
      rate: 0,
      type: "Ad Valorem",
      lastUpdated: "2024-01-15",
      category: "Meat & Poultry",
      tradeAgreements: ["USMCA"]
    },
    {
      hsCode: "0203.12.90",
      description: "Fresh pork hams, shoulders and cuts",
      country: "United States",
      rate: 1.4,
      type: "Ad Valorem",
      lastUpdated: "2024-01-15",
      category: "Meat & Poultry"
    },
    {
      hsCode: "0207.14.10",
      description: "Fresh or chilled chicken cuts",
      country: "United States",
      rate: 17.6,
      type: "Specific",
      lastUpdated: "2024-01-15",
      category: "Meat & Poultry"
    },
    {
      hsCode: "0402.10.05",
      description: "Milk powder, whole, not sweetened",
      country: "United States",
      rate: 13.8,
      type: "Ad Valorem",
      lastUpdated: "2024-01-15",
      category: "Dairy Products"
    },
    {
      hsCode: "0406.90.54",
      description: "Aged cheddar cheese",
      country: "United States",
      rate: 17.5,
      type: "Ad Valorem",
      lastUpdated: "2024-01-15",
      category: "Dairy Products"
    },
    {
      hsCode: "0302.12.00",
      description: "Fresh Atlantic salmon",
      country: "United States",
      rate: 0,
      type: "Ad Valorem",
      lastUpdated: "2024-01-15",
      category: "Seafood"
    },
    {
      hsCode: "0306.17.00",
      description: "Frozen shrimp and prawns",
      country: "United States",
      rate: 0,
      type: "Ad Valorem",
      lastUpdated: "2024-01-15",
      category: "Seafood"
    },
    {
      hsCode: "0803.90.30",
      description: "Fresh bananas",
      country: "United States",
      rate: 0,
      type: "Ad Valorem",
      lastUpdated: "2024-01-15",
      category: "Fruits & Vegetables"
    },
    {
      hsCode: "0804.40.00",
      description: "Fresh avocados",
      country: "United States",
      rate: 11.2,
      type: "Specific",
      lastUpdated: "2024-01-15",
      category: "Fruits & Vegetables",
      tradeAgreements: ["USMCA"]
    },
    {
      hsCode: "0901.21.00",
      description: "Arabica coffee beans, not roasted",
      country: "United States",
      rate: 0,
      type: "Ad Valorem",
      lastUpdated: "2024-01-15",
      category: "Coffee & Tea"
    },
    {
      hsCode: "0901.21.00",
      description: "Arabica coffee beans, not roasted",
      country: "European Union",
      rate: 7.5,
      type: "Ad Valorem",
      lastUpdated: "2024-01-15",
      category: "Coffee & Tea"
    },
    {
      hsCode: "0902.30.00",
      description: "Black tea in packages exceeding 3kg",
      country: "United States",
      rate: 6.4,
      type: "Ad Valorem",
      lastUpdated: "2024-01-15",
      category: "Coffee & Tea"
    },
    {
      hsCode: "1509.10.20",
      description: "Extra virgin olive oil",
      country: "United States",
      rate: 5,
      type: "Specific",
      lastUpdated: "2024-01-15",
      category: "Oils & Fats"
    },
    {
      hsCode: "1006.30.90",
      description: "Semi-milled or wholly milled rice",
      country: "United States",
      rate: 2.1,
      type: "Specific",
      lastUpdated: "2024-01-15",
      category: "Grains & Legumes"
    },
    {
      hsCode: "1008.50.90",
      description: "Quinoa seeds",
      country: "United States",
      rate: 0.6,
      type: "Ad Valorem",
      lastUpdated: "2024-01-15",
      category: "Grains & Legumes"
    },
    {
      hsCode: "1806.32.70",
      description: "Chocolate confectionery, filled",
      country: "United States",
      rate: 5.1,
      type: "Ad Valorem",
      lastUpdated: "2024-01-15",
      category: "Confectionery"
    },
    {
      hsCode: "1701.14.20",
      description: "Raw cane sugar",
      country: "United States",
      rate: 1.4,
      type: "Specific",
      lastUpdated: "2024-01-15",
      category: "Sugar & Sweeteners"
    },
    {
      hsCode: "2009.11.00",
      description: "Frozen orange juice concentrate",
      country: "United States",
      rate: 7.9,
      type: "Specific",
      lastUpdated: "2024-01-15",
      category: "Beverages"
    },
    {
      hsCode: "2204.21.30",
      description: "Wine from grapes, in containers 2L or less",
      country: "United States",
      rate: 6.3,
      type: "Specific",
      lastUpdated: "2024-01-15",
      category: "Beverages"
    },
    {
      hsCode: "1604.14.30",
      description: "Prepared or preserved tuna",
      country: "United States",
      rate: 12.5,
      type: "Ad Valorem",
      lastUpdated: "2024-01-15",
      category: "Preserved Foods"
    },
    {
      hsCode: "0804.10.80",
      description: "Dried dates",
      country: "United States",
      rate: 2.9,
      type: "Specific",
      lastUpdated: "2024-01-15",
      category: "Dried Fruits & Nuts"
    }
  ];

  const filteredRates = tariffRates.filter(rate => {
    const matchesSearch = searchTerm === "" || 
      rate.hsCode.toLowerCase().includes(searchTerm.toLowerCase()) ||
      rate.description.toLowerCase().includes(searchTerm.toLowerCase());
    
    const matchesCountry = selectedCountry === "" || selectedCountry === "all" || 
      rate.country === selectedCountry;
    
    const matchesCategory = selectedCategory === "" || selectedCategory === "all" ||
      rate.category === selectedCategory;
    
    return matchesSearch && matchesCountry && matchesCategory;
  });

  const countries = [...new Set(tariffRates.map(rate => rate.country))].sort();
  const categories = [...new Set(tariffRates.map(rate => rate.category))].sort();

  return (
    <Card>
      <CardHeader>
        <CardTitle>Food Tariff Rate Database</CardTitle>
        <CardDescription>
          Search for current food tariff rates by HS code, product description, or country
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* Search and Filter Controls */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="space-y-2">
            <Label htmlFor="search-term">Search HS Code or Product</Label>
            <div className="relative">
              <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
              <Input
                id="search-term"
                placeholder="Enter HS code or product description"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="country-filter">Filter by Destination Country</Label>
            <Select value={selectedCountry} onValueChange={setSelectedCountry}>
              <SelectTrigger>
                <SelectValue placeholder="All countries">
                  {selectedCountry && selectedCountry !== "all" && (
                    <div className="flex items-center gap-2">
                      <CountryFlag country={selectedCountry} />
                      {selectedCountry}
                    </div>
                  )}
                </SelectValue>
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">🌍 All countries</SelectItem>
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

          <div className="space-y-2">
            <Label htmlFor="category-filter">Filter by Food Category</Label>
            <Select value={selectedCategory} onValueChange={setSelectedCategory}>
              <SelectTrigger>
                <SelectValue placeholder="All food categories" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All food categories</SelectItem>
                {categories.map((category) => (
                  <SelectItem key={category} value={category}>
                    {category}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        {/* Results Summary */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Filter className="h-4 w-4 text-muted-foreground" />
            <span className="text-sm text-muted-foreground">
              Showing {filteredRates.length} of {tariffRates.length} food tariff rates
            </span>
          </div>
            <Button variant="secondary" size="sm">
                <ExternalLink className="h-4 w-4 mr-2" />
                Export Results
            </Button>
        </div>

        {/* Results Table */}
        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>HS Code</TableHead>
                <TableHead>Food Product Description</TableHead>
                <TableHead>Food Category</TableHead>
                <TableHead>Destination Country</TableHead>
                <TableHead>MFN Rate</TableHead>
                <TableHead>Trade Agreements</TableHead>
                <TableHead>Updated</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredRates.map((rate, index) => (
                <TableRow key={index}>
                  <TableCell className="font-mono text-sm font-medium">{rate.hsCode}</TableCell>
                  <TableCell className="max-w-xs">{rate.description}</TableCell>
                  <TableCell>
                    <Badge variant="outline" className="text-xs">
                      {rate.category}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      <CountryFlag country={rate.country} />
                      {rate.country}
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge variant={rate.rate === 0 ? "secondary" : "default"}>
                      {rate.type === "Specific" ? `${rate.rate}` : `${rate.rate}%`}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    {rate.tradeAgreements && rate.tradeAgreements.length > 0 ? (
                      <div className="flex flex-wrap gap-1">
                        {rate.tradeAgreements.map((agreement, idx) => (
                          <Badge key={idx} variant="secondary" className="text-xs">
                            {agreement}
                          </Badge>
                        ))}
                      </div>
                    ) : (
                      <span className="text-sm text-muted-foreground">None</span>
                    )}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {new Date(rate.lastUpdated).toLocaleDateString()}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>

        {filteredRates.length === 0 && (
          <div className="text-center py-8">
            <AlertCircle className="h-8 w-8 mx-auto mb-2 text-muted-foreground" />
            <p className="text-sm text-muted-foreground">
              No food tariff rates found matching your search criteria.
            </p>
          </div>
        )}

        {/* Quick Stats */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-4 border-t">
          <Card>
            <CardContent className="pt-4">
              <div className="flex items-center gap-2">
                <TrendingUp className="h-4 w-4 text-green-600" />
                <div>
                  <p className="text-sm text-muted-foreground">Duty-Free Food Items</p>
                  <p className="text-lg font-medium">
                    {tariffRates.filter(rate => rate.rate === 0).length}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-4">
              <div className="flex items-center gap-2">
                <AlertCircle className="h-4 w-4 text-orange-600" />
                <div>
                  <p className="text-sm text-muted-foreground">Food Categories</p>
                  <p className="text-lg font-medium">{categories.length}</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-4">
              <div className="flex items-center gap-2">
                <ExternalLink className="h-4 w-4 text-blue-600" />
                <div>
                  <p className="text-sm text-muted-foreground">With Trade Agreements</p>
                  <p className="text-lg font-medium">
                    {tariffRates.filter(rate => rate.tradeAgreements && rate.tradeAgreements.length > 0).length}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </CardContent>
    </Card>
  );
}