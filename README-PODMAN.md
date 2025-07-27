# Log Analysis System - Podman Setup

This guide helps you run the Log Analysis System using **Podman** instead of Docker.

## Prerequisites

1. **Podman** - Container engine
   ```bash
   # macOS (using Homebrew)
   brew install podman
   
   # Linux (Ubuntu/Debian)
   sudo apt-get update
   sudo apt-get install podman
   
   # Linux (RHEL/CentOS/Fedora)
   sudo dnf install podman
   ```

2. **podman-compose** - Docker Compose compatibility
   ```bash
   pip3 install podman-compose
   ```

## Quick Start

### 1. Initial Setup
```bash
./setup-podman.sh
```
This script will:
- Check Podman and podman-compose installation
- Initialize Podman machine (on macOS)
- Create necessary volumes and networks
- Set up directories

### 2. Start Services
```bash
./start-podman.sh
```
This will start all services including:
- PostgreSQL (port 5432)
- Redis (port 6379)
- Elasticsearch (port 9200)
- Kafka + Zookeeper (ports 9092, 2181)
- Application (port 8081)
- Grafana (port 4000)
- Prometheus (port 9090)

### 3. Test the System
```bash
./test-podman.sh
```
Runs comprehensive tests to verify all components are working.

## Available Scripts

| Script | Purpose |
|--------|---------|
| `./setup-podman.sh` | Initial setup and configuration |
| `./start-podman.sh` | Start all services |
| `./stop-podman.sh` | Stop all services |
| `./status-podman.sh` | Check service status and health |
| `./logs-podman.sh [service]` | View logs for a service |
| `./test-podman.sh` | Run system tests |
| `./clean-podman.sh` | Remove all containers and data |

## Service Access

### Application Endpoints
- **Main API**: http://localhost:8081/api/v1
- **Health Check**: http://localhost:8081/actuator/health
- **Dashboard Stats**: http://localhost:8081/api/v1/dashboard/stats

### User Credentials
| Username | Password | Role | Access |
|----------|----------|------|--------|
| `admin` | `admin123` | ADMIN | Full system access |
| `dev` | `dev123` | DEVELOPER | Log ingestion, deployment |
| `qa` | `qa123` | QA | Testing, monitoring |
| `devops` | `devops123` | ADMIN+DEV | Combined access |
| `qaread` | `qaread123` | QA | Read-only QA access |

### External Tools
- **Grafana**: http://localhost:4000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Elasticsearch**: http://localhost:9200

## Example API Usage

### Authentication
All API calls require HTTP Basic Authentication:

```bash
# Get current user info
curl -u admin:admin123 http://localhost:8081/api/v1/auth/me

# Get test credentials
curl http://localhost:8081/api/v1/auth/test-credentials
```

### Log Ingestion (Developer role required)
```bash
# Ingest a single log via HTTP
curl -X POST -u dev:dev123 \
  -H "Content-Type: application/json" \
  -d '{"message":"Application started successfully","level":"INFO","source":"app-server","application":"log-analyzer"}' \
  http://localhost:8080/api/v1/logs/ingest

# Kafka-based ingestion (automatic processing)
# Logs sent to Kafka topic 'logs' are automatically processed
```

### Dashboard Queries
```bash
# Get dashboard statistics
curl -u admin:admin123 http://localhost:8080/api/v1/dashboard/stats

# Get real-time metrics
curl -u qa:qa123 http://localhost:8080/api/v1/dashboard/realtime

# Get log volume over time
curl -u admin:admin123 http://localhost:8080/api/v1/dashboard/volume

# Get top log sources
curl -u admin:admin123 http://localhost:8080/api/v1/dashboard/top-sources

# Get error trends
curl -u admin:admin123 http://localhost:8080/api/v1/dashboard/error-trends
```

### Alerts
```bash
# Get current alerts
curl -u admin:admin123 http://localhost:8080/api/v1/alerts

# Get alert statistics
curl -u admin:admin123 http://localhost:8080/api/v1/alerts/stats
```

### Search Logs
```bash
# Search logs
curl -u admin:admin123 "http://localhost:8080/api/v1/logs/search?query=error&level=ERROR"
```

## System Architecture

### Components
1. **Application**: Spring Boot with Kafka & Redis integration
2. **PostgreSQL**: Primary data storage
3. **Redis**: Caching and real-time metrics
4. **Kafka**: High-throughput log ingestion
5. **Elasticsearch**: Full-text search and analytics
6. **Grafana**: Dashboard and visualization
7. **Prometheus**: Metrics collection

### Data Flow
1. **Logs** → Kafka topic → Real-time processing → Database + Redis cache
2. **Alerts** → Generated during log processing → Stored in database
3. **Dashboard** → Reads from database and Redis cache → Real-time updates
4. **Search** → Elasticsearch for full-text search

## Troubleshooting

### Common Issues

1. **Podman machine not running (macOS)**
   ```bash
   podman machine start
   ```

2. **Port conflicts**
   ```bash
   # Check what's using the ports
   lsof -i :8080
   lsof -i :5432
   ```

3. **Service not responding**
   ```bash
   # Check service status
   ./status-podman.sh
   
   # View service logs
   ./logs-podman.sh app
   ./logs-podman.sh postgres
   ```

4. **Permission issues**
   ```bash
   # Make scripts executable
   chmod +x *.sh
   ```

5. **Kafka connection issues**
   ```bash
   # Check Kafka topics
   podman exec log-analysis-kafka kafka-topics --list --bootstrap-server localhost:9092
   ```

### Logs and Debugging
```bash
# View all service logs
./logs-podman.sh all

# View specific service logs
./logs-podman.sh app
./logs-podman.sh kafka
./logs-podman.sh postgres

# Check container status
podman ps -a

# Check system resources
podman system df
```

## Performance Tuning

### For Development
- Reduce memory allocation in `docker-compose-podman.yml`
- Disable unnecessary services temporarily

### For Production
- Increase memory limits for Elasticsearch and Kafka
- Set up proper monitoring and alerting
- Configure log retention policies
- Use external databases for persistence

## Cleaning Up

### Stop services only
```bash
./stop-podman.sh
```

### Remove everything (data will be lost)
```bash
./clean-podman.sh
```

## Support

If you encounter issues:
1. Check the troubleshooting section above
2. Run `./status-podman.sh` to check service health
3. Run `./test-podman.sh` to verify system functionality
4. View logs with `./logs-podman.sh [service-name]`

## Differences from Docker

Podman is largely compatible with Docker, but has some differences:
- Rootless containers by default
- No daemon required
- Pod-based architecture
- Better security model
- Direct systemd integration on Linux

The system should work identically whether using Docker or Podman.
