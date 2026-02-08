import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base.page';

/**
 * Page Object for the Organization pages
 * Handles organization dashboard, settings, and navigation
 */
export class OrganizationPage extends BasePage {
  // Navigation
  readonly sidebar: Locator;
  readonly dashboardLink: Locator;
  readonly tasksLink: Locator;
  readonly goalsLink: Locator;
  readonly calendarLink: Locator;
  readonly notesLink: Locator;
  readonly membersLink: Locator;
  readonly teamsLink: Locator;
  readonly analyticsLink: Locator;
  readonly settingsLink: Locator;
  readonly backToHubLink: Locator;

  // Dashboard elements
  readonly orgTitle: Locator;
  readonly statsCards: Locator;
  readonly activityFeed: Locator;

  // Settings elements
  readonly settingsForm: Locator;
  readonly orgNameInput: Locator;
  readonly orgDescriptionInput: Locator;
  readonly saveSettingsButton: Locator;
  readonly deleteOrgButton: Locator;
  readonly confirmDeleteInput: Locator;
  readonly confirmDeleteButton: Locator;

  constructor(page: Page) {
    super(page);

    // Sidebar navigation
    this.sidebar = page.locator('[data-testid="sidebar"], aside, nav').first();
    this.dashboardLink = page.getByRole('link', { name: /tableau de bord|dashboard/i });
    this.tasksLink = page.getByRole('link', { name: /taches/i });
    this.goalsLink = page.getByRole('link', { name: /objectifs/i });
    this.calendarLink = page.getByRole('link', { name: /calendrier/i });
    this.notesLink = page.getByRole('link', { name: /notes/i });
    this.membersLink = page.getByRole('link', { name: /membres/i });
    this.teamsLink = page.getByRole('link', { name: /equipes/i });
    this.analyticsLink = page.getByRole('link', { name: /analytics|statistiques/i });
    this.settingsLink = page.getByRole('link', { name: /parametres|settings/i });
    this.backToHubLink = page.getByRole('link', { name: /hub|retour/i });

    // Dashboard
    this.orgTitle = page.getByRole('heading', { level: 1 });
    this.statsCards = page.locator('[data-testid="stat-card"], .stat-card');
    this.activityFeed = page.locator('[data-testid="activity-feed"]');

    // Settings
    this.settingsForm = page.locator('form');
    this.orgNameInput = page.getByRole('textbox', { name: /nom/i });
    this.orgDescriptionInput = page.getByRole('textbox', { name: /description/i });
    this.saveSettingsButton = page.getByRole('button', { name: /enregistrer|sauvegarder/i });
    this.deleteOrgButton = page.getByRole('button', { name: /supprimer.*organisation/i });
    this.confirmDeleteInput = page.getByRole('textbox', { name: /confirmer|supprimer/i });
    this.confirmDeleteButton = page.getByRole('button', { name: /confirmer la suppression/i });
  }

  /**
   * Navigate to organization by ID
   */
  async goto(orgId?: string): Promise<void> {
    if (orgId) {
      await this.page.goto(`/organization/${orgId}/dashboard`);
    }
  }

  /**
   * Wait for the organization page to be fully loaded
   */
  async waitForPageLoad(): Promise<void> {
    await this.page.waitForURL(/\/organization\//);
    await this.sidebar.waitFor({ state: 'visible', timeout: 10000 });
  }

  /**
   * Navigate to tasks page
   */
  async goToTasks(): Promise<void> {
    await this.tasksLink.click();
    await this.page.waitForURL(/\/tasks/);
  }

  /**
   * Navigate to goals page
   */
  async goToGoals(): Promise<void> {
    await this.goalsLink.click();
    await this.page.waitForURL(/\/goals/);
  }

  /**
   * Navigate to calendar page
   */
  async goToCalendar(): Promise<void> {
    await this.calendarLink.click();
    await this.page.waitForURL(/\/calendar/);
  }

  /**
   * Navigate to notes page
   */
  async goToNotes(): Promise<void> {
    await this.notesLink.click();
    await this.page.waitForURL(/\/notes/);
  }

  /**
   * Navigate to members page
   */
  async goToMembers(): Promise<void> {
    await this.membersLink.click();
    await this.page.waitForURL(/\/members/);
  }

  /**
   * Navigate to teams page
   */
  async goToTeams(): Promise<void> {
    await this.teamsLink.click();
    await this.page.waitForURL(/\/teams/);
  }

  /**
   * Navigate to analytics page
   */
  async goToAnalytics(): Promise<void> {
    await this.analyticsLink.click();
    await this.page.waitForURL(/\/analytics/);
  }

  /**
   * Navigate to settings page
   */
  async goToSettings(): Promise<void> {
    await this.settingsLink.click();
    await this.page.waitForURL(/\/settings/);
  }

  /**
   * Go back to hub
   */
  async goBackToHub(): Promise<void> {
    await this.backToHubLink.click();
    await this.page.waitForURL('/hub');
  }

  /**
   * Get organization title
   */
  async getOrganizationTitle(): Promise<string> {
    return (await this.orgTitle.textContent()) || '';
  }

  /**
   * Update organization name
   */
  async updateOrganizationName(newName: string): Promise<void> {
    await this.goToSettings();
    await this.fillInput(this.orgNameInput, newName);
    await this.saveSettingsButton.click();
  }

  /**
   * Update organization description
   */
  async updateOrganizationDescription(newDescription: string): Promise<void> {
    await this.goToSettings();
    await this.fillInput(this.orgDescriptionInput, newDescription);
    await this.saveSettingsButton.click();
  }

  /**
   * Delete the organization
   */
  async deleteOrganization(confirmText: string = 'SUPPRIMER'): Promise<void> {
    await this.goToSettings();
    await this.deleteOrgButton.click();

    // Fill confirmation dialog
    const modal = this.page.locator('[role="dialog"]');
    await modal.waitFor({ state: 'visible' });

    const confirmInput = modal.getByRole('textbox');
    await confirmInput.fill(confirmText);

    const deleteBtn = modal.getByRole('button', { name: /supprimer|confirmer/i }).last();
    await deleteBtn.click();

    // Wait for navigation to hub
    await this.page.waitForURL('/hub', { timeout: 10000 });
  }

  /**
   * Verify we're on the organization page
   */
  async expectToBeOnOrganizationPage(): Promise<void> {
    await expect(this.page).toHaveURL(/\/organization\//);
    await expect(this.sidebar).toBeVisible();
  }

  /**
   * Verify we're on the dashboard
   */
  async expectToBeOnDashboard(): Promise<void> {
    await expect(this.page).toHaveURL(/\/dashboard/);
  }

  /**
   * Get current organization ID from URL
   */
  getOrganizationIdFromUrl(): string | null {
    const url = this.page.url();
    const match = url.match(/\/organization\/([^/]+)/);
    return match ? match[1] : null;
  }
}
