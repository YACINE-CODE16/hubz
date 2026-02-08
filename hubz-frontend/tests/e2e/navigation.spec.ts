import { test, expect, testData } from './fixtures';
import { HubPage, OrganizationPage, TasksPage } from './pages';

/**
 * Navigation E2E Tests
 * Tests navigation between different sections of the application
 */

test.describe('Application Navigation', () => {
  // Use authenticated state for all tests
  test.use({ storageState: './tests/e2e/.auth/user.json' });

  test.describe('Hub Navigation', () => {
    test('should display hub page after login', async ({ page, hubPage }) => {
      await hubPage.goto();
      await hubPage.waitForPageLoad();

      await hubPage.expectToBeOnHubPage();
    });

    test('should display user welcome message', async ({ page, hubPage }) => {
      await hubPage.goto();
      await hubPage.waitForPageLoad();

      // Check for some form of welcome or user indicator
      const header = page.locator('header, [role="banner"]');
      await expect(header).toBeVisible();
    });

    test('should navigate to personal space from hub', async ({ page, hubPage }) => {
      await hubPage.goto();
      await hubPage.waitForPageLoad();

      await hubPage.goToPersonalSpace();

      await expect(page).toHaveURL(/\/personal/);
    });

    test('should navigate to organization from hub', async ({ page, hubPage }) => {
      const orgName = testData.generateOrgName();

      await hubPage.goto();
      await hubPage.waitForPageLoad();

      // Create an organization
      await hubPage.createOrganization(orgName);
      await page.waitForTimeout(1000);

      // If not automatically navigated, click on it
      if (!page.url().includes('/organization/')) {
        await hubPage.clickOrganization(orgName);
      }

      await expect(page).toHaveURL(/\/organization\//);
    });
  });

  test.describe('Personal Space Navigation', () => {
    test('should navigate to personal dashboard', async ({ page }) => {
      await page.goto('/personal/dashboard');

      await expect(page).toHaveURL(/\/personal\/dashboard/);
    });

    test('should navigate to personal habits', async ({ page }) => {
      await page.goto('/personal/habits');

      await expect(page).toHaveURL(/\/personal\/habits/);
    });

    test('should navigate to personal goals', async ({ page }) => {
      await page.goto('/personal/goals');

      await expect(page).toHaveURL(/\/personal\/goals/);
    });

    test('should navigate to personal calendar', async ({ page }) => {
      await page.goto('/personal/calendar');

      await expect(page).toHaveURL(/\/personal\/calendar/);
    });

    test('should navigate to personal messages', async ({ page }) => {
      await page.goto('/personal/messages');

      await expect(page).toHaveURL(/\/personal\/messages/);
    });

    test('should navigate between personal sections using sidebar', async ({ page }) => {
      await page.goto('/personal/dashboard');

      // Navigate to habits using sidebar
      const habitsLink = page.getByRole('link', { name: /habitudes/i });
      await habitsLink.click();
      await expect(page).toHaveURL(/\/personal\/habits/);

      // Navigate to goals
      const goalsLink = page.getByRole('link', { name: /objectifs/i });
      await goalsLink.click();
      await expect(page).toHaveURL(/\/personal\/goals/);

      // Navigate to calendar
      const calendarLink = page.getByRole('link', { name: /calendrier/i });
      await calendarLink.click();
      await expect(page).toHaveURL(/\/personal\/calendar/);
    });

    test('should navigate back to hub from personal space', async ({ page, hubPage }) => {
      await page.goto('/personal/dashboard');

      // Find and click hub link
      const hubLink = page.getByRole('link', { name: /hub/i });
      await hubLink.click();

      await hubPage.expectToBeOnHubPage();
    });
  });

  test.describe('Organization Navigation', () => {
    test('should navigate between organization sections', async ({ page, hubPage, organizationPage }) => {
      const orgName = testData.generateOrgName();

      await hubPage.goto();
      await hubPage.waitForPageLoad();
      await hubPage.createOrganization(orgName);

      if (!page.url().includes('/organization/')) {
        await hubPage.clickOrganization(orgName);
      }

      await organizationPage.waitForPageLoad();

      // Navigate to Tasks
      await organizationPage.goToTasks();
      await expect(page).toHaveURL(/\/tasks/);

      // Navigate to Goals
      await organizationPage.goToGoals();
      await expect(page).toHaveURL(/\/goals/);

      // Navigate to Calendar
      await organizationPage.goToCalendar();
      await expect(page).toHaveURL(/\/calendar/);

      // Navigate to Notes
      await organizationPage.goToNotes();
      await expect(page).toHaveURL(/\/notes/);

      // Navigate to Members
      await organizationPage.goToMembers();
      await expect(page).toHaveURL(/\/members/);

      // Navigate to Teams
      await organizationPage.goToTeams();
      await expect(page).toHaveURL(/\/teams/);

      // Navigate to Analytics
      await organizationPage.goToAnalytics();
      await expect(page).toHaveURL(/\/analytics/);

      // Navigate to Settings
      await organizationPage.goToSettings();
      await expect(page).toHaveURL(/\/settings/);
    });

    test('should navigate back to hub from organization', async ({ page, hubPage, organizationPage }) => {
      const orgName = testData.generateOrgName();

      await hubPage.goto();
      await hubPage.waitForPageLoad();
      await hubPage.createOrganization(orgName);

      if (!page.url().includes('/organization/')) {
        await hubPage.clickOrganization(orgName);
      }

      await organizationPage.waitForPageLoad();
      await organizationPage.goBackToHub();

      await hubPage.expectToBeOnHubPage();
    });

    test('should preserve context when navigating within organization', async ({ page, hubPage, organizationPage }) => {
      const orgName = testData.generateOrgName();

      await hubPage.goto();
      await hubPage.waitForPageLoad();
      await hubPage.createOrganization(orgName);

      if (!page.url().includes('/organization/')) {
        await hubPage.clickOrganization(orgName);
      }

      await organizationPage.waitForPageLoad();

      // Get the organization ID from URL
      const orgId = organizationPage.getOrganizationIdFromUrl();
      expect(orgId).toBeTruthy();

      // Navigate to different sections and verify org ID is preserved
      await organizationPage.goToTasks();
      expect(page.url()).toContain(orgId);

      await organizationPage.goToGoals();
      expect(page.url()).toContain(orgId);

      await organizationPage.goToCalendar();
      expect(page.url()).toContain(orgId);
    });
  });

  test.describe('Settings Navigation', () => {
    test('should navigate to profile settings', async ({ page }) => {
      await page.goto('/personal/settings');

      await expect(page).toHaveURL(/\/personal\/settings/);
    });

    test('should navigate to security settings', async ({ page }) => {
      await page.goto('/personal/security');

      await expect(page).toHaveURL(/\/personal\/security/);
    });

    test('should navigate to preferences settings', async ({ page }) => {
      await page.goto('/personal/preferences');

      await expect(page).toHaveURL(/\/personal\/preferences/);
    });

    test('should navigate between settings pages', async ({ page }) => {
      await page.goto('/personal/settings');

      // Navigate to security
      const securityLink = page.getByRole('link', { name: /securite/i });
      if (await securityLink.isVisible()) {
        await securityLink.click();
        await expect(page).toHaveURL(/\/personal\/security/);
      }

      // Navigate to preferences
      const prefsLink = page.getByRole('link', { name: /preferences/i });
      if (await prefsLink.isVisible()) {
        await prefsLink.click();
        await expect(page).toHaveURL(/\/personal\/preferences/);
      }
    });
  });

  test.describe('Browser Navigation', () => {
    test('should handle browser back button', async ({ page, hubPage, organizationPage }) => {
      const orgName = testData.generateOrgName();

      await hubPage.goto();
      await hubPage.waitForPageLoad();
      await hubPage.createOrganization(orgName);

      if (!page.url().includes('/organization/')) {
        await hubPage.clickOrganization(orgName);
      }

      await organizationPage.waitForPageLoad();
      await organizationPage.goToTasks();
      await expect(page).toHaveURL(/\/tasks/);

      // Go back
      await page.goBack();
      await expect(page).toHaveURL(/\/dashboard/);

      // Go back again to hub
      await page.goBack();
      await hubPage.expectToBeOnHubPage();
    });

    test('should handle browser forward button', async ({ page, hubPage }) => {
      await hubPage.goto();
      await hubPage.waitForPageLoad();

      // Navigate to personal space
      await hubPage.goToPersonalSpace();
      await expect(page).toHaveURL(/\/personal/);

      // Go back
      await page.goBack();
      await hubPage.expectToBeOnHubPage();

      // Go forward
      await page.goForward();
      await expect(page).toHaveURL(/\/personal/);
    });

    test('should handle page refresh', async ({ page, hubPage }) => {
      await hubPage.goto();
      await hubPage.waitForPageLoad();

      // Refresh the page
      await page.reload();

      // Should still be on hub
      await hubPage.expectToBeOnHubPage();
    });
  });

  test.describe('Deep Linking', () => {
    test('should navigate directly to organization tasks', async ({ page, hubPage }) => {
      const orgName = testData.generateOrgName();

      await hubPage.goto();
      await hubPage.waitForPageLoad();
      await hubPage.createOrganization(orgName);
      await page.waitForTimeout(1000);

      // Get org ID from URL
      const url = page.url();
      const match = url.match(/\/organization\/([^/]+)/);
      if (match) {
        const orgId = match[1];

        // Navigate directly to tasks
        await page.goto(`/organization/${orgId}/tasks`);
        await expect(page).toHaveURL(/\/tasks/);
      }
    });

    test('should navigate directly to personal goals', async ({ page }) => {
      await page.goto('/personal/goals');

      await expect(page).toHaveURL(/\/personal\/goals/);
    });

    test('should redirect invalid organization to hub or 404', async ({ page }) => {
      // Navigate to a non-existent organization
      await page.goto('/organization/non-existent-id/dashboard');

      // Should either redirect to hub or show error
      await page.waitForTimeout(2000);

      // Check if redirected or showing error
      const currentUrl = page.url();
      const hasError = await page.locator('text=/error|not found|introuvable/i').isVisible();

      expect(currentUrl.includes('/hub') || hasError).toBeTruthy();
    });
  });

  test.describe('Keyboard Navigation', () => {
    test('should open search with Ctrl+K', async ({ page, hubPage }) => {
      await hubPage.goto();
      await hubPage.waitForPageLoad();

      // Press Ctrl+K
      await page.keyboard.press('Control+k');

      // Search modal or input should be visible
      const searchInput = page.getByPlaceholder(/rechercher/i);
      await expect(searchInput).toBeFocused({ timeout: 5000 });
    });

    test('should close modal with Escape', async ({ page, hubPage }) => {
      await hubPage.goto();
      await hubPage.waitForPageLoad();

      // Open create organization modal
      await hubPage.openCreateOrganizationModal();
      const modal = page.locator('[role="dialog"]');
      await expect(modal).toBeVisible();

      // Press Escape
      await page.keyboard.press('Escape');

      // Modal should be closed
      await expect(modal).toBeHidden();
    });
  });
});
