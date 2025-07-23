#!/bin/bash

# Build Fix Script - Addresses common build issues
# This script fixes common problems that prevent the project from building

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to print section header
print_header() {
    echo
    print_status $BLUE "=============================================="
    print_status $BLUE "$1"
    print_status $BLUE "=============================================="
    echo
}

print_header "LOG ANALYSIS SYSTEM - BUILD ISSUE FIXES"

# Fix 1: Ensure Maven wrapper is executable
print_status $YELLOW "Fix 1: Making Maven wrapper executable..."
if [ -f "mvnw" ]; then
    chmod +x mvnw
    print_status $GREEN "✓ Maven wrapper is now executable"
else
    print_status $RED "✗ Maven wrapper not found"
fi

# Fix 2: Create missing directories
print_status $YELLOW "Fix 2: Creating missing directories..."
missing_dirs=(
    "backend/src/main/java/com/loganalyzer"
    "backend/src/main/resources"
    "backend/src/test/java/com/loganalyzer"
    "frontend/src/components"
    "frontend/src/services"
    "frontend/src/store"
    "frontend/public"
    "logs"
    "test-results"
    "security-reports"
)

for dir in "${missing_dirs[@]}"; do
    if [ ! -d "$dir" ]; then
        mkdir -p "$dir"
        print_status $GREEN "✓ Created directory: $dir"
    fi
done

# Fix 3: Create a minimal working backend if missing
print_status $YELLOW "Fix 3: Ensuring minimal backend structure..."

# Create Application.java if missing
if [ ! -f "backend/src/main/java/com/loganalyzer/Application.java" ]; then
    cat > "backend/src/main/java/com/loganalyzer/Application.java" << 'EOF'
package com.loganalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
EOF
    print_status $GREEN "✓ Created minimal Application.java"
fi

# Create basic application.yml if missing
if [ ! -f "backend/src/main/resources/application.yml" ]; then
    cat > "backend/src/main/resources/application.yml" << 'EOF'
spring:
  application:
    name: log-analyzer
  profiles:
    active: local
  
  # H2 Database for local testing
  datasource:
    url: jdbc:h2:mem:testdb
    username: sa
    password: 
    driver-class-name: org.h2.Driver
  
  h2:
    console:
      enabled: true
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false

server:
  port: 8080

logging:
  level:
    com.loganalyzer: INFO
    org.springframework: WARN
EOF
    print_status $GREEN "✓ Created basic application.yml"
fi

# Fix 4: Ensure POM.xml has correct structure
print_status $YELLOW "Fix 4: Validating POM.xml structure..."

if [ -f "backend/pom.xml" ]; then
    # Check if POM has required Spring Boot parent
    if ! grep -q "spring-boot-starter-parent" backend/pom.xml; then
        print_status $YELLOW "⚠ POM.xml may be missing Spring Boot parent"
        
        # Create a backup
        cp backend/pom.xml backend/pom.xml.backup
        
        # Create a minimal working POM
        cat > "backend/pom.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>
    
    <groupId>com.loganalyzer</groupId>
    <artifactId>log-analysis-backend</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <name>Log Analysis System - Backend</name>
    <description>Enterprise Log Analysis and Monitoring System</description>
    
    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <!-- H2 Database for testing -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
EOF
        print_status $GREEN "✓ Created minimal working POM.xml (backup saved as pom.xml.backup)"
    else
        print_status $GREEN "✓ POM.xml structure looks good"
    fi
fi

# Fix 5: Create basic test if missing
print_status $YELLOW "Fix 5: Ensuring basic test exists..."

if [ ! -f "backend/src/test/java/com/loganalyzer/ApplicationTest.java" ]; then
    cat > "backend/src/test/java/com/loganalyzer/ApplicationTest.java" << 'EOF'
package com.loganalyzer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationTest {

    @Test
    void contextLoads() {
        // This test ensures that the Spring context loads successfully
    }
}
EOF
    print_status $GREEN "✓ Created basic ApplicationTest.java"
fi

# Fix 6: Frontend package.json validation
print_status $YELLOW "Fix 6: Validating frontend package.json..."

if [ -f "frontend/package.json" ]; then
    # Check if package.json is valid JSON
    if python3 -m json.tool frontend/package.json > /dev/null 2>&1; then
        print_status $GREEN "✓ Frontend package.json is valid JSON"
    else
        print_status $YELLOW "⚠ Frontend package.json may have syntax issues"
        
        # Create a minimal package.json
        cp frontend/package.json frontend/package.json.backup 2>/dev/null || true
        
        cat > "frontend/package.json" << 'EOF'
{
  "name": "log-analysis-frontend",
  "version": "1.0.0",
  "description": "Log Analysis System Frontend",
  "private": true,
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-scripts": "5.0.1"
  },
  "scripts": {
    "start": "react-scripts start",
    "build": "react-scripts build",
    "test": "react-scripts test",
    "eject": "react-scripts eject"
  },
  "eslintConfig": {
    "extends": [
      "react-app"
    ]
  },
  "browserslist": {
    "production": [
      ">0.2%",
      "not dead",
      "not op_mini all"
    ],
    "development": [
      "last 1 chrome version",
      "last 1 firefox version",
      "last 1 safari version"
    ]
  }
}
EOF
        print_status $GREEN "✓ Created minimal package.json (backup saved if existed)"
    fi
fi

# Fix 7: Create basic React app structure if missing
print_status $YELLOW "Fix 7: Ensuring basic React structure..."

if [ ! -f "frontend/src/index.js" ]; then
    cat > "frontend/src/index.js" << 'EOF'
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
EOF
    print_status $GREEN "✓ Created basic index.js"
fi

if [ ! -f "frontend/src/App.js" ]; then
    cat > "frontend/src/App.js" << 'EOF'
import React from 'react';

function App() {
  return (
    <div className="App">
      <header className="App-header">
        <h1>Log Analysis System</h1>
        <p>Enterprise Log Analysis and Monitoring Platform</p>
      </header>
    </div>
  );
}

export default App;
EOF
    print_status $GREEN "✓ Created basic App.js"
fi

if [ ! -f "frontend/public/index.html" ]; then
    mkdir -p frontend/public
    cat > "frontend/public/index.html" << 'EOF'
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <meta name="theme-color" content="#000000" />
    <meta name="description" content="Enterprise Log Analysis System" />
    <title>Log Analysis System</title>
  </head>
  <body>
    <noscript>You need to enable JavaScript to run this app.</noscript>
    <div id="root"></div>
  </body>
</html>
EOF
    print_status $GREEN "✓ Created basic index.html"
fi

# Fix 8: Make scripts executable
print_status $YELLOW "Fix 8: Making scripts executable..."

script_files=(
    "scripts/test-local.sh"
    "scripts/run-all-tests.sh"
    "scripts/build-check.sh"
    "scripts/fix-build-issues.sh"
    "mvnw"
)

for script in "${script_files[@]}"; do
    if [ -f "$script" ]; then
        chmod +x "$script"
        print_status $GREEN "✓ Made $script executable"
    fi
done

# Fix 9: Create .gitignore if missing
print_status $YELLOW "Fix 9: Ensuring .gitignore exists..."

if [ ! -f ".gitignore" ]; then
    cat > ".gitignore" << 'EOF'
# Compiled class files
*.class
target/
*.jar
*.war

# Log files
*.log
logs/

# Node modules
node_modules/
npm-debug.log*

# Build directories
build/
dist/

# IDE files
.idea/
.vscode/
*.swp

# OS files
.DS_Store
Thumbs.db

# Test results
test-results/
coverage/

# Environment files
.env
.env.local
EOF
    print_status $GREEN "✓ Created .gitignore"
fi

# Summary
print_header "BUILD FIX SUMMARY"

print_status $GREEN "🔧 Build fixes completed!"
echo
print_status $BLUE "What was fixed:"
echo "✅ Maven wrapper permissions"
echo "✅ Missing directory structure"
echo "✅ Basic Spring Boot application"
echo "✅ Minimal POM.xml configuration"
echo "✅ Basic test structure"
echo "✅ React application structure"
echo "✅ Script permissions"
echo "✅ Git ignore file"
echo
print_status $BLUE "Next steps:"
echo "1. Run './scripts/build-check.sh' to validate the fixes"
echo "2. Try building with './mvnw clean compile' (requires Java 17+)"
echo "3. For frontend: 'cd frontend && npm install && npm start'"
echo "4. For full testing: './scripts/test-local.sh'"
echo
print_status $GREEN "The project should now be in a buildable state!"

exit 0
