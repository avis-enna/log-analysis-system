package com.loganalyzer.e2e;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * End-to-End tests for the Log Analysis System.
 * Tests complete user workflows using Playwright for browser automation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("Log Analysis E2E Tests")
class LogAnalysisE2ETest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("loganalyzer_test")
            .withUsername("test")
            .withPassword("test");
    
    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.0")
                    .asCompatibleSubstituteFor("elasticsearch"))
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("elasticsearch.host", elasticsearch::getHost);
        registry.add("elasticsearch.port", () -> elasticsearch.getMappedPort(9200));
    }
    
    @LocalServerPort
    private int port;
    
    private static Playwright playwright;
    private static Browser browser;
    private BrowserContext context;
    private Page page;
    
    private String baseUrl;
    
    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setSlowMo(100)); // Add slight delay for better test stability
    }
    
    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
    
    @BeforeEach
    void createContextAndPage() {
        baseUrl = "http://localhost:" + port;
        
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setRecordVideoDir(Paths.get("target/videos/")));
        
        page = context.newPage();
        
        // Set up request/response logging for debugging
        page.onRequest(request -> 
            System.out.println(">> " + request.method() + " " + request.url()));
        page.onResponse(response -> 
            System.out.println("<< " + response.status() + " " + response.url()));
    }
    
    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }
    
    @Test
    @DisplayName("Should load dashboard and display log statistics")
    void shouldLoadDashboardAndDisplayLogStatistics() {
        // Navigate to dashboard
        page.navigate(baseUrl + "/dashboard");
        
        // Wait for page to load
        page.waitForLoadState(LoadState.NETWORKIDLE);
        
        // Verify page title
        assertThat(page).hasTitle("Log Analysis Dashboard");
        
        // Verify main dashboard elements are present
        assertThat(page.locator("h1")).containsText("Log Analysis Dashboard");
        assertThat(page.locator("[data-testid='stats-cards']")).isVisible();
        assertThat(page.locator("[data-testid='search-bar']")).isVisible();
        
        // Verify statistics cards
        assertThat(page.locator("[data-testid='total-logs-card']")).isVisible();
        assertThat(page.locator("[data-testid='error-logs-card']")).isVisible();
        assertThat(page.locator("[data-testid='active-sources-card']")).isVisible();
        assertThat(page.locator("[data-testid='alerts-card']")).isVisible();
    }
    
    @Test
    @DisplayName("Should perform basic log search")
    void shouldPerformBasicLogSearch() {
        // Navigate to search page
        page.navigate(baseUrl + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        
        // Enter search query
        page.locator("[data-testid='search-input']").fill("ERROR");
        
        // Click search button
        page.locator("[data-testid='search-button']").click();
        
        // Wait for search results
        page.waitForSelector("[data-testid='search-results']", new Page.WaitForSelectorOptions()
                .setTimeout(10000));
        
        // Verify search results are displayed
        assertThat(page.locator("[data-testid='search-results']")).isVisible();
        assertThat(page.locator("[data-testid='results-count']")).isVisible();
        
        // Verify that results contain error logs
        Locator logEntries = page.locator("[data-testid='log-entry']");
        assertThat(logEntries).not().hasCount(0);
        
        // Check that each visible log entry contains ERROR level
        for (int i = 0; i < Math.min(5, logEntries.count()); i++) {
            assertThat(logEntries.nth(i).locator("[data-testid='log-level']"))
                    .containsText("ERROR");
        }
    }
    
    @Test
    @DisplayName("Should filter logs by time range")
    void shouldFilterLogsByTimeRange() {
        page.navigate(baseUrl + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        
        // Open time range filter
        page.locator("[data-testid='time-filter-button']").click();
        
        // Select "Last 24 hours" option
        page.locator("[data-testid='time-range-24h']").click();
        
        // Apply filter
        page.locator("[data-testid='apply-filters-button']").click();
        
        // Wait for filtered results
        page.waitForSelector("[data-testid='search-results']");
        
        // Verify time filter is applied
        assertThat(page.locator("[data-testid='active-filters']")).containsText("Last 24 hours");
        assertThat(page.locator("[data-testid='search-results']")).isVisible();
    }
    
    @Test
    @DisplayName("Should filter logs by log level")
    void shouldFilterLogsByLogLevel() {
        page.navigate(baseUrl + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        
        // Open level filter
        page.locator("[data-testid='level-filter-button']").click();
        
        // Select ERROR and FATAL levels
        page.locator("[data-testid='level-error-checkbox']").check();
        page.locator("[data-testid='level-fatal-checkbox']").check();
        
        // Apply filter
        page.locator("[data-testid='apply-filters-button']").click();
        
        // Wait for filtered results
        page.waitForSelector("[data-testid='search-results']");
        
        // Verify only ERROR and FATAL logs are shown
        Locator logEntries = page.locator("[data-testid='log-entry']");
        for (int i = 0; i < Math.min(5, logEntries.count()); i++) {
            Locator levelElement = logEntries.nth(i).locator("[data-testid='log-level']");
            String levelText = levelElement.textContent();
            assertThat(levelText).matches("ERROR|FATAL");
        }
    }
    
    @Test
    @DisplayName("Should display log details in modal")
    void shouldDisplayLogDetailsInModal() {
        page.navigate(baseUrl + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        
        // Perform a search first
        page.locator("[data-testid='search-input']").fill("*");
        page.locator("[data-testid='search-button']").click();
        page.waitForSelector("[data-testid='search-results']");
        
        // Click on first log entry
        page.locator("[data-testid='log-entry']").first().click();
        
        // Wait for modal to appear
        page.waitForSelector("[data-testid='log-details-modal']");
        
        // Verify modal content
        assertThat(page.locator("[data-testid='log-details-modal']")).isVisible();
        assertThat(page.locator("[data-testid='log-message']")).isVisible();
        assertThat(page.locator("[data-testid='log-timestamp']")).isVisible();
        assertThat(page.locator("[data-testid='log-source']")).isVisible();
        
        // Close modal
        page.locator("[data-testid='close-modal-button']").click();
        assertThat(page.locator("[data-testid='log-details-modal']")).not().isVisible();
    }
    
    @Test
    @DisplayName("Should navigate through search result pages")
    void shouldNavigateThroughSearchResultPages() {
        page.navigate(baseUrl + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        
        // Perform search that returns multiple pages
        page.locator("[data-testid='search-input']").fill("*");
        page.locator("[data-testid='search-button']").click();
        page.waitForSelector("[data-testid='search-results']");
        
        // Verify pagination controls are present
        assertThat(page.locator("[data-testid='pagination']")).isVisible();
        
        // Check if next page button is enabled
        Locator nextButton = page.locator("[data-testid='next-page-button']");
        if (nextButton.isEnabled()) {
            // Click next page
            nextButton.click();
            page.waitForLoadState(LoadState.NETWORKIDLE);
            
            // Verify page number changed
            assertThat(page.locator("[data-testid='current-page']")).containsText("2");
            
            // Go back to first page
            page.locator("[data-testid='prev-page-button']").click();
            page.waitForLoadState(LoadState.NETWORKIDLE);
            
            assertThat(page.locator("[data-testid='current-page']")).containsText("1");
        }
    }
    
    @Test
    @DisplayName("Should display real-time log updates")
    void shouldDisplayRealtimeLogUpdates() {
        page.navigate(baseUrl + "/dashboard");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        
        // Enable real-time updates
        page.locator("[data-testid='realtime-toggle']").check();
        
        // Wait for WebSocket connection
        page.waitForTimeout(2000);
        
        // Verify real-time indicator is active
        assertThat(page.locator("[data-testid='realtime-indicator']")).hasClass(Pattern.compile(".*active.*"));
        
        // Verify log stream is visible
        assertThat(page.locator("[data-testid='realtime-logs']")).isVisible();
    }
    
    @Test
    @DisplayName("Should create and manage alerts")
    void shouldCreateAndManageAlerts() {
        page.navigate(baseUrl + "/alerts");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        
        // Click create alert button
        page.locator("[data-testid='create-alert-button']").click();
        
        // Fill alert form
        page.locator("[data-testid='alert-name-input']").fill("High Error Rate Alert");
        page.locator("[data-testid='alert-query-input']").fill("level:ERROR");
        page.locator("[data-testid='alert-threshold-input']").fill("10");
        
        // Select severity
        page.locator("[data-testid='severity-select']").selectOption("HIGH");
        
        // Save alert
        page.locator("[data-testid='save-alert-button']").click();
        
        // Verify alert was created
        page.waitForSelector("[data-testid='alert-success-message']");
        assertThat(page.locator("[data-testid='alert-success-message']"))
                .containsText("Alert created successfully");
        
        // Verify alert appears in list
        assertThat(page.locator("[data-testid='alerts-list']")).containsText("High Error Rate Alert");
    }
    
    @Test
    @DisplayName("Should export search results")
    void shouldExportSearchResults() {
        page.navigate(baseUrl + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        
        // Perform search
        page.locator("[data-testid='search-input']").fill("ERROR");
        page.locator("[data-testid='search-button']").click();
        page.waitForSelector("[data-testid='search-results']");
        
        // Set up download handler
        Download download = page.waitForDownload(() -> {
            page.locator("[data-testid='export-button']").click();
            page.locator("[data-testid='export-csv-option']").click();
        });
        
        // Verify download
        assertThat(download.suggestedFilename()).contains("search_results");
        assertThat(download.suggestedFilename()).endsWith(".csv");
    }
    
    @Test
    @DisplayName("Should handle search errors gracefully")
    void shouldHandleSearchErrorsGracefully() {
        page.navigate(baseUrl + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        
        // Enter invalid search query
        page.locator("[data-testid='search-input']").fill("invalid:query:syntax[[[");
        page.locator("[data-testid='search-button']").click();
        
        // Wait for error message
        page.waitForSelector("[data-testid='error-message']");
        
        // Verify error is displayed
        assertThat(page.locator("[data-testid='error-message']")).isVisible();
        assertThat(page.locator("[data-testid='error-message']"))
                .containsText("Invalid search query");
    }
    
    @Test
    @DisplayName("Should be responsive on mobile devices")
    void shouldBeResponsiveOnMobileDevices() {
        // Set mobile viewport
        page.setViewportSize(375, 667); // iPhone SE dimensions
        
        page.navigate(baseUrl + "/dashboard");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        
        // Verify mobile navigation menu
        assertThat(page.locator("[data-testid='mobile-menu-button']")).isVisible();
        
        // Open mobile menu
        page.locator("[data-testid='mobile-menu-button']").click();
        assertThat(page.locator("[data-testid='mobile-menu']")).isVisible();
        
        // Verify navigation items are accessible
        assertThat(page.locator("[data-testid='mobile-nav-search']")).isVisible();
        assertThat(page.locator("[data-testid='mobile-nav-alerts']")).isVisible();
        
        // Test search on mobile
        page.locator("[data-testid='mobile-nav-search']").click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        
        // Verify search interface is mobile-friendly
        assertThat(page.locator("[data-testid='search-input']")).isVisible();
        assertThat(page.locator("[data-testid='mobile-search-filters']")).isVisible();
    }
    
    @Test
    @DisplayName("Should maintain session across page refreshes")
    void shouldMaintainSessionAcrossPageRefreshes() {
        page.navigate(baseUrl + "/search");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        
        // Perform search and apply filters
        page.locator("[data-testid='search-input']").fill("ERROR");
        page.locator("[data-testid='level-filter-button']").click();
        page.locator("[data-testid='level-error-checkbox']").check();
        page.locator("[data-testid='apply-filters-button']").click();
        
        // Wait for results
        page.waitForSelector("[data-testid='search-results']");
        
        // Refresh page
        page.reload();
        page.waitForLoadState(LoadState.NETWORKIDLE);
        
        // Verify search state is maintained
        assertThat(page.locator("[data-testid='search-input']")).hasValue("ERROR");
        assertThat(page.locator("[data-testid='level-error-checkbox']")).isChecked();
    }
}
