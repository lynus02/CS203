import * as React from 'react';
import { useState } from 'react';
import { Button } from './ui/button';
import { Bookmark } from 'lucide-react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from './ui/dialog';
import { SavedProducts, SavedProductConfig } from './SavedProducts';

interface Props {
  onLoadProduct?: (config: SavedProductConfig) => void;
  className?: string;
}

export default function MySavedProductsButton({ onLoadProduct, className }: Props) {
  const [open, setOpen] = useState(false);
  const [showLoginPrompt, setShowLoginPrompt] = useState(false);

  const isLoggedIn = !!localStorage.getItem('token');

  const handleClick = () => {
    if (isLoggedIn) {
      setOpen(true);
    } else {
      setShowLoginPrompt(true);
    }
  };

  return (
    <>
      <Button
        variant="outline"
        size="sm"
        onClick={handleClick}
        className={className ?? 'bg-background text-foreground flex items-center gap-2'}
        id="my-saved-products-btn"
      >
        <Bookmark className="h-4 w-4" />
        My Saved Products
      </Button>

      {/* SavedProducts dialog shown when logged in */}
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <SavedProducts onLoadProduct={onLoadProduct} />
        </DialogContent>
      </Dialog>

      {/* Simple login prompt for unauthenticated users */}
      <Dialog open={showLoginPrompt} onOpenChange={setShowLoginPrompt}>
        <DialogContent className="max-w-md text-primary">
          <DialogHeader>
            <DialogTitle>Login required</DialogTitle>
          </DialogHeader>
          <div className="py-2">
            <p className="text-sm text-muted-foreground">Login to view your saved products</p>
            <div className="mt-4 flex justify-end gap-2">
              <Button className="bg-white text-black" variant="outline" onClick={() => setShowLoginPrompt(false)}>Cancel</Button>
              <Button onClick={() => { setShowLoginPrompt(false); window.dispatchEvent(new CustomEvent('openLogin')); }}>Login</Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}
