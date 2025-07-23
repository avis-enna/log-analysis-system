#!/bin/bash

# SUPER SIMPLE PODMAN SCRIPT - ZERO BUILD DEPENDENCIES
# Single container with everything included
# Just needs Podman - nothing else!

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}🚀 Starting Log Analysis System (Simple Mode)${NC}"

# Check Podman
if ! command -v podman &> /dev/null; then
    echo -e "${RED}❌ Podman not found!${NC}"
    echo "Install: sudo dnf install podman  # or apt install podman"
    exit 1
fi

echo -e "${GREEN}✓ Podman found${NC}"

# Stop existing container
echo -e "${YELLOW}🛑 Stopping existing container...${NC}"
podman stop log-analyzer 2>/dev/null || true
podman rm log-analyzer 2>/dev/null || true

# Start all-in-one container
echo -e "${YELLOW}🔄 Starting Log Analysis System...${NC}"
podman run -d \
  --name log-analyzer \
  -p 3000:3000 \
  -p 8080:8080 \
  docker.io/nginx:alpine \
  sh -c '
# Install curl for health checks
apk add --no-cache curl

# Create the web application
mkdir -p /usr/share/nginx/html
cat > /usr/share/nginx/html/index.html << "EOF"
<!DOCTYPE html>
<html>
<head>
    <title>Log Analysis System</title>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #f8fafc; }
        .container { max-width: 1200px; margin: 0 auto; padding: 20px; }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 12px; margin-bottom: 30px; text-align: center; }
        .header h1 { font-size: 2.5rem; margin-bottom: 10px; }
        .header p { font-size: 1.1rem; opacity: 0.9; }
        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; margin-bottom: 30px; }
        .card { background: white; padding: 25px; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.05); border: 1px solid #e2e8f0; }
        .card h2 { color: #2d3748; margin-bottom: 15px; font-size: 1.3rem; }
        .stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 15px; }
        .stat { text-align: center; padding: 15px; background: #f7fafc; border-radius: 8px; }
        .stat-number { font-size: 1.8rem; font-weight: bold; color: #4299e1; }
        .stat-label { font-size: 0.9rem; color: #718096; margin-top: 5px; }
        .search-container { margin-bottom: 20px; }
        .search-input { width: 100%; padding: 12px 16px; border: 2px solid #e2e8f0; border-radius: 8px; font-size: 1rem; }
        .search-input:focus { outline: none; border-color: #4299e1; }
        .btn-group { display: flex; gap: 10px; margin-top: 15px; flex-wrap: wrap; }
        .btn { padding: 10px 20px; border: none; border-radius: 6px; cursor: pointer; font-weight: 500; transition: all 0.2s; }
        .btn-primary { background: #4299e1; color: white; }
        .btn-primary:hover { background: #3182ce; }
        .btn-secondary { background: #e2e8f0; color: #4a5568; }
        .btn-secondary:hover { background: #cbd5e0; }
        .log-container { max-height: 600px; overflow-y: auto; }
        .log-entry { padding: 15px; border-bottom: 1px solid #e2e8f0; transition: background 0.2s; }
        .log-entry:hover { background: #f7fafc; }
        .log-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
        .log-level { padding: 4px 8px; border-radius: 4px; font-size: 0.8rem; font-weight: bold; color: white; }
        .ERROR { background: #e53e3e; } .FATAL { background: #c53030; }
        .WARN { background: #dd6b20; } .INFO { background: #38a169; }
        .DEBUG { background: #718096; }
        .log-meta { font-size: 0.9rem; color: #718096; }
        .log-message { margin-top: 8px; color: #2d3748; line-height: 1.5; }
        .status-indicator { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 8px; }
        .status-up { background: #38a169; }
        .status-down { background: #e53e3e; }
        .loading { text-align: center; padding: 40px; color: #718096; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🚀 Log Analysis System</h1>
            <p>Enterprise Log Analysis & Monitoring Platform</p>
            <p><span class="status-indicator status-up"></span>System Status: <span id="systemStatus">Online</span></p>
        </div>
        
        <div class="grid">
            <div class="card">
                <h2>📊 System Statistics</h2>
                <div class="stats">
                    <div class="stat">
                        <div class="stat-number" id="totalLogs">1,247</div>
                        <div class="stat-label">Total Logs</div>
                    </div>
                    <div class="stat">
                        <div class="stat-number" id="errorCount">23</div>
                        <div class="stat-label">Errors</div>
                    </div>
                    <div class="stat">
                        <div class="stat-number" id="warningCount">156</div>
                        <div class="stat-label">Warnings</div>
                    </div>
                    <div class="stat">
                        <div class="stat-number" id="uptime">99.9%</div>
                        <div class="stat-label">Uptime</div>
                    </div>
                </div>
            </div>
            
            <div class="card">
                <h2>🔍 Quick Actions</h2>
                <div class="btn-group">
                    <button class="btn btn-primary" onclick="showLogs(\"ERROR\")">View Errors</button>
                    <button class="btn btn-primary" onclick="showLogs(\"WARN\")">View Warnings</button>
                    <button class="btn btn-secondary" onclick="showLogs(\"INFO\")">View Info</button>
                    <button class="btn btn-secondary" onclick="showLogs(\"*\")">View All</button>
                </div>
            </div>
        </div>
        
        <div class="card">
            <h2>🔍 Search Logs</h2>
            <div class="search-container">
                <input type="text" class="search-input" id="searchInput" placeholder="Search logs... (try: error, payment, login, database)" value="*">
                <div class="btn-group">
                    <button class="btn btn-primary" onclick="searchLogs()">Search</button>
                    <button class="btn btn-secondary" onclick="clearSearch()">Clear</button>
                </div>
            </div>
        </div>
        
        <div class="card">
            <h2>📋 Recent Log Entries</h2>
            <div id="logResults" class="log-container">
                <div class="loading">Loading log entries...</div>
            </div>
        </div>
    </div>

    <script>
        // Sample log data
        const sampleLogs = [
            { id: 1, timestamp: new Date(Date.now() - 300000), level: "INFO", app: "web-service", host: "server-1", message: "User authentication successful for user: john.doe@company.com" },
            { id: 2, timestamp: new Date(Date.now() - 240000), level: "ERROR", app: "payment-service", host: "server-2", message: "Payment processing failed: Invalid credit card number" },
            { id: 3, timestamp: new Date(Date.now() - 180000), level: "WARN", app: "auth-service", host: "server-1", message: "Multiple failed login attempts detected from IP: 192.168.1.100" },
            { id: 4, timestamp: new Date(Date.now() - 120000), level: "INFO", app: "database-service", host: "server-3", message: "Database backup completed successfully (2.3GB)" },
            { id: 5, timestamp: new Date(Date.now() - 60000), level: "DEBUG", app: "web-service", host: "server-1", message: "Cache hit rate: 94.2% for user session data" },
            { id: 6, timestamp: new Date(Date.now() - 30000), level: "ERROR", app: "notification-service", host: "server-2", message: "Failed to send email notification: SMTP server timeout" },
            { id: 7, timestamp: new Date(Date.now() - 15000), level: "INFO", app: "api-gateway", host: "server-1", message: "API request processed: GET /api/v1/users (200ms)" },
            { id: 8, timestamp: new Date(Date.now() - 5000), level: "WARN", app: "monitoring-service", host: "server-3", message: "High memory usage detected: 87% of available RAM" },
            { id: 9, timestamp: new Date(), level: "INFO", app: "web-service", host: "server-1", message: "New user registration: jane.smith@company.com" },
            { id: 10, timestamp: new Date(), level: "FATAL", app: "database-service", host: "server-3", message: "Database connection pool exhausted - system critical" }
        ];
        
        function showLogs(level) {
            document.getElementById("searchInput").value = level;
            searchLogs();
        }
        
        function searchLogs() {
            const query = document.getElementById("searchInput").value.toLowerCase();
            const resultsDiv = document.getElementById("logResults");
            
            let filteredLogs = sampleLogs;
            if (query && query !== "*") {
                filteredLogs = sampleLogs.filter(log => 
                    log.level.toLowerCase().includes(query) ||
                    log.message.toLowerCase().includes(query) ||
                    log.app.toLowerCase().includes(query)
                );
            }
            
            if (filteredLogs.length === 0) {
                resultsDiv.innerHTML = "<div class=\"loading\">No logs found matching your search.</div>";
                return;
            }
            
            resultsDiv.innerHTML = filteredLogs.map(log => `
                <div class="log-entry">
                    <div class="log-header">
                        <span class="log-level ${log.level}">${log.level}</span>
                        <span class="log-meta">${log.app} @ ${log.host}</span>
                        <span class="log-meta">${log.timestamp.toLocaleString()}</span>
                    </div>
                    <div class="log-message">${log.message}</div>
                </div>
            `).join("");
        }
        
        function clearSearch() {
            document.getElementById("searchInput").value = "*";
            searchLogs();
        }
        
        function updateStats() {
            const errorCount = sampleLogs.filter(log => log.level === "ERROR" || log.level === "FATAL").length;
            const warningCount = sampleLogs.filter(log => log.level === "WARN").length;
            
            document.getElementById("totalLogs").textContent = sampleLogs.length.toLocaleString();
            document.getElementById("errorCount").textContent = errorCount;
            document.getElementById("warningCount").textContent = warningCount;
        }
        
        // Initialize
        searchLogs();
        updateStats();
        
        // Enter key search
        document.getElementById("searchInput").addEventListener("keypress", function(e) {
            if (e.key === "Enter") searchLogs();
        });
        
        // Auto-refresh simulation
        setInterval(() => {
            // Simulate new log entry
            const levels = ["INFO", "WARN", "ERROR", "DEBUG"];
            const apps = ["web-service", "auth-service", "payment-service"];
            const messages = [
                "User session created",
                "API request processed", 
                "Database query executed",
                "Cache updated",
                "Security scan completed"
            ];
            
            const newLog = {
                id: sampleLogs.length + 1,
                timestamp: new Date(),
                level: levels[Math.floor(Math.random() * levels.length)],
                app: apps[Math.floor(Math.random() * apps.length)],
                host: "server-" + (Math.floor(Math.random() * 3) + 1),
                message: messages[Math.floor(Math.random() * messages.length)] + " #" + (sampleLogs.length + 1)
            };
            
            sampleLogs.unshift(newLog);
            if (sampleLogs.length > 50) sampleLogs.pop(); // Keep only recent logs
            
            updateStats();
            if (document.getElementById("searchInput").value === "*") {
                searchLogs(); // Refresh if showing all logs
            }
        }, 10000); // Add new log every 10 seconds
    </script>
</body>
</html>
EOF

# Start nginx
nginx -g "daemon off;"
'

echo -e "${YELLOW}⏳ Waiting for system to start...${NC}"
sleep 5

# Check if container is running
if podman ps | grep -q log-analyzer; then
    echo -e "${GREEN}🎉 Log Analysis System is running!${NC}"
    echo
    echo -e "${BLUE}Access the application:${NC}"
    echo -e "${YELLOW}  🌐 Web Interface: http://localhost:3000${NC}"
    echo
    echo -e "${BLUE}Features:${NC}"
    echo "  ✅ Interactive log search and filtering"
    echo "  ✅ Real-time log statistics"
    echo "  ✅ Sample log data with different levels"
    echo "  ✅ Responsive web interface"
    echo "  ✅ Auto-refreshing log entries"
    echo
    echo -e "${BLUE}To stop: ${YELLOW}podman stop log-analyzer${NC}"
    echo -e "${BLUE}To remove: ${YELLOW}podman rm log-analyzer${NC}"
    echo
    echo -e "${GREEN}🚀 Happy log analyzing!${NC}"
else
    echo -e "${RED}❌ Failed to start container${NC}"
    echo "Check logs with: podman logs log-analyzer"
    exit 1
fi
