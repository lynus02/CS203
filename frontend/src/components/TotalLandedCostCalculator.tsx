import { useState, useEffect } from "react";
import { Badge } from "./ui/badge";
import { Separator } from "./ui/separator";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { Button } from "./ui/button";

interface LandedCostResult {
  productValue: number;
  shippingCost: number;
  customsDuty: number;
  totalLandedCost: number;
  costPerUnit: number;
}

interface TotalLandedCostCalculatorProps {
  customsResults?: any;
  shippingResults?: any;
}

export function TotalLandedCostCalculator({ customsResults, shippingResults }: TotalLandedCostCalculatorProps) {
  const [quantity, setQuantity] = useState("");
  const [result, setResult] = useState<LandedCostResult | null>(null);

    useEffect(() => {
        if (customsResults?.dutyAmount !== undefined && shippingResults) {
            calculateLandedCost()
        }
    }, [customsResults?.dutyAmount, shippingResults, quantity])


    const calculateLandedCost = () => {
    if (!customsResults || !shippingResults) {
      return;
    }

    const productValue =
        customsResults.productValue ??
        (customsResults.totalCost && customsResults.dutyAmount
            ? customsResults.totalCost - customsResults.dutyAmount
            : 0);

      const shippingCost =
          shippingResults?.totalShippingCost ??
          shippingResults?.shippingCost ??
          shippingResults?.cost ??
          0;

      const customsDuty = customsResults.dutyAmount || 0;
    const qty = parseFloat(quantity) || 1;

    // Calculate total landed cost
    const totalLandedCost = productValue + shippingCost + customsDuty;
    const costPerUnit = totalLandedCost / qty;

    setResult({
      productValue,
      shippingCost,
      customsDuty,
      totalLandedCost,
      costPerUnit
    });
  };

  const clearCalculation = () => {
    setQuantity("");
    setResult(null);
  };

  // Show message if no calculations have been made yet
  if (!customsResults && !shippingResults) {
    return (
      <div className="text-center py-8">
        <p className="text-muted-foreground mb-2">Calculate food duty and shipping costs above to see your total landed cost</p>
        <p className="text-sm text-muted-foreground">Complete both calculations to get an automatic total</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Optional quantity input for per-unit calculation */}
      <div className="space-y-2">
        <Label htmlFor="quantity">Quantity (optional - for per-unit cost calculation)</Label>
        <Input
          id="quantity"
          type="number"
          placeholder="Enter quantity"
          value={quantity}
          onChange={(e) => setQuantity(e.target.value)}
        />
      </div>

      {/* Auto-calculated results */}
      {(customsResults || shippingResults) && (
        <div className="p-4 bg-muted rounded-lg">
          <h3 className="mb-4">Total Landed Cost Breakdown</h3>

          <div className="space-y-3">
            {customsResults ? (
                <>
                    <div className="flex justify-between items-center">
                        <span>Product Value:</span>
                        <span>${(
                            customsResults.productValue ??
                            (customsResults.totalCost && customsResults.dutyAmount
                                ? customsResults.totalCost - customsResults.dutyAmount
                                : 0)
                        ).toFixed(2)}
                              </span>
                    </div>
                    <div className="flex justify-between items-center">
                        <span>Customs Duty:</span>
                        <span>${(customsResults.dutyAmount || 0).toFixed(2)}</span>
                    </div>
                </>
            ): (
                <div className="text-muted-foreground">No customs data available yet</div>
            )}

              {shippingResults && (
                  <div className="flex justify-between items-center">
                      <span>Shipping Cost:</span>
                      <span>${(
                          shippingResults.totalShippingCost ??
                          shippingResults.shippingCost ??
                          shippingResults.cost ??
                          0
                      ).toFixed(2)}</span>
                  </div>
              )}

              <Separator />

            {result && (
              <>
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
              </>
            )}
          </div>

          <div className="mt-4 p-3 bg-primary/10 rounded-lg">
            <p className="text-sm text-muted-foreground">
              This total is automatically calculated from your food duty and shipping calculations above.
              Complete both sections for the full landed cost.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}