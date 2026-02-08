import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base.page';

/**
 * Page Object for the Register Page
 * Handles all interactions with the registration form
 */
export class RegisterPage extends BasePage {
  // Locators
  readonly firstNameInput: Locator;
  readonly lastNameInput: Locator;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly confirmPasswordInput: Locator;
  readonly submitButton: Locator;
  readonly errorMessage: Locator;
  readonly loginLink: Locator;
  readonly pageTitle: Locator;
  readonly successMessage: Locator;

  constructor(page: Page) {
    super(page);

    // Form elements - using more flexible selectors
    this.firstNameInput = page.getByRole('textbox', { name: /prenom/i });
    this.lastNameInput = page.getByRole('textbox', { name: /nom/i });
    this.emailInput = page.getByRole('textbox', { name: /email/i });
    this.passwordInput = page.locator('input[type="password"]').first();
    this.confirmPasswordInput = page.locator('input[type="password"]').nth(1);
    this.submitButton = page.getByRole('button', { name: /creer mon compte|s'inscrire/i });
    this.errorMessage = page.locator('.bg-error\\/10, [class*="error"]');
    this.loginLink = page.getByRole('link', { name: /se connecter/i });
    this.successMessage = page.locator('.bg-success\\/10, [class*="success"]');

    // Page identification
    this.pageTitle = page.getByRole('heading', { level: 1 });
  }

  /**
   * Navigate to the register page
   */
  async goto(): Promise<void> {
    await this.page.goto('/register');
  }

  /**
   * Wait for the register page to be fully loaded
   */
  async waitForPageLoad(): Promise<void> {
    await this.emailInput.waitFor({ state: 'visible' });
    await this.submitButton.waitFor({ state: 'visible' });
  }

  /**
   * Fill in the registration form
   */
  async fillRegistrationForm(data: {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    confirmPassword?: string;
  }): Promise<void> {
    await this.fillInput(this.firstNameInput, data.firstName);
    await this.fillInput(this.lastNameInput, data.lastName);
    await this.fillInput(this.emailInput, data.email);
    await this.fillInput(this.passwordInput, data.password);
    if (this.confirmPasswordInput) {
      await this.fillInput(this.confirmPasswordInput, data.confirmPassword || data.password);
    }
  }

  /**
   * Submit the registration form
   */
  async submit(): Promise<void> {
    await this.submitButton.click();
  }

  /**
   * Perform complete registration flow
   */
  async register(data: {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    confirmPassword?: string;
  }): Promise<void> {
    await this.fillRegistrationForm(data);
    await this.submit();
  }

  /**
   * Register and wait for navigation to hub
   */
  async registerAndWaitForHub(data: {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
  }): Promise<void> {
    await this.register(data);
    await this.waitForUrl('/hub', { timeout: 10000 });
  }

  /**
   * Check if registration error is displayed
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
   * Navigate to login page
   */
  async goToLogin(): Promise<void> {
    await this.loginLink.click();
    await this.page.waitForURL('/login');
  }

  /**
   * Verify register page is displayed
   */
  async expectToBeOnRegisterPage(): Promise<void> {
    await expect(this.firstNameInput).toBeVisible();
    await expect(this.lastNameInput).toBeVisible();
    await expect(this.emailInput).toBeVisible();
    await expect(this.passwordInput).toBeVisible();
    await expect(this.submitButton).toBeVisible();
  }

  /**
   * Generate a unique test email
   */
  static generateTestEmail(): string {
    const timestamp = Date.now();
    const random = Math.random().toString(36).substring(7);
    return `test-${timestamp}-${random}@hubz-e2e.test`;
  }

  /**
   * Generate test user data
   */
  static generateTestUser(): {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
  } {
    return {
      firstName: 'Test',
      lastName: 'User',
      email: RegisterPage.generateTestEmail(),
      password: 'TestPassword123!',
    };
  }
}
