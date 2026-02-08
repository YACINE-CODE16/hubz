import { chromium, FullConfig } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

/**
 * Global setup for Playwright E2E tests
 * Creates the auth directory if it doesn't exist
 */
async function globalSetup(config: FullConfig): Promise<void> {
  // Ensure auth directory exists
  const authDir = path.join(__dirname, '.auth');
  if (!fs.existsSync(authDir)) {
    fs.mkdirSync(authDir, { recursive: true });
  }

  // Create an empty user.json if it doesn't exist
  // This will be populated by auth.setup.ts
  const userAuthFile = path.join(authDir, 'user.json');
  if (!fs.existsSync(userAuthFile)) {
    fs.writeFileSync(userAuthFile, JSON.stringify({ cookies: [], origins: [] }));
  }

  // Verify the frontend is accessible (optional warm-up)
  const baseURL = config.projects[0]?.use?.baseURL || 'http://localhost:5173';

  console.log('[Global Setup] Playwright E2E tests initialization complete');
  console.log(`[Global Setup] Base URL: ${baseURL}`);
}

export default globalSetup;
