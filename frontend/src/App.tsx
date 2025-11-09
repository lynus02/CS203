// ========== IMPORTS ========== //
import { useState, useEffect } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "./components/ui/tabs";
import { Card, CardContent, CardHeader, CardTitle } from "./components/ui/card";
import { Badge } from "./components/ui/badge";
import { Button } from "./components/ui/button";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    DialogTrigger
} from "./components/ui/dialog";

// Calculate and Database Modules
import { CustomsDutyCalculator } from "./components/CustomsDutyCalculator";
import { ShippingCalculator } from "./components/ShippingCalculator";
import { TotalLandedCostCalculator } from "./components/TotalLandedCostCalculator";
import { TariffRateDatabase } from "./components/TariffRateDatabase";

// Auth Pages
import LoginPage from "./components/LoginPage";
import SignupPage from "./components/SignupPage";
import api from "./services/api";

// Context: Saved Products (from DB)
import { useSavedProducts } from "./components/context/SavedProductsContext";

// Icons
import {
    Calculator,
    Ship,
    DollarSign,
    Database,
    Globe,
    TrendingUp,
    LogIn,
    User
} from "lucide-react";

// 🧠 Import AI Chatbot
import { AIChat } from "./components/AIChatBot";

// ========== MAIN APP COMPONENT ========== //
export default function App() {
    const [activeTab, setActiveTab] = useState("customs");

    // Shared state for calculated values
    const [customsResults, setCustomsResults] = useState(null);
    const [shippingResults, setShippingResults] = useState(null);

    // Authentication State
    const [showLogin, setShowLogin] = useState(false);
    const [showSignup, setShowSignup] = useState(false);
    const [user, setUser] = useState(null);

    // Saved Products Context
    const { savedProducts, fetchSavedProducts, removeSavedProduct, isLoading } = useSavedProducts();

    // ========== EFFECTS ========== //
    useEffect(() => {
        if (user?.id) {
            fetchSavedProducts(user.id);
        }
    }, [user]);

    useEffect(() => {
        const checkAuthStatus = async () => {
            const token = localStorage.getItem("token");
            if (token && !user) {
                try {
                    const userProfile = await api.get("/auth/me");
                    const userData = userProfile.data;

                    const userObj = {
                        id: userData.id,
                        name: userData.firstName + " " + userData.lastName,
                        email: userData.email,
                        token: token,
                        role: Array.isArray(userData.roles)
                            ? userData.roles[0]
                            : userData.role
                    };

                    setUser(userObj);
                } catch (error) {
                    localStorage.removeItem("token");
                    console.error("Token validation failed:", error);
                }
            }
        };

        if (!user) {
            checkAuthStatus();
        }
    }, []);

    // ========== AUTH HANDLERS ========== //
    const handleLogin = (userData) => {
        setUser(userData);
        setShowLogin(false);
    };

    const handleLogout = () => {
        setUser(null);
        localStorage.removeItem("token");
    };

    const handleSignUp = () => {
        setShowLogin(false);   // Hide login page
        setShowSignup(true);   // Show signup page
    };

    const handleSignupSuccess = (userData) => {
        setUser(userData);
        setShowSignup(false);
    };

    const handleShowLogin = () => setShowLogin(true);
    const handleBackFromLogin = () => setShowLogin(false);


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

    // ========== CONDITIONAL RENDERING ========== //
    if (showSignup) {
        return (
            <SignupPage
                onSignup={handleSignupSuccess}
                onBack={() => setShowSignup(false)}
                onLogin={() => {
                    setShowSignup(false);
                    setShowLogin(true);
                }}
            />
        );
    }

    // Show login page if showLogin is true
  if (showLogin) {
    return <LoginPage onLogin={handleLogin} onBack={handleBackFromLogin} onSignUp={handleSignUp} />;
  }

    return (
        <>
            <div className="min-h-screen bg-background flex flex-col w-full">
                {/* Top Navigation Bar */}
                <div className="border-b border-border ">
                    <div className="border-b border-gray-250 px-6 py-6 bg-primary">
                        <div className="flex items-center justify-between gap-4">
                            {/* Header with logo */}
                            <div className="w-full px-8 py-8 flex items-center">
                                <a href="/" className="flex items-center gap-2 hover:opacity-80">
                                    <Globe className="h-6 w-6 text-white" />
                                    <span className="text-xl font-medium text-white">FoodTariff Pro</span>
                                </a>
                            </div>

                        {/* Saved Products Button */}
                        {user ? (
                            <Dialog>
                                <DialogTrigger asChild>
                                    <Button className="flex items-center gap-2">
                                        My Saved Products
                                    </Button>
                                </DialogTrigger>
                              <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
                                 <DialogHeader>
                                    <DialogTitle>My Saved Products</DialogTitle>
                                      <DialogDescription>
                                        View and manage your saved products
                                      </DialogDescription>
                                 </DialogHeader>
                                 <div className="grid gap-4 py-4">
                                    {isLoading ? (
                                        <p className="text-center text-muted-foreground py-8">
                                           Loading...
                                        </p>
                                    ) : savedProducts && savedProducts.length > 0 ? (
                                        savedProducts.map((product) => (
                                          <div key={product.id} className="flex items-center gap-4 p-4 border rounded-lg">
                                              {product.image && (
                                                 <img
                                                     src={product.image}
                                                     alt={product.name}
                                                     className="w-20 h-20 object-cover rounded"
                                                 />
                                                )}
                                                <div className="flex-1">
                                                   <h3 className="font-semibold">{product.name}</h3>
                                                    <p className="text-sm text-muted-foreground">HS Code: {product.hsCode}</p>
                                                    <p className="text-sm text-muted-foreground">Category: {product.category}</p>
                                                </div>
                                                    <Button
                                                        variant="outline"
                                                        size="sm"
                                                        onClick={() => removeSavedProduct(product.id)}
                                                    >
                                                        Remove
                                                    </Button>
                                                </div>
                                            ))
                                        ) : (
                                            <p className="text-center text-muted-foreground py-8">
                                                No saved products yet
                                            </p>
                                        )}
                                    </div>
                                </DialogContent>
                            </Dialog>
                        ) : (
                            <Dialog>
                                <DialogTrigger asChild>
                                    <Button className="flex items-center gap-2">
                                        My Saved Products
                                    </Button>
                                </DialogTrigger>
                                <DialogContent className="max-w-md min-h-[300px] flex flex-col">
                                    <DialogHeader>
                                        <DialogTitle className="text-2xl">Login Required</DialogTitle>
                                        <DialogDescription className="text-lg">
            <span onClick={handleShowLogin} className="text-primary hover:underline font-medium cursor-pointer text-lg">
                Login
            </span>{" "}
                to view your saved products
                    </DialogDescription>
                  </DialogHeader>
                </DialogContent>
            </Dialog>
           )}

              {/* Login/User Button */}
              {user ? (
                  <div className="flex items-center gap-2">
                      <span className="text-sm text-muted-foreground">Welcome, {user.name || user.email}</span>
                      <Button variant="outline" onClick={handleLogout} className="flex items-center gap-2">
                          <User className="h-4 w-4" />
                          Logout
                      </Button>
                  </div>
              ) : (
                  <Button variant="outline" onClick={handleShowLogin} className="!bg-white !text-black flex items-center gap-2">
                      <LogIn className="h-4 w-4" />
                      Login
                  </Button>
              )}

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
          <TabsList className="bg-primary grid w-full" style={{ gridTemplateColumns: "75% 25%" }}>
            <TabsTrigger value="customs" className="flex items-center gap-2">
              <Calculator className="h-4 w-4" />
              <span className="hidden sm:inline">Food Duty</span>
            </TabsTrigger>
            <TabsTrigger value="database" className="flex items-center gap-2">
              <Database className="h-4 w-4" />
              <span className="hidden sm:inline">Database</span>
            </TabsTrigger>
          </TabsList>

          <TabsContent value="customs" className="mt-6">
            <div className="space-y-6">
              {/* Food Duty Calculator */}
              <CustomsDutyCalculator onResultsChange={setCustomsResults} />
              
              {/* Shipping Section */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <Ship className="h-5 w-5" />
                    Shipping Cost Calculator
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <ShippingCalculator onResultsChange={setShippingResults} />
                </CardContent>
              </Card>

              {/* Total Landed Cost Section */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <DollarSign className="h-5 w-5" />
                    Total Landed Cost
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <TotalLandedCostCalculator 
                    customsResults={customsResults}
                    shippingResults={shippingResults}
                  />
                </CardContent>
              </Card>
            </div>
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
                <Badge variant="secondary">Food Safety</Badge>
                <p className="text-sm">
                  Food imports require additional documentation like health certificates, 
                  FDA registration, and may face inspection delays at customs.
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
            </div>

            {/* AI Chat Assistant */}
            <AIChat />
        </>
    );
}

