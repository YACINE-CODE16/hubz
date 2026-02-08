/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/setupTests.ts',
    css: true,
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    exclude: ['node_modules', 'dist'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html', 'lcov'],
      include: ['src/**/*.{ts,tsx}'],
      exclude: [
        'src/**/*.test.{ts,tsx}',
        'src/**/*.spec.{ts,tsx}',
        'src/setupTests.ts',
        'src/main.tsx',
        'src/vite-env.d.ts',
      ],
    },
  },
  plugins: [
    react(),
    VitePWA({
      // Use 'prompt' to give users control over when to update
      registerType: 'prompt',
      // Use injectManifest to use our custom service worker
      strategies: 'injectManifest',
      // Source of our custom service worker
      srcDir: 'public',
      filename: 'sw.js',
      // Include assets to precache
      includeAssets: ['favicon.svg', 'apple-touch-icon.svg', 'offline.html'],
      manifest: {
        name: 'Hubz',
        short_name: 'Hubz',
        description: 'Productivity app for managing organizations, teams, tasks, goals, and personal habits',
        theme_color: '#3B82F6',
        background_color: '#0A0A0F',
        display: 'standalone',
        start_url: '/',
        orientation: 'portrait-primary',
        categories: ['productivity', 'business', 'utilities'],
        icons: [
          {
            src: 'pwa-192x192.svg',
            sizes: '192x192',
            type: 'image/svg+xml',
            purpose: 'any'
          },
          {
            src: 'pwa-512x512.svg',
            sizes: '512x512',
            type: 'image/svg+xml',
            purpose: 'any'
          },
          {
            src: 'pwa-maskable-192x192.svg',
            sizes: '192x192',
            type: 'image/svg+xml',
            purpose: 'maskable'
          },
          {
            src: 'pwa-maskable-512x512.svg',
            sizes: '512x512',
            type: 'image/svg+xml',
            purpose: 'maskable'
          }
        ],
        shortcuts: [
          {
            name: 'Hub',
            short_name: 'Hub',
            description: 'Go to Hub',
            url: '/hub',
            icons: [{ src: 'pwa-192x192.svg', sizes: '192x192' }]
          },
          {
            name: 'Personal Space',
            short_name: 'Personal',
            description: 'Go to Personal Space',
            url: '/personal',
            icons: [{ src: 'pwa-192x192.svg', sizes: '192x192' }]
          }
        ]
      },
      injectManifest: {
        // Inject precache manifest into the custom SW
        globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2}'],
        // Maximum file size for precache (2 MB)
        maximumFileSizeToCacheInBytes: 2 * 1024 * 1024,
      },
      devOptions: {
        enabled: true,
        type: 'module',
        // Use the custom sw.js in dev mode too
        navigateFallback: 'index.html',
      }
    })
  ],
  build: {
    // Generate source maps for debugging in production
    sourcemap: false,
    // Chunk size warning threshold (in KB)
    chunkSizeWarningLimit: 500,
    rollupOptions: {
      output: {
        // Manual chunk configuration for better caching
        manualChunks: (id) => {
          // Vendor chunks - rarely change, good for caching
          if (id.includes('node_modules')) {
            // React core
            if (id.includes('react-dom') || id.includes('react-router') || id.includes('/react/')) {
              return 'vendor-react';
            }
            // UI libraries
            if (id.includes('lucide-react') || id.includes('react-hot-toast')) {
              return 'vendor-ui';
            }
            // Form handling
            if (id.includes('react-hook-form') || id.includes('@hookform') || id.includes('zod')) {
              return 'vendor-forms';
            }
            // Charts
            if (id.includes('recharts') || id.includes('d3-')) {
              return 'vendor-charts';
            }
            // Editor
            if (id.includes('@tiptap') || id.includes('prosemirror') || id.includes('lowlight')) {
              return 'vendor-editor';
            }
            // Utils
            if (id.includes('axios') || id.includes('zustand') || id.includes('date-fns')) {
              return 'vendor-utils';
            }
          }
          return undefined;
        },
        // Naming patterns for chunks
        chunkFileNames: (chunkInfo) => {
          // Get the facadeModuleId or name for categorization
          const facadeModuleId = chunkInfo.facadeModuleId || '';
          const name = chunkInfo.name || '';

          // Vendor chunks
          if (name.startsWith('vendor-')) {
            return 'assets/[name]-[hash].js';
          }

          // Auth pages chunk
          if (facadeModuleId.includes('/pages/auth/')) {
            return 'assets/auth-[hash].js';
          }
          // Hub pages chunk
          if (facadeModuleId.includes('/pages/hub/') || facadeModuleId.includes('JoinOrganizationPage')) {
            return 'assets/hub-[hash].js';
          }
          // Organization pages chunk
          if (facadeModuleId.includes('/pages/organization/')) {
            return 'assets/organization-[hash].js';
          }
          // Personal pages chunk
          if (facadeModuleId.includes('/pages/personal/')) {
            // Settings are in personal folder but we want separate chunk naming
            if (facadeModuleId.includes('SettingsPage') || facadeModuleId.includes('SecuritySettings') || facadeModuleId.includes('PreferencesSettings')) {
              return 'assets/settings-[hash].js';
            }
            return 'assets/personal-[hash].js';
          }
          // Default chunk naming
          return 'assets/[name]-[hash].js';
        },
        // Entry file naming
        entryFileNames: 'assets/[name]-[hash].js',
        // Asset file naming (CSS, images, etc.)
        assetFileNames: 'assets/[name]-[hash].[ext]',
      },
    },
  },
  // Optimize dependency pre-bundling
  optimizeDeps: {
    include: [
      'react',
      'react-dom',
      'react-router-dom',
      'zustand',
      'axios',
      'lucide-react',
      'react-hot-toast',
      'react-hook-form',
      'recharts',
      'date-fns',
    ],
  },
})
