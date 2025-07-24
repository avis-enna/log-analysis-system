#!/bin/bash

# =============================================================================
# Database Management Tools for Log Analysis System
# Provides backup, restore, migration, and maintenance utilities
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
BACKUP_DIR="./backups"
DB_NAME="loganalyzer"
DB_USER="loguser"

# Print functions
print_status() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_header() { echo -e "${PURPLE}=== $1 ===${NC}"; }

# Create backup directory
ensure_backup_dir() {
    if [[ ! -d "$BACKUP_DIR" ]]; then
        mkdir -p "$BACKUP_DIR"
        print_status "Created backup directory: $BACKUP_DIR"
    fi
}

# Database backup
backup_database() {
    print_header "DATABASE BACKUP"
    
    ensure_backup_dir
    
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local backup_file="$BACKUP_DIR/postgres_backup_$timestamp.sql"
    
    if ! podman ps --format "{{.Names}}" | grep -q "^${PROJECT_NAME}-postgres$"; then
        print_error "PostgreSQL container is not running"
        return 1
    fi
    
    print_status "Creating database backup..."
    podman exec "${PROJECT_NAME}-postgres" pg_dump -U "$DB_USER" -d "$DB_NAME" > "$backup_file"
    
    # Compress backup
    gzip "$backup_file"
    backup_file="${backup_file}.gz"
    
    local size=$(du -h "$backup_file" | cut -f1)
    print_success "Backup created: $backup_file ($size)"
    
    # List recent backups
    echo ""
    echo -e "${CYAN}📁 Recent Backups:${NC}"
    ls -lh "$BACKUP_DIR"/postgres_backup_*.sql.gz 2>/dev/null | tail -5 || echo "No backups found"
}

# Database restore
restore_database() {
    print_header "DATABASE RESTORE"
    
    local backup_file="$1"
    
    if [[ -z "$backup_file" ]]; then
        print_error "Please specify backup file"
        echo "Available backups:"
        ls -1 "$BACKUP_DIR"/postgres_backup_*.sql.gz 2>/dev/null || echo "No backups found"
        return 1
    fi
    
    if [[ ! -f "$backup_file" ]]; then
        print_error "Backup file not found: $backup_file"
        return 1
    fi
    
    if ! podman ps --format "{{.Names}}" | grep -q "^${PROJECT_NAME}-postgres$"; then
        print_error "PostgreSQL container is not running"
        return 1
    fi
    
    print_warning "This will overwrite the current database!"
    read -p "Are you sure? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_status "Restore cancelled"
        return 0
    fi
    
    print_status "Restoring database from: $backup_file"
    
    # Drop and recreate database
    podman exec "${PROJECT_NAME}-postgres" psql -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;"
    podman exec "${PROJECT_NAME}-postgres" psql -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME;"
    
    # Restore data
    if [[ "$backup_file" == *.gz ]]; then
        zcat "$backup_file" | podman exec -i "${PROJECT_NAME}-postgres" psql -U "$DB_USER" -d "$DB_NAME"
    else
        cat "$backup_file" | podman exec -i "${PROJECT_NAME}-postgres" psql -U "$DB_USER" -d "$DB_NAME"
    fi
    
    print_success "Database restored successfully"
}

# Database statistics
database_stats() {
    print_header "DATABASE STATISTICS"
    
    if ! podman ps --format "{{.Names}}" | grep -q "^${PROJECT_NAME}-postgres$"; then
        print_error "PostgreSQL container is not running"
        return 1
    fi
    
    echo -e "${CYAN}📊 Database Overview:${NC}"
    podman exec "${PROJECT_NAME}-postgres" psql -U "$DB_USER" -d "$DB_NAME" -c "
        SELECT 
            schemaname,
            tablename,
            n_tup_ins as inserts,
            n_tup_upd as updates,
            n_tup_del as deletes,
            n_live_tup as live_rows,
            n_dead_tup as dead_rows
        FROM pg_stat_user_tables 
        ORDER BY n_live_tup DESC;
    "
    
    echo ""
    echo -e "${CYAN}💾 Database Size:${NC}"
    podman exec "${PROJECT_NAME}-postgres" psql -U "$DB_USER" -d "$DB_NAME" -c "
        SELECT 
            pg_database.datname,
            pg_size_pretty(pg_database_size(pg_database.datname)) AS size
        FROM pg_database
        WHERE datname = '$DB_NAME';
    "
    
    echo ""
    echo -e "${CYAN}🔍 Table Sizes:${NC}"
    podman exec "${PROJECT_NAME}-postgres" psql -U "$DB_USER" -d "$DB_NAME" -c "
        SELECT 
            tablename,
            pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
        FROM pg_tables 
        WHERE schemaname = 'public'
        ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
    "
}

# Database maintenance
database_maintenance() {
    print_header "DATABASE MAINTENANCE"
    
    if ! podman ps --format "{{.Names}}" | grep -q "^${PROJECT_NAME}-postgres$"; then
        print_error "PostgreSQL container is not running"
        return 1
    fi
    
    print_status "Running database maintenance..."
    
    # Vacuum and analyze
    print_status "Running VACUUM ANALYZE..."
    podman exec "${PROJECT_NAME}-postgres" psql -U "$DB_USER" -d "$DB_NAME" -c "VACUUM ANALYZE;"
    
    # Update statistics
    print_status "Updating table statistics..."
    podman exec "${PROJECT_NAME}-postgres" psql -U "$DB_USER" -d "$DB_NAME" -c "ANALYZE;"
    
    # Check for dead tuples
    echo ""
    echo -e "${CYAN}🧹 Dead Tuples Check:${NC}"
    podman exec "${PROJECT_NAME}-postgres" psql -U "$DB_USER" -d "$DB_NAME" -c "
        SELECT 
            tablename,
            n_dead_tup as dead_tuples,
            n_live_tup as live_tuples,
            CASE 
                WHEN n_live_tup > 0 THEN round((n_dead_tup::float / n_live_tup::float) * 100, 2)
                ELSE 0 
            END as dead_percentage
        FROM pg_stat_user_tables 
        WHERE n_dead_tup > 0
        ORDER BY dead_percentage DESC;
    "
    
    print_success "Database maintenance completed"
}

# Redis operations
redis_operations() {
    print_header "REDIS OPERATIONS"
    
    local operation="$1"
    
    if ! podman ps --format "{{.Names}}" | grep -q "^${PROJECT_NAME}-redis$"; then
        print_error "Redis container is not running"
        return 1
    fi
    
    case "$operation" in
        "info")
            echo -e "${CYAN}📊 Redis Information:${NC}"
            podman exec "${PROJECT_NAME}-redis" redis-cli info
            ;;
        "stats")
            echo -e "${CYAN}📈 Redis Statistics:${NC}"
            podman exec "${PROJECT_NAME}-redis" redis-cli info stats
            ;;
        "memory")
            echo -e "${CYAN}💾 Redis Memory Usage:${NC}"
            podman exec "${PROJECT_NAME}-redis" redis-cli info memory
            ;;
        "keys")
            echo -e "${CYAN}🔑 Redis Keys:${NC}"
            podman exec "${PROJECT_NAME}-redis" redis-cli keys "*"
            ;;
        "flush")
            print_warning "This will delete all Redis data!"
            read -p "Are you sure? (y/N): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                podman exec "${PROJECT_NAME}-redis" redis-cli flushall
                print_success "Redis cache cleared"
            else
                print_status "Operation cancelled"
            fi
            ;;
        *)
            print_error "Unknown Redis operation: $operation"
            echo "Available operations: info, stats, memory, keys, flush"
            return 1
            ;;
    esac
}

# Data migration
migrate_data() {
    print_header "DATA MIGRATION"
    
    local action="$1"
    
    case "$action" in
        "export")
            print_status "Exporting data to JSON..."
            ensure_backup_dir
            
            local timestamp=$(date +%Y%m%d_%H%M%S)
            local export_file="$BACKUP_DIR/data_export_$timestamp.json"
            
            # Export log entries (example)
            podman exec "${PROJECT_NAME}-postgres" psql -U "$DB_USER" -d "$DB_NAME" -c "
                COPY (
                    SELECT row_to_json(t) FROM (
                        SELECT * FROM log_entries ORDER BY timestamp DESC LIMIT 1000
                    ) t
                ) TO STDOUT;
            " > "$export_file"
            
            print_success "Data exported to: $export_file"
            ;;
        "import")
            local import_file="$2"
            if [[ -z "$import_file" || ! -f "$import_file" ]]; then
                print_error "Please specify a valid import file"
                return 1
            fi
            
            print_status "Importing data from: $import_file"
            # Implementation depends on data format
            print_warning "Import functionality needs to be implemented based on your data format"
            ;;
        *)
            print_error "Unknown migration action: $action"
            echo "Available actions: export, import"
            return 1
            ;;
    esac
}

# Main command handler
case "${1:-help}" in
    "backup")
        backup_database
        ;;
    "restore")
        restore_database "$2"
        ;;
    "stats")
        database_stats
        ;;
    "maintenance"|"maintain")
        database_maintenance
        ;;
    "redis")
        redis_operations "$2"
        ;;
    "migrate")
        migrate_data "$2" "$3"
        ;;
    "help"|"-h"|"--help")
        echo "Database Management Tools for Log Analysis System"
        echo ""
        echo "Usage: $0 [command] [options]"
        echo ""
        echo "Commands:"
        echo "  backup                    - Create database backup"
        echo "  restore <backup_file>     - Restore database from backup"
        echo "  stats                     - Show database statistics"
        echo "  maintenance               - Run database maintenance"
        echo "  redis <operation>         - Redis operations (info|stats|memory|keys|flush)"
        echo "  migrate <action> [file]   - Data migration (export|import)"
        echo "  help                      - Show this help"
        echo ""
        echo "Examples:"
        echo "  $0 backup                           # Create backup"
        echo "  $0 restore backups/postgres_backup_20231201_120000.sql.gz"
        echo "  $0 stats                            # Show database statistics"
        echo "  $0 redis info                       # Show Redis info"
        echo "  $0 migrate export                   # Export data to JSON"
        ;;
    *)
        print_error "Unknown command: $1"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac
