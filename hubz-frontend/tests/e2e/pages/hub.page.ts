import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base.page';

/**
 * Page Object for the Hub Page
 * The main dashboard showing all organizations
 */
export class HubPage extends BasePage {
  // Locators
  readonly pageTitle: Locator;
  readonly welcomeMessage: Locator;
  readonly createOrganizationButton: Locator;
  readonly organizationCards: Locator;
  readonly searchInput: Locator;
  readonly personalSpaceLink: Locator;
  readonly logoutButton: Locator;
  readonly userMenu: Locator;
  readonly notificationButton: Locator;

  // Create organization modal
  readonly createOrgModal: Locator;
  readonly orgNameInput: Locator;
  readonly orgDescriptionInput: Locator;
  readonly orgSubmitButton: Locator;
  readonly orgCancelButton: Locator;

  constructor(page: Page) {
    super(page);

    // Main page elements
    this.pageTitle = page.getByRole('heading', { name: /hub|mes organisations/i });
    this.welcomeMessage = page.locator('[class*="welcome"], h1, h2').filter({ hasText: /bonjour|bienvenue/i });
    this.createOrganizationButton = page.getByRole('button', { name: /nouvelle organisation|creer|ajouter/i });
    this.organizationCards = page.locator('[data-testid="organization-card"], .space-card, [class*="SpaceCard"]');
    this.searchInput = page.getByPlaceholder(/rechercher/i);
    this.personalSpaceLink = page.getByRole('link', { name: /espace personnel|personnel/i });

    // Header elements
    this.userMenu = page.locator('[data-testid="user-menu"], [class*="user-menu"]');
    this.logoutButton = page.getByRole('button', { name: /deconnexion|logout/i });
    this.notificationButton = page.locator('[data-testid="notification-button"], [aria-label*="notification"]');

    // Create organization modal
    this.createOrgModal = page.locator('[role="dialog"], .modal');
    this.orgNameInput = page.getByRole('textbox', { name: /nom.*organisation/i });
    this.orgDescriptionInput = page.getByRole('textbox', { name: /description/i });
    this.orgSubmitButton = page.getByRole('button', { name: /creer|enregistrer/i });
    this.orgCancelButton = page.getByRole('button', { name: /annuler/i });
  }

  /**
   * Navigate to the hub page
   */
  async goto(): Promise<void> {
    await this.page.goto('/hub');
  }

  /**
   * Wait for the hub page to be fully loaded
   */
  async waitForPageLoad(): Promise<void> {
    await this.page.waitForURL('/hub');
    // Wait for either the create button or an organization card
    await this.page.waitForSelector('[data-testid="organization-card"], button', { timeout: 10000 });
  }

  /**
   * Verify hub page is displayed
   */
  async expectToBeOnHubPage(): Promise<void> {
    await expect(this.page).toHaveURL('/hub');
  }

  /**
   * Open the create organization modal
   */
  async openCreateOrganizationModal(): Promise<void> {
    await this.createOrganizationButton.click();
    await this.createOrgModal.waitFor({ state: 'visible' });
  }

  /**
   * Fill organization creation form
   */
  async fillOrganizationForm(name: string, description?: string): Promise<void> {
    // Find the name input in the modal
    const nameInput = this.page.locator('[role="dialog"]').getByRole('textbox').first();
    await nameInput.fill(name);

    if (description) {
      const descInput = this.page.locator('[role="dialog"]').getByRole('textbox').nth(1);
      await descInput.fill(description);
    }
  }

  /**
   * Submit the create organization form
   */
  async submitOrganizationForm(): Promise<void> {
    const submitButton = this.page.locator('[role="dialog"]').getByRole('button', { name: /creer/i });
    await submitButton.click();
  }

  /**
   * Create a new organization
   */
  async createOrganization(name: string, description?: string): Promise<void> {
    await this.openCreateOrganizationModal();
    await this.fillOrganizationForm(name, description);
    await this.submitOrganizationForm();
    // Wait for modal to close
    await this.createOrgModal.waitFor({ state: 'hidden', timeout: 5000 });
  }

  /**
   * Get organization card by name
   */
  getOrganizationCard(name: string): Locator {
    return this.page.locator('[data-testid="organization-card"], .space-card').filter({ hasText: name });
  }

  /**
   * Click on an organization card
   */
  async clickOrganization(name: string): Promise<void> {
    const card = this.getOrganizationCard(name);
    await card.click();
  }

  /**
   * Check if organization exists
   */
  async hasOrganization(name: string): Promise<boolean> {
    const card = this.getOrganizationCard(name);
    return await card.isVisible();
  }

  /**
   * Get count of organizations
   */
  async getOrganizationCount(): Promise<number> {
    return await this.organizationCards.count();
  }

  /**
   * Search for organizations
   */
  async search(query: string): Promise<void> {
    await this.fillInput(this.searchInput, query);
    // Wait for search results to update
    await this.page.waitForTimeout(500);
  }

  /**
   * Navigate to personal space
   */
  async goToPersonalSpace(): Promise<void> {
    await this.personalSpaceLink.click();
    await this.page.waitForURL(/\/personal/);
  }

  /**
   * Logout from the application
   */
  async logout(): Promise<void> {
    // Look for logout button in header or sidebar
    const logoutBtn = this.page.getByRole('button', { name: /deconnexion/i });
    if (await logoutBtn.isVisible()) {
      await logoutBtn.click();
    } else {
      // Try opening user menu first
      const userMenuBtn = this.page.locator('[data-testid="user-menu"]');
      if (await userMenuBtn.isVisible()) {
        await userMenuBtn.click();
        await logoutBtn.click();
      }
    }
    await this.page.waitForURL('/login');
  }

  /**
   * Open organization options menu
   */
  async openOrganizationMenu(orgName: string): Promise<void> {
    const card = this.getOrganizationCard(orgName);
    const menuButton = card.locator('[data-testid="org-menu"], [aria-label*="menu"], button').filter({ hasText: '' });
    await menuButton.click();
  }

  /**
   * Generate unique organization name for testing
   */
  static generateOrgName(): string {
    const timestamp = Date.now();
    return `E2E Test Org ${timestamp}`;
  }
}
