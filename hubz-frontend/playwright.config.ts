import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright E2E test configuration for Hubz
 * @see https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  // Test directory
  testDir: './tests/e2e',

  // Output directories
  outputDir: './test-results/e2e',

  // Global setup/teardown
  globalSetup: './tests/e2e/global-setup.ts',

  // Test timeout (60 seconds for E2E tests)
  timeout: 60000,

  // Expect timeout
  expect: {
    timeout: 10000,
  },

  // Run tests in parallel
  fullyParallel: true,

  // Fail the build on CI if you accidentally left test.only in the source code
  forbidOnly: !!process.env.CI,

  // Retry on CI only
  retries: process.env.CI ? 2 : 0,

  // Limit parallel workers on CI
  workers: process.env.CI ? 1 : undefined,

  // Reporter configuration
  reporter: [
    ['html', { outputFolder: './test-results/e2e-report', open: 'never' }],
    ['list'],
    ...(process.env.CI ? [['github' as const]] : []),
  ],

  // Shared settings for all projects
  use: {
    // Base URL for the frontend
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:5173',

    // Collect trace when retrying the failed test
    trace: 'on-first-retry',

    // Screenshot on failure
    screenshot: 'only-on-failure',

    // Video recording
    video: process.env.CI ? 'on-first-retry' : 'off',

    // Viewport size
    viewport: { width: 1280, height: 720 },

    // Timeout for each action
    actionTimeout: 15000,

    // Timeout for navigation
    navigationTimeout: 30000,
  },

  // Configure projects for major browsers
  projects: [
    // Setup project - runs authentication and stores state
    {
      name: 'setup',
      testMatch: /.*\.setup\.ts/,
    },

    // Chromium tests
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        storageState: './tests/e2e/.auth/user.json',
      },
      dependencies: ['setup'],
    },

    // Firefox tests
    {
      name: 'firefox',
      use: {
        ...devices['Desktop Firefox'],
        storageState: './tests/e2e/.auth/user.json',
      },
      dependencies: ['setup'],
    },

    // WebKit tests
    {
      name: 'webkit',
      use: {
        ...devices['Desktop Safari'],
        storageState: './tests/e2e/.auth/user.json',
      },
      dependencies: ['setup'],
    },

    // Mobile Chrome (optional - for responsive testing)
    {
      name: 'mobile-chrome',
      use: {
        ...devices['Pixel 5'],
        storageState: './tests/e2e/.auth/user.json',
      },
      dependencies: ['setup'],
    },

    // Mobile Safari (optional - for responsive testing)
    {
      name: 'mobile-safari',
      use: {
        ...devices['iPhone 12'],
        storageState: './tests/e2e/.auth/user.json',
      },
      dependencies: ['setup'],
    },
  ],

  // Web server configuration (starts frontend before tests)
  webServer: [
    {
      command: 'npm run dev',
      url: 'http://localhost:5173',
      reuseExistingServer: !process.env.CI,
      timeout: 120000,
    },
  ],
});
