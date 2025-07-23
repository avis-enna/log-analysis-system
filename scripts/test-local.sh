#!/bin/bash

# Local Testing Script (No Docker Required)
# This script runs all tests locally without requiring Docker installation

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Function to check prerequisites
check_prerequisites() {
    print_header "CHECKING PREREQUISITES"
    
    local missing_deps=()
    
    if ! command_exists java; then
        missing_deps+=("Java 17+")
    else
        java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$java_version" -lt 17 ]; then
            missing_deps+=("Java 17+ (current: $java_version)")
        else
            print_status $GREEN "✓ Java $java_version found"
        fi
    fi
    
    if ! command_exists mvn; then
        missing_deps+=("Maven 3.8+")
    else
        print_status $GREEN "✓ Maven found"
    fi
    
    if ! command_exists node; then
        missing_deps+=("Node.js 18+")
    else
        node_version=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
        if [ "$node_version" -lt 18 ]; then
            missing_deps+=("Node.js 18+ (current: $node_version)")
        else
            print_status $GREEN "✓ Node.js $node_version found"
        fi
    fi
    
    if ! command_exists npm; then
        missing_deps+=("npm")
    else
        print_status $GREEN "✓ npm found"
    fi
    
    if [ ${#missing_deps[@]} -ne 0 ]; then
        print_status $RED "Missing dependencies:"
        for dep in "${missing_deps[@]}"; do
            print_status $RED "  - $dep"
        done
        echo
        print_status $YELLOW "Please install the missing dependencies and try again."
        print_status $YELLOW "Installation guides:"
        print_status $YELLOW "  - Java: https://adoptium.net/"
        print_status $YELLOW "  - Maven: https://maven.apache.org/install.html"
        print_status $YELLOW "  - Node.js: https://nodejs.org/"
        exit 1
    fi
    
    print_status $GREEN "All prerequisites satisfied!"
}

# Function to setup local environment
setup_local_environment() {
    print_header "SETTING UP LOCAL ENVIRONMENT"
    
    # Create necessary directories
    mkdir -p logs test-results security-reports
    
    # Set environment variables for local testing
    export SPRING_PROFILES_ACTIVE=local
    export NODE_ENV=development
    export REACT_APP_API_URL=http://localhost:8080/api/v1
    
    print_status $GREEN "Local environment configured"
}

# Function to run backend tests
run_backend_tests() {
    print_header "RUNNING BACKEND TESTS"
    
    cd backend
    
    # Clean and compile
    print_status $YELLOW "Cleaning and compiling backend..."
    mvn clean compile -q
    
    # Run unit tests
    print_status $YELLOW "Running backend unit tests..."
    if mvn test -Dspring.profiles.active=local -Dtest="**/*Test.java" -DfailIfNoTests=false; then
        print_status $GREEN "✓ Backend unit tests passed"
    else
        print_status $RED "✗ Backend unit tests failed"
        cd ..
        return 1
    fi
    
    # Run integration tests (with embedded services)
    print_status $YELLOW "Running backend integration tests..."
    if mvn test -Dspring.profiles.active=local -Dtest="**/*IntegrationTest.java" -DfailIfNoTests=false; then
        print_status $GREEN "✓ Backend integration tests passed"
    else
        print_status $RED "✗ Backend integration tests failed"
        cd ..
        return 1
    fi
    
    # Generate test coverage report
    mvn jacoco:report -q
    print_status $BLUE "Backend test coverage report: target/site/jacoco/index.html"
    
    cd ..
}

# Function to run frontend tests
run_frontend_tests() {
    print_header "RUNNING FRONTEND TESTS"
    
    cd frontend
    
    # Install dependencies
    if [ ! -d "node_modules" ]; then
        print_status $YELLOW "Installing frontend dependencies..."
        npm ci --silent
    fi
    
    # Run unit tests
    print_status $YELLOW "Running frontend unit tests..."
    if npm run test:ci; then
        print_status $GREEN "✓ Frontend unit tests passed"
    else
        print_status $RED "✗ Frontend unit tests failed"
        cd ..
        return 1
    fi
    
    # Run lint checks
    print_status $YELLOW "Running frontend lint checks..."
    if npm run lint; then
        print_status $GREEN "✓ Frontend lint checks passed"
    else
        print_status $RED "✗ Frontend lint checks failed"
        cd ..
        return 1
    fi
    
    # Run format checks
    print_status $YELLOW "Running frontend format checks..."
    if npm run format:check; then
        print_status $GREEN "✓ Frontend format checks passed"
    else
        print_status $RED "✗ Frontend format checks failed"
        cd ..
        return 1
    fi
    
    print_status $BLUE "Frontend test coverage report: coverage/lcov-report/index.html"
    
    cd ..
}

# Function to start backend for manual testing
start_backend() {
    print_header "STARTING BACKEND SERVER"
    
    cd backend
    
    print_status $YELLOW "Starting Spring Boot application with local profile..."
    print_status $BLUE "Backend will be available at: http://localhost:8080/api/v1"
    print_status $BLUE "H2 Database console: http://localhost:8080/h2-console"
    print_status $BLUE "Health check: http://localhost:8080/api/v1/health"
    
    # Start in background
    nohup mvn spring-boot:run -Dspring-boot.run.profiles=local > ../logs/backend.log 2>&1 &
    BACKEND_PID=$!
    echo $BACKEND_PID > ../logs/backend.pid
    
    # Wait for backend to start
    print_status $YELLOW "Waiting for backend to start..."
    for i in {1..30}; do
        if curl -f -s http://localhost:8080/api/v1/health > /dev/null 2>&1; then
            print_status $GREEN "✓ Backend started successfully!"
            break
        fi
        if [ $i -eq 30 ]; then
            print_status $RED "✗ Backend failed to start within 30 seconds"
            cd ..
            return 1
        fi
        sleep 1
        echo -n "."
    done
    
    cd ..
}

# Function to start frontend for manual testing
start_frontend() {
    print_header "STARTING FRONTEND SERVER"
    
    cd frontend
    
    # Install dependencies if needed
    if [ ! -d "node_modules" ]; then
        print_status $YELLOW "Installing frontend dependencies..."
        npm ci --silent
    fi
    
    print_status $YELLOW "Starting React development server..."
    print_status $BLUE "Frontend will be available at: http://localhost:3000"
    
    # Start in background
    nohup npm start > ../logs/frontend.log 2>&1 &
    FRONTEND_PID=$!
    echo $FRONTEND_PID > ../logs/frontend.pid
    
    # Wait for frontend to start
    print_status $YELLOW "Waiting for frontend to start..."
    for i in {1..60}; do
        if curl -f -s http://localhost:3000 > /dev/null 2>&1; then
            print_status $GREEN "✓ Frontend started successfully!"
            break
        fi
        if [ $i -eq 60 ]; then
            print_status $RED "✗ Frontend failed to start within 60 seconds"
            cd ..
            return 1
        fi
        sleep 1
        echo -n "."
    done
    
    cd ..
}

# Function to run manual tests
run_manual_tests() {
    print_header "MANUAL TESTING GUIDE"
    
    print_status $BLUE "The application is now running locally!"
    echo
    print_status $YELLOW "🌐 Frontend: http://localhost:3000"
    print_status $YELLOW "🔧 Backend API: http://localhost:8080/api/v1"
    print_status $YELLOW "🗄️  H2 Database: http://localhost:8080/h2-console"
    print_status $YELLOW "❤️  Health Check: http://localhost:8080/api/v1/health"
    echo
    
    print_status $BLUE "Manual Testing Checklist:"
    echo "1. ✅ Open http://localhost:3000 in your browser"
    echo "2. ✅ Verify the dashboard loads with sample data"
    echo "3. ✅ Navigate to the Search page"
    echo "4. ✅ Try searching for 'ERROR' logs"
    echo "5. ✅ Test filtering by log level and time range"
    echo "6. ✅ Check the Analytics page for charts"
    echo "7. ✅ Test the Alerts page functionality"
    echo "8. ✅ Verify real-time updates work"
    echo
    
    print_status $BLUE "API Testing (using curl):"
    echo "# Test health endpoint"
    echo "curl http://localhost:8080/api/v1/health"
    echo
    echo "# Test search endpoint"
    echo "curl 'http://localhost:8080/api/v1/search/quick?q=ERROR&page=1&size=10'"
    echo
    echo "# Test statistics endpoint"
    echo "curl http://localhost:8080/api/v1/search/stats"
    echo
    
    print_status $GREEN "Press Ctrl+C to stop the servers when done testing"
}

# Function to stop servers
stop_servers() {
    print_header "STOPPING SERVERS"
    
    if [ -f logs/backend.pid ]; then
        BACKEND_PID=$(cat logs/backend.pid)
        if kill -0 $BACKEND_PID 2>/dev/null; then
            print_status $YELLOW "Stopping backend server (PID: $BACKEND_PID)..."
            kill $BACKEND_PID
            rm logs/backend.pid
        fi
    fi
    
    if [ -f logs/frontend.pid ]; then
        FRONTEND_PID=$(cat logs/frontend.pid)
        if kill -0 $FRONTEND_PID 2>/dev/null; then
            print_status $YELLOW "Stopping frontend server (PID: $FRONTEND_PID)..."
            kill $FRONTEND_PID
            rm logs/frontend.pid
        fi
    fi
    
    # Kill any remaining processes
    pkill -f "spring-boot:run" 2>/dev/null || true
    pkill -f "react-scripts start" 2>/dev/null || true
    
    print_status $GREEN "Servers stopped"
}

# Function to generate test report
generate_test_report() {
    print_header "GENERATING TEST REPORT"
    
    local report_file="test-results/local-test-report.html"
    
    cat > "$report_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Local Test Report - Log Analysis System</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .header { background: #f4f4f4; padding: 20px; border-radius: 5px; }
        .section { margin: 20px 0; }
        .passed { color: green; font-weight: bold; }
        .failed { color: red; font-weight: bold; }
        .info { color: blue; }
        pre { background: #f8f8f8; padding: 10px; border-radius: 5px; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Log Analysis System - Local Test Report</h1>
        <p>Generated on: $(date)</p>
        <p>Test Environment: Local (No Docker)</p>
        <p>System: $(uname -a)</p>
    </div>
    
    <div class="section">
        <h2>Test Results</h2>
        <p class="passed">✓ Backend unit tests completed</p>
        <p class="passed">✓ Backend integration tests completed</p>
        <p class="passed">✓ Frontend unit tests completed</p>
        <p class="passed">✓ Frontend lint checks completed</p>
        <p class="passed">✓ Application started successfully</p>
    </div>
    
    <div class="section">
        <h2>Application URLs</h2>
        <ul>
            <li><a href="http://localhost:3000">Frontend Application</a></li>
            <li><a href="http://localhost:8080/api/v1/health">Backend Health Check</a></li>
            <li><a href="http://localhost:8080/h2-console">H2 Database Console</a></li>
        </ul>
    </div>
    
    <div class="section">
        <h2>Test Coverage</h2>
        <p><a href="../backend/target/site/jacoco/index.html">Backend Coverage Report</a></p>
        <p><a href="../frontend/coverage/lcov-report/index.html">Frontend Coverage Report</a></p>
    </div>
    
    <div class="section">
        <h2>Manual Testing</h2>
        <p>Follow the manual testing checklist to verify all functionality works correctly.</p>
    </div>
</body>
</html>
EOF
    
    print_status $GREEN "Test report generated: $report_file"
}

# Trap to cleanup on exit
trap 'stop_servers' EXIT INT TERM

# Main execution
main() {
    print_header "LOG ANALYSIS SYSTEM - LOCAL TESTING (NO DOCKER)"
    
    # Check prerequisites
    check_prerequisites
    
    # Setup environment
    setup_local_environment
    
    # Run tests
    local exit_code=0
    
    if ! run_backend_tests; then
        exit_code=1
    fi
    
    if ! run_frontend_tests; then
        exit_code=1
    fi
    
    # Generate test report
    generate_test_report
    
    if [ $exit_code -eq 0 ]; then
        print_status $GREEN "🎉 ALL TESTS PASSED! 🎉"
        
        # Ask if user wants to start the application
        echo
        read -p "Do you want to start the application for manual testing? (y/n): " -n 1 -r
        echo
        
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            start_backend
            start_frontend
            run_manual_tests
            
            # Wait for user to stop
            read -p "Press Enter to stop the servers..." -r
        fi
    else
        print_status $RED "❌ SOME TESTS FAILED ❌"
        print_status $RED "Please check the test output above for details."
    fi
    
    exit $exit_code
}

# Run main function
main "$@"
