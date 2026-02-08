/**
 * Hubz Service Worker
 *
 * This service worker provides offline functionality with intelligent caching strategies:
 * - NetworkFirst: API calls (with fallback to cache)
 * - CacheFirst: Static assets with hashes (JS, CSS, images, fonts)
 * - StaleWhileRevalidate: HTML pages and navigation requests
 *
 * The service worker also handles:
 * - Offline page fallback when network is unavailable
 * - Cache versioning and cleanup of old caches
 * - Skip waiting for immediate updates
 */

// Cache version - increment this to force cache refresh
const CACHE_VERSION = 'v1.0.0';

// Cache names
const CACHE_NAMES = {
  static: `hubz-static-${CACHE_VERSION}`,
  images: `hubz-images-${CACHE_VERSION}`,
  fonts: `hubz-fonts-${CACHE_VERSION}`,
  api: `hubz-api-${CACHE_VERSION}`,
  pages: `hubz-pages-${CACHE_VERSION}`,
};

// Workbox will inject the manifest here
const WB_MANIFEST = self.__WB_MANIFEST || [];

// Static assets to precache on install
const PRECACHE_ASSETS = [
  '/',
  '/offline.html',
  '/manifest.json',
  '/favicon.svg',
  '/pwa-192x192.svg',
  '/pwa-512x512.svg',
  '/apple-touch-icon.svg',
  ...WB_MANIFEST.map(entry => entry.url),
];

// API cache settings
const API_CACHE_MAX_AGE = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
const API_NETWORK_TIMEOUT = 10000; // 10 seconds

// =============================================================================
// INSTALL EVENT
// =============================================================================

self.addEventListener('install', (event) => {
  console.log('[SW] Installing service worker...');

  event.waitUntil(
    (async () => {
      // Open the static cache
      const cache = await caches.open(CACHE_NAMES.static);

      // Precache essential assets
      console.log('[SW] Precaching static assets...');
      await cache.addAll(PRECACHE_ASSETS);

      // Skip waiting to activate immediately
      await self.skipWaiting();

      console.log('[SW] Service worker installed successfully');
    })()
  );
});

// =============================================================================
// ACTIVATE EVENT
// =============================================================================

self.addEventListener('activate', (event) => {
  console.log('[SW] Activating service worker...');

  event.waitUntil(
    (async () => {
      // Get all cache names
      const cacheNames = await caches.keys();

      // Delete old caches that don't match current version
      const validCacheNames = Object.values(CACHE_NAMES);

      await Promise.all(
        cacheNames.map(async (cacheName) => {
          if (!validCacheNames.includes(cacheName)) {
            console.log(`[SW] Deleting old cache: ${cacheName}`);
            await caches.delete(cacheName);
          }
        })
      );

      // Take control of all clients immediately
      await self.clients.claim();

      console.log('[SW] Service worker activated successfully');
    })()
  );
});

// =============================================================================
// FETCH EVENT
// =============================================================================

self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // Only handle same-origin requests and http/https protocols
  if (!url.protocol.startsWith('http')) {
    return;
  }

  // Determine the caching strategy based on the request type
  if (isApiRequest(url)) {
    event.respondWith(networkFirstStrategy(request, CACHE_NAMES.api));
  } else if (isImageRequest(url)) {
    event.respondWith(cacheFirstStrategy(request, CACHE_NAMES.images));
  } else if (isFontRequest(url)) {
    event.respondWith(cacheFirstStrategy(request, CACHE_NAMES.fonts));
  } else if (isStaticAssetRequest(url)) {
    event.respondWith(cacheFirstStrategy(request, CACHE_NAMES.static));
  } else if (isNavigationRequest(request)) {
    event.respondWith(staleWhileRevalidateStrategy(request, CACHE_NAMES.pages));
  } else {
    // Default: try network, fall back to cache
    event.respondWith(networkFirstStrategy(request, CACHE_NAMES.static));
  }
});

// =============================================================================
// REQUEST TYPE DETECTION
// =============================================================================

function isApiRequest(url) {
  return url.pathname.startsWith('/api/');
}

function isImageRequest(url) {
  return /\.(png|jpg|jpeg|gif|webp|svg|ico)$/i.test(url.pathname);
}

function isFontRequest(url) {
  return /\.(woff|woff2|ttf|eot|otf)$/i.test(url.pathname);
}

function isStaticAssetRequest(url) {
  // JS and CSS files with hashes (e.g., assets/main-abc123.js)
  return /\.(js|css)$/i.test(url.pathname) && url.pathname.includes('/assets/');
}

function isNavigationRequest(request) {
  return request.mode === 'navigate' ||
         request.headers.get('Accept')?.includes('text/html');
}

// =============================================================================
// CACHING STRATEGIES
// =============================================================================

/**
 * Network First Strategy
 *
 * Try to fetch from network first. If network fails, fall back to cache.
 * Good for API calls where fresh data is important.
 */
async function networkFirstStrategy(request, cacheName) {
  const cache = await caches.open(cacheName);

  try {
    // Try to fetch from network with timeout
    const networkResponse = await fetchWithTimeout(request, API_NETWORK_TIMEOUT);

    // Only cache successful responses
    if (networkResponse.ok) {
      // Clone the response before caching (response can only be consumed once)
      cache.put(request, networkResponse.clone());
    }

    return networkResponse;
  } catch (error) {
    console.log(`[SW] Network failed for ${request.url}, trying cache...`);

    // Try to get from cache
    const cachedResponse = await cache.match(request);

    if (cachedResponse) {
      // Check if cache is still valid
      const cacheDate = new Date(cachedResponse.headers.get('date') || 0);
      const now = new Date();
      const age = now.getTime() - cacheDate.getTime();

      if (age < API_CACHE_MAX_AGE) {
        console.log(`[SW] Serving from cache: ${request.url}`);
        return cachedResponse;
      }
    }

    // If this is an API request, return a JSON error response
    if (isApiRequest(new URL(request.url))) {
      return new Response(
        JSON.stringify({
          error: 'You are offline',
          message: 'Please check your internet connection and try again.',
          offline: true
        }),
        {
          status: 503,
          headers: { 'Content-Type': 'application/json' }
        }
      );
    }

    // For navigation requests, show offline page
    if (isNavigationRequest(request)) {
      const offlinePage = await caches.match('/offline.html');
      if (offlinePage) {
        return offlinePage;
      }
    }

    throw error;
  }
}

/**
 * Cache First Strategy
 *
 * Try to serve from cache first. If not in cache, fetch from network and cache it.
 * Good for static assets that rarely change (images, fonts, hashed JS/CSS).
 */
async function cacheFirstStrategy(request, cacheName) {
  const cache = await caches.open(cacheName);

  // Try to get from cache first
  const cachedResponse = await cache.match(request);

  if (cachedResponse) {
    console.log(`[SW] Serving from cache: ${request.url}`);
    return cachedResponse;
  }

  // Not in cache, fetch from network
  try {
    const networkResponse = await fetch(request);

    // Cache successful responses
    if (networkResponse.ok) {
      cache.put(request, networkResponse.clone());
    }

    return networkResponse;
  } catch (error) {
    console.log(`[SW] Failed to fetch ${request.url}:`, error);

    // For images, return a placeholder or transparent pixel
    if (isImageRequest(new URL(request.url))) {
      return new Response(
        '<svg xmlns="http://www.w3.org/2000/svg" width="1" height="1"/>',
        { headers: { 'Content-Type': 'image/svg+xml' } }
      );
    }

    throw error;
  }
}

/**
 * Stale While Revalidate Strategy
 *
 * Serve from cache immediately (if available), then fetch from network
 * in the background to update the cache for next time.
 * Good for content that can be slightly stale (HTML pages).
 */
async function staleWhileRevalidateStrategy(request, cacheName) {
  const cache = await caches.open(cacheName);

  // Try to get from cache
  const cachedResponse = await cache.match(request);

  // Start fetching from network in the background
  const networkPromise = fetch(request)
    .then((networkResponse) => {
      if (networkResponse.ok) {
        cache.put(request, networkResponse.clone());
      }
      return networkResponse;
    })
    .catch((error) => {
      console.log(`[SW] Background fetch failed for ${request.url}:`, error);
      return null;
    });

  // If we have a cached response, return it immediately
  if (cachedResponse) {
    console.log(`[SW] Serving stale from cache: ${request.url}`);
    return cachedResponse;
  }

  // Wait for network response
  try {
    const networkResponse = await networkPromise;
    if (networkResponse) {
      return networkResponse;
    }
  } catch (error) {
    // Network failed
  }

  // If all else fails, show offline page for navigation requests
  if (isNavigationRequest(request)) {
    const offlinePage = await caches.match('/offline.html');
    if (offlinePage) {
      return offlinePage;
    }
  }

  // Return a generic offline response
  return new Response('Offline', { status: 503 });
}

// =============================================================================
// UTILITY FUNCTIONS
// =============================================================================

/**
 * Fetch with timeout
 */
function fetchWithTimeout(request, timeout) {
  return new Promise((resolve, reject) => {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => {
      controller.abort();
      reject(new Error('Request timeout'));
    }, timeout);

    fetch(request, { signal: controller.signal })
      .then((response) => {
        clearTimeout(timeoutId);
        resolve(response);
      })
      .catch((error) => {
        clearTimeout(timeoutId);
        reject(error);
      });
  });
}

// =============================================================================
// MESSAGE EVENT - Communication with main thread
// =============================================================================

self.addEventListener('message', (event) => {
  const { type, payload } = event.data || {};

  switch (type) {
    case 'SKIP_WAITING':
      // Force the waiting service worker to become active
      self.skipWaiting();
      break;

    case 'GET_VERSION':
      // Send back the current cache version
      event.source?.postMessage({
        type: 'VERSION',
        payload: CACHE_VERSION
      });
      break;

    case 'CLEAR_CACHE':
      // Clear specific cache or all caches
      event.waitUntil(
        (async () => {
          const cacheName = payload?.cacheName;

          if (cacheName) {
            await caches.delete(cacheName);
          } else {
            const cacheNames = await caches.keys();
            await Promise.all(cacheNames.map((name) => caches.delete(name)));
          }

          event.source?.postMessage({
            type: 'CACHE_CLEARED',
            payload: { cacheName: cacheName || 'all' }
          });
        })()
      );
      break;

    default:
      console.log(`[SW] Unknown message type: ${type}`);
  }
});

// =============================================================================
// BACKGROUND SYNC (if supported)
// =============================================================================

self.addEventListener('sync', (event) => {
  console.log(`[SW] Background sync event: ${event.tag}`);

  if (event.tag === 'sync-pending-requests') {
    event.waitUntil(syncPendingRequests());
  }
});

async function syncPendingRequests() {
  // This can be extended to replay failed requests when back online
  console.log('[SW] Syncing pending requests...');
}

// =============================================================================
// PERIODIC BACKGROUND SYNC (if supported)
// =============================================================================

self.addEventListener('periodicsync', (event) => {
  console.log(`[SW] Periodic sync event: ${event.tag}`);

  if (event.tag === 'update-cache') {
    event.waitUntil(updateCacheInBackground());
  }
});

async function updateCacheInBackground() {
  console.log('[SW] Updating cache in background...');

  try {
    // Refresh the home page cache
    const cache = await caches.open(CACHE_NAMES.pages);
    await cache.add('/');
    console.log('[SW] Background cache update complete');
  } catch (error) {
    console.log('[SW] Background cache update failed:', error);
  }
}

console.log('[SW] Service worker script loaded');
