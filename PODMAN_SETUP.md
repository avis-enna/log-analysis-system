# 🐳 Running Log Analysis System with Podman

Since you have Podman instead of Docker, here's how to run the complete system.

## 🚀 Quick Start with Podman

### **Step 1: Install Requirements**

```bash
# Install podman-compose if not already installed
pip3 install podman-compose

# OR on Fedora/RHEL:
sudo dnf install podman-compose

# OR on Ubuntu/Debian:
sudo apt install podman-compose
```

### **Step 2: Clone and Start**

```bash
# Clone the repository
git clone https://github.com/avis-enna/log-analysis-system.git
cd log-analysis-system

# Start with Podman
chmod +x scripts/start-with-podman.sh
./scripts/start-with-podman.sh
```

### **Step 3: Access the Application**

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api/v1
- **Health Check**: http://localhost:8080/api/v1/health

## 🛠️ Manual Podman Commands

If the script doesn't work, try these manual commands:

### **Using podman-compose**
```bash
# Start all services
podman-compose -f podman-compose.yml up -d

# Check status
podman-compose -f podman-compose.yml ps

# View logs
podman-compose -f podman-compose.yml logs -f

# Stop services
podman-compose -f podman-compose.yml down
```

### **Using docker-compose with Podman**
```bash
# Start Podman socket
systemctl --user start podman.socket

# Set Docker host
export DOCKER_HOST="unix:///run/user/$(id -u)/podman/podman.sock"

# Use docker-compose normally
docker-compose -f podman-compose.yml up -d
```

### **Using Podman directly**
```bash
# Create network
podman network create log-analyzer-network

# Start PostgreSQL
podman run -d --name postgres \
  --network log-analyzer-network \
  -p 5432:5432 \
  -e POSTGRES_DB=loganalyzer \
  -e POSTGRES_USER=loganalyzer \
  -e POSTGRES_PASSWORD=password \
  docker.io/postgres:15

# Build and start backend
podman build -t log-analyzer-backend ./backend
podman run -d --name backend \
  --network log-analyzer-network \
  -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://postgres:5432/loganalyzer \
  -e DATABASE_USERNAME=loganalyzer \
  -e DATABASE_PASSWORD=password \
  -e SPRING_PROFILES_ACTIVE=podman \
  log-analyzer-backend

# Build and start frontend
podman build -t log-analyzer-frontend ./frontend
podman run -d --name frontend \
  --network log-analyzer-network \
  -p 3000:3000 \
  -e REACT_APP_API_URL=http://localhost:8080/api/v1 \
  log-analyzer-frontend
```

## 🔧 Troubleshooting

### **Issue: "podman-compose not found"**
```bash
# Install podman-compose
pip3 install podman-compose

# OR use docker-compose with Podman socket
systemctl --user start podman.socket
export DOCKER_HOST="unix:///run/user/$(id -u)/podman/podman.sock"
docker-compose -f podman-compose.yml up -d
```

### **Issue: "Permission denied"**
```bash
# Make sure Podman socket is running
systemctl --user start podman.socket
systemctl --user enable podman.socket

# Check if you're in the right group (some distros)
sudo usermod -aG podman $USER
# Then logout and login again
```

### **Issue: "Cannot connect to database"**
```bash
# Check if PostgreSQL container is running
podman ps

# Check PostgreSQL logs
podman logs postgres

# Restart PostgreSQL if needed
podman restart postgres
```

### **Issue: "Build fails"**
```bash
# Build images separately to see errors
podman build -t log-analyzer-backend ./backend
podman build -t log-analyzer-frontend ./frontend

# Check build logs for specific errors
```

## 🎯 Verification

Once running, verify everything works:

```bash
# Check all containers are running
podman ps

# Test backend health
curl http://localhost:8080/api/v1/health
# Should return: {"status":"UP"}

# Test frontend
curl http://localhost:3000
# Should return HTML content

# Test API endpoint
curl "http://localhost:8080/api/v1/search/quick?q=*&page=1&size=10"
# Should return JSON with log data
```

## 🛑 Stopping the System

```bash
# Stop all services
podman-compose -f podman-compose.yml down

# OR stop individual containers
podman stop frontend backend postgres

# Remove containers (optional)
podman rm frontend backend postgres

# Remove images (optional)
podman rmi log-analyzer-frontend log-analyzer-backend
```

## 💡 Tips

1. **Use podman-compose** - It's the easiest way to manage multi-container applications
2. **Enable Podman socket** - Allows docker-compose to work with Podman
3. **Check logs** - Use `podman logs <container-name>` to debug issues
4. **Rootless mode** - Podman runs rootless by default, which is more secure
5. **SELinux** - If on RHEL/Fedora, SELinux might block some operations

## 🚀 Next Steps

Once the system is running:
1. Open http://localhost:3000 in your browser
2. Explore the dashboard with sample data
3. Try searching for logs with different filters
4. Check the analytics and alerts sections

The system will automatically generate 1000 sample log entries for testing!
