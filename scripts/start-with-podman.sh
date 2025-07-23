#!/bin/bash

# Start Log Analysis System with Podman
# Simple script to run the system using Podman instead of Docker

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

print_status() {
    echo -e "${1}${2}${NC}"
}

print_status $BLUE "🚀 Starting Log Analysis System with Podman..."

# Check Podman
if ! command -v podman &> /dev/null; then
    print_status $RED "❌ Podman not found. Please install Podman first."
    exit 1
fi

print_status $GREEN "✓ Podman found: $(podman --version)"

# Check for compose tool
if command -v podman-compose &> /dev/null; then
    COMPOSE_CMD="podman-compose"
    print_status $GREEN "✓ Using podman-compose"
elif command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker-compose"
    print_status $YELLOW "⚠ Using docker-compose with Podman"
    # Set up Podman socket for docker-compose compatibility
    export DOCKER_HOST="unix:///run/user/$(id -u)/podman/podman.sock"
    systemctl --user start podman.socket 2>/dev/null || true
else
    print_status $RED "❌ No compose tool found. Install podman-compose:"
    echo "pip3 install podman-compose"
    exit 1
fi

# Create logs directory
mkdir -p logs

# Stop any existing containers
print_status $YELLOW "🛑 Stopping existing containers..."
$COMPOSE_CMD -f podman-compose.yml down 2>/dev/null || true

# Start services
print_status $YELLOW "🔄 Starting services..."
$COMPOSE_CMD -f podman-compose.yml up -d

# Wait for services
print_status $YELLOW "⏳ Waiting for services to start..."

# Wait for backend
for i in {1..30}; do
    if curl -f -s http://localhost:8080/api/v1/health > /dev/null 2>&1; then
        print_status $GREEN "✓ Backend is ready!"
        break
    fi
    echo -n "."
    sleep 2
done

# Wait for frontend
for i in {1..30}; do
    if curl -f -s http://localhost:3000 > /dev/null 2>&1; then
        print_status $GREEN "✓ Frontend is ready!"
        break
    fi
    echo -n "."
    sleep 2
done

echo
print_status $GREEN "🎉 Log Analysis System is running!"
echo
print_status $BLUE "Access the application:"
print_status $YELLOW "  🌐 Frontend:     http://localhost:3000"
print_status $YELLOW "  🔧 Backend API:  http://localhost:8080/api/v1"
print_status $YELLOW "  ❤️  Health:      http://localhost:8080/api/v1/health"
echo
print_status $BLUE "To stop: $COMPOSE_CMD -f podman-compose.yml down"
print_status $BLUE "To view logs: $COMPOSE_CMD -f podman-compose.yml logs -f"
