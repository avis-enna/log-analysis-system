#!/bin/bash

# Quick diagnosis script for build issues
echo "=== QUICK BUILD DIAGNOSIS ==="
echo

echo "1. Java Version:"
java -version
echo

echo "2. Maven Version:"
mvn -version 2>/dev/null || echo "Maven not found in PATH"
echo

echo "3. JAVA_HOME:"
echo "JAVA_HOME = $JAVA_HOME"
echo

echo "4. Maven Wrapper:"
if [ -f "mvnw" ]; then
    if [ -x "mvnw" ]; then
        echo "Maven wrapper exists and is executable"
        ./mvnw -version 2>/dev/null || echo "Maven wrapper fails to run"
    else
        echo "Maven wrapper exists but not executable"
        chmod +x mvnw
        echo "Fixed: Made mvnw executable"
    fi
else
    echo "Maven wrapper not found"
fi
echo

echo "5. Project Structure:"
if [ -f "backend/pom.xml" ]; then
    echo "✓ backend/pom.xml exists"
else
    echo "✗ backend/pom.xml missing"
fi

if [ -f "backend/src/main/java/com/loganalyzer/Application.java" ]; then
    echo "✓ Application.java exists"
else
    echo "✗ Application.java missing"
fi
echo

echo "6. Attempting build..."
cd backend 2>/dev/null || { echo "Cannot enter backend directory"; exit 1; }

if [ -x "../mvnw" ]; then
    echo "Trying with Maven wrapper:"
    ../mvnw clean compile 2>&1 | head -20
else
    echo "Trying with system Maven:"
    mvn clean compile 2>&1 | head -20
fi

echo
echo "=== DIAGNOSIS COMPLETE ==="
echo "Please share this output to get specific help!"
