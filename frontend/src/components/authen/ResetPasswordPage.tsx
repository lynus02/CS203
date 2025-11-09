import * as React from 'react';
import { useState } from 'react';
import { ArrowLeft, Globe } from 'lucide-react';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import ThemeToggle from '../togglethemebutton/ThemeToggle';
import api from '../../services/api';

interface ResetPasswordPageProps {
    onBack?: () => void;
    onSuccess?: () => void;
}

const ResetPasswordPage: React.FC<ResetPasswordPageProps> = ({ onBack, onSuccess }) => {
    const [oldPassword, setOldPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setMessage(null);

        if (!oldPassword || !newPassword) {
            setMessage('Please fill both fields');
            return;
        }

        setLoading(true);
        try {
            const payload = { oldPassword, newPassword };
            const res = await api.post('/users/change-password', payload);
            setMessage(res.data?.message || 'Password changed successfully');
            setOldPassword('');
            setNewPassword('');
            onSuccess && onSuccess();
        } catch (err: any) {
            console.error('Reset password error', err);
            setMessage(err.response?.data?.message || 'Failed to reset password');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-background flex flex-col w-full">
            {/* Top Navigation Bar */}
            <div className="border-b border-border ">
                <div className="border-b border-gray-250 px-6 py-8 bg-primary">
                    <div className="flex items-center justify-between gap-4">
                        <div className="px-8 py-8 flex items-center gap-4">
                            <a href="/" className="flex items-center gap-2 hover:opacity-80">
                                <Globe className="h-6 w-6 text-white" />
                                <span className="text-xl font-medium text-white">FoodTariff Pro</span>
                            </a>
                            <ThemeToggle />
                        </div>
                    </div>
                </div>
            </div>

            <div className="flex flex-1 items-center justify-center py-8">
                <Card className="w-full max-w-md shadow-xl">
                    <CardHeader>
                        <div className="flex items-center gap-3 mb-4">
                            {onBack && (
                                <Button variant="ghost" size="lg" onClick={onBack} className="p-2">
                                    <ArrowLeft className="h-6 w-6" />
                                </Button>
                            )}
                            <CardTitle style={{ fontSize: '22px' }} className="font-bold">Reset Password</CardTitle>
                        </div>
                        <p className="text-lg text-muted-foreground">Enter your current password and your new password.</p>
                    </CardHeader>

                    <CardContent>
                        <form onSubmit={handleSubmit} className="space-y-4">
                            {message && <div className="p-3 bg-gray-50 text-sm text-center">{message}</div>}

                            <div>
                                <label className="block text-sm font-medium">Current Password</label>
                                <Input type="password" value={oldPassword} onChange={e => setOldPassword(e.target.value)} />
                            </div>

                            <div>
                                <label className="block text-sm font-medium">New Password</label>
                                <Input type="password" value={newPassword} onChange={e => setNewPassword(e.target.value)} />
                            </div>

                            <div className="flex justify-end gap-2">
                                <Button variant="ghost" onClick={onBack}>Cancel</Button>
                                <Button type="submit" disabled={loading}>{loading ? 'Saving...' : 'Change Password'}</Button>
                            </div>
                        </form>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
};

export default ResetPasswordPage;

