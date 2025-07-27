#!/bin/bash

# Real-time Log Data Pusher - Simplified Version
# Continuously generates and pushes log data to the log analysis system
# Usage: ./simple-log-pusher.sh [interval_seconds] [batch_size] [max_runs]

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
SOURCES=("web-server" "database" "api-gateway" "auth-service" "cache-service")
APPLICATIONS=("frontend" "backend" "worker" "scheduler" "monitor")
ENVIRONMENTS=("production" "staging" "development")
HOSTS=("web-01" "web-02" "db-master" "cache-01" "api-01")

# Simple message templates
INFO_MESSAGES=(
    "User session created successfully"
    "Database connection established"
    "API request processed successfully"
    "File upload completed"
    "Email sent successfully"
    "Health check passed"
    "Configuration reloaded"
    "Service startup completed"
)

WARN_MESSAGES=(
    "High memory usage detected"
    "Slow database query detected"
    "Rate limit approaching"
    "Disk space warning"
    "SSL certificate expires soon"
    "Connection pool near capacity"
    "Cache miss rate high"
    "Response time degraded"
)

ERROR_MESSAGES=(
    "Database connection failed"
    "Authentication service unavailable"
    "File not found"
    "Out of memory error"
    "Network unreachable"
    "Invalid API token provided"
    "Service crash detected"
    "Permission denied"
)

DEBUG_MESSAGES=(
    "Processing request"
    "Cache key generated"
    "SQL query executed"
    "Method entry point"
    "Variable state updated"
    "Thread pool statistics"
    "Memory usage statistics"
    "Connection statistics"
)

# Function to get random array element
get_random_element() {
    local arr=("$@")
    echo "${arr[RANDOM % ${#arr[@]}]}"
}

# Function to get weighted random level (more INFO, fewer ERROR)
get_weighted_level() {
    local rand=$((RANDOM % 100))
    if [ $rand -lt 60 ]; then
        echo "INFO"
    elif [ $rand -lt 85 ]; then
        echo "WARN"
    elif [ $rand -lt 95 ]; then
        echo "ERROR"
    else
        echo "DEBUG"
    fi
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
    local thread="thread-$((RANDOM % 10 + 1))"
    local logger="com.loganalyzer.service.${source}"
    
    # Select message based on level
    local message
    case $level in
        "INFO")
            message=$(get_random_element "${INFO_MESSAGES[@]}")
            ;;
        "WARN")
            message=$(get_random_element "${WARN_MESSAGES[@]}")
            ;;
        "ERROR")
            message=$(get_random_element "${ERROR_MESSAGES[@]}")
            ;;
        "DEBUG")
            message=$(get_random_element "${DEBUG_MESSAGES[@]}")
            ;;
    esac
    
    # Add some randomness to messages
    local id=$((RANDOM % 1000 + 1))
    message="${message} (ID: ${id})"
    
    # Format log entry
    echo "${timestamp} [${thread}] ${level} ${logger} - ${message} [source=${source}, app=${application}, env=${environment}, host=${host}]"
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

# Function to escape JSON string
escape_json() {
    echo "$1" | sed 's/\\/\\\\/g; s/"/\\"/g; s/\x0A/\\n/g; s/\x0D/\\r/g; s/\x09/\\t/g'
}

# Function to push log batch
push_log_batch() {
    local logs=()
    
    # Generate batch of logs
    for ((i=1; i<=BATCH_SIZE; i++)); do
        local log_entry=$(generate_log_entry)
        local escaped_log=$(escape_json "$log_entry")
        logs+=("\"$escaped_log\"")
    done
    
    # Create JSON payload
    local logs_json=$(IFS=','; echo "${logs[*]}")
    local json_payload="{\"logs\":[${logs_json}],\"source\":\"realtime-generator\"}"
    
    # Push to backend
    local temp_file=$(mktemp)
    local status_code=$(curl -s -X POST "${BACKEND_URL}/logs/ingest/batch" \
        -H "Content-Type: application/json" \
        -d "$json_payload" \
        -w "%{http_code}" \
        -o "$temp_file")
    
    local response_body=$(cat "$temp_file")
    rm "$temp_file"
    
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
        if command -v jq >/dev/null 2>&1; then
            echo "$stats" | jq -r '. | "📈 Total Logs: \(.totalLogs)\n🚨 Total Errors: \(.totalErrors)\n⚠️  Total Warnings: \(.totalWarnings)\n💡 Total Info: \(.totalInfo)\n🔧 Error Rate: \(.errorRate)%"' 2>/dev/null || echo "$stats"
        else
            echo "$stats"
        fi
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
        error_count=0  # Reset error count on success
    else
        error_count=$((error_count + 1))
        echo -e "${RED}   ✗ Batch failed (consecutive errors: $error_count)${NC}"
        
        # Exit if too many consecutive errors
        if [ $error_count -ge 5 ]; then
            echo -e "${RED}❌ Too many consecutive errors. Exiting.${NC}"
            exit 1
        fi
    fi
    
    # Show stats every 5 pushes
    if [ $((run_count % 5)) -eq 0 ]; then
        echo ""
        show_stats
        echo ""
    fi
    
    # Check if we've reached max runs
    if [ $MAX_RUNS -gt 0 ] && [ $run_count -ge $MAX_RUNS ]; then
        echo -e "${GREEN}✅ Completed $MAX_RUNS pushes. Exiting.${NC}"
        break
    fi
    
    sleep $INTERVAL
done

echo ""
echo -e "${GREEN}🎉 Final stats:${NC}"
show_stats
echo -e "${GREEN}✅ Real-time log pusher completed successfully!${NC}"
