#!/bin/bash

# =============================================================================
# Advanced Observability Stack for Log Analysis System
# Provides Prometheus, Grafana, Jaeger, and advanced monitoring
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
OBSERVABILITY_NETWORK="${PROJECT_NAME}-observability"

# Print functions
print_status() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_header() { echo -e "${PURPLE}=== $1 ===${NC}"; }

# Create observability stack
deploy_observability() {
    print_header "DEPLOYING OBSERVABILITY STACK"
    
    # Create network
    if ! podman network ls --format "{{.Name}}" | grep -q "^${OBSERVABILITY_NETWORK}$"; then
        podman network create "$OBSERVABILITY_NETWORK"
        print_success "Created observability network"
    fi
    
    # Create configuration directories
    mkdir -p observability/{prometheus,grafana,jaeger}
    
    # Prometheus configuration
    cat > observability/prometheus/prometheus.yml << 'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "alert_rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'log-analyzer-backend'
    static_configs:
      - targets: ['log-analyzer-backend:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s

  - job_name: 'log-analyzer-postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']

  - job_name: 'log-analyzer-redis'
    static_configs:
      - targets: ['redis-exporter:9121']

  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']
EOF
    
    # Alert rules
    cat > observability/prometheus/alert_rules.yml << 'EOF'
groups:
  - name: log-analyzer-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_total{status=~"5.."}[5m]) > 0.1
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} errors per second"

      - alert: HighMemoryUsage
        expr: (jvm_memory_used_bytes / jvm_memory_max_bytes) > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage"
          description: "Memory usage is {{ $value | humanizePercentage }}"

      - alert: DatabaseConnectionFailure
        expr: up{job="log-analyzer-postgres"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Database connection failure"
          description: "PostgreSQL database is not responding"

      - alert: RedisConnectionFailure
        expr: up{job="log-analyzer-redis"} == 0
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "Redis connection failure"
          description: "Redis cache is not responding"
EOF
    
    # Deploy Prometheus
    print_status "Deploying Prometheus..."
    podman run -d \
        --name "${PROJECT_NAME}-prometheus" \
        --network "$OBSERVABILITY_NETWORK" \
        -p "9090:9090" \
        -v "$(pwd)/observability/prometheus:/etc/prometheus:Z" \
        prom/prometheus:latest \
        --config.file=/etc/prometheus/prometheus.yml \
        --storage.tsdb.path=/prometheus \
        --web.console.libraries=/etc/prometheus/console_libraries \
        --web.console.templates=/etc/prometheus/consoles \
        --web.enable-lifecycle
    
    # Deploy Grafana
    print_status "Deploying Grafana..."
    podman run -d \
        --name "${PROJECT_NAME}-grafana" \
        --network "$OBSERVABILITY_NETWORK" \
        -p "3001:3000" \
        -e "GF_SECURITY_ADMIN_PASSWORD=admin123" \
        -v "${PROJECT_NAME}-grafana-data:/var/lib/grafana" \
        grafana/grafana:latest
    
    # Deploy Jaeger (distributed tracing)
    print_status "Deploying Jaeger..."
    podman run -d \
        --name "${PROJECT_NAME}-jaeger" \
        --network "$OBSERVABILITY_NETWORK" \
        -p "16686:16686" \
        -p "14268:14268" \
        -p "14250:14250" \
        jaegertracing/all-in-one:latest
    
    # Deploy Node Exporter
    print_status "Deploying Node Exporter..."
    podman run -d \
        --name "${PROJECT_NAME}-node-exporter" \
        --network "$OBSERVABILITY_NETWORK" \
        -p "9100:9100" \
        --pid="host" \
        -v "/:/host:ro,rslave" \
        prom/node-exporter:latest \
        --path.rootfs=/host
    
    # Deploy Postgres Exporter
    print_status "Deploying Postgres Exporter..."
    podman run -d \
        --name "${PROJECT_NAME}-postgres-exporter" \
        --network "$OBSERVABILITY_NETWORK" \
        -p "9187:9187" \
        -e "DATA_SOURCE_NAME=postgresql://loguser:logpass123@log-analyzer-postgres:5432/loganalyzer?sslmode=disable" \
        prometheuscommunity/postgres-exporter:latest
    
    # Deploy Redis Exporter
    print_status "Deploying Redis Exporter..."
    podman run -d \
        --name "${PROJECT_NAME}-redis-exporter" \
        --network "$OBSERVABILITY_NETWORK" \
        -p "9121:9121" \
        -e "REDIS_ADDR=redis://log-analyzer-redis:6379" \
        oliver006/redis_exporter:latest
    
    # Wait for services to start
    print_status "Waiting for services to start..."
    sleep 30
    
    # Configure Grafana datasources
    configure_grafana_datasources
    
    print_success "Observability stack deployed successfully!"
    show_observability_urls
}

# Configure Grafana datasources
configure_grafana_datasources() {
    print_status "Configuring Grafana datasources..."
    
    # Wait for Grafana to be ready
    for i in {1..30}; do
        if curl -sf "http://admin:admin123@localhost:3001/api/health" >/dev/null 2>&1; then
            break
        fi
        sleep 2
    done
    
    # Add Prometheus datasource
    curl -X POST \
        -H "Content-Type: application/json" \
        -d '{
            "name": "Prometheus",
            "type": "prometheus",
            "url": "http://log-analyzer-prometheus:9090",
            "access": "proxy",
            "isDefault": true
        }' \
        "http://admin:admin123@localhost:3001/api/datasources" >/dev/null 2>&1 || true
    
    # Add Jaeger datasource
    curl -X POST \
        -H "Content-Type: application/json" \
        -d '{
            "name": "Jaeger",
            "type": "jaeger",
            "url": "http://log-analyzer-jaeger:16686",
            "access": "proxy"
        }' \
        "http://admin:admin123@localhost:3001/api/datasources" >/dev/null 2>&1 || true
    
    print_success "Grafana datasources configured"
}

# Create Grafana dashboards
create_dashboards() {
    print_header "CREATING GRAFANA DASHBOARDS"
    
    mkdir -p observability/grafana/dashboards
    
    # Application dashboard
    cat > observability/grafana/dashboards/application-dashboard.json << 'EOF'
{
  "dashboard": {
    "id": null,
    "title": "Log Analysis System - Application Metrics",
    "tags": ["log-analyzer"],
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "HTTP Requests Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_server_requests_total[5m])",
            "legendFormat": "{{method}} {{uri}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0}
      },
      {
        "id": 2,
        "title": "Response Time",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_server_requests_duration_seconds_bucket[5m]))",
            "legendFormat": "95th percentile"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 0}
      },
      {
        "id": 3,
        "title": "JVM Memory Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes",
            "legendFormat": "{{area}}"
          }
        ],
        "gridPos": {"h": 8, "w": 24, "x": 0, "y": 8}
      }
    ],
    "time": {"from": "now-1h", "to": "now"},
    "refresh": "5s"
  }
}
EOF
    
    # Infrastructure dashboard
    cat > observability/grafana/dashboards/infrastructure-dashboard.json << 'EOF'
{
  "dashboard": {
    "id": null,
    "title": "Log Analysis System - Infrastructure Metrics",
    "tags": ["log-analyzer", "infrastructure"],
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "CPU Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "100 - (avg by (instance) (rate(node_cpu_seconds_total{mode=\"idle\"}[5m])) * 100)",
            "legendFormat": "CPU Usage %"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0}
      },
      {
        "id": 2,
        "title": "Memory Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100",
            "legendFormat": "Memory Usage %"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 0}
      },
      {
        "id": 3,
        "title": "Database Connections",
        "type": "graph",
        "targets": [
          {
            "expr": "pg_stat_database_numbackends",
            "legendFormat": "Active Connections"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 8}
      },
      {
        "id": 4,
        "title": "Redis Memory Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "redis_memory_used_bytes",
            "legendFormat": "Redis Memory"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 8}
      }
    ],
    "time": {"from": "now-1h", "to": "now"},
    "refresh": "5s"
  }
}
EOF
    
    print_success "Dashboard templates created"
}

# Show observability URLs
show_observability_urls() {
    print_header "OBSERVABILITY STACK READY"
    
    echo -e "${GREEN}🎉 Observability stack is running!${NC}"
    echo ""
    echo -e "${CYAN}📊 Monitoring URLs:${NC}"
    echo -e "  Prometheus: ${YELLOW}http://localhost:9090${NC}"
    echo -e "  Grafana:    ${YELLOW}http://localhost:3001${NC} (admin/admin123)"
    echo -e "  Jaeger:     ${YELLOW}http://localhost:16686${NC}"
    echo ""
    echo -e "${CYAN}📈 Metrics Endpoints:${NC}"
    echo -e "  Node Exporter:     ${YELLOW}http://localhost:9100${NC}"
    echo -e "  Postgres Exporter: ${YELLOW}http://localhost:9187${NC}"
    echo -e "  Redis Exporter:    ${YELLOW}http://localhost:9121${NC}"
    echo ""
    echo -e "${CYAN}🔧 Next Steps:${NC}"
    echo -e "  1. Import dashboard templates in Grafana"
    echo -e "  2. Configure alerting rules in Prometheus"
    echo -e "  3. Set up notification channels"
    echo -e "  4. Enable distributed tracing in application"
}

# Stop observability stack
stop_observability() {
    print_header "STOPPING OBSERVABILITY STACK"
    
    local services=("prometheus" "grafana" "jaeger" "node-exporter" "postgres-exporter" "redis-exporter")
    
    for service in "${services[@]}"; do
        container_name="${PROJECT_NAME}-${service}"
        if podman ps --format "{{.Names}}" | grep -q "^${container_name}$"; then
            print_status "Stopping $service"
            podman stop "$container_name"
            podman rm "$container_name"
        fi
    done
    
    print_success "Observability stack stopped"
}

# Clean observability stack
clean_observability() {
    print_header "CLEANING OBSERVABILITY STACK"
    
    print_warning "This will remove all monitoring data!"
    read -p "Are you sure? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_status "Clean cancelled"
        return 0
    fi
    
    stop_observability
    
    # Remove volumes
    podman volume rm "${PROJECT_NAME}-grafana-data" 2>/dev/null || true
    
    # Remove network
    podman network rm "$OBSERVABILITY_NETWORK" 2>/dev/null || true
    
    # Remove configuration files
    rm -rf observability/
    
    print_success "Observability stack cleaned"
}

# Main command handler
case "${1:-help}" in
    "deploy")
        deploy_observability
        create_dashboards
        ;;
    "dashboards")
        create_dashboards
        ;;
    "status")
        show_observability_urls
        ;;
    "stop")
        stop_observability
        ;;
    "clean")
        clean_observability
        ;;
    "help"|"-h"|"--help")
        echo "Advanced Observability Stack for Log Analysis System"
        echo ""
        echo "Usage: $0 [command]"
        echo ""
        echo "Commands:"
        echo "  deploy      - Deploy complete observability stack"
        echo "  dashboards  - Create Grafana dashboard templates"
        echo "  status      - Show observability URLs and status"
        echo "  stop        - Stop observability stack"
        echo "  clean       - Clean observability stack (removes data)"
        echo "  help        - Show this help"
        echo ""
        echo "Stack includes:"
        echo "  - Prometheus (metrics collection)"
        echo "  - Grafana (visualization)"
        echo "  - Jaeger (distributed tracing)"
        echo "  - Node Exporter (system metrics)"
        echo "  - Postgres Exporter (database metrics)"
        echo "  - Redis Exporter (cache metrics)"
        echo ""
        echo "Examples:"
        echo "  $0 deploy       # Deploy full observability stack"
        echo "  $0 status       # Show access URLs"
        echo "  $0 dashboards   # Create dashboard templates"
        ;;
    *)
        print_error "Unknown command: $1"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac
