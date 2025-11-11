import { useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select";
import { Button } from "./ui/button";
import { Badge } from "./ui/badge";
import { Truck, Ship, Plane } from "lucide-react";

interface ShippingResult {
  shippingCost: number;
  transitTime: string;
  insuranceCost: number;
  totalShippingCost: number;
}

interface ShippingCalculatorProps {
  onResultsChange?: (results: ShippingResult | null) => void;
}

export function ShippingCalculator({ onResultsChange }: ShippingCalculatorProps) {
  const [weight, setWeight] = useState("");
  const [dimensions, setDimensions] = useState({ length: "", width: "", height: "" });
  const [shippingMethod, setShippingMethod] = useState("");
  const [distance, setDistance] = useState("");
  const [insuranceValue, setInsuranceValue] = useState("");
  const [result, setResult] = useState<ShippingResult | null>(null);

  const shippingMethods = [
    {
      value: "air",
      label: "Air Freight",
      icon: Plane,
      ratePerKg: 5.50,
      ratePerKm: 0.02,
      transitBase: "1-3 days",
      insuranceRate: 0.5
    },
    {
      value: "sea",
      label: "Sea Freight",
      icon: Ship,
      ratePerKg: 1.20,
      ratePerKm: 0.005,
      transitBase: "20-45 days",
      insuranceRate: 0.3
    },
    {
      value: "ground",
      label: "Ground Transport",
      icon: Truck,
      ratePerKg: 2.80,
      ratePerKm: 0.015,
      transitBase: "3-10 days",
      insuranceRate: 0.4
    }
  ];

  const calculateShipping = () => {
    const weightKg = parseFloat(weight);
    const distanceKm = parseFloat(distance);
    const insValue = parseFloat(insuranceValue) || 0;
    
    const selectedMethod = shippingMethods.find(method => method.value === shippingMethod);
    
    if (weightKg && distanceKm && selectedMethod) {
      // Calculate dimensional weight (L x W x H / 5000 for air, 6000 for sea/ground)
      const length = parseFloat(dimensions.length) || 0;
      const width = parseFloat(dimensions.width) || 0;
      const height = parseFloat(dimensions.height) || 0;
      
      const divisor = selectedMethod.value === "air" ? 5000 : 6000;
      const dimensionalWeight = (length * width * height) / divisor;
      const chargeableWeight = Math.max(weightKg, dimensionalWeight);
      
      const weightCost = chargeableWeight * selectedMethod.ratePerKg;
      const distanceCost = distanceKm * selectedMethod.ratePerKm;
      const shippingCost = weightCost + distanceCost;
      
      const insuranceCost = (insValue * selectedMethod.insuranceRate) / 100;
      const totalShippingCost = shippingCost + insuranceCost;
      
      const calculationResult = {
        shippingCost,
        transitTime: selectedMethod.transitBase,
        insuranceCost,
        totalShippingCost
      };
      
      setResult(calculationResult);
      onResultsChange?.(calculationResult);
    }
  };

  const clearCalculation = () => {
    setWeight("");
    setDimensions({ length: "", width: "", height: "" });
    setShippingMethod("");
    setDistance("");
    setInsuranceValue("");
    setResult(null);
    onResultsChange?.(null);
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Shipping Cost Calculator</CardTitle>
        <CardDescription>
          Calculate shipping costs for different transportation methods
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="weight">Weight (kg)</Label>
            <Input
              id="weight"
              type="number"
              placeholder="Enter weight in kg"
              value={weight}
              onChange={(e) => setWeight(e.target.value)}
            />
          </div>
          
          <div className="space-y-2">
            <Label htmlFor="distance">Distance (km)</Label>
            <Input
              id="distance"
              type="number"
              placeholder="Enter distance in km"
              value={distance}
              onChange={(e) => setDistance(e.target.value)}
            />
          </div>
          
          <div className="space-y-2 md:col-span-2">
            <Label>Dimensions (cm)</Label>
            <div className="grid grid-cols-3 gap-2">
              <Input
                placeholder="Length"
                value={dimensions.length}
                onChange={(e) => setDimensions(prev => ({ ...prev, length: e.target.value }))}
              />
              <Input
                placeholder="Width"
                value={dimensions.width}
                onChange={(e) => setDimensions(prev => ({ ...prev, width: e.target.value }))}
              />
              <Input
                placeholder="Height"
                value={dimensions.height}
                onChange={(e) => setDimensions(prev => ({ ...prev, height: e.target.value }))}
              />
            </div>
          </div>
          
          <div className="space-y-2 md:col-span-2">
            <Label htmlFor="shipping-method">Shipping Method</Label>
            <Select value={shippingMethod} onValueChange={setShippingMethod}>
              <SelectTrigger>
                <SelectValue placeholder="Select shipping method" />
              </SelectTrigger>
              <SelectContent>
                {shippingMethods.map((method) => {
                  const IconComponent = method.icon;
                  return (
                    <SelectItem key={method.value} value={method.value}>
                      <div className="flex items-center gap-2">
                        <IconComponent className="h-4 w-4" />
                        {method.label}
                      </div>
                    </SelectItem>
                  );
                })}
              </SelectContent>
            </Select>
          </div>
          
          <div className="space-y-2 md:col-span-2">
            <Label htmlFor="insurance-value">Insurance Value (USD) - Optional</Label>
            <Input
              id="insurance-value"
              type="number"
              placeholder="Enter value for insurance calculation"
              value={insuranceValue}
              onChange={(e) => setInsuranceValue(e.target.value)}
            />
          </div>
        </div>
        
        <div className="flex gap-2">
            <Button onClick={calculateShipping} className="flex-1">
                Calculate Shipping
            </Button>
            <Button variant="secondary" onClick={clearCalculation}>
                Clear
            </Button>
        </div>
        
        {result && (
          <div className="mt-6 p-4 bg-muted rounded-lg">
            <h3 className="mb-3">Shipping Results</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              <div className="text-center">
                <div className="text-2xl text-primary">${result.shippingCost.toFixed(2)}</div>
                <div className="text-sm text-muted-foreground">Base Shipping</div>
              </div>
              <div className="text-center">
                <div className="text-2xl text-primary">${result.insuranceCost.toFixed(2)}</div>
                <div className="text-sm text-muted-foreground">Insurance</div>
              </div>
              <div className="text-center">
                <div className="text-2xl text-primary">${result.totalShippingCost.toFixed(2)}</div>
                <div className="text-sm text-muted-foreground">Total Cost</div>
              </div>
              <div className="text-center">
                <Badge variant="secondary" className="text-lg px-3 py-1">
                  {result.transitTime}
                </Badge>
                <div className="text-sm text-muted-foreground mt-1">Transit Time</div>
              </div>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}