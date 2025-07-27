#!/bin/bash

# Test script for log analysis system using Podman
set -e

echo "🧪 Testing Log Analysis System with Podman"
echo "=========================================="

BASE_URL="http://localhost:8080/api/v1"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

test_passed=0
test_failed=0

run_test() {
    local test_name="$1"
    local command="$2"
    local expected_status="${3:-200}"
    
    echo -n "  $test_name: "
    
    if eval "$command" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ PASS${NC}"
        ((test_passed++))
    else
        echo -e "${RED}❌ FAIL${NC}"
        ((test_failed++))
    fi
}

echo ""
echo "🔍 1. Testing service availability..."

# Test basic service connectivity
run_test "PostgreSQL" "podman exec log-analysis-postgres pg_isready -U postgres"
run_test "Redis" "podman exec log-analysis-redis redis-cli ping | grep -q PONG"
run_test "Elasticsearch" "curl -s http://localhost:9200/_cluster/health | grep -q cluster_name"
run_test "Application" "curl -s http://localhost:8080/actuator/health | grep -q UP"

echo ""
echo "🔐 2. Testing authentication endpoints..."

# Test authentication
run_test "Test credentials endpoint" "curl -s $BASE_URL/auth/test-credentials"
run_test "Admin login" "curl -s -u admin:admin123 $BASE_URL/auth/me"
run_test "Developer login" "curl -s -u dev:dev123 $BASE_URL/auth/me"
run_test "QA login" "curl -s -u qa:qa123 $BASE_URL/auth/me"

echo ""
echo "📊 3. Testing dashboard endpoints..."

# Test dashboard endpoints with different user roles
run_test "Dashboard stats (admin)" "curl -s -u admin:admin123 $BASE_URL/dashboard/stats"
run_test "Dashboard realtime (dev)" "curl -s -u dev:dev123 $BASE_URL/dashboard/realtime"
run_test "Dashboard volume (qa)" "curl -s -u qa:qa123 $BASE_URL/dashboard/volume"
run_test "Dashboard sources" "curl -s -u admin:admin123 $BASE_URL/dashboard/top-sources"
run_test "Error trends" "curl -s -u admin:admin123 $BASE_URL/dashboard/error-trends"

echo ""
echo "📝 4. Testing log ingestion..."

# Test log ingestion (requires developer role)
run_test "Ingest sample log" "curl -s -X POST -u dev:dev123 -H 'Content-Type: application/json' -d '{\"message\":\"Test log from Podman setup\",\"level\":\"INFO\",\"source\":\"test-podman\",\"application\":\"log-analyzer\"}' $BASE_URL/logs/ingest"

# Wait a moment for processing
sleep 2

run_test "Search ingested log" "curl -s -u admin:admin123 '$BASE_URL/logs/search?query=Podman'"

echo ""
echo "🚨 5. Testing alerts system..."

run_test "Get alerts" "curl -s -u admin:admin123 $BASE_URL/alerts"
run_test "Alert statistics" "curl -s -u admin:admin123 $BASE_URL/alerts/stats"

echo ""
echo "🔧 6. Testing Kafka integration..."

# Test Kafka topics (if accessible)
run_test "Check Kafka topics" "podman exec log-analysis-kafka kafka-topics --list --bootstrap-server localhost:9092 | grep -q logs"

echo ""
echo "💾 7. Testing Redis cache..."

run_test "Redis info" "podman exec log-analysis-redis redis-cli info replication | grep -q role:master"

echo ""
echo "🎯 8. Testing role-based access..."

# Test unauthorized access
run_test "Unauthorized access fails" "! curl -s $BASE_URL/dashboard/stats | grep -q totalLogs"
run_test "QA cannot ingest logs" "! curl -s -X POST -u qa:qa123 -H 'Content-Type: application/json' -d '{\"message\":\"test\"}' $BASE_URL/logs/ingest"

echo ""
echo "📈 Results Summary"
echo "=================="
echo -e "Tests passed: ${GREEN}$test_passed${NC}"
echo -e "Tests failed: ${RED}$test_failed${NC}"
echo "Total tests: $((test_passed + test_failed))"

if [ $test_failed -eq 0 ]; then
    echo -e "\n🎉 ${GREEN}All tests passed! The system is working correctly with Podman.${NC}"
    exit 0
else
    echo -e "\n⚠️  ${YELLOW}Some tests failed. Please check the logs and service status.${NC}"
    echo "   Run: ./status-podman.sh"
    echo "   View logs: ./logs-podman.sh [service-name]"
    exit 1
fi
