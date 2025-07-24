#!/bin/bash

# Log Analysis System - Local Development Startup Script
# This script helps start the system locally without Docker

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Java is installed
check_java() {
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
        JAVA_MAJOR=$(echo $JAVA_VERSION | cut -d'.' -f1)
        if [ "$JAVA_MAJOR" -ge 17 ]; then
            print_success "Java is installed: $JAVA_VERSION"
            return 0
        else
            print_error "Java 17+ is required. Found: $JAVA_VERSION"
            return 1
        fi
    else
        print_error "Java is not installed. Please install Java 17+ first."
        echo "Download from: https://openjdk.org/ or https://www.oracle.com/java/technologies/downloads/"
        return 1
    fi
}

# Check if Node.js is installed
check_node() {
    if command -v node &> /dev/null; then
        NODE_VERSION=$(node --version | sed 's/v//')
        NODE_MAJOR=$(echo $NODE_VERSION | cut -d'.' -f1)
        if [ "$NODE_MAJOR" -ge 18 ]; then
            print_success "Node.js is installed: v$NODE_VERSION"
            return 0
        else
            print_error "Node.js 18+ is required. Found: v$NODE_VERSION"
            return 1
        fi
    else
        print_error "Node.js is not installed. Please install Node.js 18+ first."
        echo "Download from: https://nodejs.org/"
        return 1
    fi
}

# Check if ports are available
check_ports() {
    print_status "Checking if ports 8080 and 3001 are available..."

    if lsof -i :8080 &> /dev/null; then
        print_error "Port 8080 is already in use. Please stop the process using this port."
        lsof -i :8080
        return 1
    fi

    if lsof -i :3001 &> /dev/null; then
        print_error "Port 3001 is already in use. Please stop the process using this port."
        lsof -i :3001
        return 1
    fi

    print_success "Ports 8080 and 3001 are available"
    return 0
}

# Install backend dependencies
setup_backend() {
    print_status "Setting up backend..."
    
    cd backend
    
    # Make Maven wrapper executable
    chmod +x mvnw 2>/dev/null || true
    
    # Install dependencies
    print_status "Installing backend dependencies..."
    ./mvnw clean install -DskipTests
    
    print_success "Backend setup completed"
    cd ..
}

# Install frontend dependencies
setup_frontend() {
    print_status "Setting up frontend..."
    
    cd frontend
    
    # Install dependencies
    print_status "Installing frontend dependencies..."
    npm install --legacy-peer-deps
    
    print_success "Frontend setup completed"
    cd ..
}

# Start backend
start_backend() {
    print_status "Starting backend server..."
    
    cd backend
    
    # Start backend in background
    nohup ./mvnw spring-boot:run > ../logs/backend.log 2>&1 &
    BACKEND_PID=$!
    echo $BACKEND_PID > ../logs/backend.pid
    
    print_success "Backend started with PID: $BACKEND_PID"
    print_status "Backend logs: logs/backend.log"
    
    cd ..
    
    # Wait for backend to start
    print_status "Waiting for backend to start..."
    for i in {1..30}; do
        if curl -f http://localhost:8080/actuator/health &> /dev/null; then
            print_success "Backend is ready!"
            return 0
        fi
        sleep 2
        echo -n "."
    done
    
    print_error "Backend failed to start within 60 seconds"
    return 1
}

# Start frontend
start_frontend() {
    print_status "Starting frontend server..."
    
    cd frontend
    
    # Start frontend in background
    nohup npm start > ../logs/frontend.log 2>&1 &
    FRONTEND_PID=$!
    echo $FRONTEND_PID > ../logs/frontend.pid
    
    print_success "Frontend started with PID: $FRONTEND_PID"
    print_status "Frontend logs: logs/frontend.log"
    
    cd ..
    
    # Wait for frontend to start
    print_status "Waiting for frontend to start..."
    for i in {1..30}; do
        if curl -f http://localhost:3001 &> /dev/null; then
            print_success "Frontend is ready!"
            return 0
        fi
        sleep 2
        echo -n "."
    done
    
    print_error "Frontend failed to start within 60 seconds"
    return 1
}

# Stop services
stop_services() {
    print_status "Stopping services..."
    
    # Stop backend
    if [ -f logs/backend.pid ]; then
        BACKEND_PID=$(cat logs/backend.pid)
        if kill -0 $BACKEND_PID 2>/dev/null; then
            kill $BACKEND_PID
            print_success "Backend stopped"
        fi
        rm -f logs/backend.pid
    fi
    
    # Stop frontend
    if [ -f logs/frontend.pid ]; then
        FRONTEND_PID=$(cat logs/frontend.pid)
        if kill -0 $FRONTEND_PID 2>/dev/null; then
            kill $FRONTEND_PID
            print_success "Frontend stopped"
        fi
        rm -f logs/frontend.pid
    fi
    
    # Kill any remaining processes on ports 8080 and 3001
    lsof -ti:8080 | xargs kill -9 2>/dev/null || true
    lsof -ti:3001 | xargs kill -9 2>/dev/null || true
    
    print_success "All services stopped"
}

# Show service status
show_status() {
    print_status "Checking service status..."
    
    # Check backend
    if curl -f http://localhost:8080/actuator/health &> /dev/null; then
        print_success "Backend is running on http://localhost:8080"
    else
        print_warning "Backend is not responding"
    fi
    
    # Check frontend
    if curl -f http://localhost:3001 &> /dev/null; then
        print_success "Frontend is running on http://localhost:3001"
    else
        print_warning "Frontend is not responding"
    fi
}

# Show logs
show_logs() {
    SERVICE=${1:-}
    if [ -z "$SERVICE" ]; then
        echo "=== Backend Logs ==="
        tail -n 20 logs/backend.log 2>/dev/null || echo "No backend logs found"
        echo ""
        echo "=== Frontend Logs ==="
        tail -n 20 logs/frontend.log 2>/dev/null || echo "No frontend logs found"
    elif [ "$SERVICE" = "backend" ]; then
        tail -f logs/backend.log
    elif [ "$SERVICE" = "frontend" ]; then
        tail -f logs/frontend.log
    else
        print_error "Unknown service: $SERVICE. Use 'backend' or 'frontend'"
    fi
}

# Create logs directory
mkdir -p logs

# Main menu
show_menu() {
    echo ""
    echo "🚀 Log Analysis System - Local Development"
    echo "=========================================="
    echo ""
    echo "1) Start System (Setup + Start)"
    echo "2) Start Services Only"
    echo "3) Stop Services"
    echo "4) Show Status"
    echo "5) Show Logs"
    echo "6) Setup Only (Install Dependencies)"
    echo "7) Check Prerequisites"
    echo "8) Exit"
    echo ""
}

# Handle menu selection
handle_menu() {
    read -p "Select an option [1-8]: " choice
    case $choice in
        1)
            if check_java && check_node && check_ports; then
                setup_backend
                setup_frontend
                start_backend
                start_frontend
                echo ""
                print_success "🎉 System is ready!"
                echo ""
                echo "🌐 Frontend: http://localhost:3001"
                echo "🔧 Backend API: http://localhost:8080/api/v1"
                echo "❤️ Health Check: http://localhost:8080/actuator/health"
                echo ""
                echo "Use option 3 to stop services when done."
            fi
            ;;
        2)
            if check_java && check_node && check_ports; then
                start_backend
                start_frontend
                echo ""
                print_success "🎉 Services started!"
                echo ""
                echo "🌐 Frontend: http://localhost:3001"
                echo "🔧 Backend API: http://localhost:8080/api/v1"
            fi
            ;;
        3)
            stop_services
            ;;
        4)
            show_status
            ;;
        5)
            read -p "Show logs for (all/backend/frontend): " service
            show_logs "$service"
            ;;
        6)
            if check_java && check_node; then
                setup_backend
                setup_frontend
                print_success "Setup completed!"
            fi
            ;;
        7)
            check_java
            check_node
            check_ports
            ;;
        8)
            print_success "Goodbye!"
            exit 0
            ;;
        *)
            print_error "Invalid option. Please try again."
            ;;
    esac
}

# Main execution
main() {
    # Check if running with arguments
    if [ $# -eq 0 ]; then
        # Interactive mode
        while true; do
            show_menu
            handle_menu
            echo ""
            read -p "Press Enter to continue..."
        done
    else
        # Command line mode
        case $1 in
            start)
                if check_java && check_node && check_ports; then
                    setup_backend
                    setup_frontend
                    start_backend
                    start_frontend
                    print_success "System started successfully!"
                fi
                ;;
            stop)
                stop_services
                ;;
            status)
                show_status
                ;;
            logs)
                show_logs "$2"
                ;;
            setup)
                if check_java && check_node; then
                    setup_backend
                    setup_frontend
                fi
                ;;
            check)
                check_java
                check_node
                check_ports
                ;;
            *)
                echo "Usage: $0 [start|stop|status|logs|setup|check]"
                exit 1
                ;;
        esac
    fi
}

# Run main function
main "$@"
