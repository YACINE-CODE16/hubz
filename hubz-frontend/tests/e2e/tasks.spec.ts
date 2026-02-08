import { test, expect, testData } from './fixtures';
import { HubPage, OrganizationPage, TasksPage } from './pages';

/**
 * Task Management E2E Tests
 * Tests task CRUD operations, status changes, and Kanban board interactions
 */

test.describe('Task Management', () => {
  // Use authenticated state for all tests
  test.use({ storageState: './tests/e2e/.auth/user.json' });

  // Helper to create an organization and navigate to tasks
  async function setupOrgAndNavigateToTasks(
    hubPage: HubPage,
    organizationPage: OrganizationPage,
    tasksPage: TasksPage
  ): Promise<void> {
    const orgName = testData.generateOrgName();

    await hubPage.goto();
    await hubPage.waitForPageLoad();
    await hubPage.createOrganization(orgName);

    // Navigate to organization if not already there
    if (!hubPage.page.url().includes('/organization/')) {
      await hubPage.clickOrganization(orgName);
    }

    await organizationPage.waitForPageLoad();
    await organizationPage.goToTasks();
    await tasksPage.waitForPageLoad();
  }

  test.describe('Create Task', () => {
    test('should create a new task successfully', async ({ page, hubPage, organizationPage, tasksPage }) => {
      await setupOrgAndNavigateToTasks(hubPage, organizationPage, tasksPage);

      const taskTitle = testData.generateTaskTitle();

      await tasksPage.createTask({
        title: taskTitle,
        description: 'E2E test task description',
      });

      // Verify task was created
      await page.waitForTimeout(1000);
      const taskExists = await tasksPage.hasTask(taskTitle);
      expect(taskExists).toBeTruthy();
    });

    test('should create task with priority', async ({ page, hubPage, organizationPage, tasksPage }) => {
      await setupOrgAndNavigateToTasks(hubPage, organizationPage, tasksPage);

      const taskTitle = testData.generateTaskTitle();

      await tasksPage.createTask({
        title: taskTitle,
        priority: 'HIGH',
      });

      await page.waitForTimeout(1000);
      const taskExists = await tasksPage.hasTask(taskTitle);
      expect(taskExists).toBeTruthy();
    });

    test('should create task with due date', async ({ page, hubPage, organizationPage, tasksPage }) => {
      await setupOrgAndNavigateToTasks(hubPage, organizationPage, tasksPage);

      const taskTitle = testData.generateTaskTitle();
      const tomorrow = new Date();
      tomorrow.setDate(tomorrow.getDate() + 1);
      const dueDate = tomorrow.toISOString().split('T')[0];

      await tasksPage.createTask({
        title: taskTitle,
        dueDate: dueDate,
      });

      await page.waitForTimeout(1000);
      const taskExists = await tasksPage.hasTask(taskTitle);
      expect(taskExists).toBeTruthy();
    });

    test('should show validation error for empty title', async ({ page, hubPage, organizationPage, tasksPage }) => {
      await setupOrgAndNavigateToTasks(hubPage, organizationPage, tasksPage);

      await tasksPage.openCreateTaskModal();

      // Try to submit without title
      await tasksPage.submitTaskForm();

      // Modal should still be open
      const modal = page.locator('[role="dialog"]');
      await expect(modal).toBeVisible();
    });

    test('should cancel task creation', async ({ page, hubPage, organizationPage, tasksPage }) => {
      await setupOrgAndNavigateToTasks(hubPage, organizationPage, tasksPage);

      const initialCount = await tasksPage.getTotalTaskCount();

      await tasksPage.openCreateTaskModal();
      await tasksPage.fillTaskForm({ title: 'Cancelled Task' });

      // Cancel
      const cancelBtn = page.locator('[role="dialog"]').getByRole('button', { name: /annuler/i });
      await cancelBtn.click();

      // Modal should close
      const modal = page.locator('[role="dialog"]');
      await expect(modal).toBeHidden();

      // Task count should be the same
      const finalCount = await tasksPage.getTotalTaskCount();
      expect(finalCount).toBe(initialCount);
    });
  });

  test.describe('View Tasks', () => {
    test('should display task in Kanban view', async ({ page, hubPage, organizationPage, tasksPage }) => {
      await setupOrgAndNavigateToTasks(hubPage, organizationPage, tasksPage);

      const taskTitle = testData.generateTaskTitle();
      await tasksPage.createTask({ title: taskTitle });
      await page.waitForTimeout(1000);

      // Should be in TODO column by default
      await tasksPage.switchToKanbanView();
      const taskCard = tasksPage.getTaskCard(taskTitle);
      await expect(taskCard).toBeVisible();
    });

    test('should switch to list view', async ({ page, hubPage, organizationPage, tasksPage }) => {
      await setupOrgAndNavigateToTasks(hubPage, organizationPage, tasksPage);

      const taskTitle = testData.generateTaskTitle();
      await tasksPage.createTask({ title: taskTitle });
      await page.waitForTimeout(1000);

      // Switch to list view
      await tasksPage.switchToListView();

      // Task should still be visible
      const taskCard = tasksPage.getTaskCard(taskTitle);
      await expect(taskCard).toBeVisible();
    });

    test('should switch to calendar view', async ({ page, hubPage, organizationPage, tasksPage }) => {
      await setupOrgAndNavigateToTasks(hubPage, organizationPage, tasksPage);

      // Create task with due date
      const taskTitle = testData.generateTaskTitle();
      const today = new Date().toISOString().split('T')[0];

      await tasksPage.createTask({
        title: taskTitle,
        dueDate: today,
      });
      await page.waitForTimeout(1000);

      // Switch to calendar view
      await tasksPage.switchToCalendarView();

      // Calendar should be displayed
      await expect(page.locator('.calendar, [class*="calendar"]')).toBeVisible();
    });

    test('should open task detail modal', async ({ page, hubPage, organizationPage, tasksPage }) => {
      await setupOrgAndNavigateToTasks(hubPage, organizationPage, tasksPage);

      const taskTitle = testData.generateTaskTitle();
      await tasksPage.createTask({
        title: taskTitle,
        description: 'Test description',
      });
      await page.waitForTimeout(1000);

      // Click on task to open details
      await tasksPage.openTaskDetail(taskTitle);

      // Modal should be visible
      const modal = page.locator('[role="dialog"]');
      await expect(modal).toBeVisible();

      // Task title should be in the modal
      await expect(modal).toContainText(taskTitle);
    });
  });

  test.describe('Update Task', () => {
    test('should update task title', async ({ page, hubPage, organizationPage, tasksPage }) => {
      await setupOrgAndNavigateToTasks(hubPage, organizationPage, tasksPage);

      const originalTitle = testData.generateTaskTitle();
      const newTitle = `${originalTitle} Updated`;

      await tasksPage.createTask({ title: originalTitle });
      await page.waitForTimeout(1000);

      // Edit task
      await tasksPage.editTaskTitle(originalTitle, newTitle);
      await page.waitForTimeout(1000);

      // Original title should not exist
      const originalExists = await tasksPage.hasTask(originalTitle);
      expect(originalExists).toBeFalsy();

      // New title should exist
      const newExists = await tasksPage.hasTask(newTitle);
      expect(newExists).toBeTruthy();
    });

    test('should change task status to In Progress', async ({ page, hubPage, organizationPage, tasksPage }) => {
      await setupOrgAndNavigateToTasks(hubPage, organizationPage, tasksPage);

      const taskTitle = testData.generateTaskTitle();
      await tasksPage.createTask({ title: taskTitle });
      await page.waitForTimeout(1000);

      // Change status
      await tasksPage.changeTaskStatus(taskTitle, 'IN_PROGRESS');
      await page.waitForTimeout(1000);

      // Verify task moved (in Kanban view)
      await tasksPage.switchToKanbanView();
      const inProgressCount = await tasksPage.getTaskCountInColumn('IN_PROGRESS');
      expect(inProgressCount).toBeGreaterThan(0);
    });

    test('should change task status to Done', async ({ page, hubPage, organizationPage, tasksPage }) => {
      await setupOrgAndNavigateToTasks(hubPage, organizationPage, tasksPage);

      const taskTitle = testData.generateTaskTitle();
      await tasksPage.createTask({ title: taskTitle });
      await page.waitForTimeout(1000);

      // Change status to Done
      await tasksPage.changeTaskStatus(taskTitle, 'DONE');
      await page.waitForTimeout(1000);

      // Verify task moved
      await tasksPage.switchToKanbanView();
      const doneCount = await tasksPage.getTaskCountInColumn('DONE');
      expect(doneCount).toBeGreaterThan(0);
    });
  });

  test.describe('Kanban Drag and Drop', () => {
    test('should drag task from TODO to IN_PROGRESS', async ({ page, hubPage, organizationPage, tasksPage }) => {
      await setupOrgAndNavigateToTasks(hubPage, organizationPage, tasksPage);

      const taskTitle = testData.generateTaskTitle();
      await tasksPage.createTask({ title: taskTitle });
      await page.waitForTimeout(1000);

      // Ensure we're in Kanban view
      await tasksPage.switchToKanbanView();

      // Get initial counts
      const initialTodoCount = await tasksPage.getTaskCountInColumn('TODO');
      const initialInProgressCount = await tasksPage.getTaskCountInColumn('IN_PROGRESS');

      // Drag task
      await tasksPage.dragTaskToColumn(taskTitle, 'IN_PROGRESS');
      await page.waitForTimeout(1000);

      // Verify counts changed
      const finalTodoCount = await tasksPage.getTaskCountInColumn('TODO');
      const finalInProgressCount = await tasksPage.getTaskCountInColumn('IN_PROGRESS');

      expect(finalTodoCount).toBe(initialTodoCount - 1);
      expect(finalInProgressCount).toBe(initialInProgressCount + 1);
    });

    test('should drag task from IN_PROGRESS to DONE', async ({ page, hubPage, organizationPage, tasksPage }) => {
      await setupOrgAndNavigateToTasks(hubPage, organizationPage, tasksPage);

      const taskTitle = testData.generateTaskTitle();
      await tasksPage.createTask({ title: taskTitle });
      await page.waitForTimeout(1000);

      await tasksPage.switchToKanbanView();

      // First move to IN_PROGRESS
      await tasksPage.dragTaskToColumn(taskTitle, 'IN_PROGRESS');
      await page.waitForTimeout(500);

      // Then move to DONE
      await tasksPage.dragTaskToColumn(taskTitle, 'DONE');
      await page.waitForTimeout(1000);

      const doneCount = await tasksPage.getTaskCountInColumn('DONE');
      expect(doneCount).toBeGreaterThan(0);
    });
  });

  test.describe('Delete Task', () => {
    test('should delete task successfully', async ({ page, hubPage, organizationPage, tasksPage }) => {
      await setupOrgAndNavigateToTasks(hubPage, organizationPage, tasksPage);

      const taskTitle = testData.generateTaskTitle();
      await tasksPage.createTask({ title: taskTitle });
      await page.waitForTimeout(1000);

      // Verify task exists
      let taskExists = await tasksPage.hasTask(taskTitle);
      expect(taskExists).toBeTruthy();

      // Delete task
      await tasksPage.deleteTask(taskTitle);
      await page.waitForTimeout(1000);

      // Verify task is gone
      taskExists = await tasksPage.hasTask(taskTitle);
      expect(taskExists).toBeFalsy();
    });

    test('should cancel task deletion', async ({ page, hubPage, organizationPage, tasksPage }) => {
      await setupOrgAndNavigateToTasks(hubPage, organizationPage, tasksPage);

      const taskTitle = testData.generateTaskTitle();
      await tasksPage.createTask({ title: taskTitle });
      await page.waitForTimeout(1000);

      // Open task detail
      await tasksPage.openTaskDetail(taskTitle);

      // Click delete but then cancel
      const modal = page.locator('[role="dialog"]');
      const deleteBtn = modal.getByRole('button', { name: /supprimer/i });
      await deleteBtn.click();

      // If there's a confirmation dialog, press escape to cancel
      await page.keyboard.press('Escape');
      await page.waitForTimeout(500);

      // Close the detail modal
      await page.keyboard.press('Escape');
      await page.waitForTimeout(500);

      // Task should still exist
      const taskExists = await tasksPage.hasTask(taskTitle);
      expect(taskExists).toBeTruthy();
    });
  });

  test.describe('Task Search', () => {
    test('should filter tasks by search query', async ({ page, hubPage, organizationPage, tasksPage }) => {
      await setupOrgAndNavigateToTasks(hubPage, organizationPage, tasksPage);

      // Create multiple tasks
      const uniquePrefix = `Search${Date.now()}`;
      const task1 = `${uniquePrefix} Task One`;
      const task2 = `${uniquePrefix} Task Two`;
      const task3 = `Different Task`;

      await tasksPage.createTask({ title: task1 });
      await tasksPage.createTask({ title: task2 });
      await tasksPage.createTask({ title: task3 });
      await page.waitForTimeout(1000);

      // Search for the unique prefix
      await tasksPage.searchTasks(uniquePrefix);
      await page.waitForTimeout(1000);

      // Should show matching tasks
      const task1Visible = await tasksPage.hasTask(task1);
      const task2Visible = await tasksPage.hasTask(task2);

      expect(task1Visible).toBeTruthy();
      expect(task2Visible).toBeTruthy();
    });
  });
});
