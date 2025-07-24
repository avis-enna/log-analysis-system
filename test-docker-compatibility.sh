#!/bin/bash

# =============================================================================
# Docker Compatibility Test for Log Analysis System
# Tests the deployment scripts with Docker instead of Podman
# =============================================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_test() { echo -e "${BLUE}[TEST]${NC} $1"; }
print_pass() { echo -e "${GREEN}[PASS]${NC} $1"; }
print_fail() { echo -e "${RED}[FAIL]${NC} $1"; }
print_info() { echo -e "${YELLOW}[INFO]${NC} $1"; }

# Test container runtime detection
test_container_runtime() {
    print_test "Testing container runtime detection..."
    
    # Test the deploy script can detect Docker
    if ./deploy-podman.sh help | grep -q "Docker found"; then
        print_pass "Docker detection works"
    elif ./deploy-podman.sh help | grep -q "Podman found"; then
        print_pass "Podman detection works"
    else
        print_fail "Container runtime detection failed"
        return 1
    fi
    
    return 0
}

# Test script syntax
test_script_syntax() {
    print_test "Testing script syntax..."
    
    local scripts=("deploy-podman.sh" "manage.sh" "dev-tools.sh" "monitor.sh" "db-tools.sh" "performance.sh" "security-tools.sh" "env-manager.sh" "observability.sh" "k8s-deploy.sh")
    local failed=0
    
    for script in "${scripts[@]}"; do
        if [[ -f "$script" ]]; then
            if bash -n "$script"; then
                print_pass "Syntax OK: $script"
            else
                print_fail "Syntax error: $script"
                ((failed++))
            fi
        else
            print_fail "Missing script: $script"
            ((failed++))
        fi
    done
    
    if [[ $failed -eq 0 ]]; then
        print_pass "All scripts have correct syntax"
        return 0
    else
        print_fail "$failed scripts have syntax errors"
        return 1
    fi
}

# Test help functions
test_help_functions() {
    print_test "Testing help functions..."
    
    local scripts=("deploy-podman.sh" "manage.sh" "dev-tools.sh" "monitor.sh" "db-tools.sh" "performance.sh" "security-tools.sh" "env-manager.sh" "observability.sh" "k8s-deploy.sh")
    local failed=0
    
    for script in "${scripts[@]}"; do
        if [[ -f "$script" ]]; then
            if ./"$script" help >/dev/null 2>&1; then
                print_pass "Help function works: $script"
            else
                print_fail "Help function failed: $script"
                ((failed++))
            fi
        fi
    done
    
    if [[ $failed -eq 0 ]]; then
        print_pass "All help functions work"
        return 0
    else
        print_fail "$failed help functions failed"
        return 1
    fi
}

# Test Docker availability
test_docker_availability() {
    print_test "Testing Docker availability..."
    
    if command -v docker &> /dev/null; then
        print_pass "Docker is available: $(docker --version)"
        
        # Test Docker daemon
        if docker info >/dev/null 2>&1; then
            print_pass "Docker daemon is running"
        else
            print_fail "Docker daemon is not running"
            return 1
        fi
    else
        print_fail "Docker is not installed"
        return 1
    fi
    
    return 0
}

# Test basic Docker operations
test_docker_operations() {
    print_test "Testing basic Docker operations..."
    
    # Test network creation
    if docker network create test-network >/dev/null 2>&1; then
        print_pass "Docker network creation works"
        docker network rm test-network >/dev/null 2>&1
    else
        print_fail "Docker network creation failed"
        return 1
    fi
    
    # Test image pull
    if docker pull hello-world >/dev/null 2>&1; then
        print_pass "Docker image pull works"
        docker rmi hello-world >/dev/null 2>&1
    else
        print_fail "Docker image pull failed"
        return 1
    fi
    
    return 0
}

# Main test execution
main() {
    echo "=============================================="
    echo "  Docker Compatibility Test"
    echo "=============================================="
    echo ""
    
    tests=(
        "test_script_syntax"
        "test_help_functions"
        "test_container_runtime"
        "test_docker_availability"
        "test_docker_operations"
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
        print_pass "All tests passed! Docker compatibility verified."
        echo ""
        echo "The deployment scripts are ready to use with Docker!"
        echo "Run: ./deploy-podman.sh"
        return 0
    else
        print_fail "Some tests failed. Check the issues above."
        return 1
    fi
}

# Run tests
main "$@"
