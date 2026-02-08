import { useEffect, useState } from 'react';
import { Download, X, Smartphone } from 'lucide-react';
import { cn } from '../../lib/utils';
import { canInstall, onInstallPromptChange, promptInstall, isInstalled } from '../../utils/pwa';
import Button from '../ui/Button';

interface InstallPWAPromptProps {
  className?: string;
}

/**
 * Install PWA Prompt Component
 *
 * Displays a prompt to install the app as a Progressive Web App.
 * Only shows when the install prompt is available and the app is not already installed.
 */
export default function InstallPWAPrompt({ className }: InstallPWAPromptProps) {
  const [showPrompt, setShowPrompt] = useState(false);
  const [installing, setInstalling] = useState(false);
  const [dismissed, setDismissed] = useState(false);

  useEffect(() => {
    // Check if user has previously dismissed the prompt
    const dismissedUntil = localStorage.getItem('pwa-install-dismissed-until');
    if (dismissedUntil && new Date(dismissedUntil) > new Date()) {
      setDismissed(true);
      return;
    }

    // Check if already installed
    if (isInstalled()) {
      return;
    }

    // Subscribe to install prompt changes
    const unsubscribe = onInstallPromptChange((available) => {
      setShowPrompt(available && !dismissed);
    });

    // Set initial state
    setShowPrompt(canInstall() && !dismissed);

    return unsubscribe;
  }, [dismissed]);

  const handleInstall = async () => {
    setInstalling(true);
    try {
      const accepted = await promptInstall();
      if (accepted) {
        setShowPrompt(false);
      }
    } catch (error) {
      console.error('Install failed:', error);
    } finally {
      setInstalling(false);
    }
  };

  const handleDismiss = () => {
    // Dismiss for 7 days
    const dismissUntil = new Date();
    dismissUntil.setDate(dismissUntil.getDate() + 7);
    localStorage.setItem('pwa-install-dismissed-until', dismissUntil.toISOString());
    setDismissed(true);
    setShowPrompt(false);
  };

  if (!showPrompt) {
    return null;
  }

  return (
    <div
      className={cn(
        'fixed bottom-4 left-4 right-4 z-50 md:left-auto md:right-4 md:w-96',
        'animate-in slide-in-from-bottom-4 duration-300',
        className
      )}
    >
      <div className="rounded-xl border border-gray-200/50 dark:border-white/10 bg-white dark:bg-dark-card shadow-lg backdrop-blur-md p-4">
        <div className="flex items-start gap-3">
          <div className="flex-shrink-0 p-2 rounded-lg bg-accent/10">
            <Smartphone className="h-6 w-6 text-accent" />
          </div>

          <div className="flex-1 min-w-0">
            <h3 className="text-sm font-semibold text-gray-900 dark:text-white">
              Install Hubz
            </h3>
            <p className="mt-1 text-sm text-gray-600 dark:text-gray-400">
              Install the app for a better experience with offline support and quick access.
            </p>

            <div className="mt-3 flex items-center gap-2">
              <Button
                size="sm"
                onClick={handleInstall}
                loading={installing}
                className="gap-1.5"
              >
                <Download className="h-4 w-4" />
                Install
              </Button>
              <Button
                size="sm"
                variant="ghost"
                onClick={handleDismiss}
              >
                Not now
              </Button>
            </div>
          </div>

          <button
            onClick={handleDismiss}
            className="flex-shrink-0 p-1 rounded-md text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-white/10 transition-colors"
            aria-label="Dismiss"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
