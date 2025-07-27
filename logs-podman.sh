#!/bin/bash

# Logs script for log analysis system using Podman
set -e

SERVICE=${1:-"app"}

echo "📝 Viewing logs for service: $SERVICE"
echo "====================================="

if [ "$SERVICE" = "all" ]; then
    echo "🔍 Showing logs for all services..."
    podman-compose -f docker-compose-podman.yml logs -f
else
    echo "🔍 Showing logs for $SERVICE (press Ctrl+C to exit)..."
    podman-compose -f docker-compose-podman.yml logs -f $SERVICE
fi
