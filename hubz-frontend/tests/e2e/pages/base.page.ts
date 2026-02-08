import { Page, Locator, expect } from '@playwright/test';

/**
 * Base Page Object class that provides common functionality
 * All page objects should extend this class
 */
export abstract class BasePage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  /**
   * Navigate to the page URL
   */
  abstract goto(): Promise<void>;

  /**
   * Wait for the page to be fully loaded
   */
  abstract waitForPageLoad(): Promise<void>;

  /**
   * Get the page title
   */
  async getTitle(): Promise<string> {
    return await this.page.title();
  }

  /**
   * Get the current URL
   */
  getUrl(): string {
    return this.page.url();
  }

  /**
   * Wait for a specific URL pattern
   */
  async waitForUrl(urlPattern: string | RegExp, options?: { timeout?: number }): Promise<void> {
    await this.page.waitForURL(urlPattern, options);
  }

  /**
   * Wait for a network request to complete
   */
  async waitForRequest(urlPattern: string | RegExp): Promise<void> {
    await this.page.waitForRequest(urlPattern);
  }

  /**
   * Wait for a network response
   */
  async waitForResponse(urlPattern: string | RegExp): Promise<void> {
    await this.page.waitForResponse(urlPattern);
  }

  /**
   * Check if an element is visible
   */
  async isVisible(locator: Locator): Promise<boolean> {
    return await locator.isVisible();
  }

  /**
   * Wait for element to be visible
   */
  async waitForVisible(locator: Locator, timeout?: number): Promise<void> {
    await locator.waitFor({ state: 'visible', timeout });
  }

  /**
   * Wait for element to be hidden
   */
  async waitForHidden(locator: Locator, timeout?: number): Promise<void> {
    await locator.waitFor({ state: 'hidden', timeout });
  }

  /**
   * Fill an input field
   */
  async fillInput(locator: Locator, value: string): Promise<void> {
    await locator.clear();
    await locator.fill(value);
  }

  /**
   * Click an element
   */
  async click(locator: Locator): Promise<void> {
    await locator.click();
  }

  /**
   * Take a screenshot
   */
  async takeScreenshot(name: string): Promise<void> {
    await this.page.screenshot({ path: `./test-results/screenshots/${name}.png` });
  }

  /**
   * Get toast notification text
   */
  async getToastMessage(): Promise<string | null> {
    const toast = this.page.locator('[role="status"], .toast, [data-sonner-toast]').first();
    try {
      await toast.waitFor({ state: 'visible', timeout: 5000 });
      return await toast.textContent();
    } catch {
      return null;
    }
  }

  /**
   * Wait for toast to appear and verify message
   */
  async expectToast(expectedMessage: string | RegExp): Promise<void> {
    const toast = this.page.locator('[role="status"], .toast, [data-sonner-toast]').first();
    await expect(toast).toBeVisible({ timeout: 5000 });
    await expect(toast).toContainText(expectedMessage);
  }

  /**
   * Dismiss all toasts
   */
  async dismissToasts(): Promise<void> {
    const toasts = this.page.locator('[role="status"], .toast, [data-sonner-toast]');
    const count = await toasts.count();
    for (let i = 0; i < count; i++) {
      try {
        await toasts.nth(i).click();
      } catch {
        // Toast may have already disappeared
      }
    }
  }
}
