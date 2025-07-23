# Log Upload and Generation Guide

This guide explains how to use the manual log upload features and log generators in the Enterprise Log Analysis System.

## 🔧 Manual Log Upload Features

### 1. Web Dashboard Upload
- Navigate to http://localhost:8082
- Scroll down to the "Log Management" section  
- Use the file upload form to upload log files (.log, .txt, .csv)
- Or use the manual log entry form to add individual log entries

### 2. API Endpoints

#### Upload Log File
```bash
curl -X POST http://localhost:8082/api/logs/upload \
  -F "file=@your-log-file.log"
```

#### Add Manual Log Entry
```bash
curl -X POST http://localhost:8082/api/logs/manual \
  -H "Content-Type: application/json" \
  -d '{
    "level": "INFO",
    "source": "application", 
    "message": "Your log message here",
    "host": "localhost",
    "category": "application"
  }'
```

## 📊 Log Generators

### 1. Python Log Generator (Recommended)

The Python script provides advanced log generation with multiple formats:

#### Basic Usage
```bash
# Generate 100 standard format logs
python3 log_generator.py -n 100

# Generate 200 JSON format logs  
python3 log_generator.py -n 200 -f json -o my-logs.log

# Generate demo files in all formats
python3 log_generator.py --demo
```

#### Available Formats
- **standard**: `[timestamp] [level] [source] [host] [category] message`
- **apache**: `ip - - [timestamp] "method endpoint HTTP/1.1" status size`
- **nginx**: `ip - user [timestamp] "method endpoint HTTP/1.1" status size "referer" "user_agent"`
- **syslog**: `timestamp host service[pid]: message`
- **json**: `{"timestamp": "...", "level": "...", ...}`

#### Command Line Options
```bash
python3 log_generator.py --help

options:
  -n, --lines           Number of log lines (default: 100)
  -f, --format         Log format: standard, apache, nginx, syslog, json
  -o, --output         Output filename (auto-generated if not specified)
  -t, --time-range     Time range in hours for timestamps (default: 24)
  --demo               Generate demo files in all formats
```

### 2. Bash Log Generator

Simple bash script for basic log generation:

```bash
# Generate 50 logs (default)
./generate-logs.sh

# Generate 500 logs
./generate-logs.sh 500
```

## 🎯 Testing Workflow

### 1. Generate Sample Logs
```bash
# Create various log types
python3 log_generator.py -n 100 -f standard -o test-standard.log
python3 log_generator.py -n 50 -f json -o test-json.log
```

### 2. Upload via API
```bash
# Upload the generated files
curl -X POST http://localhost:8082/api/logs/upload -F "file=@test-standard.log"
curl -X POST http://localhost:8082/api/logs/upload -F "file=@test-json.log"
```

### 3. Add Manual Test Entries
```bash
# Add a critical security alert
curl -X POST http://localhost:8082/api/logs/manual \
  -H "Content-Type: application/json" \
  -d '{
    "level": "CRITICAL",
    "source": "security",
    "message": "Unauthorized access attempt detected from 192.168.1.100",
    "host": "firewall-01",
    "category": "security"
  }'

# Add an application error
curl -X POST http://localhost:8082/api/logs/manual \
  -H "Content-Type: application/json" \
  -d '{
    "level": "ERROR", 
    "source": "application",
    "message": "Database connection timeout after 30 seconds",
    "host": "app-server-01",
    "category": "database"
  }'
```

### 4. Verify in Dashboard
- Open http://localhost:8082
- Check the Live Event Stream for new entries
- View updated metrics and alerts
- Use the search functionality to find specific logs

## 📋 Log Format Examples

### Standard Format
```
[2025-07-09 14:04:11] [INFO] [nginx] [web-01] [application] User alice logged in successfully
[2025-07-09 14:04:12] [WARNING] [database] [db-master] [system] High memory usage detected: 85%
[2025-07-09 14:04:13] [ERROR] [application] [app-01] [network] Database connection failed
```

### JSON Format
```json
{"timestamp": "2025-07-09 14:04:11", "level": "INFO", "source": "nginx", "host": "web-01", "category": "application", "message": "User alice logged in successfully", "metadata": {"thread": "thread-1", "request_id": "req-12345", "user_agent": "Mozilla/5.0"}}
```

### Apache Access Log Format
```
192.168.1.100 - - [2025-07-09 14:04:11] "GET /api/users HTTP/1.1" 200 1024
192.168.1.101 - - [2025-07-09 14:04:12] "POST /api/auth HTTP/1.1" 401 256
```

## 🔍 Supported Log Levels

- **INFO**: General information messages
- **WARNING**: Warning conditions
- **ERROR**: Error conditions  
- **CRITICAL**: Critical error conditions
- **DEBUG**: Debug-level messages

## 📈 Real-time Features

After uploading logs or adding manual entries:

1. **Automatic Processing**: Logs are immediately ingested and processed
2. **Threat Detection**: Security alerts are generated for suspicious patterns
3. **Dashboard Updates**: Metrics and charts update in real-time
4. **Search Integration**: New logs become searchable immediately
5. **Analytics**: Logs contribute to threat intelligence and correlation analysis

## 🎯 Use Cases

### Development Testing
```bash
# Generate realistic application logs
python3 log_generator.py -n 1000 -f standard --time-range 2
```

### Security Testing  
```bash
# Add security events
python3 log_generator.py -n 100 -f json | \
  sed 's/"INFO"/"CRITICAL"/g' | \
  sed 's/logged in successfully/unauthorized access attempt/g' > security-test.log
```

### Performance Testing
```bash
# Generate large log files
python3 log_generator.py -n 10000 -f json -o large-test.log
```

## 📁 File Management

All generated log files include:
- Timestamps within configurable time ranges
- Realistic message patterns
- Appropriate log level distribution
- Metadata for correlation analysis

The system automatically handles:
- File parsing and validation
- Log level normalization
- Timestamp processing
- Error handling and reporting
