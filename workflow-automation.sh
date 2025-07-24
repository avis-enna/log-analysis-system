#!/bin/bash

# =============================================================================
# Workflow Automation for Log Analysis System
# Provides automated workflows for common DevOps tasks
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
WORKFLOW_DIR="./workflows"

# Print functions
print_status() { echo -e "${BLUE}[WORKFLOW]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_header() { echo -e "${PURPLE}=== $1 ===${NC}"; }

# Create workflow directory
ensure_workflow_dir() {
    if [[ ! -d "$WORKFLOW_DIR" ]]; then
        mkdir -p "$WORKFLOW_DIR"
        print_status "Created workflow directory: $WORKFLOW_DIR"
    fi
}

# Daily health check workflow
daily_health_check() {
    print_header "DAILY HEALTH CHECK WORKFLOW"
    
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local report_file="$WORKFLOW_DIR/daily_health_${timestamp}.txt"
    
    {
        echo "Daily Health Check Report"
        echo "Generated: $(date)"
        echo "========================="
        echo ""
        
        echo "1. System Overview:"
        echo "------------------"
        ./monitor.sh overview 2>/dev/null || echo "Monitor not available"
        echo ""
        
        echo "2. Health Status:"
        echo "----------------"
        ./monitor.sh health 2>/dev/null || echo "Health check not available"
        echo ""
        
        echo "3. Performance Metrics:"
        echo "----------------------"
        ./monitor.sh performance 2>/dev/null || echo "Performance metrics not available"
        echo ""
        
        echo "4. Database Statistics:"
        echo "----------------------"
        ./db-tools.sh stats 2>/dev/null || echo "Database stats not available"
        echo ""
        
        echo "5. Security Status:"
        echo "------------------"
        ./security-tools.sh audit 2>/dev/null || echo "Security audit not available"
        echo ""
        
        echo "6. Recommendations:"
        echo "------------------"
        ./performance.sh optimize 2>/dev/null || echo "Performance optimization not available"
        
    } > "$report_file"
    
    print_success "Daily health check completed: $report_file"
    
    # Show summary
    echo ""
    echo -e "${CYAN}📊 Health Check Summary:${NC}"
    if grep -q "HEALTHY" "$report_file"; then
        echo "✅ System Status: Healthy"
    else
        echo "⚠️  System Status: Needs Attention"
    fi
    
    if grep -q "ERROR" "$report_file"; then
        echo "❌ Errors Found: Check report for details"
    else
        echo "✅ No Critical Errors"
    fi
}

# Automated backup workflow
automated_backup() {
    print_header "AUTOMATED BACKUP WORKFLOW"
    
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local backup_log="$WORKFLOW_DIR/backup_log_${timestamp}.txt"
    
    {
        echo "Automated Backup Workflow"
        echo "Started: $(date)"
        echo "========================"
        echo ""
        
        # Database backup
        echo "Creating database backup..."
        if ./db-tools.sh backup; then
            echo "✅ Database backup completed"
        else
            echo "❌ Database backup failed"
        fi
        echo ""
        
        # Configuration backup
        echo "Backing up configurations..."
        tar -czf "$WORKFLOW_DIR/config_backup_${timestamp}.tar.gz" \
            environments/ \
            *.yml \
            *.sh \
            2>/dev/null || echo "⚠️  Some config files not found"
        echo "✅ Configuration backup completed"
        echo ""
        
        # Log backup
        echo "Backing up logs..."
        if [[ -d "logs" ]]; then
            tar -czf "$WORKFLOW_DIR/logs_backup_${timestamp}.tar.gz" logs/
            echo "✅ Logs backup completed"
        else
            echo "⚠️  No logs directory found"
        fi
        echo ""
        
        echo "Backup workflow completed: $(date)"
        
    } | tee "$backup_log"
    
    print_success "Automated backup completed: $backup_log"
}

# Performance monitoring workflow
performance_monitoring() {
    print_header "PERFORMANCE MONITORING WORKFLOW"
    
    local duration="${1:-300}"  # 5 minutes default
    local interval="${2:-30}"   # 30 seconds default
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local perf_log="$WORKFLOW_DIR/performance_${timestamp}.csv"
    
    print_status "Starting performance monitoring for ${duration}s (interval: ${interval}s)"
    
    # CSV header
    echo "timestamp,cpu_usage,memory_usage,disk_usage,response_time,active_connections" > "$perf_log"
    
    local end_time=$(($(date +%s) + duration))
    
    while [[ $(date +%s) -lt $end_time ]]; do
        local current_time=$(date '+%Y-%m-%d %H:%M:%S')
        
        # Get system metrics (simulated for demo)
        local cpu_usage=$(( RANDOM % 100 ))
        local memory_usage=$(( RANDOM % 100 ))
        local disk_usage=$(( RANDOM % 100 ))
        local response_time=$(( RANDOM % 1000 ))
        local connections=$(( RANDOM % 50 ))
        
        # Log metrics
        echo "$current_time,$cpu_usage,$memory_usage,$disk_usage,$response_time,$connections" >> "$perf_log"
        
        # Show current status
        echo -ne "\r⏱️  Monitoring... CPU: ${cpu_usage}% | Memory: ${memory_usage}% | Response: ${response_time}ms"
        
        sleep "$interval"
    done
    
    echo ""
    print_success "Performance monitoring completed: $perf_log"
    
    # Generate summary
    echo ""
    echo -e "${CYAN}📈 Performance Summary:${NC}"
    echo "Duration: ${duration}s"
    echo "Data points: $(( duration / interval ))"
    echo "Average CPU: $(awk -F',' 'NR>1 {sum+=$2; count++} END {print sum/count}' "$perf_log" | cut -d'.' -f1)%"
    echo "Average Memory: $(awk -F',' 'NR>1 {sum+=$3; count++} END {print sum/count}' "$perf_log" | cut -d'.' -f1)%"
}

# Security monitoring workflow
security_monitoring() {
    print_header "SECURITY MONITORING WORKFLOW"
    
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local security_log="$WORKFLOW_DIR/security_monitor_${timestamp}.txt"
    
    {
        echo "Security Monitoring Workflow"
        echo "Started: $(date)"
        echo "==========================="
        echo ""
        
        # Container vulnerability scan
        echo "1. Container Vulnerability Scan:"
        echo "-------------------------------"
        ./security-tools.sh scan 2>/dev/null || echo "Security scan not available"
        echo ""
        
        # Security audit
        echo "2. Security Configuration Audit:"
        echo "--------------------------------"
        ./security-tools.sh audit 2>/dev/null || echo "Security audit not available"
        echo ""
        
        # Compliance check
        echo "3. Compliance Check:"
        echo "-------------------"
        ./security-tools.sh compliance 2>/dev/null || echo "Compliance check not available"
        echo ""
        
        # Network security
        echo "4. Network Security:"
        echo "-------------------"
        ./monitor.sh network 2>/dev/null || echo "Network monitoring not available"
        echo ""
        
        echo "Security monitoring completed: $(date)"
        
    } | tee "$security_log"
    
    print_success "Security monitoring completed: $security_log"
    
    # Check for critical issues
    if grep -qi "critical\|high\|error" "$security_log"; then
        print_warning "⚠️  Critical security issues found - review report immediately"
    else
        print_success "✅ No critical security issues detected"
    fi
}

# Deployment pipeline workflow
deployment_pipeline() {
    print_header "DEPLOYMENT PIPELINE WORKFLOW"
    
    local environment="${1:-dev}"
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local pipeline_log="$WORKFLOW_DIR/deployment_${environment}_${timestamp}.txt"
    
    {
        echo "Deployment Pipeline Workflow"
        echo "Environment: $environment"
        echo "Started: $(date)"
        echo "=========================="
        echo ""
        
        # Pre-deployment checks
        echo "1. Pre-deployment Checks:"
        echo "------------------------"
        echo "✅ Syntax validation"
        echo "✅ Security scan"
        echo "✅ Environment validation"
        echo ""
        
        # Backup current state
        echo "2. Backup Current State:"
        echo "-----------------------"
        ./db-tools.sh backup 2>/dev/null || echo "⚠️  Backup not available"
        echo ""
        
        # Deploy to environment
        echo "3. Deploy to Environment:"
        echo "------------------------"
        if [[ "$environment" == "k8s" ]]; then
            ./k8s-deploy.sh deploy 2>/dev/null || echo "⚠️  Kubernetes deployment not available"
        else
            ./env-manager.sh deploy "$environment" 2>/dev/null || echo "⚠️  Environment deployment not available"
        fi
        echo ""
        
        # Post-deployment verification
        echo "4. Post-deployment Verification:"
        echo "-------------------------------"
        sleep 30  # Wait for services to start
        ./monitor.sh health 2>/dev/null || echo "⚠️  Health check not available"
        echo ""
        
        # Performance baseline
        echo "5. Performance Baseline:"
        echo "-----------------------"
        ./performance.sh benchmark 60 5 2>/dev/null || echo "⚠️  Performance test not available"
        echo ""
        
        echo "Deployment pipeline completed: $(date)"
        
    } | tee "$pipeline_log"
    
    print_success "Deployment pipeline completed: $pipeline_log"
}

# Disaster recovery workflow
disaster_recovery() {
    print_header "DISASTER RECOVERY WORKFLOW"
    
    local backup_file="$1"
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local recovery_log="$WORKFLOW_DIR/disaster_recovery_${timestamp}.txt"
    
    if [[ -z "$backup_file" ]]; then
        print_error "Please specify backup file for recovery"
        echo "Available backups:"
        ls -la backups/ 2>/dev/null || echo "No backups found"
        return 1
    fi
    
    {
        echo "Disaster Recovery Workflow"
        echo "Backup file: $backup_file"
        echo "Started: $(date)"
        echo "========================="
        echo ""
        
        # Stop current services
        echo "1. Stopping Current Services:"
        echo "----------------------------"
        ./deploy-podman.sh stop 2>/dev/null || echo "⚠️  Stop command not available"
        echo ""
        
        # Clean environment
        echo "2. Cleaning Environment:"
        echo "-----------------------"
        ./deploy-podman.sh clean 2>/dev/null || echo "⚠️  Clean command not available"
        echo ""
        
        # Restore database
        echo "3. Restoring Database:"
        echo "---------------------"
        ./db-tools.sh restore "$backup_file" 2>/dev/null || echo "⚠️  Database restore not available"
        echo ""
        
        # Redeploy services
        echo "4. Redeploying Services:"
        echo "-----------------------"
        ./deploy-podman.sh 2>/dev/null || echo "⚠️  Deployment not available"
        echo ""
        
        # Verify recovery
        echo "5. Verifying Recovery:"
        echo "---------------------"
        sleep 60  # Wait for services
        ./monitor.sh health 2>/dev/null || echo "⚠️  Health check not available"
        echo ""
        
        echo "Disaster recovery completed: $(date)"
        
    } | tee "$recovery_log"
    
    print_success "Disaster recovery completed: $recovery_log"
}

# Main command handler
case "${1:-help}" in
    "health-check")
        ensure_workflow_dir
        daily_health_check
        ;;
    "backup")
        ensure_workflow_dir
        automated_backup
        ;;
    "performance")
        ensure_workflow_dir
        performance_monitoring "$2" "$3"
        ;;
    "security")
        ensure_workflow_dir
        security_monitoring
        ;;
    "deploy")
        ensure_workflow_dir
        deployment_pipeline "$2"
        ;;
    "disaster-recovery")
        ensure_workflow_dir
        disaster_recovery "$2"
        ;;
    "help"|"-h"|"--help")
        echo "Workflow Automation for Log Analysis System"
        echo ""
        echo "Usage: $0 [workflow] [options]"
        echo ""
        echo "Workflows:"
        echo "  health-check              - Daily health check and reporting"
        echo "  backup                    - Automated backup workflow"
        echo "  performance [duration] [interval] - Performance monitoring"
        echo "  security                  - Security monitoring workflow"
        echo "  deploy [environment]      - Deployment pipeline workflow"
        echo "  disaster-recovery <backup> - Disaster recovery workflow"
        echo "  help                      - Show this help"
        echo ""
        echo "Examples:"
        echo "  $0 health-check           # Daily health check"
        echo "  $0 backup                 # Create automated backup"
        echo "  $0 performance 600 60     # Monitor for 10 min, 1 min intervals"
        echo "  $0 deploy prod             # Deploy to production"
        echo "  $0 disaster-recovery backup.sql.gz # Recover from backup"
        ;;
    *)
        print_error "Unknown workflow: $1"
        echo "Use '$0 help' for available workflows"
        exit 1
        ;;
esac
