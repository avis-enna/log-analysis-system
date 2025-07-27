#!/bin/bash

# Stop script for log analysis system using Podman
set -e

echo "🛑 Stopping Log Analysis System"
echo "==============================="

echo "⏹️  Stopping services..."
podman-compose -f docker-compose-podman.yml down

echo "🧹 Cleaning up..."
# Optional: Remove unused containers and images
# podman container prune -f
# podman image prune -f

echo "✅ All services stopped successfully"
echo ""
echo "💡 To restart: ./start-podman.sh"
echo "🗑️  To remove all data: ./clean-podman.sh"
echo ""
