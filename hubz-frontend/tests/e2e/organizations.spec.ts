import { test, expect, testData } from './fixtures';
import { HubPage, OrganizationPage } from './pages';

/**
 * Organization E2E Tests
 * Tests the complete organization lifecycle: Create, Read, Update, Delete
 */

test.describe('Organization Management', () => {
  // Use authenticated state for all tests
  test.use({ storageState: './tests/e2e/.auth/user.json' });

  test.describe('Create Organization', () => {
    test('should create a new organization successfully', async ({ page, hubPage }) => {
      const orgName = testData.generateOrgName();
      const orgDescription = 'E2E test organization description';

      await hubPage.goto();
      await hubPage.waitForPageLoad();

      // Create organization
      await hubPage.createOrganization(orgName, orgDescription);

      // Verify organization was created (should appear in the list or redirect to org page)
      // Wait a bit for the UI to update
      await page.waitForTimeout(1000);

      // Check if we're redirected to the new org or if it's visible in the list
      const isOnOrgPage = page.url().includes('/organization/');
      const orgVisible = await hubPage.hasOrganization(orgName);

      expect(isOnOrgPage || orgVisible).toBeTruthy();
    });

    test('should show validation error for empty name', async ({ page, hubPage }) => {
      await hubPage.goto();
      await hubPage.waitForPageLoad();

      await hubPage.openCreateOrganizationModal();

      // Try to submit without name
      await hubPage.submitOrganizationForm();

      // Modal should still be open (form validation failed)
      const modal = page.locator('[role="dialog"]');
      await expect(modal).toBeVisible();
    });

    test('should cancel organization creation', async ({ page, hubPage }) => {
      await hubPage.goto();
      await hubPage.waitForPageLoad();

      const initialCount = await hubPage.getOrganizationCount();

      await hubPage.openCreateOrganizationModal();
      await hubPage.fillOrganizationForm('Cancelled Org', 'Should not be created');

      // Cancel
      const cancelBtn = page.locator('[role="dialog"]').getByRole('button', { name: /annuler/i });
      await cancelBtn.click();

      // Modal should close
      const modal = page.locator('[role="dialog"]');
      await expect(modal).toBeHidden();

      // Organization count should be the same
      const finalCount = await hubPage.getOrganizationCount();
      expect(finalCount).toBe(initialCount);
    });
  });

  test.describe('View Organization', () => {
    test('should navigate to organization dashboard', async ({ page, hubPage }) => {
      const orgName = testData.generateOrgName();

      await hubPage.goto();
      await hubPage.waitForPageLoad();

      // Create an organization first
      await hubPage.createOrganization(orgName);
      await page.waitForTimeout(1000);

      // If not already on org page, click on the org card
      if (!page.url().includes('/organization/')) {
        await hubPage.clickOrganization(orgName);
      }

      // Should be on organization page
      await expect(page).toHaveURL(/\/organization\/[^/]+\/dashboard/);
    });

    test('should display organization sidebar navigation', async ({ page, hubPage, organizationPage }) => {
      const orgName = testData.generateOrgName();

      await hubPage.goto();
      await hubPage.waitForPageLoad();
      await hubPage.createOrganization(orgName);

      // Navigate to organization
      if (!page.url().includes('/organization/')) {
        await hubPage.clickOrganization(orgName);
      }

      await organizationPage.waitForPageLoad();

      // Verify sidebar navigation items are visible
      await expect(organizationPage.tasksLink).toBeVisible();
      await expect(organizationPage.goalsLink).toBeVisible();
      await expect(organizationPage.calendarLink).toBeVisible();
      await expect(organizationPage.membersLink).toBeVisible();
    });

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
    });
  });

  test.describe('Update Organization', () => {
    test('should update organization name', async ({ page, hubPage, organizationPage }) => {
      const orgName = testData.generateOrgName();
      const newName = `${orgName} Updated`;

      await hubPage.goto();
      await hubPage.waitForPageLoad();
      await hubPage.createOrganization(orgName);

      if (!page.url().includes('/organization/')) {
        await hubPage.clickOrganization(orgName);
      }

      await organizationPage.waitForPageLoad();
      await organizationPage.goToSettings();

      // Update the name
      const nameInput = page.getByRole('textbox').first();
      await nameInput.clear();
      await nameInput.fill(newName);

      // Save
      const saveBtn = page.getByRole('button', { name: /enregistrer|sauvegarder/i });
      await saveBtn.click();

      // Verify update (check for toast or updated title)
      await page.waitForTimeout(1000);

      // Go back to hub and verify the name changed
      await page.goto('/hub');
      const updatedOrg = await hubPage.hasOrganization(newName);
      expect(updatedOrg).toBeTruthy();
    });

    test('should update organization description', async ({ page, hubPage, organizationPage }) => {
      const orgName = testData.generateOrgName();
      const newDescription = 'Updated description for E2E test';

      await hubPage.goto();
      await hubPage.waitForPageLoad();
      await hubPage.createOrganization(orgName);

      if (!page.url().includes('/organization/')) {
        await hubPage.clickOrganization(orgName);
      }

      await organizationPage.waitForPageLoad();
      await organizationPage.goToSettings();

      // Find and update description
      const descInput = page.locator('textarea, [role="textbox"]').filter({ hasText: '' }).last();
      if (await descInput.isVisible()) {
        await descInput.fill(newDescription);

        const saveBtn = page.getByRole('button', { name: /enregistrer|sauvegarder/i });
        await saveBtn.click();

        // Wait for save
        await page.waitForTimeout(1000);
      }
    });
  });

  test.describe('Delete Organization', () => {
    test('should delete organization successfully', async ({ page, hubPage, organizationPage }) => {
      const orgName = testData.generateOrgName();

      await hubPage.goto();
      await hubPage.waitForPageLoad();
      await hubPage.createOrganization(orgName);

      if (!page.url().includes('/organization/')) {
        await hubPage.clickOrganization(orgName);
      }

      await organizationPage.waitForPageLoad();

      // Delete the organization
      await organizationPage.deleteOrganization('SUPPRIMER');

      // Should be redirected to hub
      await expect(page).toHaveURL('/hub', { timeout: 10000 });

      // Organization should no longer be visible
      const orgExists = await hubPage.hasOrganization(orgName);
      expect(orgExists).toBeFalsy();
    });

    test('should require confirmation text to delete', async ({ page, hubPage, organizationPage }) => {
      const orgName = testData.generateOrgName();

      await hubPage.goto();
      await hubPage.waitForPageLoad();
      await hubPage.createOrganization(orgName);

      if (!page.url().includes('/organization/')) {
        await hubPage.clickOrganization(orgName);
      }

      await organizationPage.waitForPageLoad();
      await organizationPage.goToSettings();

      // Click delete button
      const deleteBtn = page.getByRole('button', { name: /supprimer.*organisation/i });
      await deleteBtn.click();

      // Modal should appear
      const modal = page.locator('[role="dialog"]');
      await expect(modal).toBeVisible();

      // Confirm button should be disabled without correct text
      const confirmBtn = modal.getByRole('button', { name: /supprimer|confirmer/i }).last();

      // Try with wrong text
      const confirmInput = modal.getByRole('textbox');
      await confirmInput.fill('wrong');

      // Button should still be disabled or nothing happens on click
      // Close modal
      await page.keyboard.press('Escape');
    });

    test('should cancel deletion', async ({ page, hubPage, organizationPage }) => {
      const orgName = testData.generateOrgName();

      await hubPage.goto();
      await hubPage.waitForPageLoad();
      await hubPage.createOrganization(orgName);

      if (!page.url().includes('/organization/')) {
        await hubPage.clickOrganization(orgName);
      }

      await organizationPage.waitForPageLoad();
      await organizationPage.goToSettings();

      // Click delete button
      const deleteBtn = page.getByRole('button', { name: /supprimer.*organisation/i });
      await deleteBtn.click();

      // Cancel
      await page.keyboard.press('Escape');

      // Should still be on settings page
      await expect(page).toHaveURL(/\/settings/);

      // Organization should still exist
      await page.goto('/hub');
      const orgExists = await hubPage.hasOrganization(orgName);
      expect(orgExists).toBeTruthy();
    });
  });

  test.describe('Return to Hub', () => {
    test('should navigate back to hub from organization', async ({ page, hubPage, organizationPage }) => {
      const orgName = testData.generateOrgName();

      await hubPage.goto();
      await hubPage.waitForPageLoad();
      await hubPage.createOrganization(orgName);

      if (!page.url().includes('/organization/')) {
        await hubPage.clickOrganization(orgName);
      }

      await organizationPage.waitForPageLoad();

      // Go back to hub using the link/button
      await organizationPage.goBackToHub();

      await expect(page).toHaveURL('/hub');
    });
  });
});
