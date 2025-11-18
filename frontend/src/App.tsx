// ========== IMPORTS ========== //
import {useState, useEffect} from "react";
import {Tabs, TabsContent, TabsList, TabsTrigger} from "./components/ui/tabs";
import {Card, CardContent, CardHeader, CardTitle} from "./components/ui/card";
import {Badge} from "./components/ui/badge";
import {Button} from "./components/ui/button";
import {Dialog, DialogContent} from "./components/ui/dialog";
import { Toaster } from "sonner";

// Icons
import {Calculator, Ship, DollarSign, Database, Globe, TrendingUp, LogIn, User} from "lucide-react";

// Calculate and Database Modules
import {CustomsDutyCalculator} from "./components/CustomsDutyCalculator";
import {ShippingCalculator} from "./components/ShippingCalculator";
import {TotalLandedCostCalculator} from "./components/TotalLandedCostCalculator";
import {TariffRateDatabase} from "./components/TariffRateDatabase";

// Auth Pages
import LoginPage from "./components/authen/LoginPage";
import SignupPage from "./components/authen/SignupPage";
import ForgotPasswordPage from "./components/authen/ForgotPasswordPage";
import ProfilePageUser from "./components/authen/auth/ProfilePageUser";
import ResetPasswordPage from "./components/authen/ResetPasswordPage";
import api from "./services/api";

// Context: Saved Products (from DB)
import {SavedProductConfig} from "./components/SavedProducts";
import MySavedProductsButton from "./components/MySavedProductsButton";

// Import Light/Dark mode theme
import ThemeToggle from "./components/togglethemebutton/ThemeToggle";

// Import AI Chatbot
import {AIChat} from "./components/AIChatBot";

// ========== MAIN APP COMPONENT ========== //
export default function App() {
    const [activeTab, setActiveTab] = useState("customs");
    const [showLoginPrompt, setShowLoginPrompt] = useState(false);
    // Split view state: when true, show two calculators side-by-side
    const [splitView, setSplitView] = useState(false);

    // Calculation values
    const [customsResults, setCustomsResults] = useState(null);
    const [leftCustomsResults, setLeftCustomsResults] = useState(null);
    const [rightCustomsResults, setRightCustomsResults] = useState(null);
    const [shippingResults, setShippingResults] = useState(null);
    const [savedConfigToLoad, setSavedConfigToLoad] = useState<SavedProductConfig | undefined>(undefined);

    // Choose which customs result to feed downstream calculators
    const displayedCustomsResults = splitView ? (leftCustomsResults ?? rightCustomsResults ?? null) : customsResults;

    // Auth states
    const [user, setUser] = useState(null);
    const [showLogin, setShowLogin] = useState(false);
    const [showSignup, setShowSignup] = useState<boolean>(false);
    const [showProfile, setShowProfile] = useState(false);              // Profile page
    const [showResetPassword, setShowResetPassword] = useState(false);  // Reset password page
    const [showForgotPassword, setShowForgotPassword] = useState(false);    // Forgot password page

    // ========= EVENT HANDLERS ========= //
    const handleLoadProduct = (config: SavedProductConfig) => {
        setSavedConfigToLoad(config);
        setActiveTab("customs");
        // Clear after a brief delay to allow re-loading the same config multiple times
        setTimeout(() => setSavedConfigToLoad(undefined), 500);
    };

    const handleLogin = (userData) => {
        setUser(userData); // stores the logged-in user's data in state
        setShowLogin(false); // hides the login page after successful login
    };

    const handleLogout = () => {
        setUser(null); // clears the user state to log out
        localStorage.removeItem('token'); // removes the token from local storage
    };

    const handleSignUp = () => {
        setShowLogin(false);   // Hide login page
        setShowSignup(true);   // Show signup page
    };

    const handleSignupSuccess = (userData) => {
        setUser(userData);
        setShowSignup(false);
    };

    // ========= AUTH SPECIFIC EVENT HANDLERS ========= //
    const handleShowLogin = () => {
        setShowLogin(true); //shows the login page
    };

    const handleBackFromLogin = () => {
        setShowLogin(false); //hides the login page and returns to main app
    };

    const handleForgotPassword = () => {
        console.log('handleForgotPassword called - setting showForgotPassword to true');
        setShowLogin(false);
        setShowForgotPassword(true);
    }

    // Open the profile page view
    const handleShowProfile = (): void => {
        setShowProfile(true);
    }

    // ========== FEATURE DEFINITIONS ========== //
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

    // ========== useEffect - Authentication: validate token on refresh
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

    // ========== useEffect - Global Event Listeners ========= //

    // Listen for a global event so child components can trigger opening the reset password page
    useEffect(() => {
        const handler = () => {
            console.log('App received openResetPassword event, opening ResetPasswordPage');
            setShowProfile(false);
            setShowResetPassword(true);
        };
        document.addEventListener('openResetPassword', handler as EventListener);
        return () => document.removeEventListener('openResetPassword', handler as EventListener);
    }, []);

    // Listen for API unauthorized event to show login prompt
    useEffect(() => {
        const authHandler = () => {
            console.log('Received auth:unauthorized event - showing login');
            setShowLogin(true);
        };
        window.addEventListener('auth:unauthorized', authHandler as EventListener);
        return () => window.removeEventListener('auth:unauthorized', authHandler as EventListener);
    }, []);

    // Listen for openLogin event (triggered by components that want to open the login modal)
    useEffect(() => {
        const loginHandler = () => {
            console.log('Received openLogin event - showing login modal');
            setShowLogin(true);
        };
        window.addEventListener('openLogin', loginHandler as EventListener);
        return () => window.removeEventListener('openLogin', loginHandler as EventListener);
    }, []);

    // ========== CONDITIONAL RENDERING ========== //
    if (showSignup) {
        return (
            <SignupPage
                onSignup={handleSignupSuccess} // You'll need this function too
                onBack={() => setShowSignup(false)}
                onLogin={() => {
                    setShowSignup(false);
                    setShowLogin(true);
                }}
            />
        );
    }

    // Check forgot password BEFORE login (higher priority)
    if (showForgotPassword) {
        return (
            <ForgotPasswordPage
                onBack={() => {
                    setShowForgotPassword(false);
                    setShowLogin(true);
                }}
            />
        );
    }

    // Show login page if showLogin is true
    if (showLogin) {
        return <LoginPage onLogin={handleLogin} onBack={handleBackFromLogin} onSignUp={handleSignUp}
                          onForgotPassword={handleForgotPassword}/>;
    }

    // If reset password page requested, show it first (higher priority than profile)
    if (showResetPassword) {
        return (
            <ResetPasswordPage
                onBack={() => {
                    setShowResetPassword(false);
                    setShowProfile(true);
                }}
                onSuccess={() => {
                    setShowResetPassword(false);
                    setShowProfile(true);
                }}
            />
        );
    }

    if (showProfile) {
        // when navigating to reset password from profile, close profile first
        return <ProfilePageUser onBack={() => setShowProfile(false)} onLogout={handleLogout} onReset={() => {
            setShowProfile(false);
            setShowResetPassword(true);
        }}/>;
    }

    // ========== MAIN UI LAYOUT ========== //
    return (
        <>
            <div className="min-h-screen bg-background flex flex-col w-full">
                {/* Top Navigation Bar */}
                <div className="border-b border-border ">
                    <div className="border-b border-gray-250 px-6 py-8 bg-primary">
                        <div className="flex items-center justify-between gap-4">
                            {/* Header with logo and theme toggle */}
                            <div className="px-8 py-8 flex items-center gap-4">
                                <a href="/" className="flex items-center gap-2 hover:opacity-80">
                                    <Globe className="h-6 w-6 text-white"/>
                                    <span className="text-xl font-medium text-white">FoodTariff Pro</span>
                                </a>
                                <ThemeToggle/>
                            </div>

                            {/* Right side */}
                            <div className="flex items-center gap-4">
                                {/* My Saved Products button (left of login) */}
                                <MySavedProductsButton onLoadProduct={handleLoadProduct}/>

                                {/* Login/User Button */}
                                {user ? (
                                    <div className="flex items-center gap-2">
                                        <span
                                            className="text-sm text-muted-foreground">Welcome, {user.name || user.email}</span>
                                        <Button
                                            variant="outline"
                                            size="sm"
                                            onClick={handleShowProfile}
                                            className="bg-background text-foreground flex items-center gap-2"
                                        >
                                            <User className="h-9 w-9"/>
                                        </Button>
                                    </div>
                                ) : (
                                    <Button variant="outline" onClick={handleShowLogin}
                                            className="bg-background text-foreground flex items-center gap-2">
                                        <LogIn className="h-4 w-4"/>
                                        Login
                                    </Button>
                                )}
                            </div>
                        </div>
                    </div>

                    <div className="container mx-auto px-4 py-8">
                        {/* Header */}
                        <div className="text-center mb-8">
                            <div className="flex items-center justify-center gap-2 mb-4">
                                <Globe className="h-8 w-8 text-primary"/>
                                <h1 className="text-3xl">FoodTariff Pro</h1>
                            </div>
                            <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
                                Specialized tariff and shipping cost calculator for food imports and exports.
                                Calculate customs duties, shipping costs, and total landed costs for food products with
                                ease.
                            </p>
                        </div>

                        {/* Feature Overview */}
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
                            {features.map((feature, index) => {
                                const IconComponent = feature.icon;
                                return (
                                    <Card key={index} className="text-center">
                                        <CardContent className="pt-6">
                                            <IconComponent className="h-8 w-8 mx-auto mb-2 text-primary"/>
                                            <h3 className="font-medium mb-2">{feature.title}</h3>
                                            <p className="text-sm text-muted-foreground">{feature.description}</p>
                                        </CardContent>
                                    </Card>
                                );
                            })}
                        </div>

                        {/* Main Calculator Tabs */}
                        <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
                            <TabsList className="bg-primary grid w-full" style={{gridTemplateColumns: "75% 25%"}}>
                                <TabsTrigger value="customs" className="flex items-center gap-2">
                                    <Calculator className="h-4 w-4"/>
                                    <span className="hidden sm:inline">Food Duty</span>
                                </TabsTrigger>
                                <TabsTrigger value="database" className="flex items-center gap-2">
                                    <Database className="h-4 w-4"/>
                                    <span className="hidden sm:inline">Database</span>
                                </TabsTrigger>
                            </TabsList>

                            <TabsContent value="customs" className="mt-6">
                                <div className="space-y-6">
                                    {/* Food Duty Calculator (single or split view) */}
                                    <Card>
                                        <CardHeader className="flex items-center justify-between">
                                            <CardTitle className="flex items-center gap-2">
                                                <Calculator className="h-5 w-5"/>
                                                Food Duty Calculator
                                            </CardTitle>
                                            <div className="flex items-center gap-2">
                                                <Button size="sm" variant="outline" onClick={() => setSplitView(!splitView)}>
                                                    {splitView ? "Single View" : "Compare with another product"}
                                                </Button>
                                            </div>
                                        </CardHeader>
                                        <CardContent>
                                            {splitView ? (
                                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                                    <CustomsDutyCalculator
                                                        onResultsChange={setLeftCustomsResults}
                                                        savedConfig={savedConfigToLoad}
                                                    />
                                                    <CustomsDutyCalculator
                                                        onResultsChange={setRightCustomsResults}
                                                        savedConfig={savedConfigToLoad}
                                                    />
                                                </div>
                                            ) : (
                                                <CustomsDutyCalculator onResultsChange={setCustomsResults}
                                                                       savedConfig={savedConfigToLoad}/>
                                            )}
                                        </CardContent>
                                    </Card>

                                    {/* Shipping Section */}
                                    <Card>
                                        <CardHeader>
                                            <CardTitle className="flex items-center gap-2">
                                                <Ship className="h-5 w-5"/>
                                                Shipping Cost Calculator
                                            </CardTitle>
                                        </CardHeader>
                                        <CardContent>
                                            <ShippingCalculator onResultsChange={setShippingResults}/>
                                        </CardContent>
                                    </Card>

                                    {/* Total Landed Cost Section */}
                                    <Card>
                                        <CardHeader>
                                            <CardTitle className="flex items-center gap-2">
                                                <DollarSign className="h-5 w-5"/>
                                                Total Landed Cost
                                            </CardTitle>
                                        </CardHeader>
                                        <CardContent>
                                            <TotalLandedCostCalculator
                                                customsResults={displayedCustomsResults}
                                                shippingResults={shippingResults}
                                            />
                                        </CardContent>
                                    </Card>
                                </div>
                            </TabsContent>

                            <TabsContent value="database" className="mt-6">
                                <TariffRateDatabase/>
                            </TabsContent>
                        </Tabs>

                        {/* Quick Tips */}
                        <Card className="mt-8">
                            <CardHeader>
                                <CardTitle className="flex items-center gap-2">
                                    <TrendingUp className="h-5 w-5"/>
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
                                Always consult official customs authorities for accurate, up-to-date rates and
                                regulations.
                            </p>
                        </div>
                    </div>
                </div>

                {/* Prompt dialog shown when user is not logged in and clicks My Saved Products */}
                <Dialog open={showLoginPrompt} onOpenChange={setShowLoginPrompt}>
                    <DialogContent>
                        <div className="p-4">
                            <h3 className="text-lg font-medium mb-2">Login required</h3>
                            <p className="text-sm text-muted-foreground mb-4">Login to view your saved products</p>
                            <div className="flex justify-end gap-2">
                                <Button variant="outline" onClick={() => setShowLoginPrompt(false)}>Cancel</Button>
                                <Button onClick={() => {
                                    setShowLoginPrompt(false);
                                    setShowLogin(true);
                                }}>Login</Button>
                            </div>
                        </div>
                    </DialogContent>
                </Dialog>
            </div>

            {/* AI Chat Assistant */}
            <AIChat/>

            {/* REQUIRED FOR TOAST NOTIFICATIONS */}
            <Toaster richColors position="top-right" closeButton />
        </>
    );
}