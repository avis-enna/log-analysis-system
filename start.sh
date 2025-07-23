#!/bin/bash

# Maven wrapper script
./mvnw clean package -DskipTests

# Start the application
echo "Starting Log Analysis System..."
java -jar target/log-analysis-system-1.0.0.jar
