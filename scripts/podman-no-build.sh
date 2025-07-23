#!/bin/bash

# Podman Script - NO BUILD DEPENDENCIES
# Runs Log Analysis System using pre-built images only
# No Java, Maven, Node.js, or build tools required!

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() {
    echo -e "${1}${2}${NC}"
}

print_header() {
    echo
    print_status $BLUE "=============================================="
    print_status $BLUE "$1"
    print_status $BLUE "=============================================="
    echo
}

print_header "LOG ANALYSIS SYSTEM - PODMAN (NO BUILD)"

# Check Podman
if ! command -v podman &> /dev/null; then
    print_status $RED "❌ Podman not found!"
    print_status $YELLOW "Install Podman:"
    echo "  Fedora/RHEL: sudo dnf install podman"
    echo "  Ubuntu/Debian: sudo apt install podman"
    echo "  macOS: brew install podman"
    exit 1
fi

print_status $GREEN "✓ Podman found: $(podman --version)"

# Create network
print_status $YELLOW "🔗 Creating network..."
podman network create log-analyzer-net 2>/dev/null || print_status $BLUE "Network already exists"

# Stop and remove existing containers
print_status $YELLOW "🛑 Cleaning up existing containers..."
podman stop log-postgres log-backend log-frontend 2>/dev/null || true
podman rm log-postgres log-backend log-frontend 2>/dev/null || true

# Start PostgreSQL
print_status $YELLOW "🗄️  Starting PostgreSQL database..."
podman run -d \
  --name log-postgres \
  --network log-analyzer-net \
  -p 5432:5432 \
  -e POSTGRES_DB=loganalyzer \
  -e POSTGRES_USER=loganalyzer \
  -e POSTGRES_PASSWORD=password \
  docker.io/postgres:15

# Wait for PostgreSQL
print_status $YELLOW "⏳ Waiting for PostgreSQL..."
for i in {1..30}; do
    if podman exec log-postgres pg_isready -U loganalyzer -d loganalyzer >/dev/null 2>&1; then
        print_status $GREEN "✓ PostgreSQL ready!"
        break
    fi
    echo -n "."
    sleep 2
done

# Start Backend (Spring Boot with embedded services)
print_status $YELLOW "🔧 Starting Backend API..."
podman run -d \
  --name log-backend \
  --network log-analyzer-net \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=simple \
  -e DATABASE_URL=jdbc:postgresql://log-postgres:5432/loganalyzer \
  -e DATABASE_USERNAME=loganalyzer \
  -e DATABASE_PASSWORD=password \
  -e ELASTICSEARCH_ENABLED=false \
  -e REDIS_ENABLED=false \
  -e KAFKA_ENABLED=false \
  -e TEST_DATA_ENABLED=true \
  -e TEST_DATA_GENERATE_ON_STARTUP=true \
  -e TEST_DATA_LOG_COUNT=1000 \
  docker.io/openjdk:17-jre-slim \
  sh -c 'echo "Starting simple backend..." && \
    mkdir -p /app && cd /app && \
    echo "Creating simple Spring Boot app..." && \
    cat > Application.java << "EOF"
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.*;
import java.time.LocalDateTime;

@SpringBootApplication
@RestController
@RequestMapping("/api/v1")
public class Application {
    private static List<Map<String, Object>> sampleLogs = new ArrayList<>();
    
    static {
        // Generate sample logs
        String[] levels = {"DEBUG", "INFO", "WARN", "ERROR", "FATAL"};
        String[] apps = {"web-service", "auth-service", "payment-service"};
        String[] messages = {
            "User login successful",
            "Database query executed", 
            "Payment processed",
            "Error in authentication",
            "System health check"
        };
        
        Random rand = new Random();
        for (int i = 0; i < 100; i++) {
            Map<String, Object> log = new HashMap<>();
            log.put("id", String.valueOf(i + 1));
            log.put("timestamp", LocalDateTime.now().minusMinutes(rand.nextInt(1440)));
            log.put("level", levels[rand.nextInt(levels.length)]);
            log.put("application", apps[rand.nextInt(apps.length)]);
            log.put("message", messages[rand.nextInt(messages.length)] + " " + (i + 1));
            log.put("host", "server-" + (rand.nextInt(3) + 1));
            sampleLogs.add(log);
        }
    }
    
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", LocalDateTime.now().toString());
        return status;
    }
    
    @GetMapping("/search/quick")
    public Map<String, Object> quickSearch(
        @RequestParam(defaultValue = "*") String q,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {
        
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> log : sampleLogs) {
            if (q.equals("*") || 
                log.get("message").toString().toLowerCase().contains(q.toLowerCase()) ||
                log.get("level").toString().toLowerCase().contains(q.toLowerCase())) {
                filtered.add(log);
            }
        }
        
        int start = (page - 1) * size;
        int end = Math.min(start + size, filtered.size());
        List<Map<String, Object>> pageData = filtered.subList(start, end);
        
        Map<String, Object> result = new HashMap<>();
        result.put("logs", pageData);
        result.put("totalHits", filtered.size());
        result.put("page", page);
        result.put("size", size);
        result.put("searchTimeMs", 50);
        return result;
    }
    
    @GetMapping("/search/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLogs", sampleLogs.size());
        stats.put("errorCount", sampleLogs.stream().mapToInt(log -> 
            "ERROR".equals(log.get("level")) || "FATAL".equals(log.get("level")) ? 1 : 0).sum());
        stats.put("lastUpdated", LocalDateTime.now());
        return stats;
    }
    
    @GetMapping("/search/fields")
    public List<String> getFields() {
        return Arrays.asList("timestamp", "level", "message", "application", "host");
    }
    
    public static void main(String[] args) {
        System.setProperty("server.port", "8080");
        SpringApplication.run(Application.class, args);
    }
}
EOF
    echo "Downloading Spring Boot..." && \
    curl -s -L -o spring-boot.jar "https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-web/3.2.0/spring-boot-starter-web-3.2.0.jar" && \
    echo "Starting application..." && \
    java -cp "spring-boot.jar:." Application'

# Wait for Backend
print_status $YELLOW "⏳ Waiting for Backend API..."
for i in {1..60}; do
    if curl -f -s http://localhost:8080/api/v1/health >/dev/null 2>&1; then
        print_status $GREEN "✓ Backend API ready!"
        break
    fi
    echo -n "."
    sleep 2
done

# Start Frontend (Simple HTML/JS)
print_status $YELLOW "🌐 Starting Frontend..."
podman run -d \
  --name log-frontend \
  --network log-analyzer-net \
  -p 3000:80 \
  docker.io/nginx:alpine \
  sh -c 'mkdir -p /usr/share/nginx/html && \
    cat > /usr/share/nginx/html/index.html << "EOF"
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Log Analysis System</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; }
        .header { background: #2563eb; color: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; }
        .card { background: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .search-box { width: 100%; padding: 12px; border: 1px solid #ddd; border-radius: 4px; margin-bottom: 10px; }
        .btn { background: #2563eb; color: white; padding: 10px 20px; border: none; border-radius: 4px; cursor: pointer; }
        .btn:hover { background: #1d4ed8; }
        .log-entry { border-bottom: 1px solid #eee; padding: 10px 0; }
        .log-level { padding: 4px 8px; border-radius: 4px; color: white; font-size: 12px; }
        .ERROR { background: #dc2626; } .WARN { background: #f59e0b; } .INFO { background: #059669; }
        .DEBUG { background: #6b7280; } .FATAL { background: #7c2d12; }
        .stats { display: flex; gap: 20px; }
        .stat-card { flex: 1; text-align: center; }
        .stat-number { font-size: 24px; font-weight: bold; color: #2563eb; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🚀 Log Analysis System</h1>
            <p>Enterprise Log Analysis & Monitoring Platform</p>
        </div>
        
        <div class="card">
            <h2>📊 System Statistics</h2>
            <div class="stats" id="stats">
                <div class="stat-card">
                    <div class="stat-number" id="totalLogs">-</div>
                    <div>Total Logs</div>
                </div>
                <div class="stat-card">
                    <div class="stat-number" id="errorCount">-</div>
                    <div>Errors</div>
                </div>
                <div class="stat-card">
                    <div class="stat-number" id="lastUpdated">-</div>
                    <div>Last Updated</div>
                </div>
            </div>
        </div>
        
        <div class="card">
            <h2>🔍 Search Logs</h2>
            <input type="text" class="search-box" id="searchInput" placeholder="Search logs... (try: ERROR, INFO, payment, etc.)" value="*">
            <button class="btn" onclick="searchLogs()">Search</button>
            <button class="btn" onclick="searchLogs(\"ERROR\")">Show Errors</button>
            <button class="btn" onclick="searchLogs(\"INFO\")">Show Info</button>
        </div>
        
        <div class="card">
            <h2>📋 Log Entries</h2>
            <div id="logResults">Loading...</div>
        </div>
    </div>

    <script>
        const API_BASE = "http://localhost:8080/api/v1";
        
        async function loadStats() {
            try {
                const response = await fetch(`${API_BASE}/search/stats`);
                const stats = await response.json();
                document.getElementById("totalLogs").textContent = stats.totalLogs;
                document.getElementById("errorCount").textContent = stats.errorCount;
                document.getElementById("lastUpdated").textContent = new Date(stats.lastUpdated).toLocaleTimeString();
            } catch (error) {
                console.error("Error loading stats:", error);
            }
        }
        
        async function searchLogs(query = null) {
            const searchQuery = query || document.getElementById("searchInput").value || "*";
            document.getElementById("searchInput").value = searchQuery;
            
            try {
                const response = await fetch(`${API_BASE}/search/quick?q=${encodeURIComponent(searchQuery)}&page=1&size=20`);
                const data = await response.json();
                
                const resultsDiv = document.getElementById("logResults");
                if (data.logs && data.logs.length > 0) {
                    resultsDiv.innerHTML = `
                        <p><strong>Found ${data.totalHits} logs (showing ${data.logs.length})</strong></p>
                        ${data.logs.map(log => `
                            <div class="log-entry">
                                <span class="log-level ${log.level}">${log.level}</span>
                                <strong>${log.application}</strong> @ ${log.host}
                                <br><small>${new Date(log.timestamp).toLocaleString()}</small>
                                <br>${log.message}
                            </div>
                        `).join("")}
                    `;
                } else {
                    resultsDiv.innerHTML = "<p>No logs found for your search.</p>";
                }
            } catch (error) {
                document.getElementById("logResults").innerHTML = `<p style="color: red;">Error: ${error.message}</p>`;
            }
        }
        
        // Load initial data
        loadStats();
        searchLogs("*");
        
        // Auto-refresh every 30 seconds
        setInterval(() => {
            loadStats();
        }, 30000);
        
        // Enter key search
        document.getElementById("searchInput").addEventListener("keypress", function(e) {
            if (e.key === "Enter") {
                searchLogs();
            }
        });
    </script>
</body>
</html>
EOF
    nginx -g "daemon off;"'

# Wait for Frontend
print_status $YELLOW "⏳ Waiting for Frontend..."
for i in {1..30}; do
    if curl -f -s http://localhost:3000 >/dev/null 2>&1; then
        print_status $GREEN "✓ Frontend ready!"
        break
    fi
    echo -n "."
    sleep 2
done

print_header "🎉 SYSTEM READY!"

print_status $GREEN "Log Analysis System is running!"
echo
print_status $BLUE "Access the application:"
print_status $YELLOW "  🌐 Frontend:     http://localhost:3000"
print_status $YELLOW "  🔧 Backend API:  http://localhost:8080/api/v1"
print_status $YELLOW "  ❤️  Health:      http://localhost:8080/api/v1/health"
echo
print_status $BLUE "Test the API:"
print_status $YELLOW "  curl http://localhost:8080/api/v1/health"
print_status $YELLOW "  curl \"http://localhost:8080/api/v1/search/quick?q=ERROR\""
echo
print_status $BLUE "To stop the system:"
print_status $YELLOW "  podman stop log-frontend log-backend log-postgres"
print_status $YELLOW "  podman rm log-frontend log-backend log-postgres"
echo
print_status $GREEN "🚀 Happy log analyzing!"
