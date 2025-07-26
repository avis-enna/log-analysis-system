#!/bin/bash

# Real-time Log Data Pusher
# Continuously generates and pushes log data to the log analysis system
# Usage: ./realtime-log-pusher.sh [interval_seconds] [batch_size]

set -e

# Configuration
BACKEND_URL="http://localhost:8080/api/v1"
INTERVAL=${1:-2}  # Default: push every 2 seconds
BATCH_SIZE=${2:-5}  # Default: 5 logs per batch
MAX_RUNS=${3:-0}  # Default: run indefinitely (0 = infinite)

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Arrays for realistic log generation
LEVELS=("INFO" "WARN" "ERROR" "DEBUG")
SOURCES=("web-server" "database" "api-gateway" "auth-service" "cache-service" "mail-service" "file-service" "search-engine")
APPLICATIONS=("frontend" "backend" "worker" "scheduler" "monitor" "analytics")
ENVIRONMENTS=("production" "staging" "development")
HOSTS=("web-01" "web-02" "db-master" "db-slave" "cache-01" "lb-01" "auth-01" "api-01")
CATEGORIES=("authentication" "network" "application" "security" "system" "database" "web" "email")

# Weighted level distribution (more INFO, fewer ERRORs)
LEVEL_WEIGHTS=(60 25 10 5)  # INFO=60%, WARN=25%, ERROR=10%, DEBUG=5%

# Message templates by level
INFO_MESSAGES=(
    "User session created successfully"
    "Database connection established"
    "Cache hit for key: user_%d"
    "API request processed: GET /api/users"
    "File upload completed: document_%d.pdf"
    "Email sent successfully to user@example.com"
    "Backup process completed"
    "Health check passed"
    "Configuration reloaded"
    "Service startup completed"
)

WARN_MESSAGES=(
    "High memory usage detected: %d%%"
    "Slow database query detected: %d ms"
    "Rate limit approaching for IP 192.168.1.%d"
    "Disk space warning: %d%% used"
    "SSL certificate expires in %d days"
    "Connection pool near capacity: %d/%d"
    "Large response size: %d MB"
    "Cache miss rate high: %d%%"
    "Queue depth increasing: %d items"
    "Response time degraded: %d ms"
)

ERROR_MESSAGES=(
    "Database connection failed: timeout after %d seconds"
    "Authentication service unavailable"
    "File not found: /path/to/file_%d.log"
    "Out of memory error in module %s"
    "Network unreachable: host 192.168.1.%d"
    "Invalid API token provided"
    "Service crash detected: %s restarting"
    "Permission denied for user %s"
    "Configuration validation failed"
    "External service timeout: %s"
)

DEBUG_MESSAGES=(
    "Processing request with ID: req_%d"
    "Cache key generated: %s_%d"
    "SQL query executed: SELECT * FROM users WHERE id=%d"
    "Method entry: %s.process()"
    "Variable state: counter=%d, active=%s"
    "Thread pool stats: active=%d, queue=%d"
    "Memory stats: heap=%dMB, non-heap=%dMB"
    "GC stats: count=%d, time=%dms"
    "Connection stats: active=%d, idle=%d"
    "Request routing: %s -> %s"
)

# Function to get weighted random level
get_weighted_level() {
    local rand=$((RANDOM % 100))
    local cumulative=0
    
    for i in "${!LEVELS[@]}"; do
        cumulative=$((cumulative + LEVEL_WEIGHTS[i]))
        if [ $rand -lt $cumulative ]; then
            echo "${LEVELS[i]}"
            return
        fi
    done
    echo "INFO"  # fallback
}

# Function to get random array element
get_random_element() {
    local arr=("$@")
    echo "${arr[RANDOM % ${#arr[@]}]}"
}

# Function to generate realistic timestamp
generate_timestamp() {
    date "+%Y-%m-%d %H:%M:%S.%3N"
}

# Function to generate a single log entry
generate_log_entry() {
    local timestamp=$(generate_timestamp)
    local level=$(get_weighted_level)
    local source=$(get_random_element "${SOURCES[@]}")
    local application=$(get_random_element "${APPLICATIONS[@]}")
    local environment=$(get_random_element "${ENVIRONMENTS[@]}")
    local host=$(get_random_element "${HOSTS[@]}")
    local category=$(get_random_element "${CATEGORIES[@]}")
    local thread="thread-$((RANDOM % 10 + 1))"
    local logger="com.loganalyzer.${category}.$(echo $source | tr '-' '.')"
    
    # Select message template based on level
    local message
    case $level in
        "INFO")
            local template=$(get_random_element "${INFO_MESSAGES[@]}")
            message=$(printf "$template" $((RANDOM % 1000 + 1)) $((RANDOM % 100 + 1)))
            ;;
        "WARN")
            local template=$(get_random_element "${WARN_MESSAGES[@]}")
            message=$(printf "$template" $((RANDOM % 100 + 50)) $((RANDOM % 5000 + 100)) $((RANDOM % 255 + 1)) $((RANDOM % 100 + 70)) $((RANDOM % 30 + 1)) $((RANDOM % 50 + 40)) $((RANDOM % 100 + 80)) $((RANDOM % 50 + 1)) $((RANDOM % 1000 + 100)) $((RANDOM % 2000 + 500)))
            ;;
        "ERROR")
            local template=$(get_random_element "${ERROR_MESSAGES[@]}")
            local error_sources=("authentication" "database" "network" "file-system" "memory")
            local error_source=$(get_random_element "${error_sources[@]}")
            message=$(printf "$template" $((RANDOM % 30 + 5)) $((RANDOM % 1000 + 1)) "$error_source" $((RANDOM % 255 + 1)) "$source")
            ;;
        "DEBUG")
            local template=$(get_random_element "${DEBUG_MESSAGES[@]}")
            local debug_methods=("processRequest" "validateInput" "executeQuery" "handleResponse")
            local debug_method=$(get_random_element "${debug_methods[@]}")
            message=$(printf "$template" $((RANDOM % 10000 + 1)) "$category" $((RANDOM % 1000 + 1)) "$debug_method" $((RANDOM % 1000 + 1)) "true" $((RANDOM % 20 + 1)) $((RANDOM % 100 + 1)) $((RANDOM % 512 + 128)) $((RANDOM % 256 + 64)) $((RANDOM % 50 + 1)) $((RANDOM % 1000 + 100)) $((RANDOM % 10 + 1)) $((RANDOM % 20 + 1)) "$source" "$application")
            ;;
    esac
    
    # Format log entry (structured format)
    echo "${timestamp} [${thread}] ${level} ${logger} - ${message} [source=${source}, app=${application}, env=${environment}, host=${host}, category=${category}]"
}

# Function to check if backend is ready
check_backend() {
    echo -e "${BLUE}🔍 Checking backend availability...${NC}"
    for i in {1..10}; do
        if curl -s -f "${BACKEND_URL}/dashboard/realtime" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ Backend is ready!${NC}"
            return 0
        fi
        echo -e "${YELLOW}⏳ Waiting for backend... (attempt $i/10)${NC}"
        sleep 2
    done
    echo -e "${RED}❌ Backend not available at ${BACKEND_URL}${NC}"
    return 1
}

# Function to push log batch
push_log_batch() {
    local logs=()
    
    # Generate batch of logs
    for ((i=1; i<=BATCH_SIZE; i++)); do
        logs+=("$(generate_log_entry)")
    done
    
    # Create JSON payload
    local json_payload=$(printf '{"logs":["%s"],"source":"realtime-generator"}' "$(IFS='","'; echo "${logs[*]}")")
    
    # Push to backend
    local response=$(curl -s -X POST "${BACKEND_URL}/logs/ingest/batch" \
        -H "Content-Type: application/json" \
        -d "$json_payload" \
        -w "\n%{http_code}")
    
    local status_code=$(echo "$response" | tail -n1)
    local response_body=$(echo "$response" | head -n -1)
    
    if [ "$status_code" = "200" ]; then
        echo -e "${GREEN}✅ Pushed ${BATCH_SIZE} logs successfully${NC}"
        return 0
    else
        echo -e "${RED}❌ Failed to push logs. Status: $status_code${NC}"
        echo -e "${RED}Response: $response_body${NC}"
        return 1
    fi
}

# Function to display current stats
show_stats() {
    echo -e "${BLUE}📊 Fetching current dashboard stats...${NC}"
    local stats=$(curl -s "${BACKEND_URL}/dashboard/stats" 2>/dev/null)
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Current Stats:${NC}"
        echo "$stats" | jq -r '. | "📈 Total Logs: \(.totalLogs)\n🚨 Total Errors: \(.totalErrors)\n⚠️  Total Warnings: \(.totalWarnings)\n💡 Total Info: \(.totalInfo)\n🔧 Error Rate: \(.errorRate)%"' 2>/dev/null || echo "$stats"
    else
        echo -e "${YELLOW}⚠️  Could not fetch stats${NC}"
    fi
}

# Main execution
echo -e "${BLUE}🚀 Real-time Log Data Pusher${NC}"
echo -e "${BLUE}==============================${NC}"
echo -e "Backend URL: ${BACKEND_URL}"
echo -e "Push Interval: ${INTERVAL} seconds"
echo -e "Batch Size: ${BATCH_SIZE} logs per batch"
echo -e "Max Runs: $([ $MAX_RUNS -eq 0 ] && echo "∞ (infinite)" || echo $MAX_RUNS)"
echo ""

# Check backend availability
if ! check_backend; then
    exit 1
fi

# Show initial stats
show_stats
echo ""

# Start pushing logs
echo -e "${GREEN}🔄 Starting real-time log generation...${NC}"
echo -e "${YELLOW}Press Ctrl+C to stop${NC}"
echo ""

run_count=0
error_count=0

while true; do
    run_count=$((run_count + 1))
    
    echo -e "${BLUE}📤 Push #${run_count} - $(date '+%Y-%m-%d %H:%M:%S')${NC}"
    
    if push_log_batch; then
        echo -e "${GREEN}   ✓ Batch pushed successfully${NC}"
    else
        error_count=$((error_count + 1))
        echo -e "${RED}   ✗ Batch failed (errors: $error_count)${NC}"
        
        # Exit if too many consecutive errors
        if [ $error_count -ge 5 ]; then
            echo -e "${RED}❌ Too many consecutive errors. Exiting.${NC}"
            exit 1
        fi
    fi
    
    # Show stats every 10 pushes
    if [ $((run_count % 10)) -eq 0 ]; then
        echo ""
        show_stats
        echo ""
    fi
    
    # Check if we've reached max runs
    if [ $MAX_RUNS -gt 0 ] && [ $run_count -ge $MAX_RUNS ]; then
        echo -e "${GREEN}✅ Completed $MAX_RUNS pushes. Exiting.${NC}"
        break
    fi
    
    # Reset error count on successful push
    if [ $error_count -gt 0 ]; then
        error_count=0
    fi
    
    sleep $INTERVAL
done

echo ""
echo -e "${GREEN}🎉 Final stats:${NC}"
show_stats
echo -e "${GREEN}✅ Real-time log pusher completed successfully!${NC}"
