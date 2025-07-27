#!/bin/bash

# Kafka and Redis Integration Test Script
# This script demonstrates the new Kafka and Redis features with role-based authentication

echo "========================================"
echo "Log Analysis System - Kafka & Redis Demo"
echo "========================================"

BASE_URL="http://localhost:8080/api/v1"

# Test users with different roles
ADMIN_USER="admin:admin123"
DEV_USER="dev:dev123"
QA_USER="qa:qa123"
DEVOPS_USER="devops:devops123"

echo ""
echo "1. Testing Authentication and Permissions"
echo "----------------------------------------"

# Test auth endpoints
echo "Getting test credentials:"
curl -s "$BASE_URL/auth/test-credentials" | jq .

echo ""
echo "Getting user info for admin user:"
curl -s -u $ADMIN_USER "$BASE_URL/auth/me" | jq .

echo ""
echo "Getting permissions for dev user:"
curl -s -u $DEV_USER "$BASE_URL/auth/permissions" | jq .

echo ""
echo "2. Testing Kafka Log Ingestion"
echo "------------------------------"

# Test Kafka log ingestion (Developer access)
echo "Ingesting log via Kafka (as developer):"
curl -s -u $DEV_USER -X POST "$BASE_URL/logs/ingest/kafka" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "User authentication successful for user john_doe",
    "level": "INFO",
    "source": "auth-service",
    "application": "user-management",
    "environment": "production",
    "category": "security"
  }' | jq .

echo ""
echo "Ingesting error log via Kafka (as admin):"
curl -s -u $ADMIN_USER -X POST "$BASE_URL/logs/ingest/kafka" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Database connection failed: Connection timeout after 30 seconds",
    "level": "ERROR",
    "source": "database-service",
    "application": "order-processing",
    "environment": "production", 
    "category": "database"
  }' | jq .

echo ""
echo "Bulk ingesting logs via Kafka:"
curl -s -u $DEVOPS_USER -X POST "$BASE_URL/logs/ingest/kafka/bulk" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "message": "Performance warning: Response time exceeded 2 seconds",
      "level": "WARN",
      "source": "api-gateway",
      "application": "gateway",
      "environment": "production",
      "category": "performance"
    },
    {
      "message": "Deployment completed successfully for version 2.1.3",
      "level": "INFO", 
      "source": "ci-cd-pipeline",
      "application": "deployment",
      "environment": "production",
      "category": "deployment"
    },
    {
      "message": "Critical security alert: Multiple failed login attempts detected",
      "level": "CRITICAL",
      "source": "security-monitor",
      "application": "security",
      "environment": "production",
      "category": "security"
    }
  ]' | jq .

echo ""
echo "3. Testing Direct Log Ingestion"
echo "-------------------------------"

# Test direct log ingestion
echo "Ingesting log directly (as developer):"
curl -s -u $DEV_USER -X POST "$BASE_URL/logs/ingest/direct" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Application startup completed in 3.2 seconds",
    "source": "application-server"
  }' | jq .

echo ""
echo "4. Testing Redis Cache Operations"
echo "--------------------------------"

# Wait a moment for logs to be processed
echo "Waiting for logs to be processed..."
sleep 3

echo "Getting recent logs from cache (as QA):"
curl -s -u $QA_USER "$BASE_URL/logs/recent?limit=10" | jq .

echo ""
echo "Getting recent error logs from cache:"
curl -s -u $QA_USER "$BASE_URL/logs/recent/errors?limit=5" | jq .

echo ""
echo "Getting real-time statistics:"
curl -s -u $QA_USER "$BASE_URL/logs/stats/realtime" | jq .

echo ""
echo "Getting source statistics:"
curl -s -u $QA_USER "$BASE_URL/logs/stats/sources" | jq .

echo ""
echo "Getting hourly statistics:"
curl -s -u $QA_USER "$BASE_URL/logs/stats/hourly?hours=6" | jq .

echo ""
echo "5. Testing Admin-Only Features"
echo "-----------------------------"

echo "Checking service health (admin only):"
curl -s -u $ADMIN_USER "$BASE_URL/logs/health" | jq .

echo ""
echo "Getting available roles (admin only):"
curl -s -u $ADMIN_USER "$BASE_URL/auth/roles" | jq .

echo ""
echo "6. Testing Dashboard with Real-time Data"
echo "---------------------------------------"

echo "Getting dashboard statistics:"
curl -s -u $QA_USER "$BASE_URL/dashboard/stats" | jq .

echo ""
echo "Getting log volume data:"
curl -s -u $QA_USER "$BASE_URL/dashboard/volume" | jq '.[:3]' # Show first 3 entries

echo ""
echo "Getting top sources:"
curl -s -u $QA_USER "$BASE_URL/dashboard/top-sources?limit=5" | jq .

echo ""
echo "Getting error trends:"
curl -s -u $QA_USER "$BASE_URL/dashboard/error-trends" | jq '.[:3]' # Show first 3 entries

echo ""
echo "7. Testing Alert System"
echo "----------------------"

echo "Getting current alerts:"
curl -s -u $QA_USER "$BASE_URL/alerts" | jq .

echo ""
echo "Getting alert statistics:"
curl -s -u $QA_USER "$BASE_URL/alerts/stats" | jq .

echo ""
echo "8. Testing Access Control"
echo "------------------------"

echo "Testing QA user trying to ingest logs (should fail):"
curl -s -u $QA_USER -X POST "$BASE_URL/logs/ingest/kafka" \
  -H "Content-Type: application/json" \
  -d '{"message": "test", "level": "INFO", "source": "test"}' | jq .

echo ""
echo "Testing unauthorized access to admin endpoint:"
curl -s -u $DEV_USER "$BASE_URL/logs/health" | jq .

echo ""
echo "========================================"
echo "Demo completed!"
echo ""
echo "Available Users:"
echo "- admin:admin123 (Full access)"
echo "- dev:dev123 (Developer access)"
echo "- qa:qa123 (QA access)"
echo "- devops:devops123 (Admin + Developer)"
echo "- qaread:qaread123 (QA read-only)"
echo ""
echo "Key Features Demonstrated:"
echo "- Kafka-based log ingestion"
echo "- Redis caching for real-time access"
echo "- Role-based access control"
echo "- Real-time statistics and monitoring"
echo "- Enhanced dashboard with actual data"
echo "- Security and authentication"
echo "========================================"
