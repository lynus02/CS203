import { useState, useEffect } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select";
import { Button } from "./ui/button";
import { Badge } from "./ui/badge";
import { Textarea } from "./ui/textarea";
import { Search, Package, Info, CheckCircle } from "lucide-react";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "./ui/dialog";

export interface Product {
  id: string;
  name: string;
  hsCode: string;
  category: string;
  description: string;
  avgDutyRate: number;
  origin: string[];
  specifications: {
    weight?: string;
    dimensions?: string;
    material?: string;
    brand?: string;
    model?: string;
  };
  image?: string;
}

interface ProductSelectionResult {
  product: Product;
  quantity: number;
  unitValue: number;
  totalValue: number;
  specifications: Record<string, string>;
}

export function ProductSelector({ onProductSelected }: { onProductSelected?: (result: ProductSelectionResult) => void }) {
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("");
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [quantity, setQuantity] = useState("");
  const [unitValue, setUnitValue] = useState("");
  const [customSpecs, setCustomSpecs] = useState<Record<string, string>>({});
  const [selectedProductDialog, setSelectedProductDialog] = useState<Product | null>(null);

  // Sample product database
  const products: Product[] = [
    {
      id: "1",
      name: "Apple iPhone 15 Pro",
      hsCode: "8517.12.00",
      category: "Electronics",
      description: "Smartphone with advanced camera system and titanium design",
      avgDutyRate: 0,
      origin: ["China", "India"],
      specifications: {
        weight: "187g",
        dimensions: "146.6 × 70.6 × 8.25 mm",
        material: "Titanium, Glass",
        brand: "Apple"
      }
    },
    {
      id: "2",
      name: "Samsung 65\" QLED TV",
      hsCode: "8528.72.64",
      category: "Electronics",
      description: "4K QLED Television with smart features",
      avgDutyRate: 5.3,
      origin: ["South Korea", "Mexico"],
      specifications: {
        weight: "25.2kg",
        dimensions: "1440 × 823 × 59 mm",
        material: "Plastic, Metal, Glass"
      }
    },
    {
      id: "3",
      name: "Men's Cotton Dress Shirt",
      hsCode: "6205.20.20",
      category: "Textiles",
      description: "100% cotton long-sleeve dress shirt",
      avgDutyRate: 19.7,
      origin: ["Bangladesh", "Vietnam", "China"],
      specifications: {
        material: "100% Cotton",
        weight: "200g"
      }
    },
    {
      id: "4",
      name: "Toyota Camry Brake Pads",
      hsCode: "8708.30.50",
      category: "Automotive",
      description: "Front disc brake pads for Toyota Camry 2018-2024",
      avgDutyRate: 2.5,
      origin: ["Japan", "Thailand"],
      specifications: {
        material: "Ceramic composite",
        weight: "2.1kg"
      }
    },
    {
      id: "5",
      name: "Arabica Coffee Beans",
      hsCode: "0901.21.00",
      category: "Agricultural",
      description: "Premium roasted arabica coffee beans",
      avgDutyRate: 0,
      origin: ["Colombia", "Brazil", "Ethiopia"],
      specifications: {
        weight: "1kg per bag",
        material: "100% Arabica beans"
      }
    },
    {
      id: "6",
      name: "Wooden Dining Table",
      hsCode: "9403.60.80",
      category: "Furniture",
      description: "Solid oak dining table, seats 6 people",
      avgDutyRate: 0,
      origin: ["Vietnam", "Malaysia"],
      specifications: {
        weight: "45kg",
        dimensions: "180 × 90 × 75 cm",
        material: "Solid Oak Wood"
      }
    }
  ];

  const categories = Array.from(new Set(products.map(p => p.category)));

  const filteredProducts = products.filter(product => {
    const matchesSearch = searchTerm === "" || 
      product.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      product.hsCode.includes(searchTerm) ||
      product.description.toLowerCase().includes(searchTerm.toLowerCase());
    
    const matchesCategory = selectedCategory === "" || product.category === selectedCategory;
    
    return matchesSearch && matchesCategory;
  });

  const handleProductSelect = (product: Product) => {
    setSelectedProduct(product);
    setCustomSpecs({});
  };

  const handleConfirmSelection = () => {
    if (!selectedProduct) return;
    
    const qty = parseFloat(quantity) || 1;
    const value = parseFloat(unitValue) || 0;
    
    const result: ProductSelectionResult = {
      product: selectedProduct,
      quantity: qty,
      unitValue: value,
      totalValue: qty * value,
      specifications: { ...selectedProduct.specifications, ...customSpecs }
    };
    
    onProductSelected?.(result);
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Package className="h-5 w-5" />
          Product Selection
        </CardTitle>
        <CardDescription>
          Select your product and specify details for accurate tariff calculation
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* Search and Filter */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="product-search">Search Products</Label>
            <div className="relative">
              <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
              <Input
                id="product-search"
                placeholder="Search by name, HS code, or description"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>
          </div>
          
          <div className="space-y-2">
            <Label htmlFor="category-filter">Filter by Category</Label>
            <Select value={selectedCategory} onValueChange={setSelectedCategory}>
              <SelectTrigger>
                <SelectValue placeholder="All categories" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">All categories</SelectItem>
                {categories.map((category) => (
                  <SelectItem key={category} value={category}>
                    {category}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        {/* Product List */}
        <div className="space-y-3 max-h-80 overflow-y-auto">
          {filteredProducts.map((product) => (
            <div
              key={product.id}
              className={`p-4 border rounded-lg cursor-pointer transition-colors ${
                selectedProduct?.id === product.id 
                  ? "border-primary bg-primary/5" 
                  : "border-border hover:border-primary/50"
              }`}
              onClick={() => handleProductSelect(product)}
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-2">
                    <h4 className="font-medium">{product.name}</h4>
                    {selectedProduct?.id === product.id && (
                      <CheckCircle className="h-4 w-4 text-primary" />
                    )}
                  </div>
                  <p className="text-sm text-muted-foreground mb-2">{product.description}</p>
                  <div className="flex items-center gap-2 mb-2">
                    <Badge variant="secondary" className="text-xs">
                      HS: {product.hsCode}
                    </Badge>
                    <Badge variant="secondary" className="text-xs">
                      {product.category}
                    </Badge>
                    <Badge variant={product.avgDutyRate === 0 ? "secondary" : "default"} className="text-xs">
                      Avg. Duty: {product.avgDutyRate}%
                    </Badge>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    Common origins: {product.origin.join(", ")}
                  </p>
                </div>
                <Dialog>
                  <DialogTrigger asChild>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={(e) => {
                        e.stopPropagation();
                        setSelectedProductDialog(product);
                      }}
                    >
                      <Info className="h-4 w-4" />
                    </Button>
                  </DialogTrigger>
                  <DialogContent>
                    <DialogHeader>
                      <DialogTitle>{product.name}</DialogTitle>
                      <DialogDescription>Product Details</DialogDescription>
                    </DialogHeader>
                    <div className="space-y-4">
                      <div>
                        <h4 className="font-medium mb-2">Description</h4>
                        <p className="text-sm text-muted-foreground">{product.description}</p>
                      </div>
                      <div>
                        <h4 className="font-medium mb-2">Classification</h4>
                        <div className="flex gap-2">
                          <Badge>HS Code: {product.hsCode}</Badge>
                          <Badge variant="secondary">{product.category}</Badge>
                        </div>
                      </div>
                      <div>
                        <h4 className="font-medium mb-2">Specifications</h4>
                        <div className="grid grid-cols-2 gap-2 text-sm">
                          {Object.entries(product.specifications).map(([key, value]) => (
                            <div key={key}>
                              <span className="text-muted-foreground capitalize">{key}: </span>
                              <span>{value}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>
                  </DialogContent>
                </Dialog>
              </div>
            </div>
          ))}
        </div>

        {filteredProducts.length === 0 && (
          <div className="text-center py-8 text-muted-foreground">
            No products found matching your search criteria.
          </div>
        )}

        {/* Product Details Form */}
        {selectedProduct && (
          <div className="border-t pt-6 space-y-4">
            <h3 className="font-medium">Product Details</h3>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="quantity">Quantity</Label>
                <Input
                  id="quantity"
                  type="number"
                  placeholder="Enter quantity"
                  value={quantity}
                  onChange={(e) => setQuantity(e.target.value)}
                />
              </div>
              
              <div className="space-y-2">
                <Label htmlFor="unit-value">Unit Value (USD)</Label>
                <Input
                  id="unit-value"
                  type="number"
                  placeholder="Enter unit value"
                  value={unitValue}
                  onChange={(e) => setUnitValue(e.target.value)}
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="additional-specs">Additional Specifications (Optional)</Label>
              <Textarea
                id="additional-specs"
                placeholder="Enter any additional product specifications (e.g., serial numbers, special features, etc.)"
                value={customSpecs.additional || ""}
                onChange={(e) => setCustomSpecs(prev => ({ ...prev, additional: e.target.value }))}
              />
            </div>

            <Button onClick={handleConfirmSelection} className="w-full">
              Confirm Product Selection
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}