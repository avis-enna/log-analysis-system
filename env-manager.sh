#!/bin/bash

# =============================================================================
# Multi-Environment Manager for Log Analysis System
# Manages dev, staging, and production environments
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
ENVIRONMENTS=("dev" "staging" "prod")

# Print functions
print_status() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_header() { echo -e "${PURPLE}=== $1 ===${NC}"; }

# Environment configurations
get_env_config() {
    local env="$1"
    
    case "$env" in
        "dev")
            BACKEND_PORT=8080
            FRONTEND_PORT=3000
            POSTGRES_PORT=5432
            REDIS_PORT=6379
            DB_NAME="loganalyzer_dev"
            DB_USER="devuser"
            DB_PASSWORD="devpass123"
            REPLICAS=1
            RESOURCES_LIMIT="512Mi"
            ;;
        "staging")
            BACKEND_PORT=8081
            FRONTEND_PORT=3001
            POSTGRES_PORT=5433
            REDIS_PORT=6380
            DB_NAME="loganalyzer_staging"
            DB_USER="staginguser"
            DB_PASSWORD="stagingpass456"
            REPLICAS=2
            RESOURCES_LIMIT="1Gi"
            ;;
        "prod")
            BACKEND_PORT=8082
            FRONTEND_PORT=3002
            POSTGRES_PORT=5434
            REDIS_PORT=6381
            DB_NAME="loganalyzer_prod"
            DB_USER="produser"
            DB_PASSWORD="$(generate_secure_password)"
            REPLICAS=3
            RESOURCES_LIMIT="2Gi"
            ;;
        *)
            print_error "Unknown environment: $env"
            return 1
            ;;
    esac
}

# Generate secure password
generate_secure_password() {
    openssl rand -base64 32 | tr -d "=+/" | cut -c1-25
}

# Create environment-specific configurations
create_env_configs() {
    print_header "CREATING ENVIRONMENT CONFIGURATIONS"
    
    mkdir -p environments/{dev,staging,prod}
    
    for env in "${ENVIRONMENTS[@]}"; do
        print_status "Creating configuration for $env environment"
        
        get_env_config "$env"
        
        # Create environment-specific docker-compose file
        cat > "environments/$env/docker-compose.yml" << EOF
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: ${PROJECT_NAME}-postgres-${env}
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "${POSTGRES_PORT}:5432"
    volumes:
      - ${PROJECT_NAME}-postgres-${env}-data:/var/lib/postgresql/data
    networks:
      - ${PROJECT_NAME}-${env}-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER} -d ${DB_NAME}"]
      interval: 30s
      timeout: 10s
      retries: 3

  redis:
    image: redis:7-alpine
    container_name: ${PROJECT_NAME}-redis-${env}
    ports:
      - "${REDIS_PORT}:6379"
    volumes:
      - ${PROJECT_NAME}-redis-${env}-data:/data
    networks:
      - ${PROJECT_NAME}-${env}-network
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  backend:
    build:
      context: ../../backend
      dockerfile: Dockerfile.podman
    container_name: ${PROJECT_NAME}-backend-${env}
    ports:
      - "${BACKEND_PORT}:8080"
    environment:
      SPRING_PROFILES_ACTIVE: ${env}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${DB_NAME}
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
    volumes:
      - ${PROJECT_NAME}-backend-${env}-logs:/app/logs
    networks:
      - ${PROJECT_NAME}-${env}-network
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    deploy:
      replicas: ${REPLICAS}
      resources:
        limits:
          memory: ${RESOURCES_LIMIT}

  frontend:
    build:
      context: ../../frontend
      dockerfile: Dockerfile.podman
    container_name: ${PROJECT_NAME}-frontend-${env}
    ports:
      - "${FRONTEND_PORT}:3000"
    environment:
      REACT_APP_API_URL: http://localhost:${BACKEND_PORT}
      REACT_APP_ENVIRONMENT: ${env}
    networks:
      - ${PROJECT_NAME}-${env}-network
    depends_on:
      backend:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3000"]
      interval: 30s
      timeout: 10s
      retries: 3

networks:
  ${PROJECT_NAME}-${env}-network:
    driver: bridge

volumes:
  ${PROJECT_NAME}-postgres-${env}-data:
    driver: local
  ${PROJECT_NAME}-redis-${env}-data:
    driver: local
  ${PROJECT_NAME}-backend-${env}-logs:
    driver: local
EOF
        
        # Create environment-specific application properties
        cat > "environments/$env/application-${env}.yml" << EOF
# ${env} environment configuration
server:
  port: 8080

spring:
  application:
    name: log-analyzer-${env}
  
  datasource:
    url: jdbc:postgresql://postgres:5432/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: $((REPLICAS * 10))
      minimum-idle: $((REPLICAS * 2))
  
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: $([ "$env" = "prod" ] && echo "validate" || echo "update")
    show-sql: $([ "$env" = "dev" ] && echo "true" || echo "false")
  
  data:
    redis:
      host: redis
      port: 6379

# Logging configuration
logging:
  level:
    com.loganalyzer: $([ "$env" = "dev" ] && echo "DEBUG" || echo "INFO")
    org.springframework.web: $([ "$env" = "dev" ] && echo "DEBUG" || echo "WARN")
  file:
    name: /app/logs/log-analyzer-${env}.log

# Environment-specific settings
app:
  environment: ${env}
  features:
    debug-mode: $([ "$env" = "dev" ] && echo "true" || echo "false")
    metrics-enabled: true
    security-enhanced: $([ "$env" = "prod" ] && echo "true" || echo "false")
EOF
        
        # Create deployment script for environment
        cat > "environments/$env/deploy.sh" << EOF
#!/bin/bash
# Deployment script for ${env} environment

set -e

echo "🚀 Deploying to ${env} environment..."

# Load environment configuration
export ENVIRONMENT=${env}
export BACKEND_PORT=${BACKEND_PORT}
export FRONTEND_PORT=${FRONTEND_PORT}

# Deploy using docker-compose
docker-compose -f docker-compose.yml up -d

echo "✅ ${env} environment deployed successfully!"
echo "Frontend: http://localhost:${FRONTEND_PORT}"
echo "Backend: http://localhost:${BACKEND_PORT}"
EOF
        
        chmod +x "environments/$env/deploy.sh"
        
        print_success "Configuration created for $env environment"
    done
}

# Deploy to specific environment
deploy_environment() {
    local env="$1"
    
    if [[ ! " ${ENVIRONMENTS[@]} " =~ " ${env} " ]]; then
        print_error "Invalid environment: $env"
        echo "Available environments: ${ENVIRONMENTS[*]}"
        return 1
    fi
    
    print_header "DEPLOYING TO ${env^^} ENVIRONMENT"
    
    if [[ ! -d "environments/$env" ]]; then
        print_error "Environment configuration not found for: $env"
        echo "Run: $0 init to create environment configurations"
        return 1
    fi
    
    cd "environments/$env"
    
    print_status "Starting deployment to $env..."
    
    # Check if environment is already running
    if docker-compose ps | grep -q "Up"; then
        print_warning "Environment $env is already running"
        read -p "Do you want to restart it? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker-compose down
        else
            print_status "Deployment cancelled"
            return 0
        fi
    fi
    
    # Deploy
    docker-compose up -d
    
    # Wait for health checks
    print_status "Waiting for services to be healthy..."
    sleep 30
    
    # Verify deployment
    get_env_config "$env"
    
    if curl -sf "http://localhost:${BACKEND_PORT}/actuator/health" >/dev/null; then
        print_success "Backend is healthy"
    else
        print_error "Backend health check failed"
    fi
    
    if curl -sf "http://localhost:${FRONTEND_PORT}" >/dev/null; then
        print_success "Frontend is healthy"
    else
        print_error "Frontend health check failed"
    fi
    
    cd - >/dev/null
    
    print_success "Deployment to $env completed!"
    echo ""
    echo -e "${CYAN}🌐 Access URLs:${NC}"
    echo -e "  Frontend:  ${YELLOW}http://localhost:${FRONTEND_PORT}${NC}"
    echo -e "  Backend:   ${YELLOW}http://localhost:${BACKEND_PORT}${NC}"
    echo -e "  API Docs:  ${YELLOW}http://localhost:${BACKEND_PORT}/swagger-ui.html${NC}"
}

# List environments and their status
list_environments() {
    print_header "ENVIRONMENT STATUS"
    
    echo -e "${CYAN}📊 Environment Overview:${NC}"
    printf "%-10s %-8s %-12s %-12s %-8s\n" "Environment" "Status" "Frontend" "Backend" "Health"
    echo "--------------------------------------------------------"
    
    for env in "${ENVIRONMENTS[@]}"; do
        if [[ -d "environments/$env" ]]; then
            cd "environments/$env"
            
            local status="Stopped"
            local frontend_status="❌"
            local backend_status="❌"
            local health_status="❌"
            
            if docker-compose ps | grep -q "Up"; then
                status="Running"
                
                get_env_config "$env"
                
                # Check frontend
                if curl -sf "http://localhost:${FRONTEND_PORT}" >/dev/null 2>&1; then
                    frontend_status="✅"
                fi
                
                # Check backend
                if curl -sf "http://localhost:${BACKEND_PORT}/actuator/health" >/dev/null 2>&1; then
                    backend_status="✅"
                    health_status="✅"
                fi
            fi
            
            printf "%-10s %-8s %-12s %-12s %-8s\n" "$env" "$status" "$frontend_status" "$backend_status" "$health_status"
            
            cd - >/dev/null
        else
            printf "%-10s %-8s %-12s %-12s %-8s\n" "$env" "Not Init" "❌" "❌" "❌"
        fi
    done
}

# Stop environment
stop_environment() {
    local env="$1"
    
    if [[ ! " ${ENVIRONMENTS[@]} " =~ " ${env} " ]]; then
        print_error "Invalid environment: $env"
        return 1
    fi
    
    print_header "STOPPING ${env^^} ENVIRONMENT"
    
    if [[ -d "environments/$env" ]]; then
        cd "environments/$env"
        docker-compose down
        cd - >/dev/null
        print_success "Environment $env stopped"
    else
        print_warning "Environment $env is not initialized"
    fi
}

# Clean environment
clean_environment() {
    local env="$1"
    
    if [[ ! " ${ENVIRONMENTS[@]} " =~ " ${env} " ]]; then
        print_error "Invalid environment: $env"
        return 1
    fi
    
    print_header "CLEANING ${env^^} ENVIRONMENT"
    
    print_warning "This will remove all data for $env environment!"
    read -p "Are you sure? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_status "Clean cancelled"
        return 0
    fi
    
    if [[ -d "environments/$env" ]]; then
        cd "environments/$env"
        docker-compose down -v --remove-orphans
        cd - >/dev/null
        print_success "Environment $env cleaned"
    else
        print_warning "Environment $env is not initialized"
    fi
}

# Main command handler
case "${1:-help}" in
    "init")
        create_env_configs
        ;;
    "deploy")
        if [[ -z "$2" ]]; then
            print_error "Please specify environment: dev, staging, or prod"
            exit 1
        fi
        deploy_environment "$2"
        ;;
    "list"|"status")
        list_environments
        ;;
    "stop")
        if [[ -z "$2" ]]; then
            print_error "Please specify environment: dev, staging, or prod"
            exit 1
        fi
        stop_environment "$2"
        ;;
    "clean")
        if [[ -z "$2" ]]; then
            print_error "Please specify environment: dev, staging, or prod"
            exit 1
        fi
        clean_environment "$2"
        ;;
    "help"|"-h"|"--help")
        echo "Multi-Environment Manager for Log Analysis System"
        echo ""
        echo "Usage: $0 [command] [environment]"
        echo ""
        echo "Commands:"
        echo "  init                    - Initialize all environment configurations"
        echo "  deploy <env>            - Deploy to specific environment"
        echo "  list                    - List all environments and their status"
        echo "  stop <env>              - Stop specific environment"
        echo "  clean <env>             - Clean specific environment (removes data)"
        echo "  help                    - Show this help"
        echo ""
        echo "Environments: dev, staging, prod"
        echo ""
        echo "Examples:"
        echo "  $0 init                 # Initialize all environments"
        echo "  $0 deploy dev           # Deploy to development"
        echo "  $0 deploy staging       # Deploy to staging"
        echo "  $0 deploy prod          # Deploy to production"
        echo "  $0 list                 # Show environment status"
        echo "  $0 stop dev             # Stop development environment"
        ;;
    *)
        print_error "Unknown command: $1"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac
