#!/bin/bash

# Run Log Analysis System with Podman
# This script uses Podman instead of Docker to run the complete system

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

print_header() {
    echo
    print_status $BLUE "=============================================="
    print_status $BLUE "$1"
    print_status $BLUE "=============================================="
    echo
}

print_header "LOG ANALYSIS SYSTEM - PODMAN DEPLOYMENT"

# Check if Podman is installed
if ! command -v podman &> /dev/null; then
    print_status $RED "❌ Podman is not installed"
    print_status $YELLOW "Please install Podman first:"
    echo "  - Fedora/RHEL: sudo dnf install podman"
    echo "  - Ubuntu/Debian: sudo apt install podman"
    echo "  - macOS: brew install podman"
    exit 1
fi

print_status $GREEN "✓ Podman found: $(podman --version)"

# Check if podman-compose is available
if command -v podman-compose &> /dev/null; then
    print_status $GREEN "✓ Using podman-compose"
    COMPOSE_CMD="podman-compose"
elif command -v docker-compose &> /dev/null; then
    print_status $YELLOW "⚠ Using docker-compose with Podman"
    COMPOSE_CMD="docker-compose"
    export DOCKER_HOST="unix:///run/user/$(id -u)/podman/podman.sock"
else
    print_status $RED "❌ Neither podman-compose nor docker-compose found"
    print_status $YELLOW "Please install podman-compose:"
    echo "  - pip3 install podman-compose"
    echo "  - OR: sudo dnf install podman-compose"
    exit 1
fi

# Start Podman socket if needed
if [ "$COMPOSE_CMD" = "docker-compose" ]; then
    print_status $YELLOW "Starting Podman socket for docker-compose compatibility..."
    systemctl --user start podman.socket 2>/dev/null || true
fi

# Create necessary directories
mkdir -p logs

print_status $YELLOW "Starting Log Analysis System with Podman..."

# Use the Podman-specific compose file
if [ -f "podman-compose.yml" ]; then
    COMPOSE_FILE="podman-compose.yml"
else
    COMPOSE_FILE="docker-compose.yml"
fi

print_status $BLUE "Using compose file: $COMPOSE_FILE"

# Stop any existing containers
print_status $YELLOW "Stopping any existing containers..."
$COMPOSE_CMD -f $COMPOSE_FILE down 2>/dev/null || true

# Start the services
print_status $YELLOW "Starting services..."
$COMPOSE_CMD -f $COMPOSE_FILE up -d

# Wait for services to be ready
print_status $YELLOW "Waiting for services to start..."

# Function to wait for service
wait_for_service() {
    local service_name=$1
    local url=$2
    local max_attempts=30
    local attempt=1
    
    print_status $YELLOW "Waiting for $service_name..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -f -s "$url" > /dev/null 2>&1; then
            print_status $GREEN "✓ $service_name is ready!"
            return 0
        fi
        
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    print_status $RED "✗ $service_name failed to start within timeout"
    return 1
}

# Wait for backend
wait_for_service "Backend API" "http://localhost:8080/api/v1/health"

# Wait for frontend
wait_for_service "Frontend" "http://localhost:3000"

print_header "DEPLOYMENT SUCCESSFUL!"

print_status $GREEN "🎉 Log Analysis System is now running!"
echo
print_status $BLUE "Access the application:"
print_status $YELLOW "  🌐 Frontend:     http://localhost:3000"
print_status $YELLOW "  🔧 Backend API:  http://localhost:8080/api/v1"
print_status $YELLOW "  ❤️  Health Check: http://localhost:8080/api/v1/health"
print_status $YELLOW "  🗄️  Database:     http://localhost:8080/h2-console (if H2 is used)"
echo
print_status $BLUE "Service Status:"
$COMPOSE_CMD -f $COMPOSE_FILE ps
echo
print_status $BLUE "To stop the system:"
print_status $YELLOW "  $COMPOSE_CMD -f $COMPOSE_FILE down"
echo
print_status $BLUE "To view logs:"
print_status $YELLOW "  $COMPOSE_CMD -f $COMPOSE_FILE logs -f [service_name]"
echo
print_status $GREEN "Happy analyzing! 🚀"
