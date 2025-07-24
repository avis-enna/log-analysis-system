#!/bin/bash

# =============================================================================
# Test Script for Podman Deployment
# =============================================================================

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_test() { echo -e "${BLUE}[TEST]${NC} $1"; }
print_pass() { echo -e "${GREEN}[PASS]${NC} $1"; }
print_fail() { echo -e "${RED}[FAIL]${NC} $1"; }
print_info() { echo -e "${YELLOW}[INFO]${NC} $1"; }

# Test functions
test_podman_installed() {
    print_test "Checking if Podman is installed..."
    if command -v podman &> /dev/null; then
        print_pass "Podman is installed: $(podman --version)"
        return 0
    else
        print_fail "Podman is not installed"
        echo "Install with:"
        echo "  Ubuntu/Debian: sudo apt-get install podman"
        echo "  RHEL/CentOS: sudo dnf install podman"
        echo "  macOS: brew install podman"
        return 1
    fi
}

test_ports_available() {
    print_test "Checking if required ports are available..."
    ports=(3000 8080 5432 6379)
    for port in "${ports[@]}"; do
        if netstat -tuln 2>/dev/null | grep -q ":$port "; then
            print_fail "Port $port is already in use"
            echo "Kill the process using: sudo lsof -ti:$port | xargs kill -9"
            return 1
        else
            print_pass "Port $port is available"
        fi
    done
    return 0
}

test_script_executable() {
    print_test "Checking if deployment script is executable..."
    if [[ -x "./deploy-podman.sh" ]]; then
        print_pass "Deployment script is executable"
        return 0
    else
        print_fail "Deployment script is not executable"
        echo "Make it executable with: chmod +x deploy-podman.sh"
        return 1
    fi
}

test_docker_files_exist() {
    print_test "Checking if required files exist..."
    files=(
        "backend/pom.xml"
        "backend/src/main/resources/application-docker.yml"
        "frontend/package.json"
        "frontend/nginx.conf"
        "docker-compose.podman.yml"
    )
    
    for file in "${files[@]}"; do
        if [[ -f "$file" ]]; then
            print_pass "File exists: $file"
        else
            print_fail "File missing: $file"
            return 1
        fi
    done
    return 0
}

test_deployment_dry_run() {
    print_test "Testing deployment script help..."
    if ./deploy-podman.sh help &>/dev/null; then
        print_pass "Deployment script help works"
        return 0
    else
        print_fail "Deployment script help failed"
        return 1
    fi
}

# Main test execution
main() {
    echo "=============================================="
    echo "  Log Analysis System - Deployment Test"
    echo "=============================================="
    echo ""
    
    tests=(
        "test_podman_installed"
        "test_ports_available"
        "test_script_executable"
        "test_docker_files_exist"
        "test_deployment_dry_run"
    )
    
    passed=0
    failed=0
    
    for test in "${tests[@]}"; do
        if $test; then
            ((passed++))
        else
            ((failed++))
        fi
        echo ""
    done
    
    echo "=============================================="
    echo "  Test Results"
    echo "=============================================="
    echo -e "Passed: ${GREEN}$passed${NC}"
    echo -e "Failed: ${RED}$failed${NC}"
    echo ""
    
    if [[ $failed -eq 0 ]]; then
        print_pass "All tests passed! Ready to deploy."
        echo ""
        echo "Run deployment with:"
        echo "  ./deploy-podman.sh"
        echo ""
        return 0
    else
        print_fail "Some tests failed. Fix issues before deploying."
        return 1
    fi
}

# Run tests
main "$@"
