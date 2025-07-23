#!/bin/bash

# Comprehensive Test Runner for Log Analysis System
# This script runs all types of tests: unit, integration, E2E, performance, and security

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test results
UNIT_TESTS_PASSED=false
INTEGRATION_TESTS_PASSED=false
E2E_TESTS_PASSED=false
PERFORMANCE_TESTS_PASSED=false
SECURITY_TESTS_PASSED=false

# Function to print colored output
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to print section header
print_header() {
    echo
    print_status $BLUE "=============================================="
    print_status $BLUE "$1"
    print_status $BLUE "=============================================="
    echo
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to wait for service to be ready
wait_for_service() {
    local service_name=$1
    local url=$2
    local max_attempts=30
    local attempt=1
    
    print_status $YELLOW "Waiting for $service_name to be ready..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -f -s "$url" > /dev/null 2>&1; then
            print_status $GREEN "$service_name is ready!"
            return 0
        fi
        
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    print_status $RED "$service_name failed to start within timeout"
    return 1
}

# Function to run backend tests
run_backend_tests() {
    print_header "RUNNING BACKEND TESTS"
    
    cd backend
    
    # Unit Tests
    print_status $YELLOW "Running backend unit tests..."
    if mvn test -Dtest="**/*Test.java" -DfailIfNoTests=false; then
        print_status $GREEN "✓ Backend unit tests passed"
        UNIT_TESTS_PASSED=true
    else
        print_status $RED "✗ Backend unit tests failed"
        return 1
    fi
    
    # Integration Tests
    print_status $YELLOW "Running backend integration tests..."
    if mvn test -Dtest="**/*IntegrationTest.java" -DfailIfNoTests=false; then
        print_status $GREEN "✓ Backend integration tests passed"
        INTEGRATION_TESTS_PASSED=true
    else
        print_status $RED "✗ Backend integration tests failed"
        return 1
    fi
    
    # Generate test report
    mvn jacoco:report
    print_status $BLUE "Backend test coverage report generated: target/site/jacoco/index.html"
    
    cd ..
}

# Function to run frontend tests
run_frontend_tests() {
    print_header "RUNNING FRONTEND TESTS"
    
    cd frontend
    
    # Install dependencies if needed
    if [ ! -d "node_modules" ]; then
        print_status $YELLOW "Installing frontend dependencies..."
        npm ci
    fi
    
    # Unit Tests
    print_status $YELLOW "Running frontend unit tests..."
    if npm run test:ci; then
        print_status $GREEN "✓ Frontend unit tests passed"
        UNIT_TESTS_PASSED=true
    else
        print_status $RED "✗ Frontend unit tests failed"
        return 1
    fi
    
    # Lint checks
    print_status $YELLOW "Running frontend lint checks..."
    if npm run lint; then
        print_status $GREEN "✓ Frontend lint checks passed"
    else
        print_status $RED "✗ Frontend lint checks failed"
        return 1
    fi
    
    # Format checks
    print_status $YELLOW "Running frontend format checks..."
    if npm run format:check; then
        print_status $GREEN "✓ Frontend format checks passed"
    else
        print_status $RED "✗ Frontend format checks failed"
        return 1
    fi
    
    cd ..
}

# Function to start services for E2E tests
start_test_services() {
    print_header "STARTING TEST SERVICES"
    
    # Start services with docker-compose
    print_status $YELLOW "Starting test environment..."
    docker-compose -f docker-compose.test.yml up -d
    
    # Wait for services to be ready
    wait_for_service "Backend API" "http://localhost:8080/api/v1/health"
    wait_for_service "Frontend" "http://localhost:3000"
    wait_for_service "Elasticsearch" "http://localhost:9200/_cluster/health"
    
    # Seed test data
    print_status $YELLOW "Seeding test data..."
    curl -X POST "http://localhost:8080/api/v1/test/seed-data" || true
    
    sleep 5 # Give services a moment to stabilize
}

# Function to run E2E tests
run_e2e_tests() {
    print_header "RUNNING E2E TESTS"
    
    cd frontend
    
    # Install Playwright browsers if needed
    if [ ! -d "node_modules/@playwright" ]; then
        print_status $YELLOW "Installing Playwright browsers..."
        npx playwright install
    fi
    
    # Run E2E tests
    print_status $YELLOW "Running E2E tests..."
    if npm run test:e2e; then
        print_status $GREEN "✓ E2E tests passed"
        E2E_TESTS_PASSED=true
    else
        print_status $RED "✗ E2E tests failed"
        return 1
    fi
    
    cd ..
}

# Function to run performance tests
run_performance_tests() {
    print_header "RUNNING PERFORMANCE TESTS"
    
    cd backend
    
    # Run performance tests
    print_status $YELLOW "Running backend performance tests..."
    if mvn test -Dtest="**/*PerformanceTest.java" -DfailIfNoTests=false; then
        print_status $GREEN "✓ Backend performance tests passed"
        PERFORMANCE_TESTS_PASSED=true
    else
        print_status $RED "✗ Backend performance tests failed"
        return 1
    fi
    
    cd ../frontend
    
    # Run Lighthouse performance audit
    if command_exists lighthouse; then
        print_status $YELLOW "Running Lighthouse performance audit..."
        if npm run test:performance; then
            print_status $GREEN "✓ Frontend performance tests passed"
        else
            print_status $RED "✗ Frontend performance tests failed"
            return 1
        fi
    else
        print_status $YELLOW "Lighthouse not installed, skipping frontend performance tests"
    fi
    
    cd ..
}

# Function to run security tests
run_security_tests() {
    print_header "RUNNING SECURITY TESTS"
    
    # OWASP Dependency Check
    if command_exists dependency-check; then
        print_status $YELLOW "Running OWASP dependency check..."
        dependency-check --project "Log Analysis System" --scan . --format ALL --out ./security-reports/
        print_status $GREEN "✓ Security dependency check completed"
    else
        print_status $YELLOW "OWASP Dependency Check not installed, skipping"
    fi
    
    # Backend security tests
    cd backend
    if mvn test -Dtest="**/*SecurityTest.java" -DfailIfNoTests=false; then
        print_status $GREEN "✓ Backend security tests passed"
        SECURITY_TESTS_PASSED=true
    else
        print_status $RED "✗ Backend security tests failed"
        return 1
    fi
    cd ..
    
    # Frontend security audit
    cd frontend
    print_status $YELLOW "Running npm security audit..."
    if npm audit --audit-level moderate; then
        print_status $GREEN "✓ Frontend security audit passed"
    else
        print_status $YELLOW "⚠ Frontend security audit found issues (check npm audit output)"
    fi
    cd ..
}

# Function to stop test services
stop_test_services() {
    print_header "STOPPING TEST SERVICES"
    
    print_status $YELLOW "Stopping test environment..."
    docker-compose -f docker-compose.test.yml down -v
    
    # Clean up test containers
    docker system prune -f
}

# Function to generate test report
generate_test_report() {
    print_header "GENERATING TEST REPORT"
    
    local report_file="test-results/test-report.html"
    mkdir -p test-results
    
    cat > "$report_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Log Analysis System - Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .header { background: #f4f4f4; padding: 20px; border-radius: 5px; }
        .section { margin: 20px 0; }
        .passed { color: green; font-weight: bold; }
        .failed { color: red; font-weight: bold; }
        .skipped { color: orange; font-weight: bold; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Log Analysis System - Test Report</h1>
        <p>Generated on: $(date)</p>
        <p>Test Environment: $(uname -a)</p>
    </div>
    
    <div class="section">
        <h2>Test Results Summary</h2>
        <table>
            <tr><th>Test Type</th><th>Status</th><th>Details</th></tr>
            <tr><td>Unit Tests</td><td class="$([ "$UNIT_TESTS_PASSED" = true ] && echo "passed" || echo "failed")">$([ "$UNIT_TESTS_PASSED" = true ] && echo "PASSED" || echo "FAILED")</td><td>Backend and Frontend unit tests</td></tr>
            <tr><td>Integration Tests</td><td class="$([ "$INTEGRATION_TESTS_PASSED" = true ] && echo "passed" || echo "failed")">$([ "$INTEGRATION_TESTS_PASSED" = true ] && echo "PASSED" || echo "FAILED")</td><td>API and database integration tests</td></tr>
            <tr><td>E2E Tests</td><td class="$([ "$E2E_TESTS_PASSED" = true ] && echo "passed" || echo "failed")">$([ "$E2E_TESTS_PASSED" = true ] && echo "PASSED" || echo "FAILED")</td><td>Browser automation tests</td></tr>
            <tr><td>Performance Tests</td><td class="$([ "$PERFORMANCE_TESTS_PASSED" = true ] && echo "passed" || echo "failed")">$([ "$PERFORMANCE_TESTS_PASSED" = true ] && echo "PASSED" || echo "FAILED")</td><td>Load and performance tests</td></tr>
            <tr><td>Security Tests</td><td class="$([ "$SECURITY_TESTS_PASSED" = true ] && echo "passed" || echo "failed")">$([ "$SECURITY_TESTS_PASSED" = true ] && echo "PASSED" || echo "FAILED")</td><td>Security vulnerability tests</td></tr>
        </table>
    </div>
    
    <div class="section">
        <h2>Test Coverage</h2>
        <p>Backend Coverage: <a href="backend/target/site/jacoco/index.html">View Report</a></p>
        <p>Frontend Coverage: <a href="frontend/coverage/lcov-report/index.html">View Report</a></p>
    </div>
    
    <div class="section">
        <h2>Performance Metrics</h2>
        <p>Lighthouse Report: <a href="frontend/lighthouse-report.json">View Report</a></p>
    </div>
    
    <div class="section">
        <h2>Security Reports</h2>
        <p>OWASP Dependency Check: <a href="security-reports/">View Reports</a></p>
    </div>
</body>
</html>
EOF
    
    print_status $GREEN "Test report generated: $report_file"
}

# Main execution
main() {
    print_header "LOG ANALYSIS SYSTEM - COMPREHENSIVE TEST SUITE"
    
    # Check prerequisites
    print_status $YELLOW "Checking prerequisites..."
    
    if ! command_exists docker; then
        print_status $RED "Docker is required but not installed"
        exit 1
    fi
    
    if ! command_exists docker-compose; then
        print_status $RED "Docker Compose is required but not installed"
        exit 1
    fi
    
    if ! command_exists mvn; then
        print_status $RED "Maven is required but not installed"
        exit 1
    fi
    
    if ! command_exists npm; then
        print_status $RED "Node.js/npm is required but not installed"
        exit 1
    fi
    
    # Create results directory
    mkdir -p test-results security-reports
    
    # Run tests
    local exit_code=0
    
    # Backend tests
    if ! run_backend_tests; then
        exit_code=1
    fi
    
    # Frontend tests
    if ! run_frontend_tests; then
        exit_code=1
    fi
    
    # Start services for E2E tests
    if ! start_test_services; then
        exit_code=1
    fi
    
    # E2E tests
    if ! run_e2e_tests; then
        exit_code=1
    fi
    
    # Performance tests
    if ! run_performance_tests; then
        exit_code=1
    fi
    
    # Security tests
    if ! run_security_tests; then
        exit_code=1
    fi
    
    # Stop services
    stop_test_services
    
    # Generate report
    generate_test_report
    
    # Final summary
    print_header "TEST EXECUTION SUMMARY"
    
    if [ $exit_code -eq 0 ]; then
        print_status $GREEN "🎉 ALL TESTS PASSED! 🎉"
        print_status $GREEN "The Log Analysis System is ready for deployment."
    else
        print_status $RED "❌ SOME TESTS FAILED ❌"
        print_status $RED "Please review the test output and fix the issues."
    fi
    
    print_status $BLUE "Test report available at: test-results/test-report.html"
    
    exit $exit_code
}

# Run main function
main "$@"
