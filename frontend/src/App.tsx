import { useState } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "./components/ui/tabs";
import { Card, CardContent, CardHeader, CardTitle } from "./components/ui/card";
import { Badge } from "./components/ui/badge";
import { Button } from "./components/ui/button";
import { CustomsDutyCalculator } from "./components/CustomsDutyCalculator";
import { ShippingCalculator } from "./components/ShippingCalculator";
import { TotalLandedCostCalculator } from "./components/TotalLandedCostCalculator";
import { TariffRateDatabase } from "./components/TariffRateDatabase";
import { Calculator, Ship, DollarSign, Database, Globe, TrendingUp, LogIn, User } from "lucide-react";

export default function App() {
  const [activeTab, setActiveTab] = useState("customs");

  const features = [
    {
      icon: Calculator,
      title: "Food Duty Calculator",
      description: "Calculate import duties and taxes for food products based on value and tariff rates"
    },
    {
      icon: Ship,
      title: "Shipping Cost Calculator",
      description: "Estimate shipping costs for air, sea, and ground transportation"
    },
    {
      icon: DollarSign,
      title: "Total Landed Cost",
      description: "Calculate the complete cost including all fees, taxes, and duties"
    },
    {
      icon: Database,
      title: "Food Tariff Database",
      description: "Search current food tariff rates by HS code and country"
    }
  ];

  return (
    <div className="min-h-screen bg-background">
      {/* Top Navigation Bar */}
      <div className="border-b border-border bg-card">
        <div className="container mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            {/* Logo and Brand */}
            <div className="flex items-center gap-2">
              <Globe className="h-6 w-6 text-primary" />
              <span className="text-xl font-medium">FoodTariff Pro</span>
            </div>
            
            {/* Login Button */}
            <Button variant="outline" className="flex items-center gap-2">
              <LogIn className="h-4 w-4" />
              Login
            </Button>
          </div>
        </div>
      </div>

      <div className="container mx-auto px-4 py-8">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="flex items-center justify-center gap-2 mb-4">
            <Globe className="h-8 w-8 text-primary" />
            <h1 className="text-3xl">FoodTariff Pro</h1>
          </div>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            Specialized tariff and shipping cost calculator for food imports and exports. 
            Calculate customs duties, shipping costs, and total landed costs for food products with ease.
          </p>
        </div>

        {/* Feature Overview */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          {features.map((feature, index) => {
            const IconComponent = feature.icon;
            return (
              <Card key={index} className="text-center">
                <CardContent className="pt-6">
                  <IconComponent className="h-8 w-8 mx-auto mb-2 text-primary" />
                  <h3 className="font-medium mb-2">{feature.title}</h3>
                  <p className="text-sm text-muted-foreground">{feature.description}</p>
                </CardContent>
              </Card>
            );
          })}
        </div>

        {/* Main Calculator Tabs */}
        <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
          <TabsList className="grid w-full grid-cols-4">
            <TabsTrigger value="customs" className="flex items-center gap-2">
              <Calculator className="h-4 w-4" />
              <span className="hidden sm:inline">Food Duty</span>
            </TabsTrigger>
            <TabsTrigger value="shipping" className="flex items-center gap-2">
              <Ship className="h-4 w-4" />
              <span className="hidden sm:inline">Shipping</span>
            </TabsTrigger>
            <TabsTrigger value="landed" className="flex items-center gap-2">
              <DollarSign className="h-4 w-4" />
              <span className="hidden sm:inline">Landed Cost</span>
            </TabsTrigger>
            <TabsTrigger value="database" className="flex items-center gap-2">
              <Database className="h-4 w-4" />
              <span className="hidden sm:inline">Database</span>
            </TabsTrigger>
          </TabsList>

          <TabsContent value="customs" className="mt-6">
            <CustomsDutyCalculator />
          </TabsContent>

          <TabsContent value="shipping" className="mt-6">
            <ShippingCalculator />
          </TabsContent>

          <TabsContent value="landed" className="mt-6">
            <TotalLandedCostCalculator />
          </TabsContent>

          <TabsContent value="database" className="mt-6">
            <TariffRateDatabase />
          </TabsContent>
        </Tabs>

        {/* Quick Tips */}
        <Card className="mt-8">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <TrendingUp className="h-5 w-5" />
              Quick Tips
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div className="space-y-2">
                <Badge variant="secondary">HS Code</Badge>
                <p className="text-sm">
                  Use the correct 6-10 digit Harmonized System code for accurate tariff rates. 
                  Check with customs authorities for official classifications.
                </p>
              </div>
              <div className="space-y-2">
                <Badge variant="secondary">FOB vs CIF</Badge>
                <p className="text-sm">
                  Free On Board (FOB) prices exclude shipping. Cost, Insurance, and Freight (CIF) 
                  includes shipping to destination port.
                </p>
              </div>
              <div className="space-y-2">
                <Badge variant="secondary">Trade Agreements</Badge>
                <p className="text-sm">
                  Check for preferential trade agreements that may reduce or eliminate tariffs 
                  between specific countries.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Footer */}
        <div className="mt-12 text-center text-sm text-muted-foreground">
          <p>
            Disclaimer: This calculator provides estimates based on general tariff information. 
            Always consult official customs authorities for accurate, up-to-date rates and regulations.
          </p>
        </div>
      </div>
    </div>
  );
}