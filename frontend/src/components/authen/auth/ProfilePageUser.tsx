// ProfilePageUser.tsx
import * as React from "react";
import { useEffect, useState } from "react";
import { ArrowLeft, Globe, Edit2, Trash2, LogOut, RefreshCcw, HardDrive } from 'lucide-react';
import { Button } from "../../ui/button";
import { Input } from "../../ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "../../ui/card";
import ThemeToggle from "../../togglethemebutton/ThemeToggle";
import api from "../../../services/api";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "../../ui/dialog";
import { useSavedProducts } from "../../context/SavedProductsContext";

interface ProfilePageProps {
    onBack?: () => void;
    onLogout: () => void;
    onReset?: () => void;
}

interface UserProfileDto {
    userId: string;
    firstName?: string;
    lastName?: string;
    email: string;
    createdAt?: string; // ISO date string
}

const ProfilePageUser: React.FC<ProfilePageProps> = ({ onBack, onLogout, onReset }) => {
    const [profile, setProfile] = useState<UserProfileDto | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    // Edit profile modal state
    const [isEditOpen, setIsEditOpen] = useState(false);
    const [editFirstName, setEditFirstName] = useState('');
    const [editLastName, setEditLastName] = useState('');
    const [editEmail, setEditEmail] = useState('');
    const [editLoading, setEditLoading] = useState(false);

    // Saved products (reuse context)
    const { savedProducts, fetchSavedProducts, removeSavedProduct, isLoading } = useSavedProducts();
    const [isSavedDialogOpen, setIsSavedDialogOpen] = useState(false);

    useEffect(() => {
        loadProfile();
    }, []);

    const loadProfile = async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await api.get('/users/profile');
            setProfile(res.data);
            setEditFirstName(res.data.firstName || '');
            setEditLastName(res.data.lastName || '');
            setEditEmail(res.data.email || '');
            // fetch saved products for this user (if provider uses userId)
            if (res.data.userId) {
                // saved products context expects numeric id in some places; it's implementation-specific — we'll still call it safely
                // try to call fetchSavedProducts with string id if API expects string ID in backend, context handles number; protect with try/catch
                try {
                    // @ts-ignore
                    await fetchSavedProducts(res.data.userId);
                } catch (e) {
                    // ignore fetch saved products error here
                }
            }
        } catch (err: any) {
            console.error('Failed to load profile', err);
            setError(err.response?.data?.message || 'Failed to load profile');
        } finally {
            setLoading(false);
        }
    };

    const handleSaveEdit = async () => {
        setEditLoading(true);
        try {
            const payload = {
                firstName: editFirstName,
                lastName: editLastName,
                email: editEmail
            };
            const res = await api.put('/users/profile', payload);
            setProfile(res.data);
            setIsEditOpen(false);
        } catch (err: any) {
            console.error('Failed to update profile', err);
            alert(err.response?.data?.message || 'Failed to update profile');
        } finally {
            setEditLoading(false);
        }
    };

    const handleDeleteAccount = async () => {
        if (!confirm('Are you sure you want to permanently delete your account? This action cannot be undone.')) return;
        try {
            await api.delete('/users/profile');
            // after deletion, call onLogout to clear client state and redirect
            onLogout();
        } catch (err: any) {
            console.error('Failed to delete account', err);
            alert(err.response?.data?.message || 'Failed to delete account');
        }
    };

    const handleLogoutClick = () => {
        onLogout();
    };

    // Handle reset password click: prefer parent navigation (onReset), else fallback to location change
    const handleResetClick = () => {
        console.log('Reset password clicked - onReset present?', typeof onReset === 'function');
        if (typeof onReset === 'function') {
            try {
                onReset();
                // continue and dispatch global event to ensure parent opens reset page
            } catch (e) {
                console.error('onReset threw error', e);
            }
        }
        // Dispatch a global event so the App listener opens the ResetPasswordPage as a fallback/backup
        try {
            const evt = new Event('openResetPassword');
            document.dispatchEvent(evt);
            console.log('Dispatched openResetPassword event');
            return;
        } catch (e) {
            console.error('Dispatching openResetPassword event failed', e);
        }
        // Fallback: navigate to a path that should be handled by the SPA (or server)
        try {
            window.location.href = '/reset-password';
        } catch (e) {
            console.error('Fallback navigation failed', e);
        }
    };

    const formattedDate = (iso?: string) => {
        if (!iso) return '—';
        try {
            const d = new Date(iso);
            return d.toLocaleDateString();
        } catch {
            return iso;
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

            <div className="flex flex-1 flex-col items-stretch justify-start px-8 py-32 gap-16">
                {/* Header row: Back + Title and Edit button */}
                <div className="flex items-center justify-between sm:mt-40">
                    <div className="flex items-center gap-4">
                        {onBack && (
                            <Button variant="ghost" onClick={onBack} className="p-2">
                                <ArrowLeft className="h-6 w-6" />
                            </Button>
                        )}
                        <h2 className="text-2xl font-semibold">Your Profile</h2>
                    </div>

                    <div>
                        <Button variant="ghost" onClick={() => setIsEditOpen(true)} title="Edit Profile" className="flex items-center gap-2">
                            <Edit2 className="h-4 w-4" />
                            <span className="hidden sm:inline">Edit Profile</span>
                        </Button>
                    </div>
                </div>

                {/* Main info section: spans full width, no inner bordered box */}
                <section className="w-full">
                    {loading ? (
                        <p>Loading profile...</p>
                    ) : error ? (
                        <p className="text-red-600">{error}</p>
                    ) : (
                        // add responsive top margin to separate from header and increase spacing between items
                        <div className="w-full text-base mt-12 sm:mt-20 md:mt-40 lg:mt-56 xl:mt-64 2xl:mt-80 space-y-12">
                            <div>
                                <p className="text-xl"><strong>Name:</strong> {profile ? `${profile.firstName} ${profile.lastName || ''}` : '—'}</p>
                            </div>
                            <div>
                                <p className="text-xl"><strong>Email:</strong> {profile?.email || '—'}</p>
                            </div>
                            <div>
                                <p className="text-xl"><strong>Date Joined:</strong> {formattedDate(profile?.createdAt)}</p>
                            </div>
                        </div>
                    )}
                </section>

                {/* Actions row: wider spacing between action buttons */}
                <section className="w-full">
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-start gap-6">
                        <Dialog open={isSavedDialogOpen} onOpenChange={setIsSavedDialogOpen}>
                            <DialogTrigger asChild>
                                <Button variant="outline" className="flex items-center gap-3 px-6 py-3">
                                    <HardDrive className="h-5 w-5" />
                                    <span>Saved Products</span>
                                </Button>
                            </DialogTrigger>
                            <DialogContent className="max-w-3xl max-h-[70vh] overflow-y-auto">
                                <DialogHeader>
                                    <DialogTitle>My Saved Products</DialogTitle>
                                </DialogHeader>
                                <div className="grid gap-6 py-6">
                                    {isLoading ? (
                                        <p className="text-center text-muted-foreground py-8">Loading...</p>
                                    ) : savedProducts && savedProducts.length > 0 ? (
                                        savedProducts.map(p => (
                                            <div key={p.id} className="flex items-center gap-6 p-4 rounded-lg">
                                                {p.image && <img src={p.image} alt={p.name} className="w-28 h-28 object-cover rounded" />}
                                                <div className="flex-1">
                                                    <h3 className="font-semibold text-lg">{p.name}</h3>
                                                    <p className="text-sm text-muted-foreground">HS Code: {p.hsCode}</p>
                                                    <p className="text-sm text-muted-foreground">Category: {p.category}</p>
                                                </div>
                                                <Button variant="outline" size="sm" onClick={() => removeSavedProduct(p.id)}>Remove</Button>
                                            </div>
                                        ))
                                    ) : (
                                        <p className="text-center text-muted-foreground py-8">No saved products yet</p>
                                    )}
                                </div>
                            </DialogContent>
                        </Dialog>

                        <Button variant="outline" onClick={handleResetClick} className="flex items-center gap-3 px-6 py-3">
                            <RefreshCcw className="h-5 w-5" />
                            <span>Reset Password</span>
                        </Button>

                        <Button variant="ghost" onClick={handleLogoutClick} className="flex items-center gap-3 px-6 py-3">
                            <LogOut className="h-5 w-5" />
                            <span>Logout</span>
                        </Button>

                        <Button variant="destructive" onClick={handleDeleteAccount} className="flex items-center gap-3 px-6 py-3">
                            <Trash2 className="h-5 w-5" />
                            <span>Delete Account</span>
                        </Button>
                    </div>
                </section>
            </div>

            {/* Edit Profile Modal */}
            {isEditOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-40">
                    <div className="bg-white dark:bg-slate-900 rounded-lg p-6 w-full max-w-md">
                        <h3 className="text-lg font-semibold mb-4">Edit Profile</h3>
                        <div className="space-y-3">
                            <label className="block">First Name</label>
                            <Input value={editFirstName} onChange={e => setEditFirstName(e.target.value)} />
                            <label className="block">Last Name</label>
                            <Input value={editLastName} onChange={e => setEditLastName(e.target.value)} />
                            <label className="block">Email</label>
                            <Input value={editEmail} onChange={e => setEditEmail(e.target.value)} />
                        </div>
                        <div className="mt-4 flex justify-end gap-2">
                            <Button variant="ghost" onClick={() => setIsEditOpen(false)}>Cancel</Button>
                            <Button onClick={handleSaveEdit} disabled={editLoading}>{editLoading ? 'Saving...' : 'Save'}</Button>
                        </div>
                    </div>
                </div>
            )}

            {/* subtle footer separator */}
            <footer className="w-full border-t border-gray-200 mt-12 pt-6 px-8">
                <div className="max-w-7xl mx-auto text-sm text-muted-foreground">
                    <div className="flex items-center justify-between">
                        <span>© {new Date().getFullYear()} FoodTariff Pro</span>
                        <span className="text-xs">Small estimates only — consult official customs authorities for definitive rates.</span>
                    </div>
                </div>
            </footer>
        </div>
    );
};

export default ProfilePageUser;
