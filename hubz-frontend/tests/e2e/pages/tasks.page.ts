import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base.page';

/**
 * Page Object for the Tasks Page
 * Handles task management including Kanban board, list view, and task CRUD operations
 */
export class TasksPage extends BasePage {
  // View toggles
  readonly kanbanViewButton: Locator;
  readonly listViewButton: Locator;
  readonly calendarViewButton: Locator;

  // Kanban columns
  readonly todoColumn: Locator;
  readonly inProgressColumn: Locator;
  readonly doneColumn: Locator;

  // Task elements
  readonly createTaskButton: Locator;
  readonly taskCards: Locator;

  // Create task modal
  readonly createTaskModal: Locator;
  readonly taskTitleInput: Locator;
  readonly taskDescriptionInput: Locator;
  readonly taskPrioritySelect: Locator;
  readonly taskDueDateInput: Locator;
  readonly taskAssigneeSelect: Locator;
  readonly taskGoalSelect: Locator;
  readonly submitTaskButton: Locator;
  readonly cancelTaskButton: Locator;

  // Task detail modal
  readonly taskDetailModal: Locator;
  readonly taskDetailTitle: Locator;
  readonly taskDetailDescription: Locator;
  readonly taskDetailStatus: Locator;
  readonly taskDetailPriority: Locator;
  readonly editTaskButton: Locator;
  readonly deleteTaskButton: Locator;
  readonly closeDetailButton: Locator;
  readonly statusDropdown: Locator;

  // Search and filter
  readonly searchInput: Locator;
  readonly filterButton: Locator;

  constructor(page: Page) {
    super(page);

    // View toggle buttons
    this.kanbanViewButton = page.locator('button').filter({ hasText: /kanban/i });
    this.listViewButton = page.locator('button').filter({ hasText: /liste/i });
    this.calendarViewButton = page.locator('button').filter({ hasText: /calendrier/i });

    // Kanban columns - using data-testid or class patterns
    this.todoColumn = page.locator('[data-testid="column-TODO"], [data-status="TODO"]');
    this.inProgressColumn = page.locator('[data-testid="column-IN_PROGRESS"], [data-status="IN_PROGRESS"]');
    this.doneColumn = page.locator('[data-testid="column-DONE"], [data-status="DONE"]');

    // Task elements
    this.createTaskButton = page.getByRole('button', { name: /nouvelle tache|ajouter/i });
    this.taskCards = page.locator('[data-testid="task-card"], .task-card');

    // Create task modal elements
    this.createTaskModal = page.locator('[role="dialog"]');
    this.taskTitleInput = page.locator('[role="dialog"]').getByRole('textbox', { name: /titre/i });
    this.taskDescriptionInput = page.locator('[role="dialog"]').getByRole('textbox', { name: /description/i });
    this.taskPrioritySelect = page.locator('[role="dialog"]').locator('select, [role="combobox"]').filter({ hasText: /priorite/i });
    this.taskDueDateInput = page.locator('[role="dialog"]').locator('input[type="date"]');
    this.taskAssigneeSelect = page.locator('[role="dialog"]').locator('select, [role="combobox"]').filter({ hasText: /assigne/i });
    this.taskGoalSelect = page.locator('[role="dialog"]').locator('select, [role="combobox"]').filter({ hasText: /objectif/i });
    this.submitTaskButton = page.locator('[role="dialog"]').getByRole('button', { name: /creer|enregistrer/i });
    this.cancelTaskButton = page.locator('[role="dialog"]').getByRole('button', { name: /annuler/i });

    // Task detail modal
    this.taskDetailModal = page.locator('[role="dialog"]');
    this.taskDetailTitle = page.locator('[role="dialog"]').getByRole('heading');
    this.taskDetailDescription = page.locator('[role="dialog"]').locator('[data-testid="task-description"], .description');
    this.taskDetailStatus = page.locator('[role="dialog"]').locator('[data-testid="task-status"]');
    this.taskDetailPriority = page.locator('[role="dialog"]').locator('[data-testid="task-priority"]');
    this.editTaskButton = page.locator('[role="dialog"]').getByRole('button', { name: /modifier|editer/i });
    this.deleteTaskButton = page.locator('[role="dialog"]').getByRole('button', { name: /supprimer/i });
    this.closeDetailButton = page.locator('[role="dialog"]').getByRole('button', { name: /fermer/i });
    this.statusDropdown = page.locator('[role="dialog"]').locator('[data-testid="status-select"], select');

    // Search and filter
    this.searchInput = page.getByPlaceholder(/rechercher/i);
    this.filterButton = page.getByRole('button', { name: /filtrer/i });
  }

  /**
   * Navigate to tasks page (requires being in an organization context)
   */
  async goto(orgId?: string): Promise<void> {
    if (orgId) {
      await this.page.goto(`/organization/${orgId}/tasks`);
    }
  }

  /**
   * Wait for the tasks page to be fully loaded
   */
  async waitForPageLoad(): Promise<void> {
    await this.page.waitForURL(/\/tasks/);
    // Wait for either Kanban columns or task list to load
    await this.page.waitForSelector('[data-status], [data-testid*="task"], button', { timeout: 10000 });
  }

  /**
   * Switch to Kanban view
   */
  async switchToKanbanView(): Promise<void> {
    await this.kanbanViewButton.click();
  }

  /**
   * Switch to list view
   */
  async switchToListView(): Promise<void> {
    await this.listViewButton.click();
  }

  /**
   * Switch to calendar view
   */
  async switchToCalendarView(): Promise<void> {
    await this.calendarViewButton.click();
  }

  /**
   * Open the create task modal
   */
  async openCreateTaskModal(): Promise<void> {
    await this.createTaskButton.click();
    await this.createTaskModal.waitFor({ state: 'visible' });
  }

  /**
   * Fill task form
   */
  async fillTaskForm(data: {
    title: string;
    description?: string;
    priority?: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
    dueDate?: string;
  }): Promise<void> {
    // Find inputs in the modal
    const modal = this.page.locator('[role="dialog"]');

    const titleInput = modal.getByRole('textbox').first();
    await titleInput.fill(data.title);

    if (data.description) {
      const descInput = modal.getByRole('textbox').nth(1);
      if (await descInput.isVisible()) {
        await descInput.fill(data.description);
      }
    }

    if (data.priority) {
      const prioritySelect = modal.locator('select').first();
      if (await prioritySelect.isVisible()) {
        await prioritySelect.selectOption(data.priority);
      }
    }

    if (data.dueDate) {
      const dateInput = modal.locator('input[type="date"]');
      if (await dateInput.isVisible()) {
        await dateInput.fill(data.dueDate);
      }
    }
  }

  /**
   * Submit task form
   */
  async submitTaskForm(): Promise<void> {
    const submitBtn = this.page.locator('[role="dialog"]').getByRole('button', { name: /creer/i });
    await submitBtn.click();
  }

  /**
   * Create a new task
   */
  async createTask(data: {
    title: string;
    description?: string;
    priority?: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
    dueDate?: string;
  }): Promise<void> {
    await this.openCreateTaskModal();
    await this.fillTaskForm(data);
    await this.submitTaskForm();
    // Wait for modal to close
    await this.createTaskModal.waitFor({ state: 'hidden', timeout: 5000 });
  }

  /**
   * Get task card by title
   */
  getTaskCard(title: string): Locator {
    return this.page.locator('[data-testid="task-card"], .task-card').filter({ hasText: title });
  }

  /**
   * Click on a task card to open details
   */
  async openTaskDetail(title: string): Promise<void> {
    const card = this.getTaskCard(title);
    await card.click();
    await this.taskDetailModal.waitFor({ state: 'visible' });
  }

  /**
   * Check if task exists
   */
  async hasTask(title: string): Promise<boolean> {
    const card = this.getTaskCard(title);
    return await card.isVisible();
  }

  /**
   * Get task count in a column
   */
  async getTaskCountInColumn(status: 'TODO' | 'IN_PROGRESS' | 'DONE'): Promise<number> {
    const column = this.page.locator(`[data-status="${status}"], [data-testid="column-${status}"]`);
    const tasks = column.locator('[data-testid="task-card"], .task-card');
    return await tasks.count();
  }

  /**
   * Get total task count
   */
  async getTotalTaskCount(): Promise<number> {
    return await this.taskCards.count();
  }

  /**
   * Drag task from one column to another
   */
  async dragTaskToColumn(taskTitle: string, targetStatus: 'TODO' | 'IN_PROGRESS' | 'DONE'): Promise<void> {
    const taskCard = this.getTaskCard(taskTitle);
    const targetColumn = this.page.locator(`[data-status="${targetStatus}"], [data-testid="column-${targetStatus}"]`);

    // Perform drag and drop
    await taskCard.dragTo(targetColumn);
  }

  /**
   * Change task status from detail modal
   */
  async changeTaskStatus(taskTitle: string, newStatus: 'TODO' | 'IN_PROGRESS' | 'DONE'): Promise<void> {
    await this.openTaskDetail(taskTitle);

    // Find status dropdown/buttons in modal
    const modal = this.page.locator('[role="dialog"]');
    const statusBtn = modal.locator(`button, [role="option"]`).filter({ hasText: new RegExp(newStatus.replace('_', ' '), 'i') });

    if (await statusBtn.isVisible()) {
      await statusBtn.click();
    } else {
      // Try dropdown select
      const statusSelect = modal.locator('select');
      if (await statusSelect.isVisible()) {
        await statusSelect.selectOption(newStatus);
      }
    }

    // Close modal
    await this.closeTaskDetail();
  }

  /**
   * Close task detail modal
   */
  async closeTaskDetail(): Promise<void> {
    // Try clicking close button or clicking outside
    const closeBtn = this.page.locator('[role="dialog"]').locator('button[aria-label*="close"], button:has(svg)').first();
    if (await closeBtn.isVisible()) {
      await closeBtn.click();
    } else {
      // Click outside the modal
      await this.page.keyboard.press('Escape');
    }
    await this.taskDetailModal.waitFor({ state: 'hidden' });
  }

  /**
   * Delete a task
   */
  async deleteTask(title: string): Promise<void> {
    await this.openTaskDetail(title);

    const modal = this.page.locator('[role="dialog"]');
    const deleteBtn = modal.getByRole('button', { name: /supprimer/i });
    await deleteBtn.click();

    // Confirm deletion if there's a confirmation dialog
    const confirmBtn = this.page.getByRole('button', { name: /confirmer|supprimer/i }).last();
    if (await confirmBtn.isVisible()) {
      await confirmBtn.click();
    }

    await this.taskDetailModal.waitFor({ state: 'hidden' });
  }

  /**
   * Edit task title
   */
  async editTaskTitle(currentTitle: string, newTitle: string): Promise<void> {
    await this.openTaskDetail(currentTitle);

    const modal = this.page.locator('[role="dialog"]');
    const titleInput = modal.getByRole('textbox').first();
    await titleInput.clear();
    await titleInput.fill(newTitle);

    const saveBtn = modal.getByRole('button', { name: /enregistrer|sauvegarder/i });
    await saveBtn.click();

    await this.taskDetailModal.waitFor({ state: 'hidden' });
  }

  /**
   * Search for tasks
   */
  async searchTasks(query: string): Promise<void> {
    await this.fillInput(this.searchInput, query);
    // Wait for results to update
    await this.page.waitForTimeout(500);
  }

  /**
   * Verify we're on the tasks page
   */
  async expectToBeOnTasksPage(): Promise<void> {
    await expect(this.page).toHaveURL(/\/tasks/);
    await expect(this.createTaskButton).toBeVisible();
  }

  /**
   * Generate unique task title for testing
   */
  static generateTaskTitle(): string {
    const timestamp = Date.now();
    return `E2E Test Task ${timestamp}`;
  }
}
