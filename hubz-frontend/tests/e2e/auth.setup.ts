import { test as setup, expect } from '@playwright/test';
import { LoginPage, RegisterPage } from './pages';
import { testData, apiHelpers } from './fixtures';
import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const authFile = path.join(__dirname, '.auth', 'user.json');

/**
 * Authentication setup - runs before all tests that need authentication
 * Creates a test user and stores the authenticated state
 */
setup('authenticate', async ({ page }) => {
  const loginPage = new LoginPage(page);
  const registerPage = new RegisterPage(page);

  // Try to login with existing test user first
  const defaultUser = testData.defaultUser;

  try {
    // First, try to login (in case user already exists)
    await loginPage.goto();
    await loginPage.waitForPageLoad();
    await loginPage.login(defaultUser.email, defaultUser.password);

    // Wait for redirect to hub or error
    await page.waitForURL('/hub', { timeout: 5000 });
    console.log('[Auth Setup] Logged in with existing user');
  } catch {
    // User doesn't exist, need to register
    console.log('[Auth Setup] Existing user not found, creating new user...');

    await registerPage.goto();
    await registerPage.waitForPageLoad();

    await registerPage.register({
      firstName: defaultUser.firstName,
      lastName: defaultUser.lastName,
      email: defaultUser.email,
      password: defaultUser.password,
    });

    // Wait for redirect to hub
    await page.waitForURL('/hub', { timeout: 10000 });
    console.log('[Auth Setup] New user registered successfully');
  }

  // Verify we're logged in
  await expect(page).toHaveURL('/hub');

  // Store authentication state
  await page.context().storageState({ path: authFile });
  console.log(`[Auth Setup] Authentication state saved to ${authFile}`);
});

/**
 * Create additional test user (for multi-user scenarios)
 */
setup.describe.configure({ mode: 'serial' });

setup('create secondary test user', async ({ page }) => {
  // This is optional - only if tests need multiple users
  const secondaryUser = {
    ...testData.generateUser(),
    email: 'e2e-secondary@hubz.test',
  };

  try {
    const registerPage = new RegisterPage(page);
    await registerPage.goto();
    await registerPage.waitForPageLoad();

    await registerPage.register(secondaryUser);
    await page.waitForURL('/hub', { timeout: 10000 });
    console.log('[Auth Setup] Secondary test user created');
  } catch {
    // User might already exist, that's fine
    console.log('[Auth Setup] Secondary user might already exist, skipping');
  }
});
