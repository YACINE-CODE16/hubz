import { test as base } from '@playwright/test';
import { LoginPage, RegisterPage, HubPage, OrganizationPage, TasksPage } from './pages';

/**
 * Test fixtures that provide page objects and common test utilities
 */

// Extend base test with our page objects
type TestFixtures = {
  loginPage: LoginPage;
  registerPage: RegisterPage;
  hubPage: HubPage;
  organizationPage: OrganizationPage;
  tasksPage: TasksPage;
};

// Create the extended test
export const test = base.extend<TestFixtures>({
  loginPage: async ({ page }, use) => {
    const loginPage = new LoginPage(page);
    await use(loginPage);
  },

  registerPage: async ({ page }, use) => {
    const registerPage = new RegisterPage(page);
    await use(registerPage);
  },

  hubPage: async ({ page }, use) => {
    const hubPage = new HubPage(page);
    await use(hubPage);
  },

  organizationPage: async ({ page }, use) => {
    const organizationPage = new OrganizationPage(page);
    await use(organizationPage);
  },

  tasksPage: async ({ page }, use) => {
    const tasksPage = new TasksPage(page);
    await use(tasksPage);
  },
});

export { expect } from '@playwright/test';

/**
 * Test data generators
 */
export const testData = {
  /**
   * Generate a unique email for testing
   */
  generateEmail(): string {
    const timestamp = Date.now();
    const random = Math.random().toString(36).substring(7);
    return `e2e-test-${timestamp}-${random}@hubz.test`;
  },

  /**
   * Generate test user credentials
   */
  generateUser(): { firstName: string; lastName: string; email: string; password: string } {
    return {
      firstName: 'E2E',
      lastName: 'TestUser',
      email: this.generateEmail(),
      password: 'TestPassword123!',
    };
  },

  /**
   * Generate organization name
   */
  generateOrgName(): string {
    const timestamp = Date.now();
    return `E2E Test Org ${timestamp}`;
  },

  /**
   * Generate task title
   */
  generateTaskTitle(): string {
    const timestamp = Date.now();
    return `E2E Task ${timestamp}`;
  },

  /**
   * Generate goal title
   */
  generateGoalTitle(): string {
    const timestamp = Date.now();
    return `E2E Goal ${timestamp}`;
  },

  /**
   * Generate note title
   */
  generateNoteTitle(): string {
    const timestamp = Date.now();
    return `E2E Note ${timestamp}`;
  },

  /**
   * Default test user (for authenticated tests)
   */
  defaultUser: {
    email: 'e2e-default@hubz.test',
    password: 'E2ETestPassword123!',
    firstName: 'E2E',
    lastName: 'Default',
  },
};

/**
 * API helpers for test setup/teardown
 */
export const apiHelpers = {
  baseUrl: process.env.E2E_API_URL || 'http://localhost:8085/api',

  /**
   * Create a user via API (for test setup)
   */
  async createUser(userData: {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
  }): Promise<{ token: string; user: { id: string } }> {
    const response = await fetch(`${this.baseUrl}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(userData),
    });
    return response.json();
  },

  /**
   * Login via API
   */
  async login(email: string, password: string): Promise<{ token: string }> {
    const response = await fetch(`${this.baseUrl}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });
    return response.json();
  },

  /**
   * Create organization via API
   */
  async createOrganization(
    token: string,
    data: { name: string; description?: string }
  ): Promise<{ id: string; name: string }> {
    const response = await fetch(`${this.baseUrl}/organizations`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(data),
    });
    return response.json();
  },

  /**
   * Delete organization via API
   */
  async deleteOrganization(token: string, orgId: string): Promise<void> {
    await fetch(`${this.baseUrl}/organizations/${orgId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    });
  },

  /**
   * Create task via API
   */
  async createTask(
    token: string,
    orgId: string,
    data: { title: string; description?: string; priority?: string }
  ): Promise<{ id: string; title: string }> {
    const response = await fetch(`${this.baseUrl}/organizations/${orgId}/tasks`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(data),
    });
    return response.json();
  },

  /**
   * Delete task via API
   */
  async deleteTask(token: string, taskId: string): Promise<void> {
    await fetch(`${this.baseUrl}/tasks/${taskId}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    });
  },
};
