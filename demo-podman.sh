#!/bin/bash

# Quick deployment script for Podman
# This script demonstrates the Kafka + Redis integration with role-based authentication

set -e

echo "🚀 Quick Podman Deployment Demo"
echo "==============================="

# Check if services are running
if ! podman ps --format "{{.Names}}" | grep -q "log-analysis-app"; then
    echo "⚠️  Services not running. Starting them now..."
    ./start-podman.sh
    echo "⏳ Waiting for services to initialize..."
    sleep 30
fi

echo ""
echo "🧪 Testing Kafka + Redis Integration with Role-based Auth"
echo "========================================================="

BASE_URL="http://localhost:8080/api/v1"

# Test 1: Authenticate as different users
echo ""
echo "1️⃣  Testing Authentication"
echo "------------------------"

for user in "admin:admin123:ADMIN" "dev:dev123:DEVELOPER" "qa:qa123:QA"; do
    IFS=':' read -r username password role <<< "$user"
    echo -n "  Testing $username ($role): "
    
    if curl -s -u "$username:$password" "$BASE_URL/auth/me" | grep -q "\"username\":\"$username\""; then
        echo "✅ Success"
    else
        echo "❌ Failed"
    fi
done

# Test 2: Log ingestion through HTTP (will be processed by Kafka)
echo ""
echo "2️⃣  Testing Log Ingestion (HTTP → Kafka → Processing)"
echo "----------------------------------------------------"

echo "  Ingesting sample logs as developer..."
for i in {1..5}; do
    curl -s -X POST -u dev:dev123 \
        -H "Content-Type: application/json" \
        -d "{\"message\":\"Demo log entry #$i from Podman setup\",\"level\":\"INFO\",\"source\":\"podman-demo\",\"application\":\"log-analyzer\",\"environment\":\"development\"}" \
        "$BASE_URL/logs/ingest" > /dev/null
    echo -n "."
done

# Add an error log for alert testing
curl -s -X POST -u dev:dev123 \
    -H "Content-Type: application/json" \
    -d '{"message":"Critical error in Podman demo setup","level":"ERROR","source":"podman-demo","application":"log-analyzer","environment":"development"}' \
    "$BASE_URL/logs/ingest" > /dev/null

echo " ✅ 6 logs ingested"

# Wait for Kafka processing
echo "  Waiting for Kafka processing..."
sleep 5

# Test 3: Verify logs were processed and cached
echo ""
echo "3️⃣  Testing Data Processing & Caching"
echo "------------------------------------"

echo -n "  Checking dashboard stats: "
stats=$(curl -s -u admin:admin123 "$BASE_URL/dashboard/stats")
if echo "$stats" | grep -q "totalLogs"; then
    total_logs=$(echo "$stats" | grep -o '"totalLogs":[0-9]*' | cut -d':' -f2)
    echo "✅ $total_logs total logs found"
else
    echo "❌ No stats found"
fi

echo -n "  Checking real-time metrics: "
if curl -s -u qa:qa123 "$BASE_URL/dashboard/realtime" | grep -q "logsPerSecond"; then
    echo "✅ Real-time metrics available"
else
    echo "❌ Real-time metrics unavailable"
fi

# Test 4: Alert generation
echo ""
echo "4️⃣  Testing Alert Generation"
echo "---------------------------"

echo -n "  Checking for generated alerts: "
alerts=$(curl -s -u admin:admin123 "$BASE_URL/alerts/stats")
if echo "$alerts" | grep -q "total"; then
    total_alerts=$(echo "$alerts" | grep -o '"total":[0-9]*' | cut -d':' -f2)
    echo "✅ $total_alerts alerts generated"
else
    echo "❌ No alerts found"
fi

# Test 5: Role-based access control
echo ""
echo "5️⃣  Testing Role-based Access Control"
echo "------------------------------------"

echo -n "  QA user accessing dashboard: "
if curl -s -u qa:qa123 "$BASE_URL/dashboard/stats" | grep -q "totalLogs"; then
    echo "✅ Allowed"
else
    echo "❌ Denied"
fi

echo -n "  QA user trying to ingest logs: "
if curl -s -X POST -u qa:qa123 \
    -H "Content-Type: application/json" \
    -d '{"message":"test"}' \
    "$BASE_URL/logs/ingest" | grep -q "error"; then
    echo "✅ Correctly denied"
else
    echo "❌ Incorrectly allowed"
fi

echo -n "  Admin accessing all endpoints: "
if curl -s -u admin:admin123 "$BASE_URL/dashboard/health-insights" | grep -q "title"; then
    echo "✅ Full access granted"
else
    echo "❌ Access denied"
fi

# Test 6: Search functionality
echo ""
echo "6️⃣  Testing Search & Analytics"
echo "-----------------------------"

echo -n "  Searching for 'Podman' logs: "
search_results=$(curl -s -u admin:admin123 "$BASE_URL/logs/search?query=Podman")
if echo "$search_results" | grep -q "podman-demo"; then
    echo "✅ Search working"
else
    echo "❌ Search not working"
fi

echo -n "  Getting top sources: "
if curl -s -u admin:admin123 "$BASE_URL/dashboard/top-sources" | grep -q "podman-demo"; then
    echo "✅ Analytics working"
else
    echo "❌ Analytics not working"
fi

# Summary
echo ""
echo "🎉 Demo Complete!"
echo "================"
echo ""
echo "🌐 Available endpoints:"
echo "  • Application: http://localhost:8080"
echo "  • Grafana: http://localhost:3000 (admin/admin)"
echo "  • Prometheus: http://localhost:9090"
echo "  • Elasticsearch: http://localhost:9200"
echo ""
echo "🔐 User accounts:"
echo "  • admin/admin123 - Full access"
echo "  • dev/dev123 - Developer access"
echo "  • qa/qa123 - QA access"
echo ""
echo "📊 Try these commands:"
echo "  curl -u admin:admin123 http://localhost:8080/api/v1/dashboard/stats"
echo "  curl -u qa:qa123 http://localhost:8080/api/v1/dashboard/realtime"
echo "  curl -u dev:dev123 -X POST -H 'Content-Type: application/json' \\"
echo "    -d '{\"message\":\"Your log here\",\"level\":\"INFO\"}' \\"
echo "    http://localhost:8080/api/v1/logs/ingest"
echo ""
echo "🛠️  Management commands:"
echo "  ./status-podman.sh  - Check service status"
echo "  ./logs-podman.sh app - View application logs"
echo "  ./stop-podman.sh    - Stop all services"
echo ""
