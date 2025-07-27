#!/bin/bash

# Status script for log analysis system using Podman
set -e

echo "📊 Log Analysis System Status"
echo "=============================="

echo "🐳 Podman containers:"
podman-compose -f docker-compose-podman.yml ps

echo ""
echo "📡 Service health checks:"

check_service() {
    local service=$1
    local port=$2
    local endpoint=${3:-""}
    
    echo -n "  $service (port $port): "
    if curl -s http://localhost:$port$endpoint > /dev/null 2>&1; then
        echo "✅ Running"
    else
        echo "❌ Not responding"
    fi
}

check_service "PostgreSQL" "5432"
check_service "Redis" "6379"
check_service "Elasticsearch" "9200"
check_service "Kafka" "9092"
check_service "Application" "8080" "/actuator/health"
check_service "Grafana" "3000"
check_service "Prometheus" "9090"

echo ""
echo "🗄️  Podman volumes:"
podman volume ls --filter name=log-analysis

echo ""
echo "🌐 Networks:"
podman network ls --filter name=log-analysis

echo ""
echo "💾 System resources:"
echo "  Containers: $(podman ps -q | wc -l) running"
echo "  Images: $(podman images -q | wc -l) stored"
echo "  Volumes: $(podman volume ls -q | wc -l) created"

echo ""
echo "📝 To view logs: ./logs-podman.sh [service-name]"
echo ""
