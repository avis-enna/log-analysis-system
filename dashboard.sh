#!/bin/bash

# =============================================================================
# Real-time Dashboard for Log Analysis System
# Provides a live monitoring dashboard with real-time metrics
# =============================================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m'

# Configuration
PROJECT_NAME="log-analyzer"
REFRESH_INTERVAL=5

# Print functions
print_header() { echo -e "${WHITE}$1${NC}"; }
print_metric() { echo -e "${CYAN}$1:${NC} $2"; }
print_status_good() { echo -e "${GREEN}●${NC} $1"; }
print_status_warn() { echo -e "${YELLOW}●${NC} $1"; }
print_status_error() { echo -e "${RED}●${NC} $1"; }

# Get container status
get_container_status() {
    local container="$1"
    if command -v podman &> /dev/null; then
        CONTAINER_CMD="podman"
    elif command -v docker &> /dev/null; then
        CONTAINER_CMD="docker"
    else
        echo "Not Available"
        return
    fi
    
    if $CONTAINER_CMD ps --format "{{.Names}}" 2>/dev/null | grep -q "^${container}$"; then
        echo "Running"
    else
        echo "Stopped"
    fi
}

# Get system metrics
get_system_metrics() {
    # CPU usage
    if command -v top &> /dev/null; then
        CPU_USAGE=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1 2>/dev/null || echo "N/A")
    else
        CPU_USAGE="N/A"
    fi
    
    # Memory usage
    if command -v free &> /dev/null; then
        MEMORY_USAGE=$(free | grep Mem | awk '{printf "%.1f", $3/$2 * 100.0}' 2>/dev/null || echo "N/A")
    else
        MEMORY_USAGE="N/A"
    fi
    
    # Disk usage
    if command -v df &> /dev/null; then
        DISK_USAGE=$(df -h / | awk 'NR==2{printf "%s", $5}' 2>/dev/null || echo "N/A")
    else
        DISK_USAGE="N/A"
    fi
    
    # Load average
    if [[ -f /proc/loadavg ]]; then
        LOAD_AVG=$(cat /proc/loadavg | awk '{print $1}' 2>/dev/null || echo "N/A")
    else
        LOAD_AVG="N/A"
    fi
}

# Get application metrics
get_app_metrics() {
    # Backend health
    if curl -sf "http://localhost:8080/actuator/health" >/dev/null 2>&1; then
        BACKEND_HEALTH="Healthy"
        BACKEND_STATUS="good"
    else
        BACKEND_HEALTH="Unhealthy"
        BACKEND_STATUS="error"
    fi
    
    # Frontend health
    if curl -sf "http://localhost:3000" >/dev/null 2>&1; then
        FRONTEND_HEALTH="Healthy"
        FRONTEND_STATUS="good"
    else
        FRONTEND_HEALTH="Unhealthy"
        FRONTEND_STATUS="error"
    fi
    
    # Database connectivity
    if [[ "$(get_container_status "${PROJECT_NAME}-postgres")" == "Running" ]]; then
        if $CONTAINER_CMD exec "${PROJECT_NAME}-postgres" pg_isready -U loguser -d loganalyzer >/dev/null 2>&1; then
            DB_HEALTH="Connected"
            DB_STATUS="good"
        else
            DB_HEALTH="Connection Failed"
            DB_STATUS="error"
        fi
    else
        DB_HEALTH="Not Running"
        DB_STATUS="error"
    fi
    
    # Redis connectivity
    if [[ "$(get_container_status "${PROJECT_NAME}-redis")" == "Running" ]]; then
        if $CONTAINER_CMD exec "${PROJECT_NAME}-redis" redis-cli ping >/dev/null 2>&1; then
            REDIS_HEALTH="Connected"
            REDIS_STATUS="good"
        else
            REDIS_HEALTH="Connection Failed"
            REDIS_STATUS="error"
        fi
    else
        REDIS_HEALTH="Not Running"
        REDIS_STATUS="error"
    fi
}

# Get network metrics
get_network_metrics() {
    # Active connections
    if command -v netstat &> /dev/null; then
        ACTIVE_CONNECTIONS=$(netstat -an | grep ESTABLISHED | wc -l 2>/dev/null || echo "N/A")
    else
        ACTIVE_CONNECTIONS="N/A"
    fi
    
    # Listening ports
    if command -v netstat &> /dev/null; then
        LISTENING_PORTS=$(netstat -tuln | grep -E ":(3000|8080|5432|6379)" | wc -l 2>/dev/null || echo "N/A")
    else
        LISTENING_PORTS="N/A"
    fi
}

# Draw progress bar
draw_progress_bar() {
    local value="$1"
    local max="$2"
    local width="${3:-20}"
    local color="${4:-${GREEN}}"
    
    if [[ "$value" == "N/A" ]]; then
        echo -e "${YELLOW}[    N/A    ]${NC}"
        return
    fi
    
    local percentage=$((value * 100 / max))
    local filled=$((percentage * width / 100))
    local empty=$((width - filled))
    
    # Choose color based on percentage
    if [[ $percentage -gt 80 ]]; then
        color="${RED}"
    elif [[ $percentage -gt 60 ]]; then
        color="${YELLOW}"
    else
        color="${GREEN}"
    fi
    
    printf "${color}["
    printf "%*s" $filled | tr ' ' '█'
    printf "%*s" $empty | tr ' ' '░'
    printf "]${NC} %3d%%\n" $percentage
}

# Main dashboard display
display_dashboard() {
    while true; do
        # Clear screen
        clear
        
        # Get all metrics
        get_system_metrics
        get_app_metrics
        get_network_metrics
        
        # Header
        echo -e "${PURPLE}╔══════════════════════════════════════════════════════════════════════════════╗${NC}"
        echo -e "${PURPLE}║                          LOG ANALYSIS SYSTEM DASHBOARD                       ║${NC}"
        echo -e "${PURPLE}║                              $(date '+%Y-%m-%d %H:%M:%S')                              ║${NC}"
        echo -e "${PURPLE}╚══════════════════════════════════════════════════════════════════════════════╝${NC}"
        echo ""
        
        # System Metrics Section
        print_header "🖥️  SYSTEM METRICS"
        echo "────────────────────────────────────────────────────────────────────────────────"
        printf "%-20s " "CPU Usage:"
        if [[ "$CPU_USAGE" != "N/A" ]]; then
            draw_progress_bar "${CPU_USAGE%.*}" 100 30
        else
            echo -e "${YELLOW}N/A${NC}"
        fi
        
        printf "%-20s " "Memory Usage:"
        if [[ "$MEMORY_USAGE" != "N/A" ]]; then
            draw_progress_bar "${MEMORY_USAGE%.*}" 100 30
        else
            echo -e "${YELLOW}N/A${NC}"
        fi
        
        printf "%-20s " "Disk Usage:"
        if [[ "$DISK_USAGE" != "N/A" ]]; then
            local disk_num="${DISK_USAGE%\%}"
            draw_progress_bar "$disk_num" 100 30
        else
            echo -e "${YELLOW}N/A${NC}"
        fi
        
        print_metric "Load Average" "$LOAD_AVG"
        echo ""
        
        # Application Status Section
        print_header "🚀 APPLICATION STATUS"
        echo "────────────────────────────────────────────────────────────────────────────────"
        case "$BACKEND_STATUS" in
            "good") print_status_good "Backend Service: $BACKEND_HEALTH" ;;
            "error") print_status_error "Backend Service: $BACKEND_HEALTH" ;;
            *) print_status_warn "Backend Service: $BACKEND_HEALTH" ;;
        esac
        
        case "$FRONTEND_STATUS" in
            "good") print_status_good "Frontend Service: $FRONTEND_HEALTH" ;;
            "error") print_status_error "Frontend Service: $FRONTEND_HEALTH" ;;
            *) print_status_warn "Frontend Service: $FRONTEND_HEALTH" ;;
        esac
        
        case "$DB_STATUS" in
            "good") print_status_good "PostgreSQL: $DB_HEALTH" ;;
            "error") print_status_error "PostgreSQL: $DB_HEALTH" ;;
            *) print_status_warn "PostgreSQL: $DB_HEALTH" ;;
        esac
        
        case "$REDIS_STATUS" in
            "good") print_status_good "Redis Cache: $REDIS_HEALTH" ;;
            "error") print_status_error "Redis Cache: $REDIS_HEALTH" ;;
            *) print_status_warn "Redis Cache: $REDIS_HEALTH" ;;
        esac
        echo ""
        
        # Container Status Section
        print_header "🐳 CONTAINER STATUS"
        echo "────────────────────────────────────────────────────────────────────────────────"
        local containers=("backend" "frontend" "postgres" "redis")
        for container in "${containers[@]}"; do
            local status=$(get_container_status "${PROJECT_NAME}-${container}")
            case "$status" in
                "Running") print_status_good "$container: $status" ;;
                "Stopped") print_status_error "$container: $status" ;;
                *) print_status_warn "$container: $status" ;;
            esac
        done
        echo ""
        
        # Network Metrics Section
        print_header "🌐 NETWORK METRICS"
        echo "────────────────────────────────────────────────────────────────────────────────"
        print_metric "Active Connections" "$ACTIVE_CONNECTIONS"
        print_metric "Listening Ports" "$LISTENING_PORTS"
        echo ""
        
        # Quick Access URLs Section
        print_header "🔗 QUICK ACCESS"
        echo "────────────────────────────────────────────────────────────────────────────────"
        echo -e "${CYAN}Frontend:${NC}  http://localhost:3000"
        echo -e "${CYAN}Backend:${NC}   http://localhost:8080"
        echo -e "${CYAN}API Docs:${NC}  http://localhost:8080/swagger-ui.html"
        echo -e "${CYAN}Health:${NC}    http://localhost:8080/actuator/health"
        echo ""
        
        # Footer
        echo -e "${PURPLE}────────────────────────────────────────────────────────────────────────────────${NC}"
        echo -e "${WHITE}Press Ctrl+C to exit | Refresh every ${REFRESH_INTERVAL}s | $(date '+%H:%M:%S')${NC}"
        
        # Wait for refresh interval
        sleep "$REFRESH_INTERVAL"
    done
}

# Compact dashboard for smaller terminals
display_compact_dashboard() {
    while true; do
        clear
        get_system_metrics
        get_app_metrics
        
        echo -e "${PURPLE}╔═══════════════════════════════════════════════════════════╗${NC}"
        echo -e "${PURPLE}║              LOG ANALYSIS DASHBOARD                       ║${NC}"
        echo -e "${PURPLE}║                $(date '+%H:%M:%S')                                ║${NC}"
        echo -e "${PURPLE}╚═══════════════════════════════════════════════════════════╝${NC}"
        
        # System metrics in one line
        echo -e "${CYAN}System:${NC} CPU:${CPU_USAGE}% MEM:${MEMORY_USAGE}% DISK:${DISK_USAGE} LOAD:${LOAD_AVG}"
        
        # Application status
        echo -e "${CYAN}Apps:${NC} Backend:${BACKEND_HEALTH} Frontend:${FRONTEND_HEALTH}"
        echo -e "${CYAN}Data:${NC} PostgreSQL:${DB_HEALTH} Redis:${REDIS_HEALTH}"
        
        # URLs
        echo -e "${CYAN}URLs:${NC} http://localhost:3000 | http://localhost:8080"
        
        echo -e "${WHITE}Press Ctrl+C to exit | Refresh: ${REFRESH_INTERVAL}s${NC}"
        
        sleep "$REFRESH_INTERVAL"
    done
}

# Main command handler
case "${1:-full}" in
    "full")
        display_dashboard
        ;;
    "compact")
        display_compact_dashboard
        ;;
    "once")
        get_system_metrics
        get_app_metrics
        get_network_metrics
        
        echo "=== LOG ANALYSIS SYSTEM STATUS ==="
        echo "Time: $(date)"
        echo ""
        echo "System Metrics:"
        echo "  CPU Usage: ${CPU_USAGE}%"
        echo "  Memory Usage: ${MEMORY_USAGE}%"
        echo "  Disk Usage: ${DISK_USAGE}"
        echo "  Load Average: ${LOAD_AVG}"
        echo ""
        echo "Application Status:"
        echo "  Backend: ${BACKEND_HEALTH}"
        echo "  Frontend: ${FRONTEND_HEALTH}"
        echo "  PostgreSQL: ${DB_HEALTH}"
        echo "  Redis: ${REDIS_HEALTH}"
        echo ""
        echo "Network:"
        echo "  Active Connections: ${ACTIVE_CONNECTIONS}"
        echo "  Listening Ports: ${LISTENING_PORTS}"
        ;;
    "help"|"-h"|"--help")
        echo "Real-time Dashboard for Log Analysis System"
        echo ""
        echo "Usage: $0 [mode]"
        echo ""
        echo "Modes:"
        echo "  full     - Full dashboard with progress bars (default)"
        echo "  compact  - Compact dashboard for small terminals"
        echo "  once     - Show status once and exit"
        echo "  help     - Show this help"
        echo ""
        echo "Controls:"
        echo "  Ctrl+C   - Exit dashboard"
        echo ""
        echo "The dashboard refreshes every ${REFRESH_INTERVAL} seconds"
        ;;
    *)
        echo "Unknown mode: $1"
        echo "Use '$0 help' for available modes"
        exit 1
        ;;
esac
