import { useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { Button } from "./ui/button";
import { Badge } from "./ui/badge";
import { Separator } from "./ui/separator";

interface LandedCostResult {
  productValue: number;
  shippingCost: number;
  customsDuty: number;
  taxes: number;
  insuranceCost: number;
  otherFees: number;
  totalLandedCost: number;
  costPerUnit: number;
}

export function TotalLandedCostCalculator() {
  const [productValue, setProductValue] = useState("");
  const [shippingCost, setShippingCost] = useState("");
  const [dutyRate, setDutyRate] = useState("");
  const [taxRate, setTaxRate] = useState("");
  const [insuranceCost, setInsuranceCost] = useState("");
  const [otherFees, setOtherFees] = useState("");
  const [quantity, setQuantity] = useState("");
  const [result, setResult] = useState<LandedCostResult | null>(null);

  const calculateLandedCost = () => {
    const prodValue = parseFloat(productValue) || 0;
    const shipping = parseFloat(shippingCost) || 0;
    const duty = parseFloat(dutyRate) || 0;
    const tax = parseFloat(taxRate) || 0;
    const insurance = parseFloat(insuranceCost) || 0;
    const fees = parseFloat(otherFees) || 0;
    const qty = parseFloat(quantity) || 1;

    // Calculate customs duty
    const customsDuty = (prodValue * duty) / 100;
    
    // Calculate taxes (usually on product value + duty + shipping)
    const taxableValue = prodValue + customsDuty + shipping;
    const taxes = (taxableValue * tax) / 100;
    
    // Calculate total landed cost
    const totalLandedCost = prodValue + shipping + customsDuty + taxes + insurance + fees;
    const costPerUnit = totalLandedCost / qty;

    setResult({
      productValue: prodValue,
      shippingCost: shipping,
      customsDuty,
      taxes,
      insuranceCost: insurance,
      otherFees: fees,
      totalLandedCost,
      costPerUnit
    });
  };

  const clearCalculation = () => {
    setProductValue("");
    setShippingCost("");
    setDutyRate("");
    setTaxRate("");
    setInsuranceCost("");
    setOtherFees("");
    setQuantity("");
    setResult(null);
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Total Landed Cost Calculator</CardTitle>
        <CardDescription>
          Calculate the complete cost of imported goods including all fees and taxes
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="product-value">Product Value (USD)</Label>
            <Input
              id="product-value"
              type="number"
              placeholder="Enter FOB value"
              value={productValue}
              onChange={(e) => setProductValue(e.target.value)}
            />
          </div>
          
          <div className="space-y-2">
            <Label htmlFor="shipping-cost">Shipping Cost (USD)</Label>
            <Input
              id="shipping-cost"
              type="number"
              placeholder="Enter shipping cost"
              value={shippingCost}
              onChange={(e) => setShippingCost(e.target.value)}
            />
          </div>
          
          <div className="space-y-2">
            <Label htmlFor="duty-rate">Duty Rate (%)</Label>
            <Input
              id="duty-rate"
              type="number"
              placeholder="Enter duty rate"
              value={dutyRate}
              onChange={(e) => setDutyRate(e.target.value)}
            />
          </div>
          
          <div className="space-y-2">
            <Label htmlFor="tax-rate">Tax Rate (% - VAT/GST)</Label>
            <Input
              id="tax-rate"
              type="number"
              placeholder="Enter tax rate"
              value={taxRate}
              onChange={(e) => setTaxRate(e.target.value)}
            />
          </div>
          
          <div className="space-y-2">
            <Label htmlFor="insurance-cost">Insurance Cost (USD)</Label>
            <Input
              id="insurance-cost"
              type="number"
              placeholder="Enter insurance cost"
              value={insuranceCost}
              onChange={(e) => setInsuranceCost(e.target.value)}
            />
          </div>
          
          <div className="space-y-2">
            <Label htmlFor="other-fees">Other Fees (USD)</Label>
            <Input
              id="other-fees"
              type="number"
              placeholder="Broker fees, handling, etc."
              value={otherFees}
              onChange={(e) => setOtherFees(e.target.value)}
            />
          </div>
          
          <div className="space-y-2 md:col-span-2">
            <Label htmlFor="quantity">Quantity (for per-unit cost)</Label>
            <Input
              id="quantity"
              type="number"
              placeholder="Enter quantity"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
            />
          </div>
        </div>
        
        <div className="flex gap-2">
          <Button onClick={calculateLandedCost} className="flex-1">
            Calculate Landed Cost
          </Button>
          <Button variant="outline" onClick={clearCalculation}>
            Clear
          </Button>
        </div>
        
        {result && (
          <div className="mt-6 p-4 bg-muted rounded-lg">
            <h3 className="mb-4">Total Landed Cost Breakdown</h3>
            
            <div className="space-y-3">
              <div className="flex justify-between items-center">
                <span>Product Value:</span>
                <span>${result.productValue.toFixed(2)}</span>
              </div>
              
              <div className="flex justify-between items-center">
                <span>Shipping Cost:</span>
                <span>${result.shippingCost.toFixed(2)}</span>
              </div>
              
              <div className="flex justify-between items-center">
                <span>Customs Duty:</span>
                <span>${result.customsDuty.toFixed(2)}</span>
              </div>
              
              <div className="flex justify-between items-center">
                <span>Taxes (VAT/GST):</span>
                <span>${result.taxes.toFixed(2)}</span>
              </div>
              
              <div className="flex justify-between items-center">
                <span>Insurance:</span>
                <span>${result.insuranceCost.toFixed(2)}</span>
              </div>
              
              <div className="flex justify-between items-center">
                <span>Other Fees:</span>
                <span>${result.otherFees.toFixed(2)}</span>
              </div>
              
              <Separator />
              
              <div className="flex justify-between items-center text-lg">
                <span>Total Landed Cost:</span>
                <Badge variant="default" className="text-lg px-3 py-1">
                  ${result.totalLandedCost.toFixed(2)}
                </Badge>
              </div>
              
              {parseFloat(quantity) > 1 && (
                <div className="flex justify-between items-center">
                  <span>Cost Per Unit:</span>
                  <span className="text-primary">${result.costPerUnit.toFixed(2)}</span>
                </div>
              )}
            </div>
            
            <div className="mt-4 p-3 bg-primary/10 rounded-lg">
              <p className="text-sm text-muted-foreground">
                This calculation includes all major cost components for importing goods. 
                Actual costs may vary based on specific regulations and additional fees.
              </p>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}