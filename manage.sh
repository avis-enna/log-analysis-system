#!/bin/bash

# =============================================================================
# Master Management Script for Log Analysis System
# Unified interface for all deployment and management operations
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

# Print functions
print_status() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_header() { echo -e "${PURPLE}=== $1 ===${NC}"; }

# Check if required scripts exist
check_scripts() {
    local scripts=("deploy-podman.sh" "dev-tools.sh" "monitor.sh" "db-tools.sh" "performance.sh")
    local missing=0
    
    for script in "${scripts[@]}"; do
        if [[ ! -f "$script" ]]; then
            print_error "Missing script: $script"
            ((missing++))
        fi
    done
    
    if [[ $missing -gt 0 ]]; then
        print_error "Some required scripts are missing. Please ensure all scripts are present."
        exit 1
    fi
}

# Make scripts executable
make_executable() {
    local scripts=("deploy-podman.sh" "dev-tools.sh" "monitor.sh" "db-tools.sh" "performance.sh" "test-deployment.sh")
    
    for script in "${scripts[@]}"; do
        if [[ -f "$script" ]]; then
            chmod +x "$script"
        fi
    done
    
    print_success "All scripts are now executable"
}

# Show system status
show_status() {
    print_header "LOG ANALYSIS SYSTEM STATUS"
    
    # Check if containers are running
    local containers=("log-analyzer-backend" "log-analyzer-frontend" "log-analyzer-postgres" "log-analyzer-redis")
    local running=0
    
    echo -e "${CYAN}🐳 Container Status:${NC}"
    for container in "${containers[@]}"; do
        if podman ps --format "{{.Names}}" | grep -q "^${container}$"; then
            echo -e "  ✅ ${container}: ${GREEN}Running${NC}"
            ((running++))
        else
            echo -e "  ❌ ${container}: ${RED}Stopped${NC}"
        fi
    done
    
    echo ""
    echo -e "${CYAN}📊 System Health: ${running}/4 services running${NC}"
    
    if [[ $running -eq 4 ]]; then
        echo ""
        echo -e "${CYAN}🌐 Access URLs:${NC}"
        echo -e "  Frontend:  ${YELLOW}http://localhost:3000${NC}"
        echo -e "  Backend:   ${YELLOW}http://localhost:8080${NC}"
        echo -e "  API Docs:  ${YELLOW}http://localhost:8080/swagger-ui.html${NC}"
        echo -e "  Health:    ${YELLOW}http://localhost:8080/actuator/health${NC}"
    fi
}

# Quick deployment
quick_deploy() {
    print_header "QUICK DEPLOYMENT"
    
    print_status "Running pre-deployment tests..."
    if [[ -f "test-deployment.sh" ]]; then
        ./test-deployment.sh
    fi
    
    print_status "Starting deployment..."
    ./deploy-podman.sh
}

# Development setup
dev_setup() {
    print_header "DEVELOPMENT SETUP"
    
    echo -e "${CYAN}Choose development mode:${NC}"
    echo "1. Full containerized development"
    echo "2. Local development with containerized databases"
    echo "3. Debug mode with remote debugging"
    echo "4. Frontend development mode"
    
    read -p "Select option (1-4): " -n 1 -r
    echo ""
    
    case $REPLY in
        1)
            print_status "Starting full containerized development..."
            ./deploy-podman.sh
            ;;
        2)
            print_status "Starting local development mode..."
            ./dev-tools.sh dev
            ;;
        3)
            print_status "Starting debug mode..."
            ./dev-tools.sh debug
            ;;
        4)
            print_status "Starting frontend development mode..."
            ./dev-tools.sh frontend-dev
            ;;
        *)
            print_error "Invalid option"
            return 1
            ;;
    esac
}

# System monitoring
monitoring() {
    print_header "SYSTEM MONITORING"
    
    echo -e "${CYAN}Choose monitoring option:${NC}"
    echo "1. System overview"
    echo "2. Health check"
    echo "3. Performance metrics"
    echo "4. Live logs"
    echo "5. Continuous monitoring"
    
    read -p "Select option (1-5): " -n 1 -r
    echo ""
    
    case $REPLY in
        1)
            ./monitor.sh overview
            ;;
        2)
            ./monitor.sh health
            ;;
        3)
            ./monitor.sh performance
            ;;
        4)
            echo "Available services: backend, frontend, postgres, redis"
            read -p "Enter service name: " service
            ./monitor.sh logs "$service"
            ;;
        5)
            ./monitor.sh watch
            ;;
        *)
            print_error "Invalid option"
            return 1
            ;;
    esac
}

# Database management
database_mgmt() {
    print_header "DATABASE MANAGEMENT"
    
    echo -e "${CYAN}Choose database operation:${NC}"
    echo "1. Create backup"
    echo "2. Restore from backup"
    echo "3. Database statistics"
    echo "4. Database maintenance"
    echo "5. Redis operations"
    
    read -p "Select option (1-5): " -n 1 -r
    echo ""
    
    case $REPLY in
        1)
            ./db-tools.sh backup
            ;;
        2)
            echo "Available backups:"
            ls -1 ./backups/postgres_backup_*.sql.gz 2>/dev/null || echo "No backups found"
            read -p "Enter backup filename: " backup_file
            ./db-tools.sh restore "$backup_file"
            ;;
        3)
            ./db-tools.sh stats
            ;;
        4)
            ./db-tools.sh maintenance
            ;;
        5)
            echo "Redis operations: info, stats, memory, keys, flush"
            read -p "Enter operation: " operation
            ./db-tools.sh redis "$operation"
            ;;
        *)
            print_error "Invalid option"
            return 1
            ;;
    esac
}

# Performance management
performance_mgmt() {
    print_header "PERFORMANCE MANAGEMENT"
    
    echo -e "${CYAN}Choose performance operation:${NC}"
    echo "1. Run benchmark"
    echo "2. Optimize resources"
    echo "3. Load testing"
    echo "4. Memory profiling"
    echo "5. Generate performance report"
    
    read -p "Select option (1-5): " -n 1 -r
    echo ""
    
    case $REPLY in
        1)
            ./performance.sh benchmark
            ;;
        2)
            ./performance.sh optimize
            ;;
        3)
            read -p "Number of concurrent users (default: 50): " users
            read -p "Duration in seconds (default: 300): " duration
            ./performance.sh load-test "${users:-50}" "${duration:-300}"
            ;;
        4)
            read -p "Service to profile (default: backend): " service
            read -p "Duration in seconds (default: 60): " duration
            ./performance.sh memory-profile "${service:-backend}" "${duration:-60}"
            ;;
        5)
            ./performance.sh report
            ;;
        *)
            print_error "Invalid option"
            return 1
            ;;
    esac
}

# Troubleshooting guide
troubleshooting() {
    print_header "TROUBLESHOOTING GUIDE"
    
    echo -e "${CYAN}🔧 Common Issues and Solutions:${NC}"
    echo ""
    
    echo -e "${YELLOW}1. Containers won't start:${NC}"
    echo "   - Check if ports are available: netstat -tuln | grep -E ':(3000|8080|5432|6379)'"
    echo "   - Clean and restart: ./deploy-podman.sh clean && ./deploy-podman.sh"
    echo ""
    
    echo -e "${YELLOW}2. Application not accessible:${NC}"
    echo "   - Check container status: ./monitor.sh overview"
    echo "   - Check health: ./monitor.sh health"
    echo "   - View logs: ./monitor.sh logs backend"
    echo ""
    
    echo -e "${YELLOW}3. Database connection issues:${NC}"
    echo "   - Check PostgreSQL: ./db-tools.sh stats"
    echo "   - Test connection: ./dev-tools.sh db postgres"
    echo "   - Check network: ./monitor.sh network"
    echo ""
    
    echo -e "${YELLOW}4. Performance issues:${NC}"
    echo "   - Check resource usage: ./performance.sh optimize"
    echo "   - Run benchmark: ./performance.sh benchmark"
    echo "   - Profile memory: ./performance.sh memory-profile"
    echo ""
    
    echo -e "${YELLOW}5. Development issues:${NC}"
    echo "   - Use development mode: ./dev-tools.sh dev"
    echo "   - Enable debug mode: ./dev-tools.sh debug"
    echo "   - Check live logs: ./dev-tools.sh logs backend"
    echo ""
    
    echo -e "${CYAN}🆘 Emergency Commands:${NC}"
    echo "   - Stop everything: ./deploy-podman.sh stop"
    echo "   - Clean everything: ./deploy-podman.sh clean"
    echo "   - Fresh start: ./deploy-podman.sh clean && ./deploy-podman.sh"
    echo "   - System status: ./manage.sh status"
}

# Interactive menu
interactive_menu() {
    while true; do
        clear
        echo -e "${PURPLE}╔══════════════════════════════════════════════════════════════╗${NC}"
        echo -e "${PURPLE}║                    LOG ANALYSIS SYSTEM                       ║${NC}"
        echo -e "${PURPLE}║                   Management Interface                       ║${NC}"
        echo -e "${PURPLE}╚══════════════════════════════════════════════════════════════╝${NC}"
        echo ""
        
        show_status
        echo ""
        
        echo -e "${CYAN}📋 Available Operations:${NC}"
        echo "  1. 🚀 Quick Deploy"
        echo "  2. 💻 Development Setup"
        echo "  3. 📊 System Monitoring"
        echo "  4. 🗄️  Database Management"
        echo "  5. ⚡ Performance Management"
        echo "  6. 🔧 Troubleshooting Guide"
        echo "  7. 🛑 Stop All Services"
        echo "  8. 🧹 Clean Everything"
        echo "  9. ❌ Exit"
        echo ""
        
        read -p "Select operation (1-9): " -n 1 -r
        echo ""
        
        case $REPLY in
            1) quick_deploy ;;
            2) dev_setup ;;
            3) monitoring ;;
            4) database_mgmt ;;
            5) performance_mgmt ;;
            6) troubleshooting ;;
            7) ./deploy-podman.sh stop ;;
            8) ./deploy-podman.sh clean ;;
            9) print_success "Goodbye!"; exit 0 ;;
            *) print_error "Invalid option" ;;
        esac
        
        echo ""
        read -p "Press Enter to continue..."
    done
}

# Main command handler
case "${1:-interactive}" in
    "status")
        show_status
        ;;
    "deploy")
        quick_deploy
        ;;
    "dev")
        dev_setup
        ;;
    "monitor")
        monitoring
        ;;
    "db")
        database_mgmt
        ;;
    "performance")
        performance_mgmt
        ;;
    "troubleshoot")
        troubleshooting
        ;;
    "setup")
        check_scripts
        make_executable
        print_success "Setup completed!"
        ;;
    "interactive"|"menu")
        check_scripts
        make_executable
        interactive_menu
        ;;
    "help"|"-h"|"--help")
        echo "Master Management Script for Log Analysis System"
        echo ""
        echo "Usage: $0 [command]"
        echo ""
        echo "Commands:"
        echo "  status       - Show system status"
        echo "  deploy       - Quick deployment"
        echo "  dev          - Development setup"
        echo "  monitor      - System monitoring"
        echo "  db           - Database management"
        echo "  performance  - Performance management"
        echo "  troubleshoot - Troubleshooting guide"
        echo "  setup        - Setup and make scripts executable"
        echo "  interactive  - Interactive menu (default)"
        echo "  help         - Show this help"
        echo ""
        echo "Examples:"
        echo "  $0                    # Start interactive menu"
        echo "  $0 status             # Show system status"
        echo "  $0 deploy             # Quick deployment"
        echo "  $0 setup              # Setup scripts"
        ;;
    *)
        print_error "Unknown command: $1"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac
