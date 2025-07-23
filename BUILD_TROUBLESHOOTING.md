# 🔧 Build Troubleshooting Guide

This guide helps you resolve common build issues with the Log Analysis System.

## 🚨 **Quick Fix for Build Issues**

If you're experiencing build problems, run this command first:

```bash
# Fix common build issues automatically
./scripts/fix-build-issues.sh

# Validate the fixes
./scripts/build-check.sh
```

## 🔍 **Common Build Issues & Solutions**

### **Issue 1: "mvnw: command not found" or "Permission denied"**

**Problem**: Maven wrapper is not executable or missing.

**Solution**:
```bash
# Make Maven wrapper executable
chmod +x mvnw

# If mvnw is missing, download it
curl -o mvnw https://raw.githubusercontent.com/takari/maven-wrapper/master/mvnw
chmod +x mvnw
```

### **Issue 2: "Java version not supported"**

**Problem**: Wrong Java version (need Java 17+).

**Solution**:
```bash
# Check Java version
java -version

# If Java 17+ is not installed:
# - Download from: https://adoptium.net/
# - Or use SDKMAN: sdk install java 17.0.9-tem
# - Or use package manager: apt install openjdk-17-jdk

# Set JAVA_HOME if needed
export JAVA_HOME=/path/to/java17
```

### **Issue 3: "Could not resolve dependencies"**

**Problem**: Maven cannot download dependencies.

**Solution**:
```bash
# Clear Maven cache
rm -rf ~/.m2/repository

# Try building with force update
./mvnw clean compile -U

# If behind corporate firewall, configure proxy in ~/.m2/settings.xml
```

### **Issue 4: "Application failed to start"**

**Problem**: Spring Boot application won't start.

**Solution**:
```bash
# Use local profile with embedded database
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Check application.yml configuration
# Ensure H2 database is configured correctly
```

### **Issue 5: "npm: command not found"**

**Problem**: Node.js/npm not installed.

**Solution**:
```bash
# Install Node.js 18+ from: https://nodejs.org/
# Or use package manager:
# - Ubuntu: apt install nodejs npm
# - macOS: brew install node
# - Windows: Download from nodejs.org

# Verify installation
node --version  # Should be 18+
npm --version   # Should be 8+
```

### **Issue 6: "npm install fails"**

**Problem**: Frontend dependencies cannot be installed.

**Solution**:
```bash
cd frontend

# Clear npm cache
npm cache clean --force

# Delete node_modules and package-lock.json
rm -rf node_modules package-lock.json

# Reinstall dependencies
npm install

# If still failing, try with legacy peer deps
npm install --legacy-peer-deps
```

### **Issue 7: "Port already in use"**

**Problem**: Ports 8080 or 3000 are occupied.

**Solution**:
```bash
# Find and kill processes using the ports
lsof -ti:8080 | xargs kill -9
lsof -ti:3000 | xargs kill -9

# Or use different ports
# Backend: ./mvnw spring-boot:run -Dserver.port=8081
# Frontend: PORT=3001 npm start
```

### **Issue 8: "Tests failing"**

**Problem**: Unit or integration tests are failing.

**Solution**:
```bash
# Run tests with local profile
./mvnw test -Dspring.profiles.active=local

# Skip tests temporarily to check build
./mvnw clean package -DskipTests

# Run specific test
./mvnw test -Dtest=ApplicationTest
```

### **Issue 9: "Docker build fails"**

**Problem**: Docker containers won't build or start.

**Solution**:
```bash
# Build without cache
docker-compose build --no-cache

# Check Docker daemon is running
docker info

# Use local testing instead
./scripts/test-local.sh
```

### **Issue 10: "Database connection errors"**

**Problem**: Cannot connect to database.

**Solution**:
```bash
# Use embedded H2 database for local testing
# Check application-local.yml has H2 configuration:

spring:
  datasource:
    url: jdbc:h2:mem:testdb
    username: sa
    password: 
    driver-class-name: org.h2.Driver
```

## 🛠️ **Build Environment Setup**

### **Minimum Requirements**
- **Java**: 17 or higher
- **Maven**: 3.8+ (or use included `./mvnw`)
- **Node.js**: 18 or higher
- **npm**: 8 or higher
- **Git**: 2.x or higher

### **Recommended Setup**
```bash
# Check all prerequisites
java -version    # Java 17+
./mvnw -version  # Maven 3.8+
node -version    # Node 18+
npm -version     # npm 8+
git --version    # Git 2.x+

# Set environment variables
export JAVA_HOME=/path/to/java17
export MAVEN_OPTS="-Xmx1024m"
export NODE_OPTIONS="--max-old-space-size=4096"
```

## 🧪 **Testing Build Without External Dependencies**

### **Backend Only (No Database/Elasticsearch)**
```bash
# Use local profile with H2 database
./mvnw clean test -Dspring.profiles.active=local

# Start application with embedded services
./mvnw spring-boot:run -Dspring.profiles.active=local
```

### **Frontend Only**
```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm start

# Run tests
npm test
```

### **Full Local Testing**
```bash
# Run comprehensive local tests
./scripts/test-local.sh

# This will:
# - Test backend with embedded H2 database
# - Test frontend with mock API
# - Generate test reports
# - Optionally start the full application
```

## 📋 **Build Validation Checklist**

Run this checklist to ensure your build environment is correct:

```bash
# 1. Check project structure
./scripts/build-check.sh

# 2. Validate Java setup
java -version
./mvnw -version

# 3. Test backend compilation
cd backend && ../mvnw clean compile

# 4. Test frontend setup
cd frontend && npm install

# 5. Run basic tests
./mvnw test -Dspring.profiles.active=local

# 6. Start application
./scripts/test-local.sh
```

## 🆘 **Still Having Issues?**

### **Get Detailed Error Information**
```bash
# Enable debug logging
./mvnw clean compile -X -e

# Check application logs
tail -f logs/application.log

# Check system resources
free -h  # Memory
df -h    # Disk space
```

### **Reset to Clean State**
```bash
# Clean all build artifacts
./mvnw clean
rm -rf frontend/node_modules
rm -rf frontend/build

# Reset git state (if needed)
git clean -fdx
git reset --hard HEAD

# Re-run fixes
./scripts/fix-build-issues.sh
```

### **Alternative: Use Docker**
If local build continues to fail, use Docker:

```bash
# Build and run with Docker
docker-compose up --build

# This bypasses local Java/Node.js requirements
```

### **Get Help**
1. **Check logs**: Look in `logs/` directory for error details
2. **Run diagnostics**: Use `./scripts/build-check.sh` for validation
3. **Check GitHub Issues**: Visit the repository issues page
4. **Environment**: Ensure you meet minimum requirements

## 🎯 **Success Indicators**

You'll know the build is working when:

- ✅ `./mvnw clean compile` succeeds
- ✅ `npm install` in frontend/ succeeds
- ✅ `./scripts/build-check.sh` shows 80%+ validation
- ✅ `./scripts/test-local.sh` runs without errors
- ✅ Application starts on http://localhost:8080 and http://localhost:3000

## 📚 **Additional Resources**

- **Spring Boot Documentation**: https://spring.io/projects/spring-boot
- **React Documentation**: https://reactjs.org/docs
- **Maven Troubleshooting**: https://maven.apache.org/guides/mini/guide-troubleshooting.html
- **Node.js Troubleshooting**: https://nodejs.org/en/docs/guides/debugging-getting-started/

---

**Remember**: The system is designed to work with minimal external dependencies. Most issues can be resolved by using the local testing profile with embedded services.
