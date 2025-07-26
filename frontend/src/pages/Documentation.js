import React, { useState } from 'react';
import {
  DocumentTextIcon,
  BookOpenIcon,
  CommandLineIcon,
  CogIcon,
  RocketLaunchIcon,
  ShieldCheckIcon,
  BeakerIcon,
  WrenchScrewdriverIcon,
} from '@heroicons/react/24/outline';

/**
 * Documentation page that displays system documentation
 */
const Documentation = () => {
  const [activeSection, setActiveSection] = useState('overview');

  // Documentation sections with content
  const documentationSections = {
    overview: {
      title: 'System Overview',
      icon: BookOpenIcon,
      content: `
        # 🚀 Enterprise Log Analysis System

        A comprehensive, enterprise-grade log analysis and monitoring system built with modern technologies. 
        This system provides real-time log processing, advanced search capabilities, intelligent alerting, 
        and comprehensive analytics - similar to Splunk but open-source and fully customizable.

        ## ✨ Key Features

        - 🔍 **Advanced Search**: Splunk-like search syntax with powerful filtering
        - 📊 **Real-time Dashboard**: Live metrics, charts, and system health monitoring  
        - 🚨 **Intelligent Alerting**: Configurable alerts with multiple severity levels
        - 📈 **Analytics & Reporting**: Trend analysis, performance metrics, and custom reports
        - 🎨 **Modern UI**: Responsive React interface with dark mode support
        - ⚡ **High Performance**: Handles 10,000+ logs/second with sub-second search
        - 🔐 **Enterprise Security**: JWT authentication, RBAC, and audit trails
        - 🐳 **Container Ready**: Full Docker support with orchestration
        - 💻 **Local Development**: Works without Docker using embedded database

        ## Architecture

        The system is built with:
        - **Backend**: Java Spring Boot with H2/PostgreSQL database
        - **Frontend**: React 18 with Redux for state management
        - **Search**: Advanced text search with caching
        - **Real-time**: WebSocket connections for live updates
        - **API**: RESTful API with comprehensive documentation
      `
    },
    
    quickStart: {
      title: 'Quick Start Guide',
      icon: RocketLaunchIcon,
      content: `
        # 🚀 Quick Start Guide

        Get the Log Analysis System running in minutes!

        ## Prerequisites

        - Java 17 or higher
        - Node.js 18 or higher
        - Maven 3.8+
        - Git

        ## Local Development Setup

        ### 1. Clone the Repository
        \`\`\`bash
        git clone https://github.com/your-org/log-analysis-system.git
        cd log-analysis-system
        \`\`\`

        ### 2. Start Backend
        \`\`\`bash
        cd backend
        mvn spring-boot:run
        \`\`\`
        Backend will start on http://localhost:8080

        ### 3. Start Frontend
        \`\`\`bash
        cd frontend
        npm install
        npm start
        \`\`\`
        Frontend will start on http://localhost:3001

        ### 4. Access the System
        - **Main Application**: http://localhost:3001
        - **API Documentation**: http://localhost:8080/api/v1/docs
        - **Health Check**: http://localhost:8080/api/v1/health

        ## First Steps

        1. **Upload Sample Logs**: Go to Upload → paste some log entries
        2. **Search Logs**: Go to Search → try searching for "ERROR" or "INFO"
        3. **View Dashboard**: Check real-time metrics and charts
        4. **Set Up Alerts**: Configure alerts for critical log patterns

        That's it! You're ready to analyze logs! 🎉
      `
    },

    api: {
      title: 'API Reference',
      icon: CommandLineIcon,
      content: `
        # 📡 API Reference

        Complete REST API documentation for the Log Analysis System.

        ## Base URL
        \`\`\`
        http://localhost:8080/api/v1
        \`\`\`

        ## Authentication
        Currently using basic authentication. JWT tokens coming soon.

        ## Search API

        ### Quick Search
        \`\`\`http
        GET /search/quick?q={query}&page={page}&size={size}
        \`\`\`

        **Parameters:**
        - \`q\` (string): Search query
        - \`page\` (integer): Page number (0-based)
        - \`size\` (integer): Results per page (default: 100)

        **Response:**
        \`\`\`json
        {
          "logs": [...],
          "totalHits": 150,
          "page": 0,
          "size": 10,
          "totalPages": 15,
          "searchTimeMs": 45
        }
        \`\`\`

        ### Advanced Search
        \`\`\`http
        POST /search
        \`\`\`

        **Request Body:**
        \`\`\`json
        {
          "query": "ERROR",
          "startTime": "2025-01-01T00:00:00Z",
          "endTime": "2025-12-31T23:59:59Z",
          "levels": ["ERROR", "WARN"],
          "sources": ["web-server"],
          "page": 0,
          "size": 50
        }
        \`\`\`

        ## Upload API

        ### Upload File
        \`\`\`http
        POST /logs/upload/file
        Content-Type: multipart/form-data
        \`\`\`

        **Form Data:**
        - \`file\`: Log file (.log, .txt, .json)
        - \`source\`: Source identifier (optional)

        ### Upload Text
        \`\`\`http
        POST /logs/upload/text
        Content-Type: multipart/form-data
        \`\`\`

        **Form Data:**
        - \`text\`: Raw log text
        - \`source\`: Source identifier (optional)

        ## Dashboard API

        ### System Stats
        \`\`\`http
        GET /dashboard/stats
        \`\`\`

        ### Real-time Metrics
        \`\`\`http
        GET /dashboard/realtime
        \`\`\`

        ## Alerts API

        ### List Alerts
        \`\`\`http
        GET /alerts
        \`\`\`

        ### Create Alert
        \`\`\`http
        POST /alerts
        \`\`\`

        ## Error Handling

        All API endpoints return standardized error responses:

        \`\`\`json
        {
          "errorCode": "VALIDATION_ERROR",
          "message": "Invalid search query",
          "details": "Query parameter 'q' is required",
          "path": "uri=/api/v1/search/quick",
          "timestamp": "2025-07-26T15:00:00Z"
        }
        \`\`\`

        ## Rate Limiting

        - **Search API**: 100 requests/minute
        - **Upload API**: 50 requests/minute
        - **Dashboard API**: 200 requests/minute
      `
    },

    deployment: {
      title: 'Deployment Guide',
      icon: CogIcon,
      content: `
        # 🚀 Deployment Guide

        Deploy the Log Analysis System to production environments.

        ## Docker Deployment

        ### Using Docker Compose
        \`\`\`bash
        # Clone repository
        git clone https://github.com/your-org/log-analysis-system.git
        cd log-analysis-system

        # Start services
        docker-compose up -d
        \`\`\`

        Services will be available at:
        - **Frontend**: http://localhost:3001
        - **Backend**: http://localhost:8080
        - **Database**: PostgreSQL on port 5432

        ### Individual Containers

        **Backend:**
        \`\`\`bash
        cd backend
        docker build -t log-analyzer-backend .
        docker run -p 8080:8080 log-analyzer-backend
        \`\`\`

        **Frontend:**
        \`\`\`bash
        cd frontend
        docker build -t log-analyzer-frontend .
        docker run -p 3001:3001 log-analyzer-frontend
        \`\`\`

        ## Kubernetes Deployment

        ### Prerequisites
        - Kubernetes cluster
        - kubectl configured
        - Helm (optional)

        ### Deploy with Manifests
        \`\`\`bash
        kubectl apply -f k8s/
        \`\`\`

        ### Deploy with Helm
        \`\`\`bash
        helm install log-analyzer ./helm/log-analyzer
        \`\`\`

        ## Environment Configuration

        ### Backend Environment Variables
        \`\`\`bash
        # Database
        DATABASE_URL=jdbc:postgresql://localhost:5432/loganalyzer
        DATABASE_USERNAME=loganalyzer
        DATABASE_PASSWORD=secure_password

        # Server
        SERVER_PORT=8080
        SPRING_PROFILES_ACTIVE=production

        # Performance
        JVM_OPTS=-Xmx2g -Xms1g
        \`\`\`

        ### Frontend Environment Variables
        \`\`\`bash
        # API Configuration
        REACT_APP_API_URL=http://your-api-domain.com/api/v1
        REACT_APP_WS_URL=ws://your-api-domain.com/ws

        # Build Configuration
        GENERATE_SOURCEMAP=false
        DISABLE_ESLINT_PLUGIN=true
        \`\`\`

        ## Production Checklist

        ### Security
        - [ ] Enable HTTPS/TLS
        - [ ] Configure JWT authentication
        - [ ] Set up RBAC permissions
        - [ ] Enable audit logging
        - [ ] Configure CORS properly

        ### Performance
        - [ ] Configure connection pooling
        - [ ] Set up caching (Redis)
        - [ ] Optimize database indexes
        - [ ] Configure log rotation
        - [ ] Set up monitoring

        ### Monitoring
        - [ ] Application metrics (Prometheus)
        - [ ] Health checks
        - [ ] Log aggregation
        - [ ] Alerting (PagerDuty/Slack)
        - [ ] Performance monitoring (APM)

        ## Scaling

        ### Horizontal Scaling
        - Multiple backend instances behind load balancer
        - Database read replicas
        - Redis cluster for caching

        ### Vertical Scaling
        - Increase JVM heap size
        - More CPU cores for parallel processing
        - SSD storage for database

        ## Backup & Recovery

        ### Database Backup
        \`\`\`bash
        # PostgreSQL backup
        pg_dump -h localhost -U loganalyzer loganalyzer > backup.sql

        # Restore
        psql -h localhost -U loganalyzer loganalyzer < backup.sql
        \`\`\`

        ### File Backup
        - Configuration files
        - Upload directories
        - SSL certificates
      `
    },

    troubleshooting: {
      title: 'Troubleshooting',
      icon: WrenchScrewdriverIcon,
      content: `
        # 🔧 Troubleshooting Guide

        Common issues and solutions for the Log Analysis System.

        ## Backend Issues

        ### Service Won't Start

        **Problem**: Backend fails to start with database connection error
        
        **Solution**:
        \`\`\`bash
        # Check if H2 database is accessible
        ls -la ~/.h2/
        
        # Or check PostgreSQL connection
        psql -h localhost -U loganalyzer -d loganalyzer
        
        # Clear application cache
        rm -rf target/
        mvn clean install
        \`\`\`

        ### High Memory Usage

        **Problem**: Backend consuming too much memory
        
        **Solution**:
        \`\`\`bash
        # Increase JVM heap size
        export JAVA_OPTS="-Xmx2g -Xms1g"
        
        # Or edit application.yml
        spring:
          jpa:
            hibernate:
              ddl-auto: validate  # Don't recreate schema
        \`\`\`

        ### Search Performance Issues

        **Problem**: Search queries are slow
        
        **Solutions**:
        - Enable search result caching
        - Add database indexes on frequently searched fields
        - Limit search result size
        - Use pagination properly

        ## Frontend Issues

        ### Build Failures

        **Problem**: npm install or build fails
        
        **Solution**:
        \`\`\`bash
        # Clear npm cache
        npm cache clean --force
        
        # Delete node_modules and reinstall
        rm -rf node_modules package-lock.json
        npm install
        
        # Use specific Node version
        nvm use 18
        npm install
        \`\`\`

        ### API Connection Issues

        **Problem**: Frontend can't connect to backend
        
        **Solution**:
        \`\`\`bash
        # Check backend is running
        curl http://localhost:8080/api/v1/health
        
        # Check proxy configuration in package.json
        "proxy": "http://localhost:8080"
        
        # Or set environment variable
        export REACT_APP_API_URL=http://localhost:8080/api/v1
        \`\`\`

        ### Search Results Not Displaying

        **Problem**: Search returns results but UI shows "No logs found"
        
        **Solutions**:
        - Check browser console for JavaScript errors
        - Verify API response format matches expected structure
        - Clear browser cache and cookies
        - Check Redux DevTools for state issues

        ## Common Error Messages

        ### "INTERNAL_ERROR"
        - Check backend logs for stack traces
        - Verify database connection
        - Ensure all required services are running

        ### "VALIDATION_ERROR"
        - Check API request parameters
        - Verify required fields are provided
        - Check data types and formats

        ### "404 Not Found"
        - Verify endpoint URLs
        - Check if required routes are registered
        - Ensure frontend build is up to date

        ## Performance Optimization

        ### Database Optimization
        \`\`\`sql
        -- Add indexes for common queries
        CREATE INDEX idx_log_level ON log_entries(level);
        CREATE INDEX idx_log_timestamp ON log_entries(timestamp);
        CREATE INDEX idx_log_message_text ON log_entries USING gin(to_tsvector('english', message));
        \`\`\`

        ### Frontend Optimization
        - Enable production build: \`npm run build\`
        - Use code splitting for large components
        - Implement virtual scrolling for large lists
        - Optimize bundle size with webpack-bundle-analyzer

        ## Getting Help

        1. **Check Logs**: Always start with application logs
        2. **Search Issues**: Look for similar issues in documentation
        3. **Debug Mode**: Enable debug logging for more details
        4. **Community**: Join our Discord/Slack for community support
        5. **GitHub Issues**: Report bugs and feature requests

        ## Useful Commands

        \`\`\`bash
        # Check system status
        curl http://localhost:8080/api/v1/health
        
        # View backend logs
        tail -f logs/application.log
        
        # Monitor system resources
        top -p $(pgrep java)
        
        # Test database connection
        curl http://localhost:8080/api/v1/logs?size=1
        
        # Clear frontend cache
        rm -rf frontend/build frontend/node_modules/.cache
        \`\`\`
      `
    },

    security: {
      title: 'Security Guide',
      icon: ShieldCheckIcon,
      content: `
        # 🔐 Security Guide

        Security best practices and configuration for the Log Analysis System.

        ## Authentication & Authorization

        ### JWT Configuration
        \`\`\`properties
        # application.yml
        security:
          jwt:
            secret: your-256-bit-secret-key
            expiration: 86400000  # 24 hours
        \`\`\`

        ### Role-Based Access Control (RBAC)
        - **Admin**: Full system access
        - **Analyst**: Search and view logs, create alerts
        - **Viewer**: Read-only access to logs and dashboards
        - **Uploader**: Can upload logs and basic search

        ### API Security
        \`\`\`java
        @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
        @GetMapping("/logs")
        public ResponseEntity<List<LogEntry>> getLogs() {
            // Protected endpoint
        }
        \`\`\`

        ## Data Protection

        ### Sensitive Data Handling
        - **PII Detection**: Automatically detect and mask personal information
        - **Data Encryption**: Encrypt sensitive log data at rest
        - **Field Masking**: Configure which fields to mask in UI

        ### Database Security
        \`\`\`properties
        # Encrypt database connection
        spring.datasource.url=jdbc:postgresql://localhost:5432/loganalyzer?ssl=true&sslmode=require
        
        # Use connection pooling with security
        spring.datasource.hikari.maximum-pool-size=10
        spring.datasource.hikari.connection-timeout=20000
        \`\`\`

        ## Network Security

        ### HTTPS Configuration
        \`\`\`properties
        # Enable HTTPS
        server.port=8443
        server.ssl.key-store=classpath:keystore.p12
        server.ssl.key-store-password=changeit
        server.ssl.key-store-type=PKCS12
        \`\`\`

        ### CORS Configuration
        \`\`\`java
        @CrossOrigin(
            origins = {"https://your-domain.com"}, 
            allowedHeaders = "*",
            methods = {RequestMethod.GET, RequestMethod.POST}
        )
        \`\`\`

        ### Firewall Rules
        \`\`\`bash
        # Allow only necessary ports
        ufw allow 22/tcp    # SSH
        ufw allow 443/tcp   # HTTPS
        ufw allow 8080/tcp  # API (internal only)
        ufw deny 3001/tcp   # Block direct frontend access
        \`\`\`

        ## Input Validation

        ### API Input Validation
        \`\`\`java
        @Valid
        public ResponseEntity<SearchResult> search(@RequestBody SearchRequest request) {
            // Automatic validation with @Valid
        }
        \`\`\`

        ### SQL Injection Prevention
        - Use parameterized queries
        - Validate all user inputs
        - Implement query allowlists

        ### XSS Prevention
        - Sanitize all user inputs
        - Use Content Security Policy (CSP)
        - Escape output in templates

        ## Audit & Logging

        ### Security Event Logging
        \`\`\`java
        @EventListener
        public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
            auditService.logSecurityEvent("LOGIN_SUCCESS", event.getAuthentication().getName());
        }
        \`\`\`

        ### Audit Trail
        - Log all API access attempts
        - Track data modifications
        - Monitor failed authentication attempts
        - Alert on suspicious activities

        ## Secrets Management

        ### Environment Variables
        \`\`\`bash
        # Never commit secrets to code
        export DATABASE_PASSWORD=secure_password
        export JWT_SECRET=your-256-bit-secret
        export ENCRYPTION_KEY=another-secure-key
        \`\`\`

        ### External Secret Stores
        - **HashiCorp Vault**: Enterprise secret management
        - **AWS Secrets Manager**: Cloud-based secrets
        - **Kubernetes Secrets**: Container orchestration secrets

        ## Compliance

        ### GDPR Compliance
        - **Data Minimization**: Only collect necessary log data
        - **Right to Erasure**: Implement log deletion capabilities
        - **Data Portability**: Export user's log data
        - **Consent Management**: Track user consent for log collection

        ### SOX/HIPAA Compliance
        - **Data Retention**: Implement automated log retention policies
        - **Access Controls**: Strict role-based access
        - **Audit Trails**: Comprehensive activity logging
        - **Data Encryption**: Encrypt all sensitive data

        ## Security Monitoring

        ### Real-time Monitoring
        \`\`\`yaml
        # Prometheus metrics
        - alert: HighFailedLoginRate
          expr: rate(auth_failures_total[5m]) > 5
          for: 2m
          annotations:
            summary: "High number of failed login attempts"
        \`\`\`

        ### Security Alerts
        - Multiple failed login attempts
        - Unusual API access patterns
        - Large data exports
        - Administrative action alerts

        ## Incident Response

        ### Security Incident Procedure
        1. **Detect**: Automated monitoring and alerts
        2. **Contain**: Isolate affected systems
        3. **Investigate**: Analyze logs and evidence
        4. **Eradicate**: Remove threats and vulnerabilities
        5. **Recover**: Restore normal operations
        6. **Lessons Learned**: Update security measures

        ### Emergency Contacts
        - Security Team: security@company.com
        - On-call Engineer: +1-555-0123
        - Legal/Compliance: legal@company.com

        ## Security Checklist

        ### Development
        - [ ] Code security review
        - [ ] Dependency vulnerability scanning
        - [ ] Static code analysis (SonarQube)
        - [ ] Secret detection (GitLeaks)

        ### Deployment
        - [ ] HTTPS enabled
        - [ ] Security headers configured
        - [ ] Database connections encrypted
        - [ ] Firewall rules applied
        - [ ] Access logs enabled

        ### Operations
        - [ ] Regular security updates
        - [ ] Log monitoring active
        - [ ] Backup encryption verified
        - [ ] Incident response plan tested
        - [ ] Security training completed
      `
    },

    testing: {
      title: 'Testing Guide',
      icon: BeakerIcon,
      content: `
        # 🧪 Testing Guide

        Comprehensive testing strategy for the Log Analysis System.

        ## Test Structure

        \`\`\`
        backend/
        ├── src/test/java/
        │   ├── unit/          # Unit tests
        │   ├── integration/   # Integration tests
        │   ├── e2e/          # End-to-end tests
        │   └── performance/   # Performance tests

        frontend/
        ├── src/
        │   ├── __tests__/     # Component tests
        │   ├── components/__tests__/
        │   └── utils/__tests__/
        └── tests/
            └── e2e/           # Playwright E2E tests
        \`\`\`

        ## Backend Testing

        ### Unit Tests
        \`\`\`java
        @ExtendWith(MockitoExtension.class)
        class SearchServiceTest {
            
            @Mock
            private LogEntryRepository repository;
            
            @InjectMocks
            private SearchService searchService;
            
            @Test
            void shouldReturnSearchResults() {
                // Given
                String query = "ERROR";
                Page<LogEntry> mockPage = createMockPage();
                when(repository.findByMessageContaining(query, any())).thenReturn(mockPage);
                
                // When
                SearchResult result = searchService.quickSearch(query, 0, 10);
                
                // Then
                assertThat(result.getLogs()).hasSize(5);
                assertThat(result.getTotalHits()).isEqualTo(25);
            }
        }
        \`\`\`

        ### Integration Tests
        \`\`\`java
        @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
        @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
        class SearchControllerIntegrationTest {
            
            @Autowired
            private TestRestTemplate restTemplate;
            
            @Test
            void shouldSearchLogs() {
                // Given
                String url = "/api/v1/search/quick?q=ERROR&size=5";
                
                // When
                ResponseEntity<SearchResult> response = restTemplate.getForEntity(url, SearchResult.class);
                
                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().getLogs()).isNotEmpty();
            }
        }
        \`\`\`

        ### Performance Tests
        \`\`\`java
        @Test
        void searchPerformanceTest() {
            // Given
            int numQueries = 1000;
            String query = "ERROR";
            
            // When
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < numQueries; i++) {
                searchService.quickSearch(query, 0, 10);
            }
            long endTime = System.currentTimeMillis();
            
            // Then
            long avgResponseTime = (endTime - startTime) / numQueries;
            assertThat(avgResponseTime).isLessThan(100); // Less than 100ms per query
        }
        \`\`\`

        ### Running Backend Tests
        \`\`\`bash
        # Run all tests
        mvn test
        
        # Run specific test class
        mvn test -Dtest=SearchServiceTest
        
        # Run with coverage
        mvn test jacoco:report
        
        # Performance tests
        mvn test -Dtest=**/*PerformanceTest
        \`\`\`

        ## Frontend Testing

        ### Component Tests (Jest + React Testing Library)
        \`\`\`javascript
        import { render, screen, fireEvent, waitFor } from '@testing-library/react';
        import { Provider } from 'react-redux';
        import { store } from '../store/store';
        import Search from '../pages/Search';

        describe('Search Component', () => {
          const renderSearch = () => {
            return render(
              <Provider store={store}>
                <Search />
              </Provider>
            );
          };

          test('should display search results', async () => {
            // Given
            renderSearch();
            const searchInput = screen.getByPlaceholderText(/search logs/i);
            const searchButton = screen.getByRole('button', { name: /search/i });

            // When
            fireEvent.change(searchInput, { target: { value: 'ERROR' } });
            fireEvent.click(searchButton);

            // Then
            await waitFor(() => {
              expect(screen.getByText(/found.*results/i)).toBeInTheDocument();
            });
          });

          test('should handle empty search results', async () => {
            // Given
            renderSearch();
            const searchInput = screen.getByPlaceholderText(/search logs/i);

            // When
            fireEvent.change(searchInput, { target: { value: 'nonexistent' } });
            fireEvent.submit(searchInput.closest('form'));

            // Then
            await waitFor(() => {
              expect(screen.getByText(/no logs found/i)).toBeInTheDocument();
            });
          });
        });
        \`\`\`

        ### API Service Tests
        \`\`\`javascript
        import { searchAPI } from '../services/api';
        import { rest } from 'msw';
        import { setupServer } from 'msw/node';

        const server = setupServer(
          rest.get('/api/v1/search/quick', (req, res, ctx) => {
            return res(
              ctx.json({
                logs: [
                  { id: '1', message: 'Test error message', level: 'ERROR' }
                ],
                totalHits: 1,
                searchTimeMs: 15
              })
            );
          })
        );

        beforeAll(() => server.listen());
        afterEach(() => server.resetHandlers());
        afterAll(() => server.close());

        describe('Search API', () => {
          test('should fetch search results', async () => {
            // When
            const result = await searchAPI.quickSearch('ERROR', 0, 10);

            // Then
            expect(result.data.logs).toHaveLength(1);
            expect(result.data.totalHits).toBe(1);
          });
        });
        \`\`\`

        ### Running Frontend Tests
        \`\`\`bash
        # Run all tests
        npm test

        # Run with coverage
        npm test -- --coverage

        # Run specific test file
        npm test Search.test.js

        # Run in watch mode
        npm test -- --watch
        \`\`\`

        ## End-to-End Testing (Playwright)

        ### E2E Test Setup
        \`\`\`javascript
        // playwright.config.js
        module.exports = {
          testDir: './tests/e2e',
          use: {
            baseURL: 'http://localhost:3001',
            headless: true,
            screenshot: 'only-on-failure',
            video: 'retain-on-failure',
          },
          projects: [
            {
              name: 'chromium',
              use: { ...devices['Desktop Chrome'] },
            },
            {
              name: 'firefox',
              use: { ...devices['Desktop Firefox'] },
            },
          ],
        };
        \`\`\`

        ### E2E Test Example
        \`\`\`javascript
        import { test, expect } from '@playwright/test';

        test.describe('Log Analysis System', () => {
          test.beforeEach(async ({ page }) => {
            await page.goto('/');
          });

          test('should search and display results', async ({ page }) => {
            // Navigate to search page
            await page.click('text=Search');
            
            // Enter search query
            await page.fill('[placeholder*="search"]', 'ERROR');
            await page.click('button:has-text("Search")');
            
            // Verify results
            await expect(page.locator('text=Found')).toBeVisible();
            await expect(page.locator('[data-testid="log-entry"]')).toHaveCount.greaterThan(0);
          });

          test('should upload logs', async ({ page }) => {
            // Navigate to upload page
            await page.click('text=Upload');
            
            // Switch to text upload
            await page.click('text=Text Upload');
            
            // Enter log text
            await page.fill('textarea', '2025-07-26 [ERROR] Test log entry');
            await page.click('button:has-text("Upload Text")');
            
            // Verify success
            await expect(page.locator('text=Upload Successful')).toBeVisible();
          });
        });
        \`\`\`

        ### Running E2E Tests
        \`\`\`bash
        # Install Playwright
        npm install @playwright/test
        npx playwright install

        # Run E2E tests
        npx playwright test

        # Run with UI
        npx playwright test --ui

        # Run specific test
        npx playwright test search.spec.js
        \`\`\`

        ## Test Data Management

        ### Test Database
        \`\`\`properties
        # application-test.yml
        spring:
          datasource:
            url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
            username: sa
            password: 
          jpa:
            hibernate:
              ddl-auto: create-drop
        \`\`\`

        ### Test Data Factory
        \`\`\`java
        public class LogEntryFactory {
            public static LogEntry createLogEntry(String level, String message) {
                LogEntry entry = new LogEntry();
                entry.setLevel(level);
                entry.setMessage(message);
                entry.setTimestamp(LocalDateTime.now());
                entry.setSource("test-source");
                return entry;
            }
            
            public static List<LogEntry> createBulkLogEntries(int count) {
                return IntStream.range(0, count)
                    .mapToObj(i -> createLogEntry("INFO", "Test message " + i))
                    .collect(Collectors.toList());
            }
        }
        \`\`\`

        ## CI/CD Testing

        ### GitHub Actions
        \`\`\`yaml
        name: Test Suite
        on: [push, pull_request]

        jobs:
          backend-tests:
            runs-on: ubuntu-latest
            steps:
              - uses: actions/checkout@v3
              - uses: actions/setup-java@v3
                with:
                  java-version: '17'
              - name: Run backend tests
                run: |
                  cd backend
                  mvn test
                  mvn jacoco:report
              - name: Upload coverage
                uses: codecov/codecov-action@v3

          frontend-tests:
            runs-on: ubuntu-latest
            steps:
              - uses: actions/checkout@v3
              - uses: actions/setup-node@v3
                with:
                  node-version: '18'
              - name: Run frontend tests
                run: |
                  cd frontend
                  npm ci
                  npm test -- --coverage --watchAll=false

          e2e-tests:
            runs-on: ubuntu-latest
            steps:
              - uses: actions/checkout@v3
              - name: Start services
                run: docker-compose up -d
              - name: Run E2E tests
                run: npx playwright test
              - name: Upload test results
                uses: actions/upload-artifact@v3
                if: failure()
                with:
                  name: playwright-report
                  path: playwright-report/
        \`\`\`

        ## Test Coverage Goals

        - **Backend Unit Tests**: > 80% line coverage
        - **Frontend Component Tests**: > 70% line coverage
        - **Integration Tests**: Cover all API endpoints
        - **E2E Tests**: Cover all critical user journeys

        ## Testing Best Practices

        1. **AAA Pattern**: Arrange, Act, Assert
        2. **Test Isolation**: Each test should be independent
        3. **Meaningful Names**: Test names should describe the scenario
        4. **Fast Feedback**: Unit tests should run quickly
        5. **Realistic Data**: Use representative test data
        6. **Clean Up**: Always clean up test resources
        7. **Mock External Dependencies**: Don't test third-party services
        8. **Test Edge Cases**: Include boundary conditions and error scenarios
      `
    }
  };

  // Navigation menu
  const navigationItems = [
    { key: 'overview', title: 'Overview', icon: BookOpenIcon },
    { key: 'quickStart', title: 'Quick Start', icon: RocketLaunchIcon },
    { key: 'api', title: 'API Reference', icon: CommandLineIcon },
    { key: 'deployment', title: 'Deployment', icon: CogIcon },
    { key: 'security', title: 'Security', icon: ShieldCheckIcon },
    { key: 'testing', title: 'Testing', icon: BeakerIcon },
    { key: 'troubleshooting', title: 'Troubleshooting', icon: WrenchScrewdriverIcon },
  ];

  // Convert markdown to HTML (simple implementation)
  const renderMarkdown = (content) => {
    return content
      .split('\n')
      .map((line, index) => {
        // Headers
        if (line.startsWith('# ')) {
          return <h1 key={index} className="text-3xl font-bold text-gray-900 dark:text-white mt-8 mb-4">{line.slice(2)}</h1>;
        }
        if (line.startsWith('## ')) {
          return <h2 key={index} className="text-2xl font-semibold text-gray-800 dark:text-gray-200 mt-6 mb-3">{line.slice(3)}</h2>;
        }
        if (line.startsWith('### ')) {
          return <h3 key={index} className="text-xl font-medium text-gray-700 dark:text-gray-300 mt-4 mb-2">{line.slice(4)}</h3>;
        }
        
        // Code blocks
        if (line.startsWith('```')) {
          return <div key={index} className="bg-gray-100 dark:bg-gray-800 p-4 rounded-md font-mono text-sm my-4 overflow-x-auto">{line}</div>;
        }
        
        // Lists
        if (line.startsWith('- ')) {
          return <li key={index} className="ml-6 my-1 text-gray-600 dark:text-gray-400">{line.slice(2)}</li>;
        }
        
        // Regular text
        if (line.trim()) {
          return <p key={index} className="text-gray-600 dark:text-gray-400 my-2 leading-relaxed">{line}</p>;
        }
        
        return <br key={index} />;
      });
  };

  return (
    <div className="flex h-screen bg-gray-50 dark:bg-gray-900">
      {/* Sidebar Navigation */}
      <div className="w-64 bg-white dark:bg-gray-800 shadow-lg">
        <div className="p-6">
          <div className="flex items-center mb-8">
            <DocumentTextIcon className="h-8 w-8 text-primary-600 mr-3" />
            <h1 className="text-xl font-bold text-gray-900 dark:text-white">
              Documentation
            </h1>
          </div>
          
          <nav className="space-y-2">
            {navigationItems.map((item) => {
              const Icon = item.icon;
              return (
                <button
                  key={item.key}
                  onClick={() => setActiveSection(item.key)}
                  className={`w-full flex items-center px-3 py-2 text-left rounded-md transition-colors ${
                    activeSection === item.key
                      ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-700 dark:text-primary-300'
                      : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700'
                  }`}
                >
                  <Icon className="h-5 w-5 mr-3" />
                  {item.title}
                </button>
              );
            })}
          </nav>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 overflow-auto">
        <div className="max-w-4xl mx-auto p-8">
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm">
            <div className="p-8">
              <div className="prose prose-gray dark:prose-invert max-w-none">
                {renderMarkdown(documentationSections[activeSection].content)}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Documentation;
