/**
 * PWA Utilities
 *
 * This module provides utilities for Progressive Web App functionality:
 * - Service Worker registration and updates
 * - Offline detection
 * - Install prompt handling
 */

import { registerSW } from 'virtual:pwa-register';

// Type for the beforeinstallprompt event
interface BeforeInstallPromptEvent extends Event {
  readonly platforms: string[];
  readonly userChoice: Promise<{
    outcome: 'accepted' | 'dismissed';
    platform: string;
  }>;
  prompt(): Promise<void>;
}

// Store for the deferred install prompt
let deferredPrompt: BeforeInstallPromptEvent | null = null;

// Store the update function from service worker registration
let swUpdateFunction: (() => Promise<void>) | null = null;

// Callbacks for PWA events
type UpdateCallback = (needRefresh: boolean) => void;
type OfflineCallback = (isOffline: boolean) => void;
type InstallCallback = (canInstall: boolean) => void;

const updateCallbacks: UpdateCallback[] = [];
const offlineCallbacks: OfflineCallback[] = [];
const installCallbacks: InstallCallback[] = [];

/**
 * Register the service worker with update handling
 */
export function registerServiceWorker(): () => void {
  // Register the service worker using vite-plugin-pwa
  const updateSW = registerSW({
    onNeedRefresh() {
      // New content is available, notify listeners
      console.log('[PWA] New content available, update ready');
      updateCallbacks.forEach((cb) => cb(true));
    },
    onOfflineReady() {
      // App is ready to work offline
      console.log('[PWA] App is ready to work offline');
    },
    onRegistered(registration) {
      // Service worker registered successfully
      console.log('[PWA] Service Worker registered:', registration);

      // Check for updates periodically (every hour)
      if (registration) {
        setInterval(() => {
          console.log('[PWA] Checking for updates...');
          registration.update();
        }, 60 * 60 * 1000);
      }
    },
    onRegisterError(error) {
      console.error('[PWA] Service Worker registration failed:', error);
    },
  });

  // Store the update function for later use
  swUpdateFunction = updateSW;

  return updateSW;
}

/**
 * Force the service worker to skip waiting and activate immediately
 */
export async function skipWaiting(): Promise<void> {
  if (swUpdateFunction) {
    await swUpdateFunction();
  }
}

/**
 * Send a message to the service worker
 */
export function sendMessageToSW(message: { type: string; payload?: unknown }): void {
  if ('serviceWorker' in navigator && navigator.serviceWorker.controller) {
    navigator.serviceWorker.controller.postMessage(message);
  }
}

/**
 * Get the current service worker version
 */
export async function getSWVersion(): Promise<string | null> {
  return new Promise((resolve) => {
    if (!('serviceWorker' in navigator) || !navigator.serviceWorker.controller) {
      resolve(null);
      return;
    }

    const messageHandler = (event: MessageEvent) => {
      if (event.data?.type === 'VERSION') {
        navigator.serviceWorker.removeEventListener('message', messageHandler);
        resolve(event.data.payload);
      }
    };

    navigator.serviceWorker.addEventListener('message', messageHandler);
    sendMessageToSW({ type: 'GET_VERSION' });

    // Timeout after 3 seconds
    setTimeout(() => {
      navigator.serviceWorker.removeEventListener('message', messageHandler);
      resolve(null);
    }, 3000);
  });
}

/**
 * Clear all service worker caches
 */
export async function clearSWCache(cacheName?: string): Promise<void> {
  return new Promise((resolve) => {
    if (!('serviceWorker' in navigator) || !navigator.serviceWorker.controller) {
      resolve();
      return;
    }

    const messageHandler = (event: MessageEvent) => {
      if (event.data?.type === 'CACHE_CLEARED') {
        navigator.serviceWorker.removeEventListener('message', messageHandler);
        console.log('[PWA] Cache cleared:', event.data.payload);
        resolve();
      }
    };

    navigator.serviceWorker.addEventListener('message', messageHandler);
    sendMessageToSW({ type: 'CLEAR_CACHE', payload: { cacheName } });

    // Timeout after 5 seconds
    setTimeout(() => {
      navigator.serviceWorker.removeEventListener('message', messageHandler);
      resolve();
    }, 5000);
  });
}

/**
 * Subscribe to update notifications
 * @param callback Function to call when an update is available
 * @returns Unsubscribe function
 */
export function onUpdateAvailable(callback: UpdateCallback): () => void {
  updateCallbacks.push(callback);
  return () => {
    const index = updateCallbacks.indexOf(callback);
    if (index > -1) {
      updateCallbacks.splice(index, 1);
    }
  };
}

/**
 * Subscribe to offline status changes
 * @param callback Function to call when offline status changes
 * @returns Unsubscribe function
 */
export function onOfflineChange(callback: OfflineCallback): () => void {
  offlineCallbacks.push(callback);
  // Immediately call with current status
  callback(!navigator.onLine);
  return () => {
    const index = offlineCallbacks.indexOf(callback);
    if (index > -1) {
      offlineCallbacks.splice(index, 1);
    }
  };
}

/**
 * Subscribe to install prompt availability
 * @param callback Function to call when install prompt availability changes
 * @returns Unsubscribe function
 */
export function onInstallPromptChange(callback: InstallCallback): () => void {
  installCallbacks.push(callback);
  // Immediately call with current status
  callback(deferredPrompt !== null);
  return () => {
    const index = installCallbacks.indexOf(callback);
    if (index > -1) {
      installCallbacks.splice(index, 1);
    }
  };
}

/**
 * Check if the app is currently offline
 */
export function isOffline(): boolean {
  return !navigator.onLine;
}

/**
 * Check if the app can be installed (PWA install prompt is available)
 */
export function canInstall(): boolean {
  return deferredPrompt !== null;
}

/**
 * Check if the app is already installed as a PWA
 */
export function isInstalled(): boolean {
  // Check if running in standalone mode (installed PWA)
  if (window.matchMedia('(display-mode: standalone)').matches) {
    return true;
  }

  // Check for iOS standalone mode
  if ((window.navigator as Navigator & { standalone?: boolean }).standalone === true) {
    return true;
  }

  return false;
}

/**
 * Prompt the user to install the PWA
 * @returns Promise that resolves to true if the user accepted, false otherwise
 */
export async function promptInstall(): Promise<boolean> {
  if (!deferredPrompt) {
    console.warn('[PWA] No install prompt available');
    return false;
  }

  // Show the install prompt
  await deferredPrompt.prompt();

  // Wait for the user's response
  const { outcome } = await deferredPrompt.userChoice;

  // Clear the deferred prompt
  deferredPrompt = null;
  installCallbacks.forEach((cb) => cb(false));

  return outcome === 'accepted';
}

/**
 * Initialize PWA event listeners
 * Call this once when the app starts
 */
export function initializePWA(): void {
  // Listen for the beforeinstallprompt event
  window.addEventListener('beforeinstallprompt', (event) => {
    // Prevent the default browser install prompt
    event.preventDefault();

    // Store the event for later use
    deferredPrompt = event as BeforeInstallPromptEvent;

    // Notify listeners that install is available
    installCallbacks.forEach((cb) => cb(true));

    console.log('[PWA] Install prompt saved for later use');
  });

  // Listen for successful app installation
  window.addEventListener('appinstalled', () => {
    // Clear the deferred prompt
    deferredPrompt = null;
    installCallbacks.forEach((cb) => cb(false));

    console.log('[PWA] App was installed successfully');
  });

  // Listen for online/offline changes
  window.addEventListener('online', () => {
    offlineCallbacks.forEach((cb) => cb(false));
    console.log('[PWA] App is online');
  });

  window.addEventListener('offline', () => {
    offlineCallbacks.forEach((cb) => cb(true));
    console.log('[PWA] App is offline');
  });

  // Register the service worker
  registerServiceWorker();
}

/**
 * Get PWA display mode
 */
export function getDisplayMode(): 'browser' | 'standalone' | 'minimal-ui' | 'fullscreen' {
  if (window.matchMedia('(display-mode: fullscreen)').matches) {
    return 'fullscreen';
  }
  if (window.matchMedia('(display-mode: standalone)').matches) {
    return 'standalone';
  }
  if (window.matchMedia('(display-mode: minimal-ui)').matches) {
    return 'minimal-ui';
  }
  return 'browser';
}
