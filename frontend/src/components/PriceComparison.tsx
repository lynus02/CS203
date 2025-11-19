import { useState, useEffect } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card";
import { Button } from "./ui/button";
import { Badge } from "./ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "./ui/table";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select";
import { Label } from "./ui/label";
import { Input } from "./ui/input";
import { TrendingUp, TrendingDown, Minus, BarChart3 } from "lucide-react";

interface ComparisonScenario {
  id: string;
  name: string;
  originCountry: string;
  destinationCountry: string;
  shippingMethod: string;
  productValue: number;
  dutyRate: number;
  taxRate: number;
  shippingCost: number;
  totalCost: number;
  savings?: number;
}

export function PriceComparison() {
  const [scenarios, setScenarios] = useState<ComparisonScenario[]>([]);
  const [newScenario, setNewScenario] = useState({
    name: "",
    originCountry: "",
    destinationCountry: "",
    shippingMethod: "",
    productValue: "",
    dutyRate: "",
    taxRate: "",
    shippingCost: ""
  });

  const countries = [
    "United States", "Canada", "Mexico", "China", "Japan", "South Korea",
    "Germany", "France", "United Kingdom", "Italy", "Spain", "India",
    "Brazil", "Australia", "Singapore", "Thailand", "Vietnam"
  ];

  const shippingMethods = [
    { value: "air", label: "Air Freight", cost: 5.50 },
    { value: "sea", label: "Sea Freight", cost: 1.20 },
    { value: "ground", label: "Ground Transport", cost: 2.80 }
  ];

  const calculateTotalCost = (scenario: Omit<ComparisonScenario, 'id' | 'totalCost' | 'savings'>) => {
    const productValue = scenario.productValue;
    const dutyAmount = (productValue * scenario.dutyRate) / 100;
    const taxableValue = productValue + dutyAmount + scenario.shippingCost;
    const taxAmount = (taxableValue * scenario.taxRate) / 100;
    return productValue + dutyAmount + taxAmount + scenario.shippingCost;
  };

  const addScenario = () => {
    if (!newScenario.name || !newScenario.originCountry || !newScenario.destinationCountry) return;

    const scenarioData = {
      ...newScenario,
      productValue: parseFloat(newScenario.productValue) || 0,
      dutyRate: parseFloat(newScenario.dutyRate) || 0,
      taxRate: parseFloat(newScenario.taxRate) || 0,
      shippingCost: parseFloat(newScenario.shippingCost) || 0
    };

    const scenario: ComparisonScenario = {
      id: Date.now().toString(),
      name: scenarioData.name,
      originCountry: scenarioData.originCountry,
      destinationCountry: scenarioData.destinationCountry,
      shippingMethod: scenarioData.shippingMethod,
      productValue: scenarioData.productValue,
      dutyRate: scenarioData.dutyRate,
      taxRate: scenarioData.taxRate,
      shippingCost: scenarioData.shippingCost,
      totalCost: calculateTotalCost(scenarioData)
    };

    setScenarios(prev => [...prev, scenario]);
    setNewScenario({
      name: "",
      originCountry: "",
      destinationCountry: "",
      shippingMethod: "",
      productValue: "",
      dutyRate: "",
      taxRate: "",
      shippingCost: ""
    });
  };

  // Calculate savings compared to the most expensive scenario
  useEffect(() => {
    if (scenarios.length > 1) {
      const maxCost = Math.max(...scenarios.map(s => s.totalCost));
      setScenarios(prev => prev.map(scenario => ({
        ...scenario,
        savings: maxCost - scenario.totalCost
      })));
    }
  }, [scenarios.length]);

  const removeScenario = (id: string) => {
    setScenarios(prev => prev.filter(s => s.id !== id));
  };

  const getSavingsIcon = (savings: number) => {
    if (savings > 0) return <TrendingDown className="h-4 w-4 text-green-600" />;
    if (savings < 0) return <TrendingUp className="h-4 w-4 text-red-600" />;
    return <Minus className="h-4 w-4 text-muted-foreground" />;
  };

  const getBestScenario = () => {
    if (scenarios.length === 0) return null;
    return scenarios.reduce((best, current) => 
      current.totalCost < best.totalCost ? current : best
    );
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <BarChart3 className="h-5 w-5" />
          Price Comparison
        </CardTitle>
        <CardDescription>
          Compare total costs across different countries and shipping methods
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* Add New Scenario */}
        <div className="p-4 border rounded-lg space-y-4">
          <h3 className="font-medium">Add Comparison Scenario</h3>
          
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <div className="space-y-2">
              <Label htmlFor="scenario-name">Scenario Name</Label>
              <Input
                id="scenario-name"
                placeholder="e.g., China via Sea"
                value={newScenario.name}
                onChange={(e) => setNewScenario(prev => ({ ...prev, name: e.target.value }))}
              />
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="origin-country">Origin Country</Label>
              <Select value={newScenario.originCountry} onValueChange={(value) => 
                setNewScenario(prev => ({ ...prev, originCountry: value }))
              }>
                <SelectTrigger>
                  <SelectValue placeholder="Select origin" />
                </SelectTrigger>
                <SelectContent>
                  {countries.map((country) => (
                    <SelectItem key={country} value={country}>
                      {country}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="destination-country">Destination Country</Label>
              <Select value={newScenario.destinationCountry} onValueChange={(value) => 
                setNewScenario(prev => ({ ...prev, destinationCountry: value }))
              }>
                <SelectTrigger>
                  <SelectValue placeholder="Select destination" />
                </SelectTrigger>
                <SelectContent>
                  {countries.map((country) => (
                    <SelectItem key={country} value={country}>
                      {country}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="shipping-method">Shipping Method</Label>
              <Select value={newScenario.shippingMethod} onValueChange={(value) => 
                setNewScenario(prev => ({ ...prev, shippingMethod: value }))
              }>
                <SelectTrigger>
                  <SelectValue placeholder="Select method" />
                </SelectTrigger>
                <SelectContent>
                  {shippingMethods.map((method) => (
                    <SelectItem key={method.value} value={method.value}>
                      {method.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="product-value">Product Value (USD)</Label>
              <Input
                id="product-value"
                type="number"
                placeholder="Enter value"
                value={newScenario.productValue}
                onChange={(e) => setNewScenario(prev => ({ ...prev, productValue: e.target.value }))}
              />
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="duty-rate">Duty Rate (%)</Label>
              <Input
                id="duty-rate"
                type="number"
                placeholder="Enter duty rate"
                value={newScenario.dutyRate}
                onChange={(e) => setNewScenario(prev => ({ ...prev, dutyRate: e.target.value }))}
              />
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="tax-rate">Tax Rate (%)</Label>
              <Input
                id="tax-rate"
                type="number"
                placeholder="Enter tax rate"
                value={newScenario.taxRate}
                onChange={(e) => setNewScenario(prev => ({ ...prev, taxRate: e.target.value }))}
              />
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="shipping-cost">Shipping Cost (USD)</Label>
              <Input
                id="shipping-cost"
                type="number"
                placeholder="Enter shipping cost"
                value={newScenario.shippingCost}
                onChange={(e) => setNewScenario(prev => ({ ...prev, shippingCost: e.target.value }))}
              />
            </div>
          </div>
          
          <Button onClick={addScenario} className="w-full">
            Add Scenario
          </Button>
        </div>

        {/* Comparison Results */}
        {scenarios.length > 0 && (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="font-medium">Comparison Results</h3>
              {getBestScenario() && (
                <Badge variant="default">
                  Best Option: {getBestScenario()?.name}
                </Badge>
              )}
            </div>
            
            <div className="border rounded-lg overflow-hidden">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Scenario</TableHead>
                    <TableHead>Route</TableHead>
                    <TableHead>Method</TableHead>
                    <TableHead>Product Value</TableHead>
                    <TableHead>Duty</TableHead>
                    <TableHead>Tax</TableHead>
                    <TableHead>Shipping</TableHead>
                    <TableHead>Total Cost</TableHead>
                    <TableHead>Savings</TableHead>
                    <TableHead></TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {scenarios.map((scenario) => (
                    <TableRow key={scenario.id} className={getBestScenario()?.id === scenario.id ? "bg-green-50 dark:bg-green-950/20" : ""}>
                      <TableCell>{scenario.name}</TableCell>
                      <TableCell className="text-sm">
                        {scenario.originCountry} → {scenario.destinationCountry}
                      </TableCell>
                      <TableCell>
                        <Badge variant="outline" className="text-xs">
                          {shippingMethods.find(m => m.value === scenario.shippingMethod)?.label || scenario.shippingMethod}
                        </Badge>
                      </TableCell>
                      <TableCell>${scenario.productValue.toFixed(2)}</TableCell>
                      <TableCell>${((scenario.productValue * scenario.dutyRate) / 100).toFixed(2)}</TableCell>
                      <TableCell>${(((scenario.productValue + (scenario.productValue * scenario.dutyRate) / 100 + scenario.shippingCost) * scenario.taxRate) / 100).toFixed(2)}</TableCell>
                      <TableCell>${scenario.shippingCost.toFixed(2)}</TableCell>
                      <TableCell>
                        <Badge variant={getBestScenario()?.id === scenario.id ? "default" : "secondary"}>
                          ${scenario.totalCost.toFixed(2)}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-1">
                          {getSavingsIcon(scenario.savings || 0)}
                          <span className="text-sm">
                            {scenario.savings ? `$${Math.abs(scenario.savings).toFixed(2)}` : '$0.00'}
                          </span>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => removeScenario(scenario.id)}
                        >
                          Remove
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
            
            {scenarios.length > 1 && getBestScenario() && (
              <div className="p-4 bg-green-50 dark:bg-green-950/20 rounded-lg">
                <h4 className="font-medium text-green-800 dark:text-green-200 mb-2">Recommendation</h4>
                <p className="text-sm text-green-700 dark:text-green-300">
                  The <strong>{getBestScenario()?.name}</strong> scenario offers the lowest total cost at{' '}
                  <strong>${getBestScenario()?.totalCost.toFixed(2)}</strong>. You could save up to{' '}
                  <strong>${Math.max(...scenarios.map(s => s.savings || 0)).toFixed(2)}</strong> compared to other options.
                </p>
              </div>
            )}
          </div>
        )}

        {scenarios.length === 0 && (
          <div className="text-center py-8 text-muted-foreground">
            Add scenarios to compare total costs across different countries and shipping methods.
          </div>
        )}
      </CardContent>
    </Card>
  );
}