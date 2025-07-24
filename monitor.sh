#!/bin/bash

# =============================================================================
# Monitoring Tools for Log Analysis System
# Provides system monitoring, health checks, and performance metrics
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

# System overview
system_overview() {
    print_header "SYSTEM OVERVIEW"
    
    echo -e "${CYAN}📊 Container Status:${NC}"
    podman ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep "$PROJECT_NAME" || echo "No containers running"
    echo ""
    
    echo -e "${CYAN}🌐 Network Status:${NC}"
    podman network ls | grep "$PROJECT_NAME" || echo "No networks found"
    echo ""
    
    echo -e "${CYAN}💾 Volume Status:${NC}"
    podman volume ls | grep "$PROJECT_NAME" || echo "No volumes found"
    echo ""
    
    echo -e "${CYAN}🔍 Resource Usage:${NC}"
    podman stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}" | grep "$PROJECT_NAME" || echo "No containers running"
}

# Health check all services
health_check() {
    print_header "HEALTH CHECK"
    
    local services=("backend" "frontend" "postgres" "redis")
    local healthy=0
    local total=0
    
    for service in "${services[@]}"; do
        ((total++))
        container_name="${PROJECT_NAME}-${service}"
        
        if podman ps --format "{{.Names}}" | grep -q "^${container_name}$"; then
            case "$service" in
                "backend")
                    if curl -sf "http://localhost:8080/actuator/health" >/dev/null 2>&1; then
                        echo -e "✅ ${service}: ${GREEN}HEALTHY${NC}"
                        ((healthy++))
                    else
                        echo -e "❌ ${service}: ${RED}UNHEALTHY${NC}"
                    fi
                    ;;
                "frontend")
                    if curl -sf "http://localhost:3000" >/dev/null 2>&1; then
                        echo -e "✅ ${service}: ${GREEN}HEALTHY${NC}"
                        ((healthy++))
                    else
                        echo -e "❌ ${service}: ${RED}UNHEALTHY${NC}"
                    fi
                    ;;
                "postgres")
                    if podman exec "${container_name}" pg_isready -U loguser -d loganalyzer >/dev/null 2>&1; then
                        echo -e "✅ ${service}: ${GREEN}HEALTHY${NC}"
                        ((healthy++))
                    else
                        echo -e "❌ ${service}: ${RED}UNHEALTHY${NC}"
                    fi
                    ;;
                "redis")
                    if podman exec "${container_name}" redis-cli ping >/dev/null 2>&1; then
                        echo -e "✅ ${service}: ${GREEN}HEALTHY${NC}"
                        ((healthy++))
                    else
                        echo -e "❌ ${service}: ${RED}UNHEALTHY${NC}"
                    fi
                    ;;
            esac
        else
            echo -e "⚪ ${service}: ${YELLOW}NOT RUNNING${NC}"
        fi
    done
    
    echo ""
    echo -e "${CYAN}Overall Health: ${healthy}/${total} services healthy${NC}"
    
    if [[ $healthy -eq $total ]]; then
        print_success "All services are healthy!"
    elif [[ $healthy -gt 0 ]]; then
        print_warning "Some services need attention"
    else
        print_error "System is down!"
    fi
}

# Performance metrics
performance_metrics() {
    print_header "PERFORMANCE METRICS"
    
    # System resources
    echo -e "${CYAN}🖥️  System Resources:${NC}"
    echo "CPU Usage: $(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)%"
    echo "Memory Usage: $(free | grep Mem | awk '{printf "%.1f%%", $3/$2 * 100.0}')"
    echo "Disk Usage: $(df -h / | awk 'NR==2{printf "%s", $5}')"
    echo ""
    
    # Container metrics
    echo -e "${CYAN}🐳 Container Metrics:${NC}"
    podman stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}\t{{.NetIO}}\t{{.BlockIO}}" | grep "$PROJECT_NAME"
    echo ""
    
    # Application metrics (if backend is running)
    if curl -sf "http://localhost:8080/actuator/metrics" >/dev/null 2>&1; then
        echo -e "${CYAN}📊 Application Metrics:${NC}"
        
        # JVM metrics
        jvm_memory=$(curl -s "http://localhost:8080/actuator/metrics/jvm.memory.used" | grep -o '"value":[0-9]*' | cut -d':' -f2)
        if [[ -n "$jvm_memory" ]]; then
            echo "JVM Memory Used: $((jvm_memory / 1024 / 1024)) MB"
        fi
        
        # HTTP metrics
        http_requests=$(curl -s "http://localhost:8080/actuator/metrics/http.server.requests" | grep -o '"count":[0-9]*' | cut -d':' -f2)
        if [[ -n "$http_requests" ]]; then
            echo "HTTP Requests: $http_requests"
        fi
    fi
}

# Log analysis
log_analysis() {
    print_header "LOG ANALYSIS"
    
    local service="${1:-backend}"
    local lines="${2:-100}"
    
    container_name="${PROJECT_NAME}-${service}"
    
    if ! podman ps --format "{{.Names}}" | grep -q "^${container_name}$"; then
        print_error "Container $container_name is not running"
        return 1
    fi
    
    print_status "Analyzing last $lines lines from $service"
    
    # Get logs and analyze
    logs=$(podman logs --tail "$lines" "$container_name" 2>&1)
    
    echo -e "${CYAN}📈 Log Statistics:${NC}"
    echo "Total lines: $(echo "$logs" | wc -l)"
    echo "Error count: $(echo "$logs" | grep -ci "error" || echo "0")"
    echo "Warning count: $(echo "$logs" | grep -ci "warn" || echo "0")"
    echo "Info count: $(echo "$logs" | grep -ci "info" || echo "0")"
    echo ""
    
    echo -e "${CYAN}🔍 Recent Errors:${NC}"
    echo "$logs" | grep -i "error" | tail -5 || echo "No errors found"
    echo ""
    
    echo -e "${CYAN}⚠️  Recent Warnings:${NC}"
    echo "$logs" | grep -i "warn" | tail -5 || echo "No warnings found"
}

# Network diagnostics
network_diagnostics() {
    print_header "NETWORK DIAGNOSTICS"
    
    # Check network connectivity between containers
    local services=("backend" "postgres" "redis")
    
    echo -e "${CYAN}🌐 Network Connectivity:${NC}"
    
    for service in "${services[@]}"; do
        container_name="${PROJECT_NAME}-${service}"
        
        if podman ps --format "{{.Names}}" | grep -q "^${container_name}$"; then
            echo -e "\n📡 Testing from ${service}:"
            
            # Test database connections
            if [[ "$service" == "backend" ]]; then
                # Test PostgreSQL connection
                if podman exec "$container_name" nc -z "${PROJECT_NAME}-postgres" 5432 2>/dev/null; then
                    echo -e "  PostgreSQL: ${GREEN}✅ Connected${NC}"
                else
                    echo -e "  PostgreSQL: ${RED}❌ Failed${NC}"
                fi
                
                # Test Redis connection
                if podman exec "$container_name" nc -z "${PROJECT_NAME}-redis" 6379 2>/dev/null; then
                    echo -e "  Redis: ${GREEN}✅ Connected${NC}"
                else
                    echo -e "  Redis: ${RED}❌ Failed${NC}"
                fi
            fi
        else
            echo -e "${service}: ${YELLOW}Not running${NC}"
        fi
    done
    
    echo ""
    echo -e "${CYAN}🔌 Port Status:${NC}"
    netstat -tuln 2>/dev/null | grep -E ":(3000|8080|5432|6379)" || echo "No ports found"
}

# Continuous monitoring
continuous_monitor() {
    print_header "CONTINUOUS MONITORING"
    
    local interval="${1:-5}"
    
    print_status "Starting continuous monitoring (interval: ${interval}s)"
    print_status "Press Ctrl+C to stop"
    
    while true; do
        clear
        echo "$(date): Log Analysis System Monitor"
        echo "========================================"
        
        # Quick health check
        local healthy=0
        local services=("backend" "frontend" "postgres" "redis")
        
        for service in "${services[@]}"; do
            container_name="${PROJECT_NAME}-${service}"
            if podman ps --format "{{.Names}}" | grep -q "^${container_name}$"; then
                ((healthy++))
            fi
        done
        
        echo -e "Status: ${healthy}/4 services running"
        echo ""
        
        # Resource usage
        podman stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" | grep "$PROJECT_NAME"
        
        sleep "$interval"
    done
}

# Main command handler
case "${1:-help}" in
    "overview"|"status")
        system_overview
        ;;
    "health")
        health_check
        ;;
    "performance"|"perf")
        performance_metrics
        ;;
    "logs")
        log_analysis "$2" "$3"
        ;;
    "network"|"net")
        network_diagnostics
        ;;
    "watch"|"continuous")
        continuous_monitor "$2"
        ;;
    "help"|"-h"|"--help")
        echo "Monitoring Tools for Log Analysis System"
        echo ""
        echo "Usage: $0 [command] [options]"
        echo ""
        echo "Commands:"
        echo "  overview         - Show system overview"
        echo "  health           - Check health of all services"
        echo "  performance      - Show performance metrics"
        echo "  logs [service] [lines] - Analyze logs (default: backend, 100 lines)"
        echo "  network          - Network diagnostics"
        echo "  watch [interval] - Continuous monitoring (default: 5s)"
        echo "  help             - Show this help"
        echo ""
        echo "Examples:"
        echo "  $0 overview              # System overview"
        echo "  $0 health                # Health check all services"
        echo "  $0 logs backend 200      # Analyze 200 lines from backend"
        echo "  $0 watch 10              # Monitor every 10 seconds"
        ;;
    *)
        print_error "Unknown command: $1"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac
