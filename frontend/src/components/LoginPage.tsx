// LoginPage.tsx
import { useState } from "react";
import {Eye, EyeOff, Lock, Mail, ArrowLeft, Globe} from 'lucide-react';
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "./ui/card";


// TypeScript interfaces
interface LoginFormData {
    email: string;
    password: string;
}

interface LoginErrors {
    email?: string;
    password?: string;
    submit?: string;
}

interface UserData {
    id: string;
    name?: string;
    email: string;
    token: string;
    role?: string;
}

interface LoginPageProps {
    onLogin: (userData: UserData) => void;
    onBack?: () => void;
    onSignUp?: () => void;
}

const LoginPage: React.FC<LoginPageProps> = ({ onLogin, onBack, onSignUp }) => {
    const [formData, setFormData] = useState<LoginFormData>({
        email: '',
        password: ''
    });
    const [showPassword, setShowPassword] = useState<boolean>(false);
    const [errors, setErrors] = useState<LoginErrors>({});
    const [isLoading, setIsLoading] = useState<boolean>(false);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
        // Clear error when user starts typing
        if (errors[name as keyof LoginErrors]) {
            setErrors(prev => ({
                ...prev,
                [name]: ''
            }));
        }
    };

    const validateForm = (): boolean => {
        const newErrors: LoginErrors = {};

        if (!formData.email) {
            newErrors.email = 'Email is required';
        } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
            newErrors.email = 'Please enter a valid email address';
        }

        if (!formData.password) {
            newErrors.password = 'Password is required';
        } else if (formData.password.length < 6) {
            newErrors.password = 'Password must be at least 6 characters';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!validateForm()) {
            return;
        }

        setIsLoading(true);
        setErrors({});

        try {
            // Simulate API call for demo purposes
            await new Promise(resolve => setTimeout(resolve, 1000));

            // Mock successful login - replace with actual API call
            const userData: UserData = {
                id: '1',
                name: 'Demo User',
                email: formData.email,
                token: 'mock-jwt-token',
                role: 'user'
            };

            onLogin(userData);
        } catch (error) {
            setErrors({
                submit: 'Login failed. Please check your credentials and try again.'
            });
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="mt-[700px] w-[400px] h-[600px] justify-center bg-gray-50">
            <Card >
                <CardHeader>
                    {/* Logo and Brand */}
                    <div className="flex items-center justify-between pb-6 border-b border-gray-300 pb-2">
                        {/* Logo and Brand */}
                        <a href="/" className="flex items-center gap-2 hover:opacity-80">
                        <Globe className="h-6 w-6 text-primary" />
                        <span className="text-xl font-medium">FoodTariff Pro</span>
                        </a>
                    </div>
                    <div className="flex items-center gap-3 mb-4">
                        {onBack && (
                            <Button
                                variant="ghost"
                                size="lg"
                                onClick={onBack}
                                className="p-2"
                            >
                                <ArrowLeft className="h-6 w-6" />
                            </Button>
                        )}
                        <CardTitle className="text-[40px] font-bold">Login</CardTitle>
                    </div>
                    <p className="text-lg text-muted-foreground">
                        Enter your credentials to access your account
                    </p>
                </CardHeader>
                <CardContent className="flex-1 flex flex-col justify-center">
                    <form onSubmit={handleSubmit} className="space-y-6">
                        {errors.submit && (
                            <div className="p-4 text-base text-red-600 bg-red-50 border border-red-200 rounded-md">
                                {errors.submit}
                            </div>
                        )}

                        <div className="space-y-3">
                            <label htmlFor="email" className="text-base font-medium">
                                Email Address
                            </label>
                            <div className="relative">
                                <Mail className="absolute left-4 top-1/2 transform -translate-y-1/2 text-muted-foreground h-5 w-5" />
                                <Input
                                    id="email"
                                    name="email"
                                    type="email"
                                    value={formData.email}
                                    onChange={handleChange}
                                    className={`pl-12 pr-4 py-3 text-base h-12 ${errors.email ? 'border-red-500' : ''}`}
                                    placeholder="Enter your email"
                                    disabled={isLoading}
                                />
                            </div>
                            {errors.email && (
                                <p className="text-base text-red-600">{errors.email}</p>
                            )}
                        </div>

                        <div className="space-y-3">
                            <label htmlFor="password" className="text-base font-medium">
                                Password
                            </label>
                            <div className="relative">
                                <Lock className="absolute left-4 top-1/2 transform -translate-y-1/2 text-muted-foreground h-5 w-5" />
                                <Input
                                    id="password"
                                    name="password"
                                    type={showPassword ? 'text' : 'password'}
                                    value={formData.password}
                                    onChange={handleChange}
                                    className={`pl-12 pr-12 py-3 text-base h-12 ${errors.password ? 'border-red-500' : ''}`}
                                    placeholder="Enter your password"
                                    disabled={isLoading}
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    className="absolute right-4 top-1/2 transform -translate-y-1/2 text-muted-foreground hover:text-foreground"
                                    disabled={isLoading}
                                >
                                    {showPassword ? (
                                        <EyeOff className="h-5 w-5" />
                                    ) : (
                                        <Eye className="h-5 w-5" />
                                    )}
                                </button>
                            </div>
                            {errors.password && (
                                <p className="text-base text-red-600">{errors.password}</p>
                            )}
                        </div>

                        <Button
                            type="submit"
                            className="w-full h-12 text-base font-medium mt-8"
                            disabled={isLoading}
                        >
                            {isLoading ? 'Signing In...' : 'Log In'}
                        </Button>
                    </form>

                    {onSignUp && (
                        <div className="mt-8 text-center">
                            <p className="text-base text-muted-foreground">
                                Don't have an account?{' '}
                                <button
                                    onClick={onSignUp}
                                    className="font-medium text-primary hover:underline text-base"
                                    disabled={isLoading}
                                >
                                    Sign up here
                                </button>
                            </p>
                        </div>
                    )}
                </CardContent>
            </Card>
        </div>
    );
};

export default LoginPage;