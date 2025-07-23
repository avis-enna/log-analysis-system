#!/bin/bash

# Sample log data generator for testing
echo "Generating sample log data..."

# Create sample log directory
mkdir -p /Users/ssivared/MyResume/log-analysis-system/sample-logs

# Generate Apache-style access logs
cat > /Users/ssivared/MyResume/log-analysis-system/sample-logs/access.log << 'EOF'
192.168.1.1 - - [08/Jul/2025:10:00:01 +0000] "GET /api/users HTTP/1.1" 200 1234 "https://example.com" "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
192.168.1.2 - - [08/Jul/2025:10:00:02 +0000] "POST /api/login HTTP/1.1" 401 567 "https://example.com/login" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"
192.168.1.3 - - [08/Jul/2025:10:00:03 +0000] "GET /api/products HTTP/1.1" 200 2345 "https://example.com" "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15"
10.0.0.1 - - [08/Jul/2025:10:00:04 +0000] "GET /admin/users HTTP/1.1" 403 0 "" "curl/7.68.0"
192.168.1.4 - - [08/Jul/2025:10:00:05 +0000] "POST /api/orders HTTP/1.1" 500 0 "https://example.com/shop" "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
192.168.1.1 - - [08/Jul/2025:10:00:06 +0000] "GET /api/profile HTTP/1.1" 200 890 "https://example.com" "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
192.168.1.5 - - [08/Jul/2025:10:00:07 +0000] "GET /../../../etc/passwd HTTP/1.1" 404 0 "" "python-requests/2.25.1"
192.168.1.6 - - [08/Jul/2025:10:00:08 +0000] "POST /api/search HTTP/1.1" 200 1567 "https://example.com/search" "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36"
192.168.1.7 - - [08/Jul/2025:10:00:09 +0000] "GET /api/stats HTTP/1.1" 200 3456 "https://example.com/dashboard" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"
192.168.1.2 - - [08/Jul/2025:10:00:10 +0000] "POST /api/login HTTP/1.1" 401 567 "https://example.com/login" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"
EOF

# Generate application logs
cat > /Users/ssivared/MyResume/log-analysis-system/sample-logs/application.log << 'EOF'
2025-07-08 10:00:01,123 [main] INFO  com.example.Application - Application started successfully
2025-07-08 10:00:02,456 [http-nio-8080-exec-1] INFO  com.example.controller.UserController - User login attempt for user: admin
2025-07-08 10:00:03,789 [http-nio-8080-exec-2] ERROR com.example.service.OrderService - Failed to process order: Database connection timeout
2025-07-08 10:00:04,012 [http-nio-8080-exec-3] WARN  com.example.security.SecurityFilter - Suspicious activity detected from IP: 10.0.0.1
2025-07-08 10:00:05,345 [http-nio-8080-exec-4] INFO  com.example.controller.ProductController - Product search executed successfully
2025-07-08 10:00:06,678 [http-nio-8080-exec-5] ERROR com.example.service.PaymentService - Payment processing failed: Invalid credit card
2025-07-08 10:00:07,901 [http-nio-8080-exec-6] WARN  com.example.security.SecurityFilter - Multiple failed login attempts from IP: 192.168.1.2
2025-07-08 10:00:08,234 [http-nio-8080-exec-7] INFO  com.example.controller.StatsController - Dashboard statistics generated
2025-07-08 10:00:09,567 [http-nio-8080-exec-8] ERROR com.example.service.DatabaseService - Database connection pool exhausted
2025-07-08 10:00:10,890 [http-nio-8080-exec-9] CRITICAL com.example.service.SecurityService - SQL injection attempt detected: SELECT * FROM users WHERE id = '1' OR '1'='1'
EOF

# Generate system logs
cat > /Users/ssivared/MyResume/log-analysis-system/sample-logs/system.log << 'EOF'
Jul  8 10:00:01 server01 kernel: [1234567.890] Out of memory: Kill process 1234 (java) score 900 or sacrifice child
Jul  8 10:00:02 server01 sshd[5678]: Failed password for invalid user admin from 192.168.1.100 port 52345 ssh2
Jul  8 10:00:03 server01 nginx: 2025/07/08 10:00:03 [error] 9012#9012: *1 connect() failed (111: Connection refused) while connecting to upstream
Jul  8 10:00:04 server01 postgresql[3456]: LOG: database system is ready to accept connections
Jul  8 10:00:05 server01 redis[7890]: Warning: Redis is running in protected mode
Jul  8 10:00:06 server01 fail2ban.actions[2345]: NOTICE  [sshd] Ban 192.168.1.100
Jul  8 10:00:07 server01 systemd[1]: Started Log Analysis System Service
Jul  8 10:00:08 server01 cron[1111]: (CRON) INFO (pidfile fd = 3)
Jul  8 10:00:09 server01 kernel: [1234568.901] TCP: request_sock_TCP: Possible SYN flooding on port 80
Jul  8 10:00:10 server01 systemd[1]: log-analysis.service: Service has a main PID which does not match the control group
EOF

echo "Sample log files created in sample-logs directory:"
ls -la /Users/ssivared/MyResume/log-analysis-system/sample-logs/

echo ""
echo "To test the log analysis system:"
echo "1. Start the application: ./start.sh"
echo "2. Upload sample logs via the web interface at http://localhost:8080"
echo "3. Monitor the dashboard for processed logs and generated alerts"
