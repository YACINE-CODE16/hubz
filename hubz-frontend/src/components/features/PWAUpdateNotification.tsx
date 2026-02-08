import { useEffect, useState, useCallback } from 'react';
import { RefreshCw, X, Sparkles, Download } from 'lucide-react';
import { cn } from '../../lib/utils';
import { onUpdateAvailable, skipWaiting, getSWVersion } from '../../utils/pwa';
import Button from '../ui/Button';

interface PWAUpdateNotificationProps {
  className?: string;
  /** Position of the notification */
  position?: 'bottom-right' | 'bottom-center' | 'top-center';
}

/**
 * PWA Update Notification Component
 *
 * Displays a notification when a new version of the app is available.
 * Allows the user to refresh to get the latest version.
 * Features:
 * - Shows current vs new version (if available)
 * - Smooth transition animations
 * - Skip waiting to activate immediately
 * - Countdown auto-refresh option
 */
export default function PWAUpdateNotification({
  className,
  position = 'bottom-right'
}: PWAUpdateNotificationProps) {
  const [updateAvailable, setUpdateAvailable] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [dismissed, setDismissed] = useState(false);
  const [countdown, setCountdown] = useState<number | null>(null);
  const [version, setVersion] = useState<string | null>(null);

  useEffect(() => {
    // Subscribe to update notifications
    const unsubscribe = onUpdateAvailable((needRefresh) => {
      if (needRefresh) {
        setUpdateAvailable(true);
        setDismissed(false);
        // Try to get the current version
        getSWVersion().then(setVersion);
      }
    });

    return unsubscribe;
  }, []);

  // Countdown timer for auto-refresh
  useEffect(() => {
    if (countdown === null) return;

    if (countdown <= 0) {
      handleUpdate();
      return;
    }

    const timer = setTimeout(() => {
      setCountdown(countdown - 1);
    }, 1000);

    return () => clearTimeout(timer);
  }, [countdown]);

  const handleUpdate = useCallback(async () => {
    setUpdating(true);
    setCountdown(null);

    try {
      // Tell the service worker to skip waiting
      await skipWaiting();
      // Small delay to ensure SW is activated
      await new Promise((resolve) => setTimeout(resolve, 100));
      // Reload the page to get the new version
      window.location.reload();
    } catch (error) {
      console.error('[PWA] Update failed:', error);
      // Fallback: just reload
      window.location.reload();
    }
  }, []);

  const handleDismiss = useCallback(() => {
    setDismissed(true);
    setCountdown(null);
  }, []);

  const handleAutoRefresh = useCallback(() => {
    setCountdown(5);
  }, []);

  const cancelAutoRefresh = useCallback(() => {
    setCountdown(null);
  }, []);

  if (!updateAvailable || dismissed) {
    return null;
  }

  const positionClasses = {
    'bottom-right': 'bottom-4 left-4 right-4 md:left-auto md:right-4 md:w-[400px]',
    'bottom-center': 'bottom-4 left-1/2 -translate-x-1/2 w-[calc(100%-2rem)] max-w-[400px]',
    'top-center': 'top-20 left-1/2 -translate-x-1/2 w-[calc(100%-2rem)] max-w-[400px]',
  };

  return (
    <div
      className={cn(
        'fixed z-50',
        'animate-in slide-in-from-bottom-4 fade-in duration-300',
        positionClasses[position],
        className
      )}
    >
      <div className="rounded-2xl border border-accent/30 bg-white dark:bg-gray-900 shadow-2xl overflow-hidden">
        {/* Header with gradient */}
        <div className="bg-gradient-to-r from-accent to-blue-600 px-4 py-3">
          <div className="flex items-center gap-2 text-white">
            <div className="p-1.5 bg-white/20 rounded-lg">
              <Sparkles className="h-4 w-4" />
            </div>
            <span className="font-semibold text-sm">New Update Available</span>
            {version && (
              <span className="ml-auto text-xs bg-white/20 px-2 py-0.5 rounded-full">
                {version}
              </span>
            )}
          </div>
        </div>

        {/* Content */}
        <div className="p-4">
          <div className="flex items-start gap-3">
            <div className="flex-shrink-0 p-2.5 rounded-xl bg-accent/10 dark:bg-accent/20">
              <Download className="h-6 w-6 text-accent" />
            </div>

            <div className="flex-1 min-w-0">
              <p className="text-sm text-gray-600 dark:text-gray-300 leading-relaxed">
                A new version of Hubz is ready with improvements and bug fixes.
                Refresh to get the latest features.
              </p>

              {/* Countdown indicator */}
              {countdown !== null && (
                <div className="mt-3 flex items-center gap-2 text-sm text-accent">
                  <div className="w-4 h-4 rounded-full border-2 border-accent border-t-transparent animate-spin" />
                  <span>Refreshing in {countdown}s...</span>
                  <button
                    onClick={cancelAutoRefresh}
                    className="ml-auto text-xs text-gray-500 hover:text-gray-700 dark:hover:text-gray-300"
                  >
                    Cancel
                  </button>
                </div>
              )}

              {/* Actions */}
              {countdown === null && (
                <div className="mt-4 flex flex-wrap items-center gap-2">
                  <Button
                    size="sm"
                    onClick={handleUpdate}
                    loading={updating}
                    className="gap-1.5"
                  >
                    <RefreshCw className={cn('h-4 w-4', updating && 'animate-spin')} />
                    {updating ? 'Updating...' : 'Refresh now'}
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={handleAutoRefresh}
                    className="text-gray-600 dark:text-gray-300"
                  >
                    Auto-refresh in 5s
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={handleDismiss}
                    className="text-gray-500"
                  >
                    Later
                  </Button>
                </div>
              )}
            </div>

            <button
              onClick={handleDismiss}
              className="flex-shrink-0 p-1.5 rounded-lg text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-white/10 transition-colors"
              aria-label="Dismiss"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>

        {/* Progress bar for countdown */}
        {countdown !== null && (
          <div className="h-1 bg-gray-100 dark:bg-gray-800">
            <div
              className="h-full bg-accent transition-all duration-1000 ease-linear"
              style={{ width: `${(countdown / 5) * 100}%` }}
            />
          </div>
        )}
      </div>
    </div>
  );
}
