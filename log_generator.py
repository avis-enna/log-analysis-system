#!/usr/bin/env python3
"""
Advanced Log Generator for Log Analysis System
Generates realistic log files with various formats and patterns
"""

import argparse
import datetime
import json
import random
import sys
from pathlib import Path


class LogGenerator:
    def __init__(self):
        self.levels = ["INFO", "WARNING", "ERROR", "DEBUG", "CRITICAL"]
        self.sources = ["nginx", "application", "database", "security", "firewall", "auth-service", "mail-server", "api-gateway"]
        self.hosts = ["web-01", "web-02", "db-master", "db-slave", "cache-01", "lb-01", "auth-01", "api-01"]
        self.categories = ["authentication", "network", "application", "security", "system", "database", "web", "email"]
        self.users = ["alice", "bob", "charlie", "diana", "eve", "frank", "grace", "henry", "iris", "jack"]
        
        # Weighted distribution for log levels (more INFO, fewer CRITICAL)
        self.level_weights = {
            "INFO": 50,
            "WARNING": 25,
            "ERROR": 15,
            "DEBUG": 8,
            "CRITICAL": 2
        }
        
        # Message templates
        self.message_templates = {
            "INFO": [
                "User {user} logged in successfully",
                "Database connection established",
                "Cache cleared successfully",
                "Service restart completed",
                "File upload completed by {user}",
                "Backup process initiated",
                "Configuration reloaded",
                "Health check passed",
                "Session created for user {user}",
                "API request processed successfully",
                "Email sent to {user}@company.com",
                "File {file} processed successfully",
                "Transaction {tx_id} completed",
                "Report generated for user {user}",
                "Data synchronization completed"
            ],
            "WARNING": [
                "High memory usage detected: {percent}%",
                "Slow database query: {time} ms",
                "Failed authentication attempt for user {user}",
                "Disk space running low: {percent}% used",
                "Connection timeout occurred",
                "Rate limit approaching for IP {ip}",
                "SSL certificate expires in {days} days",
                "Unusual login pattern detected for user {user}",
                "Queue size growing: {count} items",
                "Response time degraded: {time}ms",
                "Memory leak detected in service {service}",
                "Deprecated API endpoint used: {endpoint}"
            ],
            "ERROR": [
                "Database connection failed",
                "Authentication service unavailable",
                "File not found: {file}",
                "Memory allocation error",
                "Network unreachable",
                "Invalid configuration parameter",
                "Service crashed and restarted",
                "Permission denied for user {user}",
                "Timeout waiting for response",
                "Failed to parse configuration file",
                "Transaction {tx_id} failed",
                "Email delivery failed for {user}@company.com",
                "API key invalid for request",
                "Data validation failed for input {input}"
            ],
            "CRITICAL": [
                "Security breach detected from IP {ip}",
                "System compromise attempt",
                "Multiple failed login attempts from {ip}",
                "Unauthorized access to sensitive data",
                "Service completely unavailable",
                "Data corruption detected",
                "Emergency shutdown initiated",
                "Critical security policy violation",
                "Database integrity check failed",
                "Ransomware activity detected",
                "Privilege escalation attempt by {user}",
                "Data exfiltration attempt detected"
            ],
            "DEBUG": [
                "Function {function} called with parameter {param}",
                "Processing request ID: {request_id}",
                "Cache hit for key: {cache_key}",
                "Thread {thread_id} started processing",
                "Validation passed for input: {input}",
                "Query executed in {time} ms",
                "Memory usage: {memory} MB",
                "Connection pool size: {pool_size}",
                "HTTP {method} request to {endpoint}",
                "Session {session_id} updated",
                "Lock acquired for resource {resource}",
                "Configuration value loaded: {config_key}={config_value}"
            ]
        }
        
        # Common log formats
        self.formats = {
            "standard": "[{timestamp}] [{level}] [{source}] [{host}] [{category}] {message}",
            "apache": "{ip} - - [{timestamp}] \"{method} {endpoint} HTTP/1.1\" {status} {size}",
            "nginx": "{ip} - {user} [{timestamp}] \"{method} {endpoint} HTTP/1.1\" {status} {size} \"{referer}\" \"{user_agent}\"",
            "syslog": "{timestamp} {host} {service}[{pid}]: {message}",
            "json": '{{"timestamp": "{timestamp}", "level": "{level}", "source": "{source}", "host": "{host}", "category": "{category}", "message": "{message}", "metadata": {metadata}}}'
        }

    def random_ip(self):
        """Generate a random IP address"""
        return f"{random.randint(1, 255)}.{random.randint(0, 255)}.{random.randint(0, 255)}.{random.randint(1, 255)}"

    def random_timestamp(self, hours_back=24):
        """Generate a random timestamp within the last N hours"""
        now = datetime.datetime.now()
        seconds_back = random.randint(0, hours_back * 3600)
        timestamp = now - datetime.timedelta(seconds=seconds_back)
        return timestamp.strftime("%Y-%m-%d %H:%M:%S")

    def weighted_choice(self, choices, weights):
        """Choose an item based on weights"""
        total = sum(weights.values())
        r = random.uniform(0, total)
        upto = 0
        for choice, weight in weights.items():
            if upto + weight >= r:
                return choice
            upto += weight
        return random.choice(list(choices.keys()))

    def fill_template(self, template, level):
        """Fill a message template with random data"""
        replacements = {
            'user': random.choice(self.users),
            'ip': self.random_ip(),
            'percent': random.randint(70, 99),
            'time': random.randint(100, 5000),
            'days': random.randint(1, 30),
            'count': random.randint(100, 10000),
            'service': random.choice(self.sources),
            'file': f"/var/log/{random.choice(['app', 'system', 'error', 'access'])}.log",
            'tx_id': f"tx-{random.randint(100000, 999999)}",
            'input': f"input-{random.randint(1, 1000)}",
            'function': random.choice(['authenticate', 'validate', 'process', 'calculate', 'transform']),
            'param': random.randint(1, 100),
            'request_id': f"req-{random.randint(10000, 99999)}",
            'cache_key': f"cache-{random.randint(1, 1000)}",
            'thread_id': random.randint(1, 20),
            'memory': random.randint(512, 2048),
            'pool_size': random.randint(10, 50),
            'method': random.choice(['GET', 'POST', 'PUT', 'DELETE']),
            'endpoint': random.choice(['/api/users', '/api/auth', '/api/data', '/admin', '/health']),
            'session_id': f"sess-{random.randint(100000, 999999)}",
            'resource': f"resource-{random.randint(1, 100)}",
            'config_key': random.choice(['database.url', 'app.timeout', 'cache.size', 'log.level']),
            'config_value': random.choice(['localhost:5432', '30000', '1024', 'INFO']),
            'status': random.choice([200, 201, 400, 401, 403, 404, 500, 502]),
            'size': random.randint(100, 50000),
            'referer': random.choice(['https://example.com', 'https://google.com', '-']),
            'user_agent': 'Mozilla/5.0 (compatible; LogBot/1.0)',
            'pid': random.randint(1000, 9999)
        }
        
        return template.format(**replacements)

    def generate_log_entry(self, format_type="standard", hours_back=24):
        """Generate a single log entry"""
        level = self.weighted_choice(self.levels, self.level_weights)
        source = random.choice(self.sources)
        host = random.choice(self.hosts)
        category = random.choice(self.categories)
        timestamp = self.random_timestamp(hours_back)
        
        # Select and fill message template
        template = random.choice(self.message_templates[level])
        message = self.fill_template(template, level)
        
        # Generate metadata for JSON format
        metadata = {
            "thread": f"thread-{random.randint(1, 10)}",
            "request_id": f"req-{random.randint(10000, 99999)}",
            "user_agent": "Mozilla/5.0 (compatible; LogBot/1.0)"
        }
        
        # Format the log entry
        format_template = self.formats[format_type]
        
        if format_type == "json":
            return format_template.format(
                timestamp=timestamp,
                level=level,
                source=source,
                host=host,
                category=category,
                message=message,
                metadata=json.dumps(metadata)
            )
        elif format_type in ["apache", "nginx"]:
            return format_template.format(
                ip=self.random_ip(),
                user=random.choice(self.users),
                timestamp=timestamp,
                method=random.choice(['GET', 'POST', 'PUT', 'DELETE']),
                endpoint=random.choice(['/api/users', '/api/auth', '/api/data', '/admin', '/health']),
                status=random.choice([200, 201, 400, 401, 403, 404, 500, 502]),
                size=random.randint(100, 50000),
                referer=random.choice(['https://example.com', 'https://google.com', '-']),
                user_agent='Mozilla/5.0 (compatible; LogBot/1.0)'
            )
        elif format_type == "syslog":
            return format_template.format(
                timestamp=timestamp,
                host=host,
                service=source,
                pid=random.randint(1000, 9999),
                message=message
            )
        else:  # standard format
            return format_template.format(
                timestamp=timestamp,
                level=level,
                source=source,
                host=host,
                category=category,
                message=message
            )

    def generate_log_file(self, filename, lines, format_type="standard", hours_back=24):
        """Generate a complete log file"""
        timestamp = datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
        if not filename:
            filename = f"sample-logs-{format_type}-{timestamp}.log"
        
        print(f"Generating {lines} log entries in {format_type} format...")
        print(f"Output file: {filename}")
        
        with open(filename, 'w') as f:
            # Write header
            f.write(f"# Generated log file - {datetime.datetime.now()}\n")
            f.write(f"# Lines: {lines}\n")
            f.write(f"# Format: {format_type}\n")
            f.write(f"# Time range: last {hours_back} hours\n")
            f.write("\n")
            
            # Generate log entries
            for i in range(1, lines + 1):
                log_entry = self.generate_log_entry(format_type, hours_back)
                f.write(log_entry + "\n")
                
                if i % 100 == 0:
                    print(f"Generated {i}/{lines} lines...")
        
        # Print summary
        file_size = Path(filename).stat().st_size
        print(f"\n✅ Log file generated successfully: {filename}")
        print(f"📊 Total lines: {lines}")
        print(f"📁 File size: {file_size:,} bytes ({file_size/1024:.1f} KB)")
        print(f"\nUsage examples:")
        print(f"  Upload via dashboard: http://localhost:8082")
        print(f"  View file: head -20 {filename}")
        print(f"  Count log levels: grep -o '\\[.*\\]' {filename} | sort | uniq -c")


def main():
    parser = argparse.ArgumentParser(description="Advanced Log Generator for Log Analysis System")
    parser.add_argument("-n", "--lines", type=int, default=100, help="Number of log lines to generate (default: 100)")
    parser.add_argument("-f", "--format", choices=["standard", "apache", "nginx", "syslog", "json"], 
                       default="standard", help="Log format (default: standard)")
    parser.add_argument("-o", "--output", help="Output filename (auto-generated if not specified)")
    parser.add_argument("-t", "--time-range", type=int, default=24, help="Time range in hours for timestamps (default: 24)")
    parser.add_argument("--demo", action="store_true", help="Generate multiple demo files with different formats")
    
    args = parser.parse_args()
    
    generator = LogGenerator()
    
    if args.demo:
        print("🎬 Generating demo log files in all formats...\n")
        formats = ["standard", "apache", "nginx", "syslog", "json"]
        for fmt in formats:
            filename = f"demo-{fmt}-logs.log"
            generator.generate_log_file(filename, 50, fmt, args.time_range)
            print()
    else:
        generator.generate_log_file(args.output, args.lines, args.format, args.time_range)


if __name__ == "__main__":
    main()
