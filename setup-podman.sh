#!/bin/bash

# Podman setup script for log analysis system
# This script sets up the environment using Podman instead of Docker

set -e

echo "🚀 Setting up Log Analysis System with Podman"
echo "============================================="

# Check if podman is installed
if ! command -v podman &> /dev/null; then
    echo "❌ Podman is not installed. Please install Podman first."
    echo "   Visit: https://podman.io/getting-started/installation"
    exit 1
fi

# Check if podman-compose is installed
if ! command -v podman-compose &> /dev/null; then
    echo "⚠️  podman-compose is not installed. Installing via pip..."
    pip3 install podman-compose || {
        echo "❌ Failed to install podman-compose. Please install it manually:"
        echo "   pip3 install podman-compose"
        exit 1
    }
fi

echo "✅ Podman and podman-compose are available"

# Create necessary directories
echo "📁 Creating necessary directories..."
mkdir -p logs uploads

# Start podman machine if on macOS
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "🍎 Detected macOS - checking Podman machine..."
    if ! podman machine list --format "{{.Name}}" | grep -q "podman-machine-default"; then
        echo "🔧 Initializing Podman machine..."
        podman machine init --memory 4096 --cpus 2
    fi
    
    if ! podman machine list --format "{{.State}}" | grep -q "running"; then
        echo "▶️  Starting Podman machine..."
        podman machine start
    fi
    
    echo "✅ Podman machine is running"
fi

# Check if volumes exist and create them if needed
echo "🗄️  Setting up volumes..."
podman volume exists postgres_data || podman volume create postgres_data
podman volume exists elasticsearch_data || podman volume create elasticsearch_data 
podman volume exists grafana_data || podman volume create grafana_data

echo "✅ Volumes created successfully"

# Create network if it doesn't exist
echo "🌐 Setting up network..."
podman network exists log-analysis-network || podman network create log-analysis-network

echo "✅ Network created successfully"

echo ""
echo "🎉 Setup complete! You can now start the services with:"
echo "   ./start-podman.sh"
echo ""
echo "Available commands:"
echo "   ./start-podman.sh     - Start all services"
echo "   ./stop-podman.sh      - Stop all services"
echo "   ./status-podman.sh    - Check service status"
echo "   ./logs-podman.sh      - View service logs"
echo ""
