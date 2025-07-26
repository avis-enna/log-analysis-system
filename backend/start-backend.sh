#!/bin/bash

# Backend startup script to avoid Maven conflicts
cd "$(dirname "$0")"

echo "🔄 Starting Enterprise Log Analysis Backend..."
echo "📍 Working directory: $(pwd)"

# Kill any existing Spring Boot processes
pkill -f "spring-boot" || true
pkill -f "LogAnalyzerApplication" || true

# Clean any previous builds
echo "🧹 Cleaning previous builds..."
mvn clean -q

# Verify we're using the correct POM
echo "📋 Using POM file:"
head -15 pom.xml | grep -E "(groupId|artifactId|version|name)"

# Build the application
echo "🔨 Building application..."
mvn compile -q

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check for compilation errors."
    exit 1
fi

# Start the application with explicit classpath
echo "🚀 Starting application..."
export MAVEN_OPTS="-Xmx1024m"

# Use exec-maven-plugin to run the main class directly
mvn exec:java \
    -Dexec.mainClass="com.loganalyzer.LogAnalyzerApplication" \
    -Dexec.classpathScope=runtime \
    -Dspring.profiles.active=local \
    -Dserver.port=8080
