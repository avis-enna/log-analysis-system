# 🚀 Log Analysis System - Deployment Reference Guide

This guide consolidates all deployment methods and scripts available for the Log Analysis System based on the existing bash scripts and documentation.

## 📋 **Available Deployment Scripts**

### 1. **`deploy.sh`** - Production Docker Deployment
**Purpose**: Complete production deployment with Docker Compose
**Location**: `/deploy.sh` (root directory)

#### **Features**:
- ✅ Interactive menu-driven deployment
- ✅ Command-line arguments support
- ✅ Automatic dependency checking
- ✅ Service health monitoring
- ✅ Environment file generation
- ✅ Cleanup and maintenance options

#### **Usage**:
```bash
# Interactive mode (recommended for first-time users)
./deploy.sh

# Command line mode
./deploy.sh deploy    # Deploy everything
./deploy.sh stop      # Stop all services
./deploy.sh status    # Check service health
./deploy.sh logs      # View all logs
./deploy.sh cleanup   # Clean up everything
./deploy.sh check     # Check prerequisites
```

#### **Menu Options**:
1. Deploy System (Docker)
2. Stop Services
3. Show Service Status
4. Show Logs (all services)
5. Show Logs (specific service)
6. Cleanup Everything
7. Check Prerequisites
8. Generate Environment File
9. Exit

#### **Services Deployed**:
- **Frontend**: React app on port 3000
- **Backend**: Spring Boot API on port 8080
- **PostgreSQL**: Database on port 5432
- **Elasticsearch**: Search engine on port 9200
- **Kafka**: Message streaming on port 9092
- **Redis**: Cache on port 6379
- **InfluxDB**: Time-series database on port 8086

### 2. **`start-local.sh`** - Local Development Setup
**Purpose**: Start the system locally without Docker
**Location**: `/start-local.sh` (root directory)

#### **Features**:
- ✅ Prerequisites validation (Java 17+, Node.js 18+)
- ✅ Port availability checking
- ✅ Automatic dependency installation
- ✅ Backend and frontend startup
- ✅ Health monitoring
- ✅ Process management

#### **Usage**:
```bash
# Interactive mode
./start-local.sh

# Command line mode
./start-local.sh start    # Start everything
./start-local.sh stop     # Stop services
./start-local.sh status   # Check status
./start-local.sh backend  # Start only backend
./start-local.sh frontend # Start only frontend
```

#### **What it does**:
1. Checks Java 17+ and Node.js 18+
2. Verifies ports 8080 and 3001 are free
3. Installs backend dependencies (`mvn clean install`)
4. Installs frontend dependencies (`npm install --legacy-peer-deps`)
5. Starts backend on port 8080
6. Starts frontend on port 3001
7. Opens browser automatically

### 3. **`start-local.bat`** - Windows Local Development
**Purpose**: Windows equivalent of start-local.sh
**Location**: `/start-local.bat` (root directory)

#### **Features**:
- ✅ Windows-compatible batch script
- ✅ Same functionality as Linux/Mac version
- ✅ Automatic browser opening
- ✅ Process management for Windows

#### **Usage**:
```cmd
REM Double-click the file or run from command prompt
start-local.bat

REM Or from PowerShell
.\start-local.bat
```

## 🐳 **Docker Deployment Methods**

### Method 1: Using deploy.sh (Recommended)
```bash
# One-command deployment
./deploy.sh deploy

# Interactive deployment
./deploy.sh
# Then select option 1
```

### Method 2: Direct Docker Compose
```bash
# Simple deployment
docker-compose -f docker-compose.simple.yml up -d

# Full deployment with all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Method 3: Manual Docker Commands
```bash
# Build images
docker build -t log-analysis-backend ./backend
docker build -t log-analysis-frontend ./frontend

# Run with network
docker network create log-analysis-network
docker run -d --name backend --network log-analysis-network -p 8080:8080 log-analysis-backend
docker run -d --name frontend --network log-analysis-network -p 3001:3000 log-analysis-frontend
```

## 💻 **Local Development Methods**

### Method 1: Using start-local.sh (Recommended)
```bash
# Linux/Mac
./start-local.sh start

# Windows
start-local.bat
```

### Method 2: Manual Setup
```bash
# Backend
cd backend
./mvnw clean install
./mvnw spring-boot:run

# Frontend (new terminal)
cd frontend
npm install --legacy-peer-deps
PORT=3001 npm start
```

### Method 3: IDE-based Development
1. **Backend**: Import `backend/pom.xml` into IntelliJ/Eclipse
2. **Frontend**: Open `frontend/` in VS Code
3. Run backend from IDE
4. Run `npm start` in frontend directory

## 🔧 **Port Configuration**

### Default Ports:
- **Frontend**: 3001 (configurable via PORT environment variable)
- **Backend**: 8080 (configurable via server.port)
- **PostgreSQL**: 5432
- **Elasticsearch**: 9200
- **Kafka**: 9092
- **Redis**: 6379
- **InfluxDB**: 8086

### Port Conflicts Resolution:
```bash
# Check what's using a port
lsof -i :8080

# Kill process using port
kill -9 <PID>

# Use different port for frontend
PORT=3002 npm start

# Use different port for backend
./mvnw spring-boot:run -Dserver.port=8081
```

## 📁 **Generated Files and Directories**

### By deploy.sh:
- `.env` - Environment configuration
- `logs/` - Application logs
- `data/postgres/` - PostgreSQL data
- `data/elasticsearch/` - Elasticsearch data
- `data/redis/` - Redis data
- `data/influxdb/` - InfluxDB data

### By start-local.sh:
- `backend/target/` - Compiled Java classes
- `frontend/node_modules/` - Node.js dependencies
- `frontend/build/` - Production build (if built)

## 🔍 **Health Checking**

### Automated Health Checks (in scripts):
```bash
# Backend health
curl -f http://localhost:8080/actuator/health

# Frontend health
curl -f http://localhost:3001

# Database health
docker-compose exec postgres pg_isready -U loganalyzer

# Elasticsearch health
curl -f http://localhost:9200/_cluster/health
```

### Manual Health Verification:
1. **Backend**: Visit http://localhost:8080/actuator/health
2. **Frontend**: Visit http://localhost:3001
3. **API Documentation**: Visit http://localhost:8080/swagger-ui.html
4. **Elasticsearch**: Visit http://localhost:9200

## 🛠️ **Troubleshooting Commands**

### View Logs:
```bash
# Docker deployment
./deploy.sh logs
docker-compose logs -f [service-name]

# Local deployment
./start-local.sh logs
tail -f logs/application.log
```

### Stop Services:
```bash
# Docker deployment
./deploy.sh stop
docker-compose down

# Local deployment
./start-local.sh stop
pkill -f spring-boot
pkill -f react-scripts
```

### Clean Up:
```bash
# Docker cleanup
./deploy.sh cleanup
docker-compose down -v
docker system prune -f

# Local cleanup
rm -rf backend/target/
rm -rf frontend/node_modules/
rm -rf frontend/build/
```

## 🚀 **Quick Command Reference**

### First Time Setup:
```bash
# Clone repository
git clone <repository-url>
cd log-analysis-system

# Option 1: Docker (if you have Docker)
./deploy.sh deploy

# Option 2: Local development
./start-local.sh start
```

### Daily Development:
```bash
# Start everything
./start-local.sh start

# Check status
./start-local.sh status

# View logs
./start-local.sh logs

# Stop everything
./start-local.sh stop
```

### Production Deployment:
```bash
# Deploy to production
./deploy.sh deploy

# Monitor services
./deploy.sh status

# View logs
./deploy.sh logs

# Maintenance
./deploy.sh cleanup
```

## 📱 **Access URLs After Deployment**

### Local Development:
- 🌐 **Frontend**: http://localhost:3001
- 🔧 **Backend API**: http://localhost:8080/api/v1
- ❤️ **Health Check**: http://localhost:8080/actuator/health
- 📚 **API Docs**: http://localhost:8080/swagger-ui.html

### Docker Deployment:
- 🌐 **Frontend**: http://localhost:3000
- 🔧 **Backend API**: http://localhost:8080/api/v1
- ❤️ **Health Check**: http://localhost:8080/actuator/health
- 📊 **Elasticsearch**: http://localhost:9200
- 🗄️ **Database**: localhost:5432 (user: loganalyzer, db: loganalyzer)
- 🚀 **Redis**: localhost:6379
- 📈 **InfluxDB**: http://localhost:8086

## 📋 **Prerequisites Summary**

### For Local Development:
- Java 17+ (OpenJDK or Oracle JDK)
- Node.js 18+ with npm
- Maven 3.6+ (or use included mvnw)
- Git
- Available ports: 8080, 3001

### For Docker Deployment:
- Docker 20.10+ 
- Docker Compose 2.0+
- Available ports: 3000, 8080, 5432, 9200, 9092, 6379, 8086
- 4GB+ RAM recommended
- 2GB+ free disk space

This reference guide consolidates all the deployment methods and scripts available in the Log Analysis System, making it easy to choose the right deployment approach for your needs.
