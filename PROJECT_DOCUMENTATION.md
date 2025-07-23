# Log Analysis System - Project Documentation

## Overview
A comprehensive log analysis and monitoring system built with Java Spring Boot, featuring real-time log processing, pattern detection, anomaly identification, and interactive visualizations.

## 🚀 Key Features

### Core Functionality
- **Real-time Log Processing**: Stream and process logs from multiple sources
- **Pattern Detection**: Identify common patterns and anomalies in log data
- **Security Analysis**: Detect potential security threats and suspicious activities
- **Performance Monitoring**: Track system performance metrics and bottlenecks
- **Interactive Dashboard**: Web-based dashboard with real-time visualizations
- **Alert System**: Configurable alerts for critical events and anomalies

### Technical Highlights
- **Multi-format Support**: Apache/Nginx, Application logs, System logs, JSON logs
- **Scalable Architecture**: Microservices-based design with Docker containerization
- **Real-time Processing**: Kafka streaming for high-throughput log ingestion
- **Advanced Analytics**: Elasticsearch integration for fast search and aggregation
- **Monitoring Integration**: Prometheus metrics and Grafana dashboards

## 🛠️ Technology Stack

### Backend
- **Java 17** - Modern Java with latest features
- **Spring Boot 3.2** - Enterprise-grade framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Database abstraction layer
- **Spring WebSocket** - Real-time communication

### Data Processing
- **Apache Kafka** - Event streaming platform
- **Elasticsearch** - Search and analytics engine
- **PostgreSQL** - Primary data storage
- **Redis** - Caching and session management

### Frontend
- **Thymeleaf** - Server-side templating
- **Bootstrap 5** - Modern CSS framework
- **Chart.js** - Interactive data visualizations
- **WebSocket** - Real-time updates

### DevOps & Monitoring
- **Docker & Docker Compose** - Containerization
- **Prometheus** - Metrics collection
- **Grafana** - Visualization and alerting
- **Micrometer** - Application metrics

## 📊 Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Log Sources   │───▶│  Kafka Streams  │───▶│  Log Processor  │
│                 │    │                 │    │                 │
│ • Web Servers   │    │ • Buffering     │    │ • Pattern Det.  │
│ • Applications  │    │ • Partitioning  │    │ • Anomaly Det.  │
│ • System Logs   │    │ • Scaling       │    │ • Security Scan │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                        │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Dashboard     │◀───│   Web API       │◀───│   Database      │
│                 │    │                 │    │                 │
│ • Real-time     │    │ • REST API      │    │ • PostgreSQL    │
│ • Analytics     │    │ • WebSocket     │    │ • Elasticsearch │
│ • Alerts        │    │ • Authentication│    │ • Redis Cache   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🔧 Installation & Setup

### Prerequisites
- Java 17 or higher
- Docker and Docker Compose
- Maven 3.8+

### Quick Start
```bash
# Clone the repository
git clone <repository-url>
cd log-analysis-system

# Start infrastructure services
docker-compose up -d postgres redis elasticsearch kafka

# Build and run the application
./mvnw spring-boot:run

# Access the dashboard
open http://localhost:8080
```

### Full Docker Setup
```bash
# Start all services
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs app
```

## 📈 Key Metrics Tracked

### Performance Metrics
- **Response Times**: Average, P95, P99 response times
- **Throughput**: Requests per second, log entries per hour
- **Error Rates**: 4xx/5xx error percentages
- **System Resources**: CPU, memory, disk usage

### Security Metrics
- **Failed Login Attempts**: Brute force detection
- **Suspicious IP Activity**: Repeated failures, scanning attempts
- **Attack Patterns**: SQL injection, XSS, directory traversal
- **Access Violations**: Unauthorized access attempts

### Business Metrics
- **User Activity**: Login patterns, feature usage
- **Application Health**: Service availability, database connections
- **Alert Response**: Time to acknowledge and resolve alerts

## 🔍 Pattern Detection Algorithms

### Security Threat Detection
```java
// SQL Injection Detection
Pattern SQL_INJECTION = Pattern.compile(
    "(?i).*(union|select|insert|delete|update|drop|create|alter|exec|script).*"
);

// XSS Attack Detection
Pattern XSS_PATTERN = Pattern.compile(
    "(?i).*(script|javascript|vbscript|onload|onerror|onclick|alert\\().*"
);

// Directory Traversal Detection
Pattern DIRECTORY_TRAVERSAL = Pattern.compile(
    ".*(\\.\\./|\\.\\\\|\\.\\./\\.\\./|\\\\\\.\\.\\\\).*"
);
```

### Performance Anomaly Detection
- **Response Time Analysis**: Statistical outlier detection
- **Memory Usage Patterns**: Trend analysis for memory leaks
- **Database Performance**: Slow query identification
- **Connection Pool Monitoring**: Resource exhaustion detection

## 📊 Dashboard Features

### Real-time Monitoring
- **Live Statistics**: Total logs, errors, alerts, sources
- **Activity Timeline**: Hourly log volume by severity
- **Source Distribution**: Pie chart of log sources
- **Recent Events**: Latest logs and alerts

### Analytics Views
- **Trend Analysis**: Historical data visualization
- **Geographic Distribution**: IP-based location mapping
- **User Behavior**: Session analysis and user journeys
- **Performance Insights**: Response time distributions

## 🚨 Alert System

### Alert Types
- **CRITICAL**: System failures, security breaches
- **HIGH**: Performance degradation, repeated failures
- **MEDIUM**: Warning conditions, unusual patterns
- **LOW**: Informational alerts, maintenance notices

### Notification Channels
- **Email**: Configurable recipient lists
- **Slack**: Integration with team channels
- **PagerDuty**: On-call escalation
- **Webhooks**: Custom integrations

## 📋 API Documentation

### Core Endpoints
```
GET  /api/stats                 - Dashboard statistics
GET  /api/logs                  - Paginated log entries
GET  /api/alerts                - Alert management
POST /api/upload                - Log file upload
GET  /api/charts/timeline       - Timeline chart data
GET  /api/charts/sources        - Source distribution
GET  /api/analytics/top-ips     - Top IP addresses
```

### WebSocket Events
```
/ws/logs        - Real-time log stream
/ws/alerts      - Alert notifications
/ws/stats       - Live statistics updates
```

## 🧪 Testing & Quality

### Unit Tests
```bash
# Run all tests
./mvnw test

# Run specific test suite
./mvnw test -Dtest=LogProcessingServiceTest

# Generate test coverage report
./mvnw jacoco:report
```

### Integration Tests
```bash
# Start test containers
docker-compose -f docker-compose.test.yml up -d

# Run integration tests
./mvnw integration-test

# Cleanup test environment
docker-compose -f docker-compose.test.yml down
```

## 🚀 Deployment

### Production Deployment
```bash
# Build production image
docker build -t log-analysis-system:latest .

# Deploy to production
docker-compose -f docker-compose.prod.yml up -d

# Monitor deployment
docker-compose logs -f app
```

### Scaling Configuration
```yaml
# Docker Compose scaling
services:
  app:
    deploy:
      replicas: 3
      resources:
        limits:
          cpus: '2'
          memory: 4G
```

## 📈 Performance Benchmarks

### Processing Capacity
- **Log Ingestion**: 10,000+ entries/second
- **Pattern Detection**: 5,000+ analyses/second
- **Alert Generation**: Sub-second latency
- **Dashboard Updates**: Real-time (WebSocket)

### Resource Usage
- **Memory**: 2GB baseline, 4GB recommended
- **CPU**: 2 cores minimum, 4 cores optimal
- **Storage**: 10GB for application, 100GB+ for logs
- **Network**: 1Gbps for high-volume environments

## 🛡️ Security Features

### Authentication & Authorization
- **JWT Token Authentication**: Stateless security
- **Role-based Access Control**: Admin, Analyst, Viewer roles
- **API Key Management**: Secure service-to-service communication
- **Session Management**: Redis-based session storage

### Data Protection
- **Input Validation**: Comprehensive sanitization
- **SQL Injection Prevention**: Parameterized queries
- **XSS Protection**: Output encoding
- **CSRF Protection**: Token-based validation

## 🔧 Configuration

### Environment Variables
```properties
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/log_analysis_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=password

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Elasticsearch
SPRING_ELASTICSEARCH_URIS=http://localhost:9200
```

### Alert Configuration
```properties
# Email alerts
ALERT_EMAIL_ENABLED=true
ALERT_EMAIL_SMTP_HOST=smtp.gmail.com
ALERT_EMAIL_SMTP_PORT=587
ALERT_RECIPIENTS=admin@company.com,ops@company.com

# Thresholds
ALERT_FAILED_LOGIN_THRESHOLD=5
ALERT_RESPONSE_TIME_THRESHOLD=5000
ALERT_ERROR_RATE_THRESHOLD=0.05
```

## 📊 Monitoring & Observability

### Prometheus Metrics
```properties
# Application metrics
log_entries_processed_total
log_processing_duration_seconds
alerts_generated_total
database_connections_active

# JVM metrics
jvm_memory_used_bytes
jvm_gc_collection_seconds
jvm_threads_current
```

### Grafana Dashboards
- **System Overview**: High-level metrics and health status
- **Log Processing**: Ingestion rates and processing times
- **Security Dashboard**: Threat detection and incident response
- **Performance Analytics**: Response times and resource usage

## 🤝 Contributing

### Development Setup
```bash
# Clone repository
git clone <repository-url>
cd log-analysis-system

# Install dependencies
./mvnw clean install

# Start development environment
docker-compose -f docker-compose.dev.yml up -d

# Run application
./mvnw spring-boot:run
```

### Code Standards
- **Java Style**: Google Java Style Guide
- **Documentation**: JavaDoc for all public APIs
- **Testing**: 80%+ code coverage requirement
- **Security**: OWASP compliance

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👤 Author

**Siva Reddy**
- Backend Developer & Cybersecurity Enthusiast
- Experience: 2+ years in server-side development
- Specialization: Microservices, Security, Performance Optimization

---

*This project demonstrates advanced Java development skills, including enterprise frameworks, real-time processing, security implementation, and scalable architecture design.*
