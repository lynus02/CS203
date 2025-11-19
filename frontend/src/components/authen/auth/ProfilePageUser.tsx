// ProfilePageUser.tsx
import * as React from "react";
import { useEffect, useState } from "react";
import {ArrowLeft, Globe, Edit2, Trash2, LogOut, RefreshCcw} from 'lucide-react';
import { Button } from "../../ui/button";
import { Input } from "../../ui/input";
import { Card, CardContent, CardHeader } from "../../ui/card";
import ThemeToggle from "../../togglethemebutton/ThemeToggle";
import api from "../../../services/api";
import {Dialog, DialogContent, DialogHeader, DialogTitle} from "../../ui/dialog";
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "../../ui/alert-dialog";
import MySavedProductsButton from "../../MySavedProductsButton";

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

    // Delete account confirmation dialog state
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [deleteLoading, setDeleteLoading] = useState(false);

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

    // open confirmation dialog (will call performDeleteAccount when user confirms)
    const handleDeleteAccount = () => {
        setIsEditOpen(false);
        setShowDeleteConfirm(true);
    };

    const performDeleteAccount = async () => {
        setDeleteLoading(true);
        try {
            await api.delete('/users/profile');
            // after deletion, call onLogout to clear client state and redirect
            setShowDeleteConfirm(false);

            // Clear state
            onLogout();
            localStorage.removeItem("token");

            // Redirect to homepage
            window.location.href = "/";
        } catch (err: any) {
            console.error('Failed to delete account', err);
            alert(err.response?.data?.message || 'Failed to delete account');
        } finally {
            setDeleteLoading(false);
        }
    };

    const handleLogoutClick = () => {
        try {
            // call parent logout to clear app state
            onLogout();
        } catch (e) {
            console.warn('onLogout threw', e);
        }

        // ensure token is removed locally
        try {
            localStorage.removeItem('token');
        } catch (e) {
            console.warn('Failed to remove token from localStorage', e);
        }

        // navigate to homepage
        try {
            window.location.href = '/';
        } catch (e) {
            console.error('Navigation to homepage failed', e);
        }
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

            <div className="flex flex-1 items-start justify-center px-4 py-12">
                <div className="w-full max-w-4xl">
                    {/* Profile card */}
                    <Card>
                        <CardHeader>
                            <div className="flex items-center justify-between gap-4">
                                <div className="flex items-center gap-4">
                                    {onBack && (
                                        <Button variant="ghost" onClick={onBack} className="p-2">
                                            <ArrowLeft className="h-6 w-6" />
                                        </Button>
                                    )}
                                    <div className="flex items-center gap-4">
                                        {/* Avatar with initials */}
                                        <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary text-white text-xl font-semibold">
                                            {profile ? ((profile.firstName || profile.email || 'U')[0] + (profile.lastName ? profile.lastName[0] : '')).toUpperCase() : 'U'}
                                        </div>
                                        <div>
                                            <h2 className="text-2xl font-semibold">{profile ? `${profile.firstName || ''} ${profile.lastName || ''}`.trim() : '—'}</h2>
                                            <div className="text-sm text-muted-foreground">{profile?.email || '—'}</div>
                                        </div>
                                    </div>
                                </div>

                                <div className="flex items-center gap-2">
                                    <Button variant="ghost" onClick={() => setIsEditOpen(true)} title="Edit Profile" className="flex items-center gap-2">
                                        <Edit2 className="h-4 w-4" />
                                        <span className="hidden sm:inline">Edit Profile</span>
                                    </Button>
                                </div>
                            </div>
                        </CardHeader>

                        <CardContent>
                            {loading ? (
                                <p>Loading profile...</p>
                            ) : error ? (
                                <p className="text-red-600">{error}</p>
                            ) : (
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                    <div className="space-y-2">
                                        <div className="text-sm text-muted-foreground">First Name</div>
                                        <div className="font-medium text-lg">{profile.firstName}</div>
                                    </div>
                                    <div className="space-y-2">
                                        <div className="text-sm text-muted-foreground">Last Name</div>
                                        <div className="font-medium text-lg">{profile.lastName}</div>
                                    </div>
                                    <div className="space-y-2">
                                        <div className="text-sm text-muted-foreground">Email</div>
                                        <div className="font-medium text-lg">{profile?.email || '—'}</div>
                                    </div>
                                    <div className="space-y-2">
                                        <div className="text-sm text-muted-foreground">Date joined</div>
                                        <div className="font-medium">{formattedDate(profile?.createdAt)}</div>
                                    </div>
                                </div>
                            )}
                        </CardContent>
                    </Card>

                    {/* Actions */}
                    <div className="mt-6 flex items-center gap-3 whitespace-nowrap overflow-auto">
                        {/* My Saved Products button (shared component) */}
                        <MySavedProductsButton />

                        <Button
                            id="reset-password-btn"
                            variant="outline"
                            size="sm"
                            onClick={handleResetClick}
                            className="bg-background text-foreground flex items-center gap-2"
                        >
                            <RefreshCcw className="h-4 w-4" />
                            <span>Reset Password</span>
                        </Button>

                        <Button
                            id="logout-btn"
                            variant="outline"
                            size="sm"
                            onClick={handleLogoutClick}
                            className="bg-background text-foreground flex items-center gap-2"
                        >
                            <LogOut className="h-4 w-4" />
                            <span>Logout</span>
                        </Button>

                        <Button
                            id="delete-account-btn"
                            aria-label="Delete account"
                            variant="destructive"
                            onClick={handleDeleteAccount}
                            className="flex items-center gap-3 px-4 py-2"
                            style={{ display: 'inline-flex', backgroundColor: '#dc2626', color: '#ffffff', border: '1px solid #991b1b' }}
                        >
                            <Trash2 className="h-5 w-5" />
                            <span>Delete Account</span>
                        </Button>
                     </div>
                 </div>
             </div>

             {/* Edit Profile Modal (Dialog) */}
             <Dialog open={isEditOpen} onOpenChange={setIsEditOpen}>
                 <DialogContent className="max-w-md">
                     <DialogHeader>
                         <DialogTitle>Edit Profile</DialogTitle>
                     </DialogHeader>
                     <div className="space-y-3 py-2">
                         <label className="block text-sm text-muted-foreground">First Name</label>
                         <Input value={editFirstName} onChange={e => setEditFirstName(e.target.value)} />
                         <label className="block text-sm text-muted-foreground">Last Name</label>
                         <Input value={editLastName} onChange={e => setEditLastName(e.target.value)} />
                         <label className="block text-sm text-muted-foreground">Email</label>
                         <Input value={editEmail} onChange={e => setEditEmail(e.target.value)} />
                     </div>
                     <div className="mt-4 flex justify-end gap-2">
                         <Button variant="ghost" onClick={() => setIsEditOpen(false)}>Cancel</Button>
                         <Button onClick={handleSaveEdit} disabled={editLoading}>{editLoading ? 'Saving...' : 'Save'}</Button>
                     </div>
                 </DialogContent>
             </Dialog>

             {/* Delete account confirmation dialog */}
             {/*<Dialog open={showDeleteConfirm} onOpenChange={setShowDeleteConfirm}>*/}
             {/*    <DialogContent className="max-w-md">*/}
             {/*        <DialogHeader>*/}
             {/*            <DialogTitle>Confirm Account Deletion</DialogTitle>*/}
             {/*        </DialogHeader>*/}
             {/*        <div className="py-2">*/}
             {/*            <p className="text-sm text-muted-foreground">Are you sure want to delete your account?</p>*/}
             {/*            <div className="mt-4 flex justify-end gap-2">*/}
             {/*                <Button variant="outline" onClick={() => setShowDeleteConfirm(false)} disabled={deleteLoading}>Cancel</Button>*/}
             {/*                <Button variant="destructive" onClick={performDeleteAccount} disabled={deleteLoading}>{deleteLoading ? 'Deleting...' : 'Confirm'}</Button>*/}
             {/*            </div>*/}
             {/*        </div>*/}
             {/*    </DialogContent>*/}
             {/*</Dialog>*/}
            <AlertDialog open={showDeleteConfirm} onOpenChange={setShowDeleteConfirm}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Delete Account?</AlertDialogTitle>
                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently delete your account and all associated data.
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel
                            onClick={() => setShowDeleteConfirm(false)}
                            disabled={deleteLoading}
                        >
                            Cancel
                        </AlertDialogCancel>

                        <AlertDialogAction
                            variant="destructive"
                            onClick={performDeleteAccount}
                            disabled={deleteLoading}
                        >
                            {deleteLoading ? "Deleting..." : "Delete Account"}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>


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
