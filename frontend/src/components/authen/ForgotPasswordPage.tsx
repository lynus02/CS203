// ForgotPasswordPage.tsx
import * as React from "react";
import { useState } from "react";
import { Mail, ArrowLeft, Globe } from 'lucide-react';
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import ThemeToggle from "../togglethemebutton/ThemeToggle";
import api from "../../services/api";

// TypeScript interfaces
interface ForgotPasswordFormData {
    email: string;
    verificationCode: string;
    newPassword: string;
    confirmPassword: string;
}

interface ForgotPasswordErrors {
    email?: string;
    verificationCode?: string;
    newPassword?: string;
    confirmPassword?: string;
    submit?: string;
}

interface ForgotPasswordPageProps {
    onBack?: () => void;
}

const ForgotPasswordPage: React.FC<ForgotPasswordPageProps> = ({ onBack }) => {
    const [formData, setFormData] = useState<ForgotPasswordFormData>({
        email: '',
        verificationCode: '',
        newPassword: '',
        confirmPassword: ''
    });
    const [errors, setErrors] = useState<ForgotPasswordErrors>({});
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [step, setStep] = useState<'email' | 'verify' | 'success'>('email');

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
        if (errors[name as keyof ForgotPasswordErrors]) {
            setErrors(prev => ({
                ...prev,
                [name]: ''
            }));
        }
    };

    const validateForm = (): boolean => {
        const newErrors: ForgotPasswordErrors = {};

        if (step === 'email') {
            if (!formData.email) {
                newErrors.email = 'Email is required';
            } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
                newErrors.email = 'Please enter a valid email address';
            }
        } else if (step === 'verify') {
            if (!formData.verificationCode) {
                newErrors.verificationCode = 'Verification code is required';
            } else if (formData.verificationCode.length !== 6) {
                newErrors.verificationCode = 'Verification code must be 6 digits';
            }

            if (!formData.newPassword) {
                newErrors.newPassword = 'New password is required';
            } else if (formData.newPassword.length < 8) {
                newErrors.newPassword = 'Password must be at least 8 characters';
            }

            if (!formData.confirmPassword) {
                newErrors.confirmPassword = 'Please confirm your password';
            } else if (formData.newPassword !== formData.confirmPassword) {
                newErrors.confirmPassword = 'Passwords do not match';
            }
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
            if (step === 'email') {
                // Send verification code to email
                await api.post('/auth/forgot-password', {
                    email: formData.email
                });
                setStep('verify');
            } else if (step === 'verify') {
                // Verify code and reset password
                await api.post('/auth/reset-password', {
                    email: formData.email,
                    verificationCode: formData.verificationCode,
                    newPassword: formData.newPassword
                });
                setStep('success');
            }
        } catch (error: any) {
            console.error('Forgot password error:', error);
            if (step === 'email') {
                setErrors({
                    submit: 'Failed to send verification code. Please try again.'
                });
            } else {
                setErrors({
                    submit: 'Invalid verification code or password reset failed. Please try again.'
                });
            }
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="flex justify-start min-h-screen bg-background">
            <div className="min-h-screen bg-background flex flex-col w-full">
                {/* Header with logo */}
                <div className="w-full border-b border-gray-300 px-10 py-8 bg-primary">
                    <div className="flex items-center gap-4">
                        <a href="/" className="flex items-center gap-2 hover:opacity-80">
                            <Globe className="h-6 w-6 text-white" />
                            <span className="text-xl font-medium text-white">FoodTariff Pro</span>
                        </a>
                        <ThemeToggle />
                    </div>
                </div>

                {/* Centered Forgot Password Box */}
                <div className="flex flex-1 items-center justify-center py-8">
                    <Card className="w-full max-w-2xl shadow-xl">
                        <CardHeader className="pb-4">
                            <div className="flex items-center gap-3 mb-4">
                                {onBack && step === 'email' && (
                                    <Button
                                        variant="ghost"
                                        size="lg"
                                        onClick={onBack}
                                        className="p-2"
                                    >
                                        <ArrowLeft className="h-6 w-6" />
                                    </Button>
                                )}
                                {step === 'verify' && (
                                    <Button
                                        variant="ghost"
                                        size="lg"
                                        onClick={() => setStep('email')}
                                        className="p-2"
                                    >
                                        <ArrowLeft className="h-6 w-6" />
                                    </Button>
                                )}
                                <CardTitle style={{ fontSize: '25px' }} className="font-bold">
                                    {step === 'success' ? 'Password Reset Successful' : 'Forgot Password'}
                                </CardTitle>
                            </div>
                            <p className="text-lg text-muted-foreground">
                                {step === 'email' && 'Enter your email to receive a verification code'}
                                {step === 'verify' && 'Check your email for the verification code sent'}
                                {step === 'success' && 'Your password has been successfully reset'}
                            </p>
                        </CardHeader>

                        <CardContent className="pt-2 pb-6">
                            {step === 'success' ? (
                                <div className="space-y-6">
                                    <div className="p-4 text-base text-green-700 bg-green-50 border border-green-200 rounded-md">
                                        Your password has been successfully reset. You can now log in with your new password.
                                    </div>
                                    <Button
                                        onClick={onBack}
                                        className="w-full h-12 text-base font-medium"
                                    >
                                        Back to Login
                                    </Button>
                                </div>
                            ) : (
                                <form onSubmit={handleSubmit} className="space-y-8">
                                    {errors.submit && (
                                        <div className="p-4 text-base text-red-600 bg-red-50 border border-red-200 rounded-md">
                                            {errors.submit}
                                        </div>
                                    )}

                                    {step === 'email' && (
                                        <div className="space-y-2">
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
                                    )}

                                    {step === 'verify' && (
                                        <>
                                            <div className="p-4 text-base text-blue-700 bg-blue-50 border border-blue-200 rounded-md">
                                                A verification code has been sent to <strong>{formData.email}</strong>. Please check your email.
                                            </div>

                                            <div className="space-y-2">
                                                <label htmlFor="verificationCode" className="text-base font-medium">
                                                    Verification Code
                                                </label>
                                                <Input
                                                    id="verificationCode"
                                                    name="verificationCode"
                                                    type="text"
                                                    value={formData.verificationCode}
                                                    onChange={handleChange}
                                                    maxLength={6}
                                                    className={`py-3 text-base h-12 text-center text-xl tracking-widest ${errors.verificationCode ? 'border-red-500' : ''}`}
                                                    placeholder="000000"
                                                    disabled={isLoading}
                                                />
                                                {errors.verificationCode && (
                                                    <p className="text-base text-red-600">{errors.verificationCode}</p>
                                                )}
                                            </div>

                                            <div className="space-y-2">
                                                <label htmlFor="newPassword" className="text-base font-medium">
                                                    New Password
                                                </label>
                                                <Input
                                                    id="newPassword"
                                                    name="newPassword"
                                                    type="password"
                                                    value={formData.newPassword}
                                                    onChange={handleChange}
                                                    className={`py-3 text-base h-12 ${errors.newPassword ? 'border-red-500' : ''}`}
                                                    placeholder="Enter new password"
                                                    disabled={isLoading}
                                                />
                                                {errors.newPassword && (
                                                    <p className="text-base text-red-600">{errors.newPassword}</p>
                                                )}
                                            </div>

                                            <div className="space-y-2">
                                                <label htmlFor="confirmPassword" className="text-base font-medium">
                                                    Confirm Password
                                                </label>
                                                <Input
                                                    id="confirmPassword"
                                                    name="confirmPassword"
                                                    type="password"
                                                    value={formData.confirmPassword}
                                                    onChange={handleChange}
                                                    className={`py-3 text-base h-12 ${errors.confirmPassword ? 'border-red-500' : ''}`}
                                                    placeholder="Confirm new password"
                                                    disabled={isLoading}
                                                />
                                                {errors.confirmPassword && (
                                                    <p className="text-base text-red-600">{errors.confirmPassword}</p>
                                                )}
                                            </div>
                                        </>
                                    )}

                                    <Button
                                        type="submit"
                                        className="w-full h-12 text-base font-medium mt-6"
                                        disabled={isLoading}
                                    >
                                        {isLoading
                                            ? (step === 'email' ? 'Sending...' : 'Resetting Password...')
                                            : (step === 'email' ? 'Send Verification Code' : 'Reset Password')
                                        }
                                    </Button>

                                    <div className="mt-6 text-center">
                                        <p className="text-base text-gray-600 font-sans">
                                            Remember your password?{' '}
                                            <button
                                                type="button"
                                                onClick={onBack}
                                                className="font-medium text-black hover:underline transition-colors text-base font-sans"
                                                disabled={isLoading}
                                            >
                                                Back to Login
                                            </button>
                                        </p>
                                    </div>
                                </form>
                            )}
                        </CardContent>
                    </Card>
                </div>
            </div>
        </div>
    );
};

export default ForgotPasswordPage;