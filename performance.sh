#!/bin/bash

# =============================================================================
# Performance Management Tools for Log Analysis System
# Provides performance monitoring, optimization, and resource management
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

# Performance benchmark
benchmark() {
    print_header "PERFORMANCE BENCHMARK"
    
    local duration="${1:-60}"
    local concurrent="${2:-10}"
    
    if ! command -v curl &> /dev/null; then
        print_error "curl is required for benchmarking"
        return 1
    fi
    
    if ! curl -sf "http://localhost:8080/actuator/health" >/dev/null; then
        print_error "Backend is not accessible at http://localhost:8080"
        return 1
    fi
    
    print_status "Running benchmark for ${duration}s with ${concurrent} concurrent requests"
    
    # Create test data
    local test_data='{"query":"*","page":0,"size":10}'
    
    # Benchmark search endpoint
    print_status "Benchmarking search endpoint..."
    
    local start_time=$(date +%s)
    local requests=0
    local errors=0
    local total_time=0
    
    for ((i=1; i<=duration; i++)); do
        for ((j=1; j<=concurrent; j++)); do
            {
                local req_start=$(date +%s.%N)
                if curl -sf -X POST "http://localhost:8080/api/v1/logs/search" \
                   -H "Content-Type: application/json" \
                   -d "$test_data" >/dev/null 2>&1; then
                    local req_end=$(date +%s.%N)
                    local req_time=$(echo "$req_end - $req_start" | bc -l)
                    total_time=$(echo "$total_time + $req_time" | bc -l)
                    ((requests++))
                else
                    ((errors++))
                fi
            } &
        done
        wait
        echo -n "."
    done
    echo ""
    
    local end_time=$(date +%s)
    local total_duration=$((end_time - start_time))
    
    echo ""
    echo -e "${CYAN}📊 Benchmark Results:${NC}"
    echo "Duration: ${total_duration}s"
    echo "Total Requests: $requests"
    echo "Failed Requests: $errors"
    echo "Requests/Second: $(echo "scale=2; $requests / $total_duration" | bc -l)"
    if [[ $requests -gt 0 ]]; then
        echo "Average Response Time: $(echo "scale=3; $total_time / $requests" | bc -l)s"
    fi
    echo "Success Rate: $(echo "scale=2; ($requests * 100) / ($requests + $errors)" | bc -l)%"
}

# Resource optimization
optimize_resources() {
    print_header "RESOURCE OPTIMIZATION"
    
    # Check current resource usage
    print_status "Current resource usage:"
    podman stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" | grep "$PROJECT_NAME"
    
    echo ""
    print_status "Optimization recommendations:"
    
    # Check memory usage
    local backend_mem=$(podman stats --no-stream --format "{{.MemPerc}}" "${PROJECT_NAME}-backend" 2>/dev/null | sed 's/%//')
    if [[ -n "$backend_mem" && $(echo "$backend_mem > 80" | bc -l) -eq 1 ]]; then
        print_warning "Backend memory usage is high (${backend_mem}%)"
        echo "  - Consider increasing container memory limits"
        echo "  - Check for memory leaks in application logs"
    fi
    
    # Check CPU usage
    local backend_cpu=$(podman stats --no-stream --format "{{.CPUPerc}}" "${PROJECT_NAME}-backend" 2>/dev/null | sed 's/%//')
    if [[ -n "$backend_cpu" && $(echo "$backend_cpu > 80" | bc -l) -eq 1 ]]; then
        print_warning "Backend CPU usage is high (${backend_cpu}%)"
        echo "  - Consider scaling horizontally"
        echo "  - Check for CPU-intensive operations"
    fi
    
    # Database optimization suggestions
    print_status "Database optimization suggestions:"
    if podman ps --format "{{.Names}}" | grep -q "^${PROJECT_NAME}-postgres$"; then
        # Check database connections
        local connections=$(podman exec "${PROJECT_NAME}-postgres" psql -U loguser -d loganalyzer -t -c "SELECT count(*) FROM pg_stat_activity;" 2>/dev/null | xargs)
        if [[ -n "$connections" && $connections -gt 50 ]]; then
            print_warning "High number of database connections: $connections"
            echo "  - Consider connection pooling optimization"
        fi
        
        # Check for slow queries (if pg_stat_statements is available)
        echo "  - Run 'VACUUM ANALYZE' regularly"
        echo "  - Monitor slow queries"
        echo "  - Consider adding indexes for frequent queries"
    fi
}

# Load testing
load_test() {
    print_header "LOAD TESTING"
    
    local users="${1:-50}"
    local duration="${2:-300}"
    
    if ! command -v ab &> /dev/null; then
        print_warning "Apache Bench (ab) not found. Installing..."
        if command -v apt-get &> /dev/null; then
            sudo apt-get update && sudo apt-get install -y apache2-utils
        elif command -v yum &> /dev/null; then
            sudo yum install -y httpd-tools
        else
            print_error "Cannot install Apache Bench. Please install manually."
            return 1
        fi
    fi
    
    print_status "Running load test with $users concurrent users for ${duration}s"
    
    # Test different endpoints
    local endpoints=(
        "http://localhost:8080/actuator/health"
        "http://localhost:3000"
    )
    
    for endpoint in "${endpoints[@]}"; do
        echo ""
        print_status "Testing endpoint: $endpoint"
        
        ab -n $((users * 10)) -c "$users" -t "$duration" "$endpoint" | grep -E "(Requests per second|Time per request|Transfer rate)"
    done
}

# Memory profiling
memory_profile() {
    print_header "MEMORY PROFILING"
    
    local service="${1:-backend}"
    local duration="${2:-60}"
    
    container_name="${PROJECT_NAME}-${service}"
    
    if ! podman ps --format "{{.Names}}" | grep -q "^${container_name}$"; then
        print_error "Container $container_name is not running"
        return 1
    fi
    
    print_status "Profiling memory usage for $service (${duration}s)"
    
    # Create memory profile log
    local profile_file="memory_profile_${service}_$(date +%Y%m%d_%H%M%S).log"
    
    echo "Timestamp,Memory_Usage_MB,Memory_Percent" > "$profile_file"
    
    for ((i=1; i<=duration; i++)); do
        local mem_usage=$(podman stats --no-stream --format "{{.MemUsage}}" "$container_name" | cut -d'/' -f1)
        local mem_percent=$(podman stats --no-stream --format "{{.MemPerc}}" "$container_name")
        
        # Convert to MB if needed
        if [[ "$mem_usage" == *"GiB" ]]; then
            mem_usage=$(echo "$mem_usage" | sed 's/GiB//' | awk '{print $1 * 1024}')
        elif [[ "$mem_usage" == *"MiB" ]]; then
            mem_usage=$(echo "$mem_usage" | sed 's/MiB//')
        fi
        
        echo "$(date '+%Y-%m-%d %H:%M:%S'),$mem_usage,$mem_percent" >> "$profile_file"
        sleep 1
    done
    
    print_success "Memory profile saved to: $profile_file"
    
    # Show summary
    echo ""
    echo -e "${CYAN}📊 Memory Profile Summary:${NC}"
    echo "Max Memory: $(tail -n +2 "$profile_file" | cut -d',' -f2 | sort -n | tail -1) MB"
    echo "Min Memory: $(tail -n +2 "$profile_file" | cut -d',' -f2 | sort -n | head -1) MB"
    echo "Avg Memory: $(tail -n +2 "$profile_file" | cut -d',' -f2 | awk '{sum+=$1} END {print sum/NR}') MB"
}

# Container scaling
scale_containers() {
    print_header "CONTAINER SCALING"
    
    local service="$1"
    local replicas="$2"
    
    if [[ -z "$service" || -z "$replicas" ]]; then
        print_error "Usage: scale_containers <service> <replicas>"
        echo "Services: backend, frontend"
        return 1
    fi
    
    case "$service" in
        "backend")
            print_status "Scaling backend to $replicas replicas"
            
            # Stop existing backend
            podman stop "${PROJECT_NAME}-backend" 2>/dev/null || true
            podman rm "${PROJECT_NAME}-backend" 2>/dev/null || true
            
            # Start multiple backend instances
            for ((i=1; i<=replicas; i++)); do
                local port=$((8080 + i - 1))
                print_status "Starting backend replica $i on port $port"
                
                podman run -d \
                    --name "${PROJECT_NAME}-backend-$i" \
                    --network "${PROJECT_NAME}-network" \
                    -p "${port}:8080" \
                    -e SPRING_PROFILES_ACTIVE=docker \
                    -e SPRING_DATASOURCE_URL="jdbc:postgresql://${PROJECT_NAME}-postgres:5432/loganalyzer" \
                    -e SPRING_DATASOURCE_USERNAME="loguser" \
                    -e SPRING_DATASOURCE_PASSWORD="logpass123" \
                    "${PROJECT_NAME}-backend"
            done
            
            print_success "Backend scaled to $replicas replicas"
            echo "Access URLs:"
            for ((i=1; i<=replicas; i++)); do
                local port=$((8080 + i - 1))
                echo "  Backend $i: http://localhost:$port"
            done
            ;;
        *)
            print_error "Scaling not implemented for service: $service"
            return 1
            ;;
    esac
}

# Performance report
performance_report() {
    print_header "PERFORMANCE REPORT"
    
    local report_file="performance_report_$(date +%Y%m%d_%H%M%S).txt"
    
    {
        echo "Log Analysis System - Performance Report"
        echo "Generated: $(date)"
        echo "========================================"
        echo ""
        
        echo "System Overview:"
        echo "---------------"
        podman ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep "$PROJECT_NAME"
        echo ""
        
        echo "Resource Usage:"
        echo "---------------"
        podman stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" | grep "$PROJECT_NAME"
        echo ""
        
        echo "System Resources:"
        echo "----------------"
        echo "CPU: $(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)%"
        echo "Memory: $(free | grep Mem | awk '{printf "%.1f%%", $3/$2 * 100.0}')"
        echo "Disk: $(df -h / | awk 'NR==2{printf "%s", $5}')"
        echo ""
        
        if curl -sf "http://localhost:8080/actuator/health" >/dev/null 2>&1; then
            echo "Application Health:"
            echo "------------------"
            curl -s "http://localhost:8080/actuator/health" | jq '.' 2>/dev/null || echo "Health check passed"
        fi
        
    } > "$report_file"
    
    print_success "Performance report saved to: $report_file"
    cat "$report_file"
}

# Main command handler
case "${1:-help}" in
    "benchmark")
        benchmark "$2" "$3"
        ;;
    "optimize")
        optimize_resources
        ;;
    "load-test")
        load_test "$2" "$3"
        ;;
    "memory-profile")
        memory_profile "$2" "$3"
        ;;
    "scale")
        scale_containers "$2" "$3"
        ;;
    "report")
        performance_report
        ;;
    "help"|"-h"|"--help")
        echo "Performance Management Tools for Log Analysis System"
        echo ""
        echo "Usage: $0 [command] [options]"
        echo ""
        echo "Commands:"
        echo "  benchmark [duration] [concurrent]    - Run performance benchmark (default: 60s, 10 concurrent)"
        echo "  optimize                              - Analyze and suggest optimizations"
        echo "  load-test [users] [duration]          - Run load test (default: 50 users, 300s)"
        echo "  memory-profile [service] [duration]   - Profile memory usage (default: backend, 60s)"
        echo "  scale <service> <replicas>            - Scale containers"
        echo "  report                                - Generate performance report"
        echo "  help                                  - Show this help"
        echo ""
        echo "Examples:"
        echo "  $0 benchmark 120 20           # 2-minute benchmark with 20 concurrent requests"
        echo "  $0 load-test 100 600          # Load test with 100 users for 10 minutes"
        echo "  $0 memory-profile backend 300 # Profile backend memory for 5 minutes"
        echo "  $0 scale backend 3             # Scale backend to 3 replicas"
        ;;
    *)
        print_error "Unknown command: $1"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac
