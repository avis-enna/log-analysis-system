#!/bin/bash

# Log Analysis System - Log Generator Script
# Generates sample log files for testing the log analysis system

LOG_FILE="sample-logs-$(date +%Y%m%d-%H%M%S).log"
LINES=${1:-100}  # Default to 100 lines if not specified

echo "Generating $LINES log entries in file: $LOG_FILE"

# Arrays for log generation
LEVELS=("INFO" "WARNING" "ERROR" "DEBUG" "CRITICAL")
SOURCES=("nginx" "application" "database" "security" "firewall" "auth-service" "mail-server" "api-gateway")
HOSTS=("web-01" "web-02" "db-master" "db-slave" "cache-01" "lb-01" "auth-01" "api-01")
CATEGORIES=("authentication" "network" "application" "security" "system" "database" "web" "email")

# Message templates for different log types
INFO_MESSAGES=(
    "User %s logged in successfully"
    "Database connection established"
    "Cache cleared successfully"
    "Service restart completed"
    "File upload completed"
    "Backup process initiated"
    "Configuration reloaded"
    "Health check passed"
    "Session created for user %s"
    "API request processed successfully"
)

WARNING_MESSAGES=(
    "High memory usage detected: %d%%"
    "Slow database query: %d ms"
    "Failed authentication attempt for user %s"
    "Disk space running low: %d%% used"
    "Connection timeout occurred"
    "Rate limit approaching for IP %s"
    "SSL certificate expires in %d days"
    "Unusual login pattern detected for user %s"
)

ERROR_MESSAGES=(
    "Database connection failed"
    "Authentication service unavailable"
    "File not found: %s"
    "Memory allocation error"
    "Network unreachable"
    "Invalid configuration parameter"
    "Service crashed and restarted"
    "Permission denied for user %s"
    "Timeout waiting for response"
    "Failed to parse configuration file"
)

CRITICAL_MESSAGES=(
    "Security breach detected from IP %s"
    "System compromise attempt"
    "Multiple failed login attempts from %s"
    "Unauthorized access to sensitive data"
    "Service completely unavailable"
    "Data corruption detected"
    "Emergency shutdown initiated"
    "Critical security policy violation"
)

DEBUG_MESSAGES=(
    "Function %s called with parameter %d"
    "Processing request ID: %s"
    "Cache hit for key: %s"
    "Thread %d started processing"
    "Validation passed for input: %s"
    "Query executed in %d ms"
    "Memory usage: %d MB"
    "Connection pool size: %d"
)

# Function to get random element from array
get_random() {
    local arr=("$@")
    echo "${arr[$RANDOM % ${#arr[@]}]}"
}

# Function to generate random IP
random_ip() {
    echo "$((RANDOM % 256)).$((RANDOM % 256)).$((RANDOM % 256)).$((RANDOM % 256))"
}

# Function to generate random username
random_user() {
    local users=("alice" "bob" "charlie" "diana" "eve" "frank" "grace" "henry" "iris" "jack")
    echo "${users[$RANDOM % ${#users[@]}]}"
}

# Function to generate log entry
generate_log_entry() {
    local timestamp=$(date -d "-$((RANDOM % 3600)) seconds" '+%Y-%m-%d %H:%M:%S')
    local level=$(get_random "${LEVELS[@]}")
    local source=$(get_random "${SOURCES[@]}")
    local host=$(get_random "${HOSTS[@]}")
    local category=$(get_random "${CATEGORIES[@]}")
    
    local message=""
    case $level in
        "INFO")
            local template=$(get_random "${INFO_MESSAGES[@]}")
            if [[ $template == *"%s"* ]]; then
                message=$(printf "$template" "$(random_user)")
            elif [[ $template == *"%d"* ]]; then
                message=$(printf "$template" "$((RANDOM % 100 + 1))")
            else
                message="$template"
            fi
            ;;
        "WARNING")
            local template=$(get_random "${WARNING_MESSAGES[@]}")
            if [[ $template == *"%s"* ]]; then
                if [[ $template == *"IP"* ]]; then
                    message=$(printf "$template" "$(random_ip)")
                else
                    message=$(printf "$template" "$(random_user)")
                fi
            elif [[ $template == *"%d"* ]]; then
                message=$(printf "$template" "$((RANDOM % 100 + 1))")
            else
                message="$template"
            fi
            ;;
        "ERROR")
            local template=$(get_random "${ERROR_MESSAGES[@]}")
            if [[ $template == *"%s"* ]]; then
                if [[ $template == *"user"* ]]; then
                    message=$(printf "$template" "$(random_user)")
                else
                    message=$(printf "$template" "/var/log/application.log")
                fi
            else
                message="$template"
            fi
            ;;
        "CRITICAL")
            local template=$(get_random "${CRITICAL_MESSAGES[@]}")
            if [[ $template == *"%s"* ]]; then
                message=$(printf "$template" "$(random_ip)")
            else
                message="$template"
            fi
            ;;
        "DEBUG")
            local template=$(get_random "${DEBUG_MESSAGES[@]}")
            if [[ $template == *"%s"* ]]; then
                if [[ $template == *"Function"* ]]; then
                    local funcs=("authenticate" "validate" "process" "calculate" "transform")
                    message=$(printf "$template" "${funcs[$RANDOM % ${#funcs[@]}]}" "$((RANDOM % 100))")
                elif [[ $template == *"ID"* ]]; then
                    message=$(printf "$template" "req-$((RANDOM % 10000))")
                elif [[ $template == *"key"* ]]; then
                    message=$(printf "$template" "cache-key-$((RANDOM % 1000))")
                else
                    message=$(printf "$template" "input-$((RANDOM % 100))")
                fi
            elif [[ $template == *"%d"* ]]; then
                if [[ $template == *"Thread"* ]]; then
                    message=$(printf "$template" "$((RANDOM % 10 + 1))")
                elif [[ $template == *"Memory"* ]]; then
                    message=$(printf "$template" "$((RANDOM % 1024 + 512))")
                elif [[ $template == *"pool"* ]]; then
                    message=$(printf "$template" "$((RANDOM % 50 + 10))")
                else
                    message=$(printf "$template" "$((RANDOM % 1000 + 1))")
                fi
            else
                message="$template"
            fi
            ;;
    esac
    
    # Output log entry in common log format
    echo "[$timestamp] [$level] [$source] [$host] [$category] $message"
}

# Generate log file
echo "# Generated log file - $(date)" > "$LOG_FILE"
echo "# Lines: $LINES" >> "$LOG_FILE"
echo "# Format: [timestamp] [level] [source] [host] [category] message" >> "$LOG_FILE"
echo "" >> "$LOG_FILE"

for ((i=1; i<=LINES; i++)); do
    generate_log_entry >> "$LOG_FILE"
    if (( i % 50 == 0 )); then
        echo "Generated $i/$LINES lines..."
    fi
done

echo ""
echo "✅ Log file generated successfully: $LOG_FILE"
echo "📊 Total lines: $LINES"
echo "📁 File size: $(du -h "$LOG_FILE" | cut -f1)"
echo ""
echo "Usage examples:"
echo "  Upload via dashboard: http://localhost:8082"
echo "  View file: cat $LOG_FILE | head -20"
echo "  Generate more logs: ./generate-logs.sh 500"
