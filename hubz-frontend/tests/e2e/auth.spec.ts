import { test, expect, testData } from './fixtures';
import { LoginPage, RegisterPage, HubPage } from './pages';

/**
 * Authentication E2E Tests
 * Tests the complete authentication flow including registration, login, and logout
 */

test.describe('Authentication Flow', () => {
  test.describe('Registration', () => {
    test('should register a new user successfully', async ({ page, registerPage }) => {
      const newUser = testData.generateUser();

      await registerPage.goto();
      await registerPage.waitForPageLoad();
      await registerPage.expectToBeOnRegisterPage();

      await registerPage.register(newUser);

      // Should redirect to hub after successful registration
      await expect(page).toHaveURL('/hub', { timeout: 10000 });
    });

    test('should show error for duplicate email', async ({ page, registerPage }) => {
      // Use the default test user email which should already exist
      const existingUser = {
        ...testData.defaultUser,
        email: testData.defaultUser.email,
      };

      await registerPage.goto();
      await registerPage.waitForPageLoad();

      await registerPage.register(existingUser);

      // Should show an error message
      await expect(registerPage.errorMessage).toBeVisible({ timeout: 5000 });
    });

    test('should validate required fields', async ({ page, registerPage }) => {
      await registerPage.goto();
      await registerPage.waitForPageLoad();

      // Try to submit empty form
      await registerPage.submit();

      // Form should not submit (still on register page)
      await expect(page).toHaveURL('/register');
    });

    test('should navigate to login page', async ({ page, registerPage }) => {
      await registerPage.goto();
      await registerPage.waitForPageLoad();

      await registerPage.goToLogin();

      await expect(page).toHaveURL('/login');
    });
  });

  test.describe('Login', () => {
    test('should login with valid credentials', async ({ page, loginPage }) => {
      await loginPage.goto();
      await loginPage.waitForPageLoad();
      await loginPage.expectToBeOnLoginPage();

      await loginPage.login(testData.defaultUser.email, testData.defaultUser.password);

      // Should redirect to hub after successful login
      await expect(page).toHaveURL('/hub', { timeout: 10000 });
    });

    test('should show error for invalid credentials', async ({ page, loginPage }) => {
      await loginPage.goto();
      await loginPage.waitForPageLoad();

      await loginPage.login('invalid@email.com', 'wrongpassword');

      // Should show error message
      await expect(loginPage.errorMessage).toBeVisible({ timeout: 5000 });

      // Should stay on login page
      await expect(page).toHaveURL('/login');
    });

    test('should show error for wrong password', async ({ page, loginPage }) => {
      await loginPage.goto();
      await loginPage.waitForPageLoad();

      await loginPage.login(testData.defaultUser.email, 'wrongpassword');

      // Should show error message
      await expect(loginPage.errorMessage).toBeVisible({ timeout: 5000 });
    });

    test('should validate email format', async ({ page, loginPage }) => {
      await loginPage.goto();
      await loginPage.waitForPageLoad();

      await loginPage.fillLoginForm('notanemail', 'password123');
      await loginPage.submit();

      // Form should show validation error or stay on login
      await expect(page).toHaveURL('/login');
    });

    test('should navigate to register page', async ({ page, loginPage }) => {
      await loginPage.goto();
      await loginPage.waitForPageLoad();

      await loginPage.goToRegister();

      await expect(page).toHaveURL('/register');
    });

    test('should navigate to forgot password page', async ({ page, loginPage }) => {
      await loginPage.goto();
      await loginPage.waitForPageLoad();

      await loginPage.goToForgotPassword();

      await expect(page).toHaveURL('/forgot-password');
    });
  });

  test.describe('Logout', () => {
    // This test uses authenticated state
    test.use({ storageState: './tests/e2e/.auth/user.json' });

    test('should logout successfully', async ({ page, hubPage }) => {
      await hubPage.goto();
      await hubPage.waitForPageLoad();

      // Click logout button (might be in header or sidebar)
      const logoutBtn = page.getByRole('button', { name: /deconnexion/i });
      await logoutBtn.click();

      // Should redirect to login page
      await expect(page).toHaveURL('/login', { timeout: 10000 });
    });

    test('should redirect to login when accessing protected route after logout', async ({ page, hubPage }) => {
      await hubPage.goto();
      await hubPage.waitForPageLoad();

      // Logout
      const logoutBtn = page.getByRole('button', { name: /deconnexion/i });
      await logoutBtn.click();
      await expect(page).toHaveURL('/login', { timeout: 10000 });

      // Clear storage to simulate logged out state
      await page.context().clearCookies();
      await page.evaluate(() => localStorage.clear());

      // Try to access protected route
      await page.goto('/hub');

      // Should redirect to login
      await expect(page).toHaveURL('/login', { timeout: 5000 });
    });
  });

  test.describe('Protected Routes', () => {
    test('should redirect to login when accessing hub without auth', async ({ page }) => {
      // Clear any auth state
      await page.context().clearCookies();
      await page.evaluate(() => localStorage.clear());

      await page.goto('/hub');

      // Should redirect to login
      await expect(page).toHaveURL('/login', { timeout: 5000 });
    });

    test('should redirect to login when accessing organization without auth', async ({ page }) => {
      await page.context().clearCookies();
      await page.evaluate(() => localStorage.clear());

      await page.goto('/organization/some-id/dashboard');

      // Should redirect to login
      await expect(page).toHaveURL('/login', { timeout: 5000 });
    });

    test('should redirect to login when accessing personal space without auth', async ({ page }) => {
      await page.context().clearCookies();
      await page.evaluate(() => localStorage.clear());

      await page.goto('/personal/dashboard');

      // Should redirect to login
      await expect(page).toHaveURL('/login', { timeout: 5000 });
    });
  });

  test.describe('Session Persistence', () => {
    test.use({ storageState: './tests/e2e/.auth/user.json' });

    test('should maintain session after page refresh', async ({ page, hubPage }) => {
      await hubPage.goto();
      await hubPage.waitForPageLoad();

      // Refresh the page
      await page.reload();

      // Should still be on hub page (authenticated)
      await expect(page).toHaveURL('/hub');
    });

    test('should maintain session when navigating between pages', async ({ page, hubPage }) => {
      await hubPage.goto();
      await hubPage.waitForPageLoad();

      // Navigate to personal space
      await hubPage.goToPersonalSpace();
      await expect(page).toHaveURL(/\/personal/);

      // Navigate back to hub
      await page.goto('/hub');
      await expect(page).toHaveURL('/hub');
    });
  });
});
