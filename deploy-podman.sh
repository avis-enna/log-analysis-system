#!/bin/bash

# =============================================================================
# Log Analysis System - Podman Deployment Script
# Solves compilation errors and local environment issues
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuration
PROJECT_NAME="log-analyzer"
NETWORK_NAME="${PROJECT_NAME}-network"
BACKEND_IMAGE="${PROJECT_NAME}-backend"
FRONTEND_IMAGE="${PROJECT_NAME}-frontend"

# Ports
BACKEND_PORT=8080
FRONTEND_PORT=3000
POSTGRES_PORT=5432
REDIS_PORT=6379

# Database config
DB_NAME="loganalyzer"
DB_USER="loguser"
DB_PASSWORD="logpass123"

# Print functions
print_status() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_header() { echo -e "${PURPLE}=== $1 ===${NC}"; }

# Check if podman is installed
check_podman() {
    if ! command -v podman &> /dev/null; then
        print_error "Podman not installed. Install with:"
        echo "  Ubuntu/Debian: sudo apt-get install podman"
        echo "  RHEL/CentOS: sudo dnf install podman"
        echo "  macOS: brew install podman"
        exit 1
    fi
    print_success "Podman found: $(podman --version)"
}

# Cleanup existing containers
cleanup() {
    print_header "CLEANING UP"

    containers=("${PROJECT_NAME}-backend" "${PROJECT_NAME}-frontend" "${PROJECT_NAME}-postgres" "${PROJECT_NAME}-redis")
    for container in "${containers[@]}"; do
        if podman ps -a --format "{{.Names}}" | grep -q "^${container}$"; then
            print_status "Removing container: $container"
            podman stop "$container" 2>/dev/null || true
            podman rm "$container" 2>/dev/null || true
        fi
    done

    if podman network ls --format "{{.Name}}" | grep -q "^${NETWORK_NAME}$"; then
        print_status "Removing network: $NETWORK_NAME"
        podman network rm "$NETWORK_NAME" 2>/dev/null || true
    fi

    print_success "Cleanup completed"
}

# Create network
create_network() {
    print_header "CREATING NETWORK"
    if ! podman network ls --format "{{.Name}}" | grep -q "^${NETWORK_NAME}$"; then
        podman network create "$NETWORK_NAME"
        print_success "Network created: $NETWORK_NAME"
    else
        print_status "Network exists: $NETWORK_NAME"
    fi
}

# Build backend with fixed Dockerfile
build_backend() {
    print_header "BUILDING BACKEND"
    cd backend

    # Create optimized Dockerfile
    cat > Dockerfile.podman << 'EOF'
FROM maven:3.9-openjdk-17-slim AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM openjdk:17-jre-slim
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
RUN groupadd -r appuser && useradd -r -g appuser appuser
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN mkdir -p logs && chown -R appuser:appuser /app
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
ENV JAVA_OPTS="-Xms256m -Xmx1g -XX:+UseContainerSupport"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
EOF

    print_status "Building backend image..."
    podman build -f Dockerfile.podman -t "$BACKEND_IMAGE" .
    cd ..
    print_success "Backend image built"
}

# Build frontend with fixed Dockerfile
build_frontend() {
    print_header "BUILDING FRONTEND"
    cd frontend

    cat > Dockerfile.podman << 'EOF'
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production --silent
COPY . .
RUN npm run build

FROM nginx:alpine
RUN apk add --no-cache curl
COPY --from=builder /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:3000 || exit 1
CMD ["nginx", "-g", "daemon off;"]
EOF

    print_status "Building frontend image..."
    podman build -f Dockerfile.podman -t "$FRONTEND_IMAGE" .
    cd ..
    print_success "Frontend image built"
}
# Start database services
start_database() {
    print_header "STARTING DATABASE SERVICES"

    # PostgreSQL
    print_status "Starting PostgreSQL..."
    podman run -d \
        --name "${PROJECT_NAME}-postgres" \
        --network "$NETWORK_NAME" \
        -p "${POSTGRES_PORT}:5432" \
        -e POSTGRES_DB="$DB_NAME" \
        -e POSTGRES_USER="$DB_USER" \
        -e POSTGRES_PASSWORD="$DB_PASSWORD" \
        -v "${PROJECT_NAME}-postgres-data:/var/lib/postgresql/data" \
        postgres:15-alpine

    # Redis
    print_status "Starting Redis..."
    podman run -d \
        --name "${PROJECT_NAME}-redis" \
        --network "$NETWORK_NAME" \
        -p "${REDIS_PORT}:6379" \
        -v "${PROJECT_NAME}-redis-data:/data" \
        redis:7-alpine redis-server --appendonly yes

    print_success "Database services started"
}

# Wait for services to be ready
wait_for_services() {
    print_header "WAITING FOR SERVICES"

    # Wait for PostgreSQL
    print_status "Waiting for PostgreSQL..."
    for i in {1..30}; do
        if podman exec "${PROJECT_NAME}-postgres" pg_isready -U "$DB_USER" -d "$DB_NAME" &>/dev/null; then
            print_success "PostgreSQL ready"
            break
        fi
        [ $i -eq 30 ] && { print_error "PostgreSQL timeout"; exit 1; }
        sleep 1
    done

    # Wait for Redis
    print_status "Waiting for Redis..."
    for i in {1..30}; do
        if podman exec "${PROJECT_NAME}-redis" redis-cli ping &>/dev/null; then
            print_success "Redis ready"
            break
        fi
        [ $i -eq 30 ] && { print_error "Redis timeout"; exit 1; }
        sleep 1
    done
}

# Start backend service
start_backend() {
    print_header "STARTING BACKEND"

    podman run -d \
        --name "${PROJECT_NAME}-backend" \
        --network "$NETWORK_NAME" \
        -p "${BACKEND_PORT}:8080" \
        -e SPRING_PROFILES_ACTIVE=docker \
        -e SPRING_DATASOURCE_URL="jdbc:postgresql://${PROJECT_NAME}-postgres:5432/${DB_NAME}" \
        -e SPRING_DATASOURCE_USERNAME="$DB_USER" \
        -e SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD" \
        -e SPRING_REDIS_HOST="${PROJECT_NAME}-redis" \
        -e SPRING_REDIS_PORT=6379 \
        -v "${PROJECT_NAME}-backend-logs:/app/logs" \
        "$BACKEND_IMAGE"

    print_success "Backend started"
}

# Start frontend service
start_frontend() {
    print_header "STARTING FRONTEND"

    podman run -d \
        --name "${PROJECT_NAME}-frontend" \
        --network "$NETWORK_NAME" \
        -p "${FRONTEND_PORT}:3000" \
        -e REACT_APP_API_URL="http://localhost:${BACKEND_PORT}" \
        "$FRONTEND_IMAGE"

    print_success "Frontend started"
}

# Wait for application to be ready
wait_for_app() {
    print_header "WAITING FOR APPLICATION"

    # Wait for backend
    print_status "Waiting for backend health check..."
    for i in {1..60}; do
        if curl -f "http://localhost:${BACKEND_PORT}/actuator/health" &>/dev/null; then
            print_success "Backend is healthy"
            break
        fi
        [ $i -eq 60 ] && { print_error "Backend health check timeout"; exit 1; }
        sleep 2
    done

    # Wait for frontend
    print_status "Waiting for frontend..."
    for i in {1..30}; do
        if curl -f "http://localhost:${FRONTEND_PORT}" &>/dev/null; then
            print_success "Frontend is ready"
            break
        fi
        [ $i -eq 30 ] && { print_error "Frontend timeout"; exit 1; }
        sleep 1
    done
}

# Show status and URLs
show_status() {
    print_header "DEPLOYMENT COMPLETE"

    echo -e "${GREEN}🎉 Log Analysis System is running!${NC}"
    echo ""
    echo -e "${CYAN}📊 Application URLs:${NC}"
    echo -e "  Frontend:  ${YELLOW}http://localhost:${FRONTEND_PORT}${NC}"
    echo -e "  Backend:   ${YELLOW}http://localhost:${BACKEND_PORT}${NC}"
    echo -e "  API Docs:  ${YELLOW}http://localhost:${BACKEND_PORT}/swagger-ui.html${NC}"
    echo -e "  Health:    ${YELLOW}http://localhost:${BACKEND_PORT}/actuator/health${NC}"
    echo ""
    echo -e "${CYAN}🗄️  Database URLs:${NC}"
    echo -e "  PostgreSQL: ${YELLOW}localhost:${POSTGRES_PORT}${NC} (db: ${DB_NAME}, user: ${DB_USER})"
    echo -e "  Redis:      ${YELLOW}localhost:${REDIS_PORT}${NC}"
    echo ""
    echo -e "${CYAN}🔧 Management Commands:${NC}"
    echo -e "  View logs:     ${YELLOW}podman logs ${PROJECT_NAME}-backend${NC}"
    echo -e "  Stop all:      ${YELLOW}./deploy-podman.sh stop${NC}"
    echo -e "  Restart:       ${YELLOW}./deploy-podman.sh restart${NC}"
    echo -e "  Clean all:     ${YELLOW}./deploy-podman.sh clean${NC}"
}

# Stop all containers
stop_all() {
    print_header "STOPPING ALL SERVICES"
    containers=("${PROJECT_NAME}-backend" "${PROJECT_NAME}-frontend" "${PROJECT_NAME}-postgres" "${PROJECT_NAME}-redis")
    for container in "${containers[@]}"; do
        if podman ps --format "{{.Names}}" | grep -q "^${container}$"; then
            print_status "Stopping $container"
            podman stop "$container"
        fi
    done
    print_success "All services stopped"
}

# Restart all services
restart_all() {
    stop_all
    sleep 2
    start_database
    wait_for_services
    start_backend
    start_frontend
    wait_for_app
    show_status
}

# Main deployment function
deploy() {
    print_header "STARTING DEPLOYMENT"
    check_podman
    cleanup
    create_network
    build_backend
    build_frontend
    start_database
    wait_for_services
    start_backend
    start_frontend
    wait_for_app
    show_status
}

# Main script logic
case "${1:-deploy}" in
    "deploy"|"start")
        deploy
        ;;
    "stop")
        stop_all
        ;;
    "restart")
        restart_all
        ;;
    "clean")
        cleanup
        ;;
    "logs")
        podman logs -f "${PROJECT_NAME}-backend"
        ;;
    "status")
        print_header "CONTAINER STATUS"
        podman ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep "$PROJECT_NAME"
        ;;
    "help"|"-h"|"--help")
        echo "Usage: $0 [command]"
        echo ""
        echo "Commands:"
        echo "  deploy/start  - Deploy the full application stack (default)"
        echo "  stop          - Stop all services"
        echo "  restart       - Restart all services"
        echo "  clean         - Clean up containers and networks"
        echo "  logs          - Follow backend logs"
        echo "  status        - Show container status"
        echo "  help          - Show this help message"
        ;;
    *)
        print_error "Unknown command: $1"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac

    print_status "Building backend image: $BACKEND_IMAGE"
    cd backend

    # Create optimized Dockerfile for local development
    cat > Dockerfile.local << 'EOF'
# Optimized Dockerfile for local development
FROM maven:3.9-openjdk-17-slim AS builder

# Set working directory
WORKDIR /app

# Copy Maven configuration
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application (skip tests for faster builds)
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM openjdk:17-jre-slim

# Install utilities
RUN apt-get update && \
    apt-get install -y curl netcat-traditional && \
    rm -rf /var/lib/apt/lists/*

# Create app user
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Copy JAR file
COPY --from=builder /app/target/*.jar app.jar

# Create directories
RUN mkdir -p logs data && chown -R appuser:appuser /app

# Switch to app user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options optimized for containers
ENV JAVA_OPTS="-Xms256m -Xmx1g -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
EOF

    podman build -f Dockerfile.local -t "$BACKEND_IMAGE" .
    cd ..

    print_success "Backend image built successfully"
}

# Function to build frontend image
build_frontend() {
    print_header "BUILDING FRONTEND IMAGE"

    print_status "Building frontend image: $FRONTEND_IMAGE"
    cd frontend

    # Create optimized Dockerfile for local development
    cat > Dockerfile.local << 'EOF'
# Optimized Dockerfile for local development
FROM node:18-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci --only=production --silent

# Copy source code
COPY . .

# Build application
RUN npm run build

# Runtime stage with nginx
FROM nginx:alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Copy built app
COPY --from=builder /app/build /usr/share/nginx/html

# Copy nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Create nginx user directories
RUN mkdir -p /var/cache/nginx/client_temp && \
    chown -R nginx:nginx /var/cache/nginx

EXPOSE 3000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:3000 || exit 1

CMD ["nginx", "-g", "daemon off;"]
EOF

    podman build -f Dockerfile.local -t "$FRONTEND_IMAGE" .
    cd ..

    print_success "Frontend image built successfully"
}

# Function to start database
start_database() {
    print_header "STARTING DATABASE SERVICES"

    # Start PostgreSQL
    print_status "Starting PostgreSQL container"
    podman run -d \
        --name "${PROJECT_NAME}-postgres" \
        --network "$NETWORK_NAME" \
        -p "${POSTGRES_PORT}:5432" \
        -e POSTGRES_DB="$DB_NAME" \
        -e POSTGRES_USER="$DB_USER" \
        -e POSTGRES_PASSWORD="$DB_PASSWORD" \
        -e POSTGRES_INITDB_ARGS="--encoding=UTF-8 --lc-collate=C --lc-ctype=C" \
        -v "${PROJECT_NAME}-postgres-data:/var/lib/postgresql/data" \
        "$POSTGRES_IMAGE"

    # Start Redis
    print_status "Starting Redis container"
    podman run -d \
        --name "${PROJECT_NAME}-redis" \
        --network "$NETWORK_NAME" \
        -p "${REDIS_PORT}:6379" \
        -v "${PROJECT_NAME}-redis-data:/data" \
        "$REDIS_IMAGE" \
        redis-server --appendonly yes

    print_success "Database services started"
}

# Function to wait for services
wait_for_services() {
    print_header "WAITING FOR SERVICES TO BE READY"

    # Wait for PostgreSQL
    print_status "Waiting for PostgreSQL to be ready..."
    for i in {1..30}; do
        if podman exec "${PROJECT_NAME}-postgres" pg_isready -U "$DB_USER" -d "$DB_NAME" &>/dev/null; then
            print_success "PostgreSQL is ready"
            break
        fi
        if [ $i -eq 30 ]; then
            print_error "PostgreSQL failed to start within 30 seconds"
            exit 1
        fi
        sleep 1
    done

    # Wait for Redis
    print_status "Waiting for Redis to be ready..."
    for i in {1..30}; do
        if podman exec "${PROJECT_NAME}-redis" redis-cli ping &>/dev/null; then
            print_success "Redis is ready"
            break
        fi
        if [ $i -eq 30 ]; then
            print_error "Redis failed to start within 30 seconds"
            exit 1
        fi
        sleep 1
    done
}
EOF
