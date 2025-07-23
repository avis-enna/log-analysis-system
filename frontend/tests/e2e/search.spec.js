import { test, expect } from '@playwright/test';

/**
 * End-to-End tests for search functionality
 * Tests complete user workflows in the browser
 */

test.describe('Search Functionality', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to the application
    await page.goto('/');
    
    // Wait for the application to load
    await page.waitForLoadState('networkidle');
    
    // Navigate to search page
    await page.click('[data-testid="nav-search"]');
    await page.waitForURL('/search');
  });

  test('should perform basic search', async ({ page }) => {
    // Enter search query
    await page.fill('[data-testid="search-input"]', 'ERROR');
    
    // Click search button
    await page.click('[data-testid="search-button"]');
    
    // Wait for search results
    await page.waitForSelector('[data-testid="search-results"]');
    
    // Verify results are displayed
    await expect(page.locator('[data-testid="search-results"]')).toBeVisible();
    await expect(page.locator('[data-testid="results-count"]')).toBeVisible();
    
    // Verify search query is preserved
    await expect(page.locator('[data-testid="search-input"]')).toHaveValue('ERROR');
  });

  test('should show search suggestions', async ({ page }) => {
    // Start typing in search input
    await page.fill('[data-testid="search-input"]', 'ERR');
    
    // Wait for suggestions to appear
    await page.waitForSelector('[data-testid="search-suggestions"]');
    
    // Verify suggestions are displayed
    await expect(page.locator('[data-testid="search-suggestions"]')).toBeVisible();
    await expect(page.locator('[data-testid="suggestion-item"]')).toHaveCount.greaterThan(0);
  });

  test('should select suggestion and perform search', async ({ page }) => {
    // Start typing
    await page.fill('[data-testid="search-input"]', 'ERR');
    
    // Wait for suggestions
    await page.waitForSelector('[data-testid="search-suggestions"]');
    
    // Click on first suggestion
    await page.click('[data-testid="suggestion-item"]:first-child');
    
    // Verify suggestion was selected
    const inputValue = await page.inputValue('[data-testid="search-input"]');
    expect(inputValue).toContain('ERROR');
    
    // Verify search was triggered
    await page.waitForSelector('[data-testid="search-results"]');
  });

  test('should filter search results by log level', async ({ page }) => {
    // Perform initial search
    await page.fill('[data-testid="search-input"]', '*');
    await page.click('[data-testid="search-button"]');
    await page.waitForSelector('[data-testid="search-results"]');
    
    // Open filters panel
    await page.click('[data-testid="filters-button"]');
    await page.waitForSelector('[data-testid="filters-panel"]');
    
    // Select ERROR level filter
    await page.check('[data-testid="level-error-checkbox"]');
    
    // Apply filters
    await page.click('[data-testid="apply-filters-button"]');
    
    // Wait for filtered results
    await page.waitForSelector('[data-testid="search-results"]');
    
    // Verify filter is applied
    await expect(page.locator('[data-testid="active-filters"]')).toContainText('ERROR');
    
    // Verify all results are ERROR level
    const logEntries = page.locator('[data-testid="log-entry"]');
    const count = await logEntries.count();
    
    for (let i = 0; i < Math.min(5, count); i++) {
      await expect(logEntries.nth(i).locator('[data-testid="log-level"]')).toContainText('ERROR');
    }
  });

  test('should filter by time range', async ({ page }) => {
    // Open filters panel
    await page.click('[data-testid="filters-button"]');
    await page.waitForSelector('[data-testid="filters-panel"]');
    
    // Select time range
    await page.click('[data-testid="time-range-select"]');
    await page.click('[data-testid="time-range-24h"]');
    
    // Apply filters
    await page.click('[data-testid="apply-filters-button"]');
    
    // Verify time filter is applied
    await expect(page.locator('[data-testid="active-filters"]')).toContainText('Last 24 hours');
  });

  test('should paginate through search results', async ({ page }) => {
    // Perform search that returns multiple pages
    await page.fill('[data-testid="search-input"]', '*');
    await page.click('[data-testid="search-button"]');
    await page.waitForSelector('[data-testid="search-results"]');
    
    // Check if pagination is available
    const nextButton = page.locator('[data-testid="next-page-button"]');
    
    if (await nextButton.isEnabled()) {
      // Go to next page
      await nextButton.click();
      await page.waitForLoadState('networkidle');
      
      // Verify page number changed
      await expect(page.locator('[data-testid="current-page"]')).toContainText('2');
      
      // Go back to first page
      await page.click('[data-testid="prev-page-button"]');
      await page.waitForLoadState('networkidle');
      
      await expect(page.locator('[data-testid="current-page"]')).toContainText('1');
    }
  });

  test('should display log details in modal', async ({ page }) => {
    // Perform search
    await page.fill('[data-testid="search-input"]', '*');
    await page.click('[data-testid="search-button"]');
    await page.waitForSelector('[data-testid="search-results"]');
    
    // Click on first log entry
    await page.click('[data-testid="log-entry"]:first-child');
    
    // Wait for modal to appear
    await page.waitForSelector('[data-testid="log-details-modal"]');
    
    // Verify modal content
    await expect(page.locator('[data-testid="log-details-modal"]')).toBeVisible();
    await expect(page.locator('[data-testid="log-message"]')).toBeVisible();
    await expect(page.locator('[data-testid="log-timestamp"]')).toBeVisible();
    
    // Close modal
    await page.click('[data-testid="close-modal-button"]');
    await expect(page.locator('[data-testid="log-details-modal"]')).not.toBeVisible();
  });

  test('should export search results', async ({ page }) => {
    // Perform search
    await page.fill('[data-testid="search-input"]', 'ERROR');
    await page.click('[data-testid="search-button"]');
    await page.waitForSelector('[data-testid="search-results"]');
    
    // Set up download handler
    const downloadPromise = page.waitForEvent('download');
    
    // Click export button
    await page.click('[data-testid="export-button"]');
    await page.click('[data-testid="export-csv-option"]');
    
    // Wait for download
    const download = await downloadPromise;
    
    // Verify download
    expect(download.suggestedFilename()).toContain('search_results');
    expect(download.suggestedFilename()).toMatch(/\.csv$/);
  });

  test('should save and load search queries', async ({ page }) => {
    // Perform search
    await page.fill('[data-testid="search-input"]', 'level:ERROR');
    await page.click('[data-testid="search-button"]');
    await page.waitForSelector('[data-testid="search-results"]');
    
    // Save search
    await page.click('[data-testid="save-search-button"]');
    await page.fill('[data-testid="search-name-input"]', 'Error Logs');
    await page.click('[data-testid="confirm-save-button"]');
    
    // Verify save confirmation
    await expect(page.locator('[data-testid="save-success-message"]')).toBeVisible();
    
    // Clear search
    await page.fill('[data-testid="search-input"]', '');
    
    // Load saved search
    await page.click('[data-testid="saved-searches-button"]');
    await page.click('[data-testid="saved-search-item"]:has-text("Error Logs")');
    
    // Verify search was loaded
    await expect(page.locator('[data-testid="search-input"]')).toHaveValue('level:ERROR');
  });

  test('should handle search errors gracefully', async ({ page }) => {
    // Mock API to return error
    await page.route('/api/v1/search', route => {
      route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Invalid search query' })
      });
    });
    
    // Perform search with invalid query
    await page.fill('[data-testid="search-input"]', 'invalid[[[query');
    await page.click('[data-testid="search-button"]');
    
    // Verify error message is displayed
    await expect(page.locator('[data-testid="error-message"]')).toBeVisible();
    await expect(page.locator('[data-testid="error-message"]')).toContainText('Invalid search query');
  });

  test('should maintain search state on page refresh', async ({ page }) => {
    // Perform search with filters
    await page.fill('[data-testid="search-input"]', 'ERROR');
    
    // Apply filters
    await page.click('[data-testid="filters-button"]');
    await page.check('[data-testid="level-error-checkbox"]');
    await page.click('[data-testid="apply-filters-button"]');
    
    // Perform search
    await page.click('[data-testid="search-button"]');
    await page.waitForSelector('[data-testid="search-results"]');
    
    // Refresh page
    await page.reload();
    await page.waitForLoadState('networkidle');
    
    // Verify search state is maintained
    await expect(page.locator('[data-testid="search-input"]')).toHaveValue('ERROR');
    await expect(page.locator('[data-testid="level-error-checkbox"]')).toBeChecked();
  });

  test('should be responsive on mobile devices', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });
    
    // Verify mobile layout
    await expect(page.locator('[data-testid="mobile-search-container"]')).toBeVisible();
    
    // Test mobile search
    await page.fill('[data-testid="search-input"]', 'ERROR');
    await page.click('[data-testid="search-button"]');
    
    // Verify results are displayed properly on mobile
    await page.waitForSelector('[data-testid="search-results"]');
    await expect(page.locator('[data-testid="search-results"]')).toBeVisible();
    
    // Test mobile filters
    await page.click('[data-testid="mobile-filters-button"]');
    await expect(page.locator('[data-testid="mobile-filters-panel"]')).toBeVisible();
  });

  test('should support keyboard navigation', async ({ page }) => {
    // Focus search input
    await page.focus('[data-testid="search-input"]');
    
    // Type search query
    await page.keyboard.type('ERROR');
    
    // Press Enter to search
    await page.keyboard.press('Enter');
    
    // Wait for results
    await page.waitForSelector('[data-testid="search-results"]');
    
    // Navigate through results with Tab
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    
    // Verify focus is on first result
    await expect(page.locator('[data-testid="log-entry"]:first-child')).toBeFocused();
    
    // Press Enter to open details
    await page.keyboard.press('Enter');
    
    // Verify modal opened
    await expect(page.locator('[data-testid="log-details-modal"]')).toBeVisible();
    
    // Press Escape to close modal
    await page.keyboard.press('Escape');
    
    // Verify modal closed
    await expect(page.locator('[data-testid="log-details-modal"]')).not.toBeVisible();
  });

  test('should show real-time search results', async ({ page }) => {
    // Enable real-time updates
    await page.check('[data-testid="realtime-toggle"]');
    
    // Perform search
    await page.fill('[data-testid="search-input"]', '*');
    await page.click('[data-testid="search-button"]');
    await page.waitForSelector('[data-testid="search-results"]');
    
    // Verify real-time indicator is active
    await expect(page.locator('[data-testid="realtime-indicator"]')).toHaveClass(/active/);
    
    // Mock new log entry via WebSocket
    await page.evaluate(() => {
      // Simulate WebSocket message
      window.dispatchEvent(new CustomEvent('websocket-message', {
        detail: {
          type: 'new-log',
          data: {
            id: 'new-log-1',
            message: 'New error occurred',
            level: 'ERROR',
            timestamp: new Date().toISOString()
          }
        }
      }));
    });
    
    // Verify new log appears in results
    await expect(page.locator('[data-testid="log-entry"]').first()).toContainText('New error occurred');
  });
});
