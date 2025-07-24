# 🐳 Podman Deployment Guide

This guide solves compilation errors and local environment issues by containerizing the entire Log Analysis System with Podman.

## 🚀 Quick Start

### Prerequisites
- **Podman** installed on your system
- **curl** for health checks
- At least **4GB RAM** available

### Install Podman
```bash
# Ubuntu/Debian
sudo apt-get update && sudo apt-get install podman

# RHEL/CentOS/Fedora
sudo dnf install podman

# macOS
brew install podman
```

### One-Command Deployment
```bash
# Make script executable and deploy
chmod +x deploy-podman.sh
./deploy-podman.sh
```

That's it! The script will:
1. ✅ Check Podman installation
2. 🧹 Clean up any existing containers
3. 🌐 Create network
4. 🏗️ Build backend and frontend images
5. 🗄️ Start PostgreSQL and Redis
6. 🚀 Deploy backend and frontend
7. 🔍 Wait for health checks
8. 📊 Show access URLs

## 📊 Access Your Application

After successful deployment:

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics

## 🔧 Management Commands

```bash
# Deploy/Start all services
./deploy-podman.sh deploy
./deploy-podman.sh start

# Stop all services
./deploy-podman.sh stop

# Restart all services
./deploy-podman.sh restart

# Clean up everything
./deploy-podman.sh clean

# View backend logs
./deploy-podman.sh logs

# Check container status
./deploy-podman.sh status

# Show help
./deploy-podman.sh help
```

## 🗄️ Database Access

### PostgreSQL
- **Host**: localhost:5432
- **Database**: loganalyzer
- **Username**: loguser
- **Password**: logpass123

```bash
# Connect to PostgreSQL
podman exec -it log-analyzer-postgres psql -U loguser -d loganalyzer
```

### Redis
- **Host**: localhost:6379

```bash
# Connect to Redis
podman exec -it log-analyzer-redis redis-cli
```

## 🐛 Troubleshooting

### Common Issues

#### 1. Port Already in Use
```bash
# Check what's using the port
sudo netstat -tulpn | grep :8080

# Kill the process or change ports in the script
```

#### 2. Podman Permission Issues
```bash
# Run podman in rootless mode (recommended)
podman system migrate

# Or use sudo if needed
sudo ./deploy-podman.sh
```

#### 3. Build Failures
```bash
# Clean everything and retry
./deploy-podman.sh clean
./deploy-podman.sh deploy
```

#### 4. Health Check Failures
```bash
# Check container logs
podman logs log-analyzer-backend
podman logs log-analyzer-frontend

# Check if services are running
podman ps
```

### Debug Commands

```bash
# View all containers
podman ps -a

# View container logs
podman logs -f log-analyzer-backend

# Execute commands in container
podman exec -it log-analyzer-backend bash

# Check network connectivity
podman exec log-analyzer-backend curl -f http://log-analyzer-postgres:5432

# View resource usage
podman stats
```

## 🔄 Alternative: Docker Compose

If you prefer docker-compose:

```bash
# Using podman-compose (if available)
podman-compose -f docker-compose.podman.yml up -d

# Using docker-compose with podman
docker-compose -f docker-compose.podman.yml up -d
```

## 📁 Container Structure

```
log-analyzer/
├── backend/
│   ├── Dockerfile.podman          # Optimized backend Dockerfile
│   └── src/main/resources/
│       └── application-docker.yml # Docker profile config
├── frontend/
│   └── Dockerfile.podman          # Optimized frontend Dockerfile
├── deploy-podman.sh               # Main deployment script
├── docker-compose.podman.yml      # Compose alternative
└── PODMAN-DEPLOYMENT.md          # This guide
```

## 🎯 Benefits of This Approach

### ✅ Solves Local Issues
- **No Java/Maven installation needed**
- **No Node.js/npm installation needed**
- **No database setup required**
- **Consistent environment across systems**

### ✅ Production-Ready
- **Multi-stage builds** for optimized images
- **Health checks** for all services
- **Proper networking** between containers
- **Persistent data** with volumes
- **Security** with non-root users

### ✅ Easy Management
- **One-command deployment**
- **Simple start/stop/restart**
- **Easy log access**
- **Clean cleanup**

## 🔒 Security Features

- **Non-root containers** for security
- **Network isolation** between services
- **Health checks** for reliability
- **Proper secret management** via environment variables

## 📈 Performance Optimizations

- **Multi-stage builds** reduce image size
- **Dependency caching** speeds up rebuilds
- **Connection pooling** for database
- **Redis caching** for performance
- **JVM tuning** for containers

## 🆘 Getting Help

If you encounter issues:

1. **Check logs**: `./deploy-podman.sh logs`
2. **Check status**: `./deploy-podman.sh status`
3. **Clean and retry**: `./deploy-podman.sh clean && ./deploy-podman.sh deploy`
4. **Check system resources**: `podman stats`

## 🎉 Success!

Once deployed, you should see:
```
=== DEPLOYMENT COMPLETE ===
🎉 Log Analysis System is running!

📊 Application URLs:
  Frontend:  http://localhost:3000
  Backend:   http://localhost:8080
  API Docs:  http://localhost:8080/swagger-ui.html
  Health:    http://localhost:8080/actuator/health
```

Your Log Analysis System is now running in containers with no local compilation issues! 🚀
