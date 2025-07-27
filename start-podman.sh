#!/bin/bash

# Start script for log analysis system using Podman
set -e

echo "🚀 Starting Log Analysis System with Podman"
echo "==========================================="

# Check if setup was run
if ! podman volume exists postgres_data; then
    echo "⚠️  Volumes not found. Running setup first..."
    ./setup-podman.sh
fi

echo "▶️  Starting services with podman-compose..."
podman-compose -f docker-compose-podman.yml up -d

echo "⏳ Waiting for services to be ready..."
sleep 10

# Check service health
echo "🔍 Checking service health..."

check_service() {
    local service=$1
    local port=$2
    local retries=30
    
    echo -n "  $service "
    for i in $(seq 1 $retries); do
        if curl -s http://localhost:$port > /dev/null 2>&1; then
            echo "✅"
            return 0
        fi
        echo -n "."
        sleep 2
    done
    echo "❌"
    return 1
}

# Wait for core services
echo "📡 Waiting for core services..."
sleep 20

echo "🔍 Service status:"
podman-compose -f docker-compose-podman.yml ps

echo ""
echo "🎉 Log Analysis System is starting up!"
echo ""
echo "📊 Available endpoints:"
echo "   Application: http://localhost:8081"
echo "   Elasticsearch: http://localhost:9200"
echo "   Grafana: http://localhost:4000 (admin/admin)"
echo "   Prometheus: http://localhost:9090"
echo ""
echo "🔐 Test credentials:"
echo "   admin/admin123 - Full access"
echo "   dev/dev123 - Developer access"
echo "   qa/qa123 - QA access"
echo ""
echo "📝 To view logs: ./logs-podman.sh [service-name]"
echo "🛑 To stop: ./stop-podman.sh"
echo ""
