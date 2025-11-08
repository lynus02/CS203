// ========== IMPORTS ========== //
import * as React from "react";
import {useState} from "react";
import {Eye, EyeOff, Lock, Mail, ArrowLeft, Globe} from 'lucide-react';
import {Button} from "./ui/button";
import {Input} from "./ui/input";
import {Card, CardContent, CardHeader, CardTitle} from "./ui/card";
import api from "../services/api";

// ========= TYPES ========== //
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

// ========= MAIN COMPONENT ========== //
const LoginPage: React.FC<LoginPageProps> = ({onLogin, onBack, onSignUp}) => {

    // Form Input States
    const [formData, setFormData] = useState<LoginFormData>({
        email: '',
        password: ''
    });

    // Toggle Password Visibility
    const [showPassword, setShowPassword] = useState<boolean>(false);

    // Handle validation and backend errors
    const [errors, setErrors] = useState<LoginErrors>({});

    // Shows loading indicator while waiting for backend response
    const [isLoading, setIsLoading] = useState<boolean>(false);

    // ========= HANDLER (Update form input) ========== //
    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const {name, value} = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
        if (errors[name as keyof LoginErrors]) {
            setErrors(prev => ({
                ...prev,
                [name]: ''
            }));
        }
    };

    // ========= FORM VALIDATION ========== //
    const validateForm = (): boolean => {
        const newErrors: LoginErrors = {};

        // Email validation
        if (!formData.email) {
            newErrors.email = 'Email is required';
        } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
            newErrors.email = 'Please enter a valid email address';
        }

        // Password validation
        if (!formData.password) {
            newErrors.password = 'Password is required';
        } else if (formData.password.length < 8) {
            newErrors.password = 'Password must be at least 8 characters';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    // ========= FORM SUBMISSION ========== //
    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        // Pass validation before calling backend
        if (!validateForm()) {
            return;
        }

        setIsLoading(true);
        setErrors({});

        try {
            // Send login request to backend
            const response = await api.post('/auth/login', {
                email: formData.email,
                password: formData.password
            });

            const { token } = response.data;
            console.log('Received token:', token);
            // Store token in localStorage
            localStorage.setItem('token', token);

            // Fetch user profile after login
            const userProfile = await api.get('/auth/me');
            const user = userProfile.data;

            const userData: UserData = {
                id: user.id,
                name: user.firstName + ' ' + user.lastName,
                email: user.email,
                token: token,
                role: Array.isArray(user.roles) ? user.roles[0] : user.role
            };

            // Notify parent component of successful login
            onLogin(userData);

        } catch (error: any) {
            console.error('Login error details:', {
                message: error.message,
                response: error.response?.data,
                status: error.response?.status
            });

            setErrors({
                submit: 'Login failed. Please check your credentials and try again.'
            });
        } finally {
            setIsLoading(false);
        }
    };

    // ========= MAIN UI LAYOUT ========== //
    return (
        <div className="flex justify-start min-h-screen bg-white">
            <div className="min-h-screen bg-white flex flex-col w-full">
                {/* Header with logo */}
                <div className="w-full border-b border-gray-300 px-10 py-8 flex items-center bg-primary">
                    <a href="/" className="flex items-center gap-2 hover:opacity-80">
                        <Globe className="h-6 w-6 text-white"/>
                        <span className="text-xl font-medium text-white">FoodTariff Pro</span>
                    </a>
                </div>

                {/* Centered Login Box */}
                <div className="flex flex-1 items-center justify-center py-8">
                    <div className="bg-white rounded-2xl shadow-xl p-8 w-full max-w-2xl">
                        <Card className="h-full">
                            <CardHeader>
                                <div className="flex items-center gap-3 mb-4">
                                    {onBack && (
                                        <Button
                                            variant="ghost"
                                            size="lg"
                                            onClick={onBack}
                                            className="p-2"
                                        >
                                            <ArrowLeft className="h-6 w-6"/>
                                        </Button>
                                    )}
                                    <CardTitle style={{fontSize: '25px'}} className="font-bold">Login</CardTitle>
                                </div>
                                <p className="text-lg text-muted-foreground">
                                    Enter your credentials to access your account
                                </p>
                            </CardHeader>

                            <CardContent className="flex-1 flex flex-col justify-center">
                                <form onSubmit={handleSubmit} className="space-y-6">
                                    {errors.submit && (
                                        <div
                                            className="p-4 text-base text-red-600 bg-red-50 border border-red-200 rounded-md">
                                            {errors.submit}
                                        </div>
                                    )}

                                    <div className="space-y-3">
                                        <label htmlFor="email" className="text-base font-medium">
                                            Email Address
                                        </label>
                                        <div className="relative flex items-center">
                                            <Mail
                                                className="absolute text-muted-foreground h-5 w-5"
                                                style={{left: '12px'}}
                                            />
                                            <Input
                                                id="email"
                                                name="email"
                                                type="email"
                                                value={formData.email}
                                                onChange={handleChange}
                                                style={{paddingLeft: '3rem'}}
                                                className={`pr-4 py-3 text-base h-12 ${errors.email ? 'border-red-500' : ''}`}
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
                                        <div className="relative flex items-center">
                                            <Lock
                                                className="absolute text-muted-foreground h-5 w-5"
                                                style={{left: '12px'}}/>

                                            <Input
                                                id="password"
                                                name="password"
                                                type={showPassword ? 'text' : 'password'}
                                                value={formData.password}
                                                onChange={handleChange}
                                                style={{paddingLeft: '3rem'}}
                                                className={`pr-4 py-3 text-base h-12 ${errors.email ? 'border-red-500' : ''}`}
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
                                                    <EyeOff className="h-5 w-5"/>
                                                ) : (
                                                    <Eye className="h-5 w-5"/>
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
                                        <p className="text-base text-gray-600 font-sans">
                                            Don't have an account?{' '}
                                            <button
                                                onClick={onSignUp}
                                                className="font-medium text-black hover:underline transition-colors text-base font-sans"
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
                </div>
            </div>
        </div>
    );
};

export default LoginPage;