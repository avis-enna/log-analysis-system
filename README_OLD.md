# 🚀 Enterprise Log Analysis & Monitoring System

[![Build Status](https://github.com/avis-enna/log-analysis-system/workflows/CI/badge.svg)](https://github.com/avis-enna/log-analysis-system/actions)
[![Test Coverage](https://codecov.io/gh/avis-enna/log-analysis-system/branch/main/graph/badge.svg)](https://codecov.io/gh/avis-enna/log-analysis-system)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=log-analysis-system&metric=security_rating)](https://sonarcloud.io/dashboard?id=log-analysis-system)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A comprehensive **Splunk-inspired** log analysis and monitoring system built with modern technologies, implementing **25-50% of Splunk's core functionality** with enterprise-grade features including real-time log processing, pattern detection, interactive visualizations, and intelligent alerting.

## Features

- **Real-time Log Processing**: Stream and process logs from multiple sources
- **Pattern Detection**: Identify common patterns and anomalies in log data
- **Security Analysis**: Detect potential security threats and suspicious activities
- **Performance Monitoring**: Track system performance metrics and bottlenecks
- **Interactive Dashboard**: Web-based dashboard with real-time visualizations
- **Alert System**: Configurable alerts for critical events and anomalies
- **Data Export**: Export analysis results to various formats (JSON, CSV, PDF)

## Tech Stack

- **Backend**: Java 17+, Spring Boot, Spring Security, Spring Data JPA
- **Data Processing**: Apache Kafka, Apache Spark, Elasticsearch
- **Visualization**: Chart.js, D3.js, Thymeleaf
- **Database**: PostgreSQL, Redis (for caching)
- **Monitoring**: Micrometer, Prometheus, Grafana integration
- **Containerization**: Docker, Docker Compose

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Log Sources   │───▶│  Log Processor  │───▶│   Dashboard     │
│                 │    │                 │    │                 │
│ • Web Servers   │    │ • Pattern Det.  │    │ • Real-time     │
│ • Applications  │    │ • Anomaly Det.  │    │ • Analytics     │
│ • System Logs   │    │ • Security Scan │    │ • Alerts        │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Installation

```bash
# Clone the repository
git clone <repository-url>
cd log-analysis-system

# Build the project
./mvnw clean install

# Set up environment variables
cp .env.example .env
# Edit .env with your configuration

# Run with Docker
docker-compose up -d

# Or run locally
./mvnw spring-boot:run
```

## Usage

1. **Start the application**:
   ```bash
   ./mvnw spring-boot:run
   ```

2. **Access the dashboard**: Open http://localhost:8080 in your browser

3. **Upload log files**: Use the REST API or web interface to upload log files

4. **Monitor alerts**: Set up custom alerts for specific patterns or anomalies

## Log Formats Supported

- Apache/Nginx access logs
- Application logs (JSON, structured text)
- System logs (syslog format)
- Security logs
- Database logs
- Custom formats (configurable)

## Key Metrics Tracked

- **Performance**: Response times, throughput, error rates
- **Security**: Failed login attempts, suspicious IPs, attack patterns
- **System**: Memory usage, CPU utilization, disk I/O
- **Business**: User activity, feature usage, conversion rates

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

MIT License - see LICENSE file for details
