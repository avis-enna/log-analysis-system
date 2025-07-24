#!/bin/bash

# =============================================================================
# Development Tools for Log Analysis System
# Provides hot reload, debugging, and development utilities
# =============================================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuration
PROJECT_NAME="log-analyzer"

# Print functions
print_status() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_header() { echo -e "${PURPLE}=== $1 ===${NC}"; }

# Hot reload development mode
dev_mode() {
    print_header "STARTING DEVELOPMENT MODE"
    
    # Stop production containers
    print_status "Stopping production containers..."
    ./deploy-podman.sh stop 2>/dev/null || true
    
    # Start databases only
    print_status "Starting development databases..."
    podman run -d \
        --name "${PROJECT_NAME}-postgres-dev" \
        --network "${PROJECT_NAME}-network" \
        -p "5432:5432" \
        -e POSTGRES_DB="loganalyzer" \
        -e POSTGRES_USER="loguser" \
        -e POSTGRES_PASSWORD="logpass123" \
        postgres:15-alpine
    
    podman run -d \
        --name "${PROJECT_NAME}-redis-dev" \
        --network "${PROJECT_NAME}-network" \
        -p "6379:6379" \
        redis:7-alpine
    
    # Wait for databases
    print_status "Waiting for databases..."
    sleep 5
    
    print_success "Development mode ready!"
    echo ""
    echo -e "${CYAN}🔧 Development Setup:${NC}"
    echo -e "  Database: ${YELLOW}localhost:5432${NC} (loganalyzer/loguser/logpass123)"
    echo -e "  Redis: ${YELLOW}localhost:6379${NC}"
    echo ""
    echo -e "${CYAN}🚀 Run your applications:${NC}"
    echo -e "  Backend: ${YELLOW}cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev${NC}"
    echo -e "  Frontend: ${YELLOW}cd frontend && npm start${NC}"
}

# Debug mode with remote debugging
debug_mode() {
    print_header "STARTING DEBUG MODE"
    
    # Build debug version of backend
    cd backend
    cat > Dockerfile.debug << 'EOF'
FROM maven:3.9-openjdk-17-slim AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM openjdk:17-jre-slim
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080 5005
ENV JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
EOF
    
    print_status "Building debug image..."
    podman build -f Dockerfile.debug -t "${PROJECT_NAME}-backend-debug" .
    cd ..
    
    # Start debug container
    print_status "Starting debug container..."
    podman run -d \
        --name "${PROJECT_NAME}-backend-debug" \
        --network "${PROJECT_NAME}-network" \
        -p "8080:8080" \
        -p "5005:5005" \
        -e SPRING_PROFILES_ACTIVE=docker \
        -e SPRING_DATASOURCE_URL="jdbc:postgresql://${PROJECT_NAME}-postgres:5432/loganalyzer" \
        -e SPRING_DATASOURCE_USERNAME="loguser" \
        -e SPRING_DATASOURCE_PASSWORD="logpass123" \
        "${PROJECT_NAME}-backend-debug"
    
    print_success "Debug mode started!"
    echo ""
    echo -e "${CYAN}🐛 Debug Configuration:${NC}"
    echo -e "  Application: ${YELLOW}http://localhost:8080${NC}"
    echo -e "  Debug Port: ${YELLOW}localhost:5005${NC}"
    echo -e "  IDE Setup: Connect remote debugger to localhost:5005"
}

# Live logs with filtering
live_logs() {
    print_header "LIVE LOGS VIEWER"
    
    local service="${1:-backend}"
    local filter="${2:-}"
    
    print_status "Showing live logs for: $service"
    if [[ -n "$filter" ]]; then
        print_status "Filtering for: $filter"
        podman logs -f "${PROJECT_NAME}-${service}" | grep --color=always "$filter"
    else
        podman logs -f "${PROJECT_NAME}-${service}"
    fi
}

# Execute commands in containers
exec_container() {
    local service="$1"
    shift
    local cmd="$*"
    
    if [[ -z "$service" || -z "$cmd" ]]; then
        print_error "Usage: exec_container <service> <command>"
        echo "Services: backend, frontend, postgres, redis"
        return 1
    fi
    
    print_status "Executing in ${service}: $cmd"
    podman exec -it "${PROJECT_NAME}-${service}" $cmd
}

# Database shell access
db_shell() {
    print_header "DATABASE SHELL ACCESS"
    
    local db_type="${1:-postgres}"
    
    case "$db_type" in
        "postgres"|"pg")
            print_status "Connecting to PostgreSQL..."
            podman exec -it "${PROJECT_NAME}-postgres" psql -U loguser -d loganalyzer
            ;;
        "redis")
            print_status "Connecting to Redis..."
            podman exec -it "${PROJECT_NAME}-redis" redis-cli
            ;;
        *)
            print_error "Unknown database type: $db_type"
            echo "Available: postgres, redis"
            return 1
            ;;
    esac
}

# Hot reload frontend
frontend_dev() {
    print_header "FRONTEND DEVELOPMENT MODE"
    
    # Stop frontend container
    podman stop "${PROJECT_NAME}-frontend" 2>/dev/null || true
    podman rm "${PROJECT_NAME}-frontend" 2>/dev/null || true
    
    print_status "Starting frontend in development mode..."
    cd frontend
    
    # Create development Dockerfile
    cat > Dockerfile.dev << 'EOF'
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
EXPOSE 3000
CMD ["npm", "start"]
EOF
    
    podman build -f Dockerfile.dev -t "${PROJECT_NAME}-frontend-dev" .
    podman run -d \
        --name "${PROJECT_NAME}-frontend-dev" \
        --network "${PROJECT_NAME}-network" \
        -p "3000:3000" \
        -v "$(pwd)/src:/app/src:Z" \
        -e REACT_APP_API_URL="http://localhost:8080" \
        "${PROJECT_NAME}-frontend-dev"
    
    cd ..
    print_success "Frontend development mode started with hot reload!"
}

# Performance profiling
profile_app() {
    print_header "APPLICATION PROFILING"
    
    local service="${1:-backend}"
    local duration="${2:-60}"
    
    print_status "Profiling $service for ${duration} seconds..."
    
    case "$service" in
        "backend")
            # Java profiling
            podman exec "${PROJECT_NAME}-backend" jcmd 1 JFR.start duration=${duration}s filename=/tmp/profile.jfr
            sleep "$duration"
            podman cp "${PROJECT_NAME}-backend:/tmp/profile.jfr" "./profile-$(date +%Y%m%d-%H%M%S).jfr"
            print_success "Profile saved to profile-$(date +%Y%m%d-%H%M%S).jfr"
            ;;
        *)
            print_error "Profiling not supported for: $service"
            ;;
    esac
}

# Main command handler
case "${1:-help}" in
    "dev"|"development")
        dev_mode
        ;;
    "debug")
        debug_mode
        ;;
    "logs")
        live_logs "${2:-backend}" "$3"
        ;;
    "exec")
        exec_container "$2" "${@:3}"
        ;;
    "db"|"database")
        db_shell "$2"
        ;;
    "frontend-dev")
        frontend_dev
        ;;
    "profile")
        profile_app "$2" "$3"
        ;;
    "help"|"-h"|"--help")
        echo "Development Tools for Log Analysis System"
        echo ""
        echo "Usage: $0 [command] [options]"
        echo ""
        echo "Commands:"
        echo "  dev              - Start development mode (databases only)"
        echo "  debug            - Start debug mode with remote debugging"
        echo "  logs [service]   - Show live logs (default: backend)"
        echo "  exec <service> <cmd> - Execute command in container"
        echo "  db [postgres|redis] - Access database shell"
        echo "  frontend-dev     - Start frontend with hot reload"
        echo "  profile [service] [duration] - Profile application"
        echo "  help             - Show this help"
        echo ""
        echo "Examples:"
        echo "  $0 dev                    # Start development databases"
        echo "  $0 debug                  # Start with remote debugging"
        echo "  $0 logs backend ERROR     # Show backend logs filtered for ERROR"
        echo "  $0 exec backend bash      # Open bash in backend container"
        echo "  $0 db postgres            # Connect to PostgreSQL"
        echo "  $0 profile backend 120    # Profile backend for 2 minutes"
        ;;
    *)
        print_error "Unknown command: $1"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac
