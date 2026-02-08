import { useEffect, useState, useCallback } from 'react';
import { WifiOff, Wifi, RefreshCw, X } from 'lucide-react';
import { cn } from '../../lib/utils';
import { isOffline, onOfflineChange } from '../../utils/pwa';

interface OfflineIndicatorProps {
  className?: string;
  /** Show a more detailed offline banner with retry button */
  detailed?: boolean;
}

/**
 * Offline Indicator Component
 *
 * Displays a banner when the app is offline.
 * Shows briefly when coming back online to confirm connection restored.
 * Supports two modes: simple banner or detailed banner with retry.
 */
export default function OfflineIndicator({ className, detailed = false }: OfflineIndicatorProps) {
  const [offline, setOffline] = useState(false);
  const [showOnlineMessage, setShowOnlineMessage] = useState(false);
  const [wasOffline, setWasOffline] = useState(false);
  const [retrying, setRetrying] = useState(false);
  const [dismissed, setDismissed] = useState(false);

  useEffect(() => {
    // Set initial state
    setOffline(isOffline());

    // Subscribe to offline changes
    const unsubscribe = onOfflineChange((isCurrentlyOffline) => {
      if (!isCurrentlyOffline && wasOffline) {
        // Coming back online - show message briefly
        setShowOnlineMessage(true);
        setDismissed(false);
        setTimeout(() => {
          setShowOnlineMessage(false);
        }, 3000);
      }

      setWasOffline(isCurrentlyOffline);
      setOffline(isCurrentlyOffline);

      // Reset dismissed state when going offline
      if (isCurrentlyOffline) {
        setDismissed(false);
      }
    });

    return unsubscribe;
  }, [wasOffline]);

  const handleRetry = useCallback(async () => {
    setRetrying(true);
    try {
      // Try to fetch the home page to check connectivity
      const response = await fetch('/', { cache: 'no-store' });
      if (response.ok) {
        // Connection restored, browser events will handle the state
        window.location.reload();
      }
    } catch {
      // Still offline
    } finally {
      setRetrying(false);
    }
  }, []);

  const handleDismiss = useCallback(() => {
    setDismissed(true);
  }, []);

  // Show online restored message
  if (showOnlineMessage && !offline) {
    return (
      <div
        className={cn(
          'fixed top-0 left-0 right-0 z-[100]',
          'animate-in slide-in-from-top-2 duration-300',
          className
        )}
      >
        <div className="bg-gradient-to-r from-green-500 to-emerald-500 text-white py-3 px-4 text-center text-sm font-medium shadow-lg">
          <div className="flex items-center justify-center gap-2">
            <div className="relative">
              <Wifi className="h-5 w-5" />
              <span className="absolute -top-0.5 -right-0.5 w-2 h-2 bg-white rounded-full animate-ping" />
            </div>
            <span className="font-semibold">Connection restored!</span>
            <span className="opacity-80">You're back online.</span>
          </div>
        </div>
      </div>
    );
  }

  // Don't show if online or dismissed
  if (!offline || dismissed) {
    return null;
  }

  // Detailed offline banner with more information and actions
  if (detailed) {
    return (
      <div
        className={cn(
          'fixed top-0 left-0 right-0 z-[100]',
          'animate-in slide-in-from-top-2 duration-300',
          className
        )}
      >
        <div className="bg-gradient-to-r from-amber-500 to-orange-500 text-white shadow-lg">
          <div className="max-w-7xl mx-auto px-4 py-3">
            <div className="flex items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-white/20 rounded-lg backdrop-blur-sm">
                  <WifiOff className="h-5 w-5" />
                </div>
                <div>
                  <p className="font-semibold text-sm">You're currently offline</p>
                  <p className="text-xs opacity-90">
                    Some features may be unavailable. Cached content is still accessible.
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={handleRetry}
                  disabled={retrying}
                  className={cn(
                    'flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium',
                    'bg-white/20 hover:bg-white/30 transition-colors',
                    'disabled:opacity-50 disabled:cursor-not-allowed'
                  )}
                >
                  <RefreshCw className={cn('h-4 w-4', retrying && 'animate-spin')} />
                  <span className="hidden sm:inline">
                    {retrying ? 'Retrying...' : 'Retry'}
                  </span>
                </button>

                <button
                  onClick={handleDismiss}
                  className="p-1.5 rounded-lg hover:bg-white/20 transition-colors"
                  aria-label="Dismiss"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Simple offline banner
  return (
    <div
      className={cn(
        'fixed top-0 left-0 right-0 z-[100]',
        'animate-in slide-in-from-top-2 duration-300',
        className
      )}
    >
      <div className="bg-gradient-to-r from-amber-500 to-orange-500 text-white py-2.5 px-4 text-center text-sm font-medium shadow-lg">
        <div className="flex items-center justify-center gap-2">
          <WifiOff className="h-4 w-4" />
          <span>You're offline</span>
          <span className="hidden sm:inline opacity-80">
            - Some features may be unavailable
          </span>
          <button
            onClick={handleRetry}
            disabled={retrying}
            className="ml-2 flex items-center gap-1 px-2 py-0.5 rounded bg-white/20 hover:bg-white/30 transition-colors text-xs font-medium disabled:opacity-50"
          >
            <RefreshCw className={cn('h-3 w-3', retrying && 'animate-spin')} />
            Retry
          </button>
        </div>
      </div>
    </div>
  );
}
