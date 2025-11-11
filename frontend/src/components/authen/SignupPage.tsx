import * as React from 'react';
import { useState } from 'react';
import {Eye, EyeOff, User, Mail, ArrowLeft, Globe, Lock} from 'lucide-react';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import {Card, CardContent, CardTitle, CardHeader} from "../ui/card";
import ThemeToggle from "../togglethemebutton/ThemeToggle";
import api from '../../services/api';

// TypeScript interfaces
interface SignupFormData {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    confirmPassword: string;
}

interface SignupErrors {
    firstName?: string;
    lastName?: string;
    email?: string;
    password?: string;
    confirmPassword?: string;
    submit?: string;
}

interface UserData {
    userId: string;
    firstName: string;
    lastName: string;
    email: string;
    avatarUrl?: string;
    isActive?: boolean;
    createdAt?: string;
    updatedAt?: string;
}

interface SignupPageProps {
    onSignup?: (userData: UserData) => void;
    onBack?: () => void;
    onLogin: () => void;
}

const SignupPage: React.FC<SignupPageProps> = ({ onSignup, onBack, onLogin }) => {
    const [formData, setFormData] = useState<SignupFormData>({
        firstName: '',
        lastName: '',
        email: '',
        password: '',
        confirmPassword: ''
    });
    const [showPassword, setShowPassword] = useState<boolean>(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState<boolean>(false);
    const [errors, setErrors] = useState<SignupErrors>({});
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [signupSuccess, setSignupSuccess] = useState<boolean>(false);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
        if (errors[name as keyof SignupErrors]) {
            setErrors(prev => ({
                ...prev,
                [name]: ''
            }));
        }
    };

    const validatePasswordStrength = (password: string): string[] => {
        const requirements = [];

        // Check length (backend requires min 8, but also has max 25 in Size annotation)
        if (password.length < 8) {
            requirements.push('at least 8 characters');
        }
        if (password.length > 25) {
            requirements.push('no more than 25 characters');
        }

        // Check character requirements (matches backend @Pattern)
        if (!/(?=.*[a-z])/.test(password)) requirements.push('one lowercase letter');
        if (!/(?=.*[A-Z])/.test(password)) requirements.push('one uppercase letter');
        if (!/(?=.*\d)/.test(password)) requirements.push('one number');
        if (!/(?=.*[@$!%*?&])/.test(password)) requirements.push('one special character (@$!%*?&)');

        return requirements;
    };

    const validateForm = (): SignupErrors => {
        const newErrors: SignupErrors = {};

        // First Name validation (matches backend: @NotBlank + @Size(max = 255))
        if (!formData.firstName.trim()) {
            newErrors.firstName = 'First name is required';
        } else if (formData.firstName.trim().length < 2) {
            newErrors.firstName = 'First name must be at least 2 characters';
        } else if (formData.firstName.trim().length > 255) {
            newErrors.firstName = 'First name must be less than 255 characters';
        }

        // Last Name validation (matches backend: @NotBlank + @Size(max = 255))
        if (!formData.lastName.trim()) {
            newErrors.lastName = 'Last name is required';
        } else if (formData.lastName.trim().length < 2) {
            newErrors.lastName = 'Last name must be at least 2 characters';
        } else if (formData.lastName.trim().length > 255) {
            newErrors.lastName = 'Last name must be less than 255 characters';
        }

        // Email validation (matches backend: @NotBlank + @Email + @Lowercase)
        if (!formData.email.trim()) {
            newErrors.email = 'Email is required';
        } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
            newErrors.email = 'Please enter a valid email address';
        } else if (formData.email !== formData.email.toLowerCase()) {
            newErrors.email = 'Email must be lowercase';
        }

        // Password validation (matches backend: @NotBlank + @Size(min = 6, max = 25) + @Pattern)
        if (!formData.password) {
            newErrors.password = 'Password is required';
        } else {
            const missingRequirements = validatePasswordStrength(formData.password);
            if (missingRequirements.length > 0) {
                newErrors.password = `Password must contain: ${missingRequirements.join(', ')}`;
            }
        }

        // Frontend-only confirm password validation
        if (!formData.confirmPassword) {
            newErrors.confirmPassword = 'Please confirm your password';
        } else if (formData.password !== formData.confirmPassword) {
            newErrors.confirmPassword = 'Passwords do not match';
        }

        return newErrors;
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        const newErrors = validateForm();
        if (Object.keys(newErrors).length > 0) {
            setErrors(newErrors);
            return;
        }

        setIsLoading(true);
        setErrors({});

        try {
            // Send signup request to backend
            const response = await api.post('/users', {
                firstName: formData.firstName.trim(),
                lastName: formData.lastName.trim(),
                email: formData.email.toLowerCase().trim(),
                password: formData.password
            });

            const userData = response.data;
            console.log('Signup successful:', userData);

            setSignupSuccess(true);

            onSignup && onSignup(userData);

        } catch (error : any) {
            const errorMessage = error.response?.data?.message ||
                error.response?.data?.error ||
                'Signup failed. Please try again.';

            // Handle specific error cases
            if (error.response?.status === 409) {
                setErrors({ email: 'An account with this email already exists' });
            } else if (error.response?.status === 400) {
                setErrors({ submit: 'Invalid data provided. Please check your information.' });
            } else {
                setErrors({ submit: errorMessage });
            }
        } finally {
            setIsLoading(false);
        }
    };

    // If signup was successful, show success message
    if (signupSuccess) {
        return (
            <div className="flex justify-start min-h-screen bg-background">
                <div className="min-h-screen bg-background flex flex-col w-full">
                    {/* Header with logo and theme toggle */}
                    <div className="px-8 py-8 flex items-center gap-4">
                        <a href="/" className="flex items-center gap-2 hover:opacity-80">
                            <Globe className="h-6 w-6 text-foreground" />
                            <span className="text-xl font-medium text-foreground">FoodTariff Pro</span>
                        </a>
                        <ThemeToggle />
                    </div>

                    {/* Success Message */}
                    <div className="flex flex-1 items-center justify-center">
                        <Card className="text-center py-12 w-[500px] shadow-xl">
                            <CardContent>
                                <div className="text-green-500 mb-4">
                                    <svg className="w-16 h-16 mx-auto" fill="currentColor" viewBox="0 0 20 20">
                                        <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                                    </svg>
                                </div>
                                <h2 className="text-2xl font-bold mb-4">Account Created Successfully!</h2>
                                <p className="text-lg text-muted-foreground mb-6">
                                    Your account has been created. Redirecting you to login...
                                </p>
                                <div className="flex justify-center">
                                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
                                </div>
                            </CardContent>
                        </Card>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-background flex flex-col w-full">
            {/* Top Navigation Bar */}
            <div className="border-b border-border ">
                <div className="border-b border-gray-250 px-6 py-8 bg-primary">
                    <div className="flex items-center justify-between gap-4">
                        {/* Header with logo and theme toggle */}
                        <div className="px-8 py-8 flex items-center gap-4">
                            <a href="/" className="flex items-center gap-2 hover:opacity-80">
                                <Globe className="h-6 w-6 text-foreground" />
                                <span className="text-xl font-medium text-foreground">FoodTariff Pro</span>
                            </a>
                            <ThemeToggle />
                        </div>
                    </div>
                </div>

                {/* Centered Signup Box */}
                <div className="flex flex-1 items-center justify-center py-8">
                    {/* Increased max width per user request */}
                    <Card className="w-full max-w-2xl shadow-xl">
                        <CardHeader className="pb-4">
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
                                <CardTitle style={{ fontSize: '25px' }} className="font-bold">Register Now</CardTitle>
                            </div>
                            <p className="text-lg text-muted-foreground">
                                Create your account to get started
                            </p>
                        </CardHeader>

                        <div className="flex-1 flex flex-col justify-center">
                            <CardContent className="flex-1 flex flex-col justify-center">
                                <form onSubmit={handleSubmit} className="space-y-6">
                                    {errors.submit && (
                                        <div className="p-4 text-base text-red-600 bg-red-50 border border-red-200 rounded-md">
                                            {errors.submit}
                                        </div>
                                    )}
                                    {/* First Name and Last Name Row */}
                                    <div className="grid grid-cols-2 gap-4">
                                        <div className="space-y-3">
                                            {/* Right-aligned label as requested */}
                                            <label htmlFor="firstName" className="text-base font-medium text-right block">
                                                First Name
                                            </label>
                                            <div className="relative flex items-center">
                                                <User
                                                    className="absolute text-muted-foreground h-5 w-5"
                                                    style={{ left: '12px' }} />
                                                <Input
                                                    id="firstName"
                                                    name="firstName"
                                                    type="text"
                                                    value={formData.firstName}
                                                    onChange={handleChange}
                                                    style={{ paddingLeft: '3rem' }}
                                                    className={`pr-4 py-3 text-base h-12 ${errors.firstName ? 'border-red-500' : ''}`}
                                                    placeholder="First Name"
                                                    disabled={isLoading}
                                                />
                                            </div>
                                            {errors.firstName && (
                                                <p className="text-base text-red-600">{errors.firstName}</p>
                                            )}
                                        </div>
                                        <div className="space-y-3">
                                            <label htmlFor="lastName" className="text-base font-medium text-right block">
                                                Last Name
                                            </label>
                                            <div className="relative flex items-center">
                                                <User
                                                    className="absolute text-muted-foreground h-5 w-5"
                                                    style={{ left: '12px' }} />
                                                <Input
                                                    id="lastName"
                                                    name="lastName"
                                                    type="text"
                                                    value={formData.lastName}
                                                    onChange={handleChange}
                                                    style={{ paddingLeft: '3rem' }}
                                                    className={`pr-4 py-3 text-base h-12 ${errors.lastName ? 'border-red-500' : ''}`}
                                                    placeholder="Last name"
                                                    disabled={isLoading}
                                                />
                                            </div>
                                            {errors.lastName && (
                                                <p className="text-base text-red-600">{errors.lastName}</p>
                                            )}
                                        </div>
                                    </div>
                                    {/* Email Field */}
                                    <div className="space-y-3">
                                        <label htmlFor="email" className="text-base font-medium">
                                            Email Address
                                        </label>
                                        <div className="relative flex items-center">
                                            <Mail
                                                className="absolute text-muted-foreground h-5 w-5"
                                                style={{ left: '12px' }}
                                            />
                                            <Input
                                                id="email"
                                                name="email"
                                                type="email"
                                                value={formData.email}
                                                onChange={handleChange}
                                                style={{ paddingLeft: '3rem' }}
                                                className={`pr-4 py-3 text-base h-12 ${errors.email ? 'border-red-500' : ''}`}
                                                placeholder="Enter your email"
                                                disabled={isLoading}
                                            />
                                        </div>
                                        {errors.email && (
                                            <p className="text-base text-red-600">{errors.email}</p>
                                        )}
                                    </div>
                                    {/* Password Field */}
                                    <div className="space-y-3">
                                        <label htmlFor="password" className="text-base font-medium">
                                            Password
                                        </label>
                                        <div className="relative flex items-center">
                                            <Lock
                                                className="absolute text-muted-foreground h-5 w-5"
                                                style={{ left: '12px' }} />

                                            <Input
                                                id="password"
                                                name="password"
                                                type={showPassword ? 'text' : 'password'}
                                                value={formData.password}
                                                onChange={handleChange}
                                                style={{ paddingLeft: '3rem' }}
                                                className={`pr-4 py-3 text-base h-12 ${errors.password ? 'border-red-500' : ''}`}
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
                                    {/* Confirm Password Field */}
                                    <div className="space-y-3">
                                        <label htmlFor="confirmPassword" className="text-base font-medium">
                                            Confirm Password
                                        </label>
                                        <div className="relative flex items-center">
                                            <Lock
                                                className="absolute text-muted-foreground h-5 w-5"
                                                style={{ left: '12px' }} />

                                            <Input
                                                id="confirmPassword"
                                                name="confirmPassword"
                                                type={showConfirmPassword ? 'text' : 'password'}
                                                value={formData.confirmPassword}
                                                onChange={handleChange}
                                                style={{ paddingLeft: '3rem' }}
                                                className={`pr-4 py-3 text-base h-12 ${errors.confirmPassword ? 'border-red-500' : ''}`}
                                                placeholder="Re-enter your password"
                                                disabled={isLoading}
                                            />
                                            <button
                                                type="button"
                                                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                                className="absolute right-4 top-1/2 transform -translate-y-1/2 text-muted-foreground hover:text-foreground"
                                                disabled={isLoading}
                                            >
                                                {showConfirmPassword ? (
                                                    <EyeOff className="h-5 w-5" />
                                                ) : (
                                                    <Eye className="h-5 w-5" />
                                                )}
                                            </button>
                                        </div>
                                        {errors.confirmPassword && (
                                            <p className="text-base text-red-600">{errors.confirmPassword}</p>
                                        )}
                                    </div>
                                    {/* Password Requirements */}
                                    <div className="text-sm text-gray-600 bg-gray-50 p-3 rounded-md">
                                        <p className="mb-2 font-medium">Password requirements:</p>
                                        <ul className="list-disc list-inside space-y-1 text-xs">
                                            <li>At least 8 characters</li>
                                            <li>One uppercase letter</li>
                                            <li>One lowercase letter</li>
                                            <li>One number</li>
                                            <li>One special character(@$!%*?&)</li>

                                        </ul>
                                    </div>
                                    <Button
                                        type="submit"
                                        className="w-full h-12 text-base font-medium mt-8"
                                        disabled={isLoading}
                                    >
                                        {isLoading ? 'Creating Account...' : 'Sign Up'}
                                    </Button>
                                </form>
                                {onLogin && (
                                    <div className="mt-8 text-center">
                                        <p className="text-base text-gray-600 font-sans">
                                            Already have an account?{' '}
                                            <button
                                                onClick={onLogin}
                                                className="font-medium text-black hover:underline transition-colors text-base font-sans"
                                                disabled={isLoading}
                                            >
                                                Sign in here
                                            </button>
                                        </p>
                                    </div>
                                )}
                            </CardContent>
                        </div>
                    </Card>
                </div>
            </div>
        </div>
    );
};

export default SignupPage;
