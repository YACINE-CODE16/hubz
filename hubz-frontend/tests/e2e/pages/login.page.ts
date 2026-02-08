import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base.page';

/**
 * Page Object for the Login Page
 * Handles all interactions with the login form
 */
export class LoginPage extends BasePage {
  // Locators
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly submitButton: Locator;
  readonly errorMessage: Locator;
  readonly forgotPasswordLink: Locator;
  readonly registerLink: Locator;
  readonly googleLoginButton: Locator;
  readonly totpInput: Locator;
  readonly totpSubmitButton: Locator;
  readonly backToLoginButton: Locator;
  readonly pageTitle: Locator;

  constructor(page: Page) {
    super(page);

    // Form elements
    this.emailInput = page.getByRole('textbox', { name: /email/i });
    this.passwordInput = page.locator('input[type="password"]');
    this.submitButton = page.getByRole('button', { name: /se connecter/i });
    this.errorMessage = page.locator('.bg-error\\/10, [class*="error"]');
    this.forgotPasswordLink = page.getByRole('link', { name: /mot de passe oublie/i });
    this.registerLink = page.getByRole('link', { name: /creer un compte/i });
    this.googleLoginButton = page.getByRole('button', { name: /google/i });

    // 2FA elements
    this.totpInput = page.getByRole('textbox', { name: /code de verification/i });
    this.totpSubmitButton = page.getByRole('button', { name: /verifier/i });
    this.backToLoginButton = page.getByRole('button', { name: /retour/i });

    // Page identification
    this.pageTitle = page.getByRole('heading', { name: /hubz/i });
  }

  /**
   * Navigate to the login page
   */
  async goto(): Promise<void> {
    await this.page.goto('/login');
  }

  /**
   * Wait for the login page to be fully loaded
   */
  async waitForPageLoad(): Promise<void> {
    await this.emailInput.waitFor({ state: 'visible' });
    await this.submitButton.waitFor({ state: 'visible' });
  }

  /**
   * Fill in the login form
   */
  async fillLoginForm(email: string, password: string): Promise<void> {
    await this.fillInput(this.emailInput, email);
    await this.fillInput(this.passwordInput, password);
  }

  /**
   * Submit the login form
   */
  async submit(): Promise<void> {
    await this.submitButton.click();
  }

  /**
   * Perform complete login flow
   */
  async login(email: string, password: string): Promise<void> {
    await this.fillLoginForm(email, password);
    await this.submit();
  }

  /**
   * Login and wait for navigation to hub
   */
  async loginAndWaitForHub(email: string, password: string): Promise<void> {
    await this.login(email, password);
    await this.waitForUrl('/hub', { timeout: 10000 });
  }

  /**
   * Check if login error is displayed
   */
  async hasError(): Promise<boolean> {
    return await this.errorMessage.isVisible();
  }

  /**
   * Get error message text
   */
  async getErrorMessage(): Promise<string> {
    await this.errorMessage.waitFor({ state: 'visible' });
    return (await this.errorMessage.textContent()) || '';
  }

  /**
   * Check if 2FA form is displayed
   */
  async is2FAFormVisible(): Promise<boolean> {
    return await this.totpInput.isVisible();
  }

  /**
   * Fill and submit 2FA code
   */
  async submit2FACode(code: string): Promise<void> {
    await this.fillInput(this.totpInput, code);
    await this.totpSubmitButton.click();
  }

  /**
   * Navigate to forgot password page
   */
  async goToForgotPassword(): Promise<void> {
    await this.forgotPasswordLink.click();
    await this.page.waitForURL('/forgot-password');
  }

  /**
   * Navigate to register page
   */
  async goToRegister(): Promise<void> {
    await this.registerLink.click();
    await this.page.waitForURL('/register');
  }

  /**
   * Verify login page is displayed
   */
  async expectToBeOnLoginPage(): Promise<void> {
    await expect(this.pageTitle).toBeVisible();
    await expect(this.emailInput).toBeVisible();
    await expect(this.passwordInput).toBeVisible();
    await expect(this.submitButton).toBeVisible();
  }
}
