#!/bin/bash

# Log Analysis System Deployment Script
# This script helps deploy the complete log analysis system

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

# Check if Docker is installed
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    
    print_success "Docker and Docker Compose are installed"
}

# Check if Java is installed (for local development)
check_java() {
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
        print_success "Java is installed: $JAVA_VERSION"
    else
        print_warning "Java is not installed. Required for local backend development."
    fi
}

# Check if Node.js is installed (for local development)
check_node() {
    if command -v node &> /dev/null; then
        NODE_VERSION=$(node --version)
        print_success "Node.js is installed: $NODE_VERSION"
    else
        print_warning "Node.js is not installed. Required for local frontend development."
    fi
}

# Create necessary directories
create_directories() {
    print_status "Creating necessary directories..."
    
    mkdir -p logs
    mkdir -p data/postgres
    mkdir -p data/elasticsearch
    mkdir -p data/redis
    mkdir -p data/influxdb
    
    print_success "Directories created"
}

# Generate environment file
generate_env() {
    print_status "Generating environment configuration..."
    
    cat > .env << EOF
# Log Analysis System Environment Configuration

# Application
APP_NAME=Log Analysis System
APP_VERSION=1.0.0
APP_ENV=production

# Database Configuration
DATABASE_URL=jdbc:postgresql://postgres:5432/loganalyzer
DATABASE_USERNAME=loganalyzer
DATABASE_PASSWORD=loganalyzer123

# Elasticsearch Configuration
ELASTICSEARCH_HOST=elasticsearch
ELASTICSEARCH_PORT=9200

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Redis Configuration
REDIS_HOST=redis
REDIS_PORT=6379

# InfluxDB Configuration
INFLUXDB_URL=http://influxdb:8086
INFLUXDB_DATABASE=loganalyzer
INFLUXDB_USERNAME=admin
INFLUXDB_PASSWORD=admin123

# Security
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production
SESSION_TIMEOUT=3600

# Frontend Configuration
REACT_APP_API_URL=http://localhost:8080/api/v1
REACT_APP_WS_URL=ws://localhost:8080/ws
REACT_APP_VERSION=1.0.0

# Logging
LOG_LEVEL=INFO
LOG_FILE=logs/application.log
EOF
    
    print_success "Environment file generated (.env)"
}

# Deploy with Docker Compose
deploy_docker() {
    print_status "Deploying with Docker Compose..."
    
    # Stop any existing containers
    docker-compose -f docker-compose.simple.yml down
    
    # Pull latest images
    print_status "Pulling latest images..."
    docker-compose -f docker-compose.simple.yml pull
    
    # Build and start services
    print_status "Building and starting services..."
    docker-compose -f docker-compose.simple.yml up -d --build
    
    print_success "Services started successfully!"
    
    # Wait for services to be ready
    print_status "Waiting for services to be ready..."
    sleep 30
    
    # Check service health
    check_services_health
}

# Check service health
check_services_health() {
    print_status "Checking service health..."
    
    # Check backend health
    if curl -f http://localhost:8080/actuator/health &> /dev/null; then
        print_success "Backend service is healthy"
    else
        print_warning "Backend service is not responding"
    fi
    
    # Check frontend
    if curl -f http://localhost:3000 &> /dev/null; then
        print_success "Frontend service is healthy"
    else
        print_warning "Frontend service is not responding"
    fi
    
    # Check database
    if docker-compose -f docker-compose.simple.yml exec -T postgres pg_isready -U loganalyzer &> /dev/null; then
        print_success "Database is healthy"
    else
        print_warning "Database is not responding"
    fi
    
    # Check Elasticsearch
    if curl -f http://localhost:9200/_cluster/health &> /dev/null; then
        print_success "Elasticsearch is healthy"
    else
        print_warning "Elasticsearch is not responding"
    fi
}

# Show service URLs
show_urls() {
    print_success "Deployment completed! Access your services:"
    echo ""
    echo "🌐 Frontend Application: http://localhost:3000"
    echo "🔧 Backend API: http://localhost:8080/api/v1"
    echo "❤️  Health Check: http://localhost:8080/actuator/health"
    echo "📊 Elasticsearch: http://localhost:9200"
    echo "🗄️  Database: localhost:5432 (user: loganalyzer, db: loganalyzer)"
    echo "🚀 Redis: localhost:6379"
    echo "📈 InfluxDB: http://localhost:8086"
    echo ""
    echo "📚 Documentation: See COMPLETE_SYSTEM_GUIDE.md"
    echo ""
}

# Stop services
stop_services() {
    print_status "Stopping all services..."
    docker-compose -f docker-compose.simple.yml down
    print_success "All services stopped"
}

# Clean up everything
cleanup() {
    print_status "Cleaning up all data and containers..."
    docker-compose -f docker-compose.simple.yml down -v
    docker system prune -f
    print_success "Cleanup completed"
}

# Show logs
show_logs() {
    SERVICE=${1:-}
    if [ -z "$SERVICE" ]; then
        docker-compose -f docker-compose.simple.yml logs -f
    else
        docker-compose -f docker-compose.simple.yml logs -f "$SERVICE"
    fi
}

# Main menu
show_menu() {
    echo ""
    echo "🚀 Log Analysis System Deployment Script"
    echo "========================================"
    echo ""
    echo "1) Deploy System (Docker)"
    echo "2) Stop Services"
    echo "3) Show Service Status"
    echo "4) Show Logs (all services)"
    echo "5) Show Logs (specific service)"
    echo "6) Cleanup Everything"
    echo "7) Check Prerequisites"
    echo "8) Generate Environment File"
    echo "9) Exit"
    echo ""
}

# Handle menu selection
handle_menu() {
    read -p "Select an option [1-9]: " choice
    case $choice in
        1)
            check_docker
            create_directories
            generate_env
            deploy_docker
            show_urls
            ;;
        2)
            stop_services
            ;;
        3)
            check_services_health
            ;;
        4)
            show_logs
            ;;
        5)
            read -p "Enter service name (frontend, backend, postgres, elasticsearch, kafka, redis, influxdb): " service
            show_logs "$service"
            ;;
        6)
            read -p "Are you sure you want to cleanup everything? (y/N): " confirm
            if [[ $confirm == [yY] || $confirm == [yY][eE][sS] ]]; then
                cleanup
            fi
            ;;
        7)
            check_docker
            check_java
            check_node
            ;;
        8)
            generate_env
            ;;
        9)
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
            deploy)
                check_docker
                create_directories
                generate_env
                deploy_docker
                show_urls
                ;;
            stop)
                stop_services
                ;;
            status)
                check_services_health
                ;;
            logs)
                show_logs "$2"
                ;;
            cleanup)
                cleanup
                ;;
            check)
                check_docker
                check_java
                check_node
                ;;
            *)
                echo "Usage: $0 [deploy|stop|status|logs|cleanup|check]"
                exit 1
                ;;
        esac
    fi
}

# Run main function
main "$@"
