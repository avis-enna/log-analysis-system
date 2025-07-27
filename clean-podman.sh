#!/bin/bash

# Clean script for log analysis system using Podman
set -e

echo "🗑️  Cleaning up Log Analysis System"
echo "==================================="

read -p "⚠️  This will remove all containers, volumes, and data. Continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Cancelled"
    exit 1
fi

echo "🛑 Stopping all services..."
podman-compose -f docker-compose-podman.yml down -v

echo "🧹 Removing containers..."
podman container prune -f

echo "🗑️  Removing volumes..."
podman volume rm postgres_data elasticsearch_data grafana_data 2>/dev/null || true

echo "🌐 Removing network..."
podman network rm log-analysis-network 2>/dev/null || true

echo "🖼️  Removing unused images..."
podman image prune -f

echo "✅ Cleanup complete!"
echo ""
echo "💡 To start fresh: ./setup-podman.sh"
echo ""
