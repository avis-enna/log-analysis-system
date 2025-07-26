#!/bin/bash

# Script to populate the log analysis system with sample data

echo "🔄 Populating log analysis system with sample data..."

# Wait for backend to be ready
echo "⏳ Waiting for backend to be ready..."
for i in {1..30}; do
    if curl -s "http://localhost:8080/api/dashboard/metrics" > /dev/null 2>&1; then
        echo "✅ Backend is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ Backend not ready after 30 attempts. Please check if the backend is running."
        exit 1
    fi
    sleep 2
done

# Generate sample logs using the new API endpoint
echo "📊 Generating 100 sample logs..."
response=$(curl -s -X POST "http://localhost:8080/api/logs/generate-sample?count=100" \
  -H "Content-Type: application/json" \
  -w "\n%{http_code}")

status_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

if [ "$status_code" = "200" ]; then
    echo "✅ Successfully generated sample logs!"
    echo "📈 Response: $response_body"
else
    echo "❌ Failed to generate sample logs. Status code: $status_code"
    echo "🔍 Response: $response_body"
    
    # Try individual log ingestion as fallback
    echo "🔄 Trying individual log ingestion as fallback..."
    
    # Sample log data
    logs=(
        "2024-01-25 10:15:32,123 [main] INFO com.loganalyzer.service - Application started successfully"
        "2024-01-25 10:16:45,456 [http-nio-8080-exec-1] WARN com.loganalyzer.controller - High memory usage detected: 85%"
        "2024-01-25 10:17:12,789 [scheduler-1] ERROR com.loganalyzer.security - Authentication failed for user admin"
        "192.168.1.100 - - [25/Jan/2024:10:18:33 +0000] \"GET /api/logs HTTP/1.1\" 200 1024"
        "2024-01-25 10:19:44,321 [pool-2-thread-1] DEBUG com.loganalyzer.database - Database connection established"
        "Jan 25 10:20:15 web-01 nginx[1234]: SSL certificate expires in 30 days"
        "2024-01-25 10:21:08,654 [main] CRITICAL com.loganalyzer.firewall - SQL injection attempt detected"
        "192.168.1.101 - - [25/Jan/2024:10:22:19 +0000] \"POST /api/upload HTTP/1.1\" 500 256"
        "2024-01-25 10:23:30,987 [worker-1] INFO com.loganalyzer.cache - Cache cleared successfully"
        "Jan 25 10:24:41 db-master mysql[5678]: Slow query detected: 2500ms"
    )
    
    success_count=0
    for log in "${logs[@]}"; do
        result=$(curl -s -X POST "http://localhost:8080/api/logs/ingest" \
          -H "Content-Type: application/json" \
          -d "{\"log\":\"$log\",\"source\":\"script\"}" \
          -w "%{http_code}")
        
        status=$(echo "$result" | tail -c 4)
        if [ "$status" = "200" ]; then
            ((success_count++))
        fi
    done
    
    echo "✅ Successfully ingested $success_count individual logs"
fi

# Check current log count
echo "📊 Checking current log count..."
metrics=$(curl -s "http://localhost:8080/api/dashboard/metrics")
if [ $? -eq 0 ]; then
    echo "📈 Current system metrics:"
    echo "$metrics" | python3 -m json.tool 2>/dev/null || echo "$metrics"
else
    echo "❌ Failed to fetch current metrics"
fi

echo "🎉 Log population complete!"
echo "🌐 You can now view the dashboard at http://localhost:3000"
