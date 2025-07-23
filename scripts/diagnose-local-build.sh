#!/bin/bash

# Local Build Diagnosis Script
# Helps diagnose build issues on user's laptop with Java 17 and Maven installed

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

print_header() {
    echo
    print_status $BLUE "=============================================="
    print_status $BLUE "$1"
    print_status $BLUE "=============================================="
    echo
}

print_header "LOCAL BUILD DIAGNOSIS - LOG ANALYSIS SYSTEM"

# Step 1: Check Java installation
print_status $YELLOW "Step 1: Checking Java installation..."
if command -v java &> /dev/null; then
    java_version=$(java -version 2>&1 | head -n 1)
    print_status $GREEN "✓ Java found: $java_version"
    
    # Check if it's Java 17+
    java_major=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_major" -ge 17 ]; then
        print_status $GREEN "✓ Java version is 17 or higher"
    else
        print_status $RED "✗ Java version is $java_major, need 17+"
        print_status $YELLOW "Please install Java 17+ or set JAVA_HOME to Java 17"
    fi
    
    # Check JAVA_HOME
    if [ -n "$JAVA_HOME" ]; then
        print_status $GREEN "✓ JAVA_HOME is set: $JAVA_HOME"
        if [ -f "$JAVA_HOME/bin/java" ]; then
            print_status $GREEN "✓ JAVA_HOME points to valid Java installation"
        else
            print_status $RED "✗ JAVA_HOME does not point to valid Java installation"
        fi
    else
        print_status $YELLOW "⚠ JAVA_HOME is not set (may cause issues)"
        print_status $BLUE "Consider setting: export JAVA_HOME=\$(dirname \$(dirname \$(readlink -f \$(which java))))"
    fi
else
    print_status $RED "✗ Java not found in PATH"
    exit 1
fi

# Step 2: Check Maven installation
print_status $YELLOW "Step 2: Checking Maven installation..."
if command -v mvn &> /dev/null; then
    mvn_version=$(mvn -version 2>&1 | head -n 1)
    print_status $GREEN "✓ Maven found: $mvn_version"
    
    # Check Maven's Java version
    mvn_java=$(mvn -version 2>&1 | grep "Java version" | cut -d' ' -f3)
    print_status $BLUE "Maven is using Java: $mvn_java"
    
    if [[ "$mvn_java" == "17"* ]]; then
        print_status $GREEN "✓ Maven is using Java 17+"
    else
        print_status $YELLOW "⚠ Maven is using Java $mvn_java, not 17+"
        print_status $BLUE "This might cause compilation issues"
    fi
else
    print_status $YELLOW "⚠ Maven not found in PATH, will use Maven wrapper"
fi

# Step 3: Check Maven wrapper
print_status $YELLOW "Step 3: Checking Maven wrapper..."
if [ -f "mvnw" ]; then
    if [ -x "mvnw" ]; then
        print_status $GREEN "✓ Maven wrapper found and executable"
        
        # Test Maven wrapper
        print_status $BLUE "Testing Maven wrapper..."
        if ./mvnw -version > /dev/null 2>&1; then
            wrapper_version=$(./mvnw -version 2>&1 | head -n 1)
            print_status $GREEN "✓ Maven wrapper works: $wrapper_version"
        else
            print_status $RED "✗ Maven wrapper fails to execute"
            print_status $BLUE "Error output:"
            ./mvnw -version 2>&1 | head -5
        fi
    else
        print_status $YELLOW "⚠ Maven wrapper not executable, fixing..."
        chmod +x mvnw
        print_status $GREEN "✓ Made Maven wrapper executable"
    fi
else
    print_status $RED "✗ Maven wrapper not found"
fi

# Step 4: Check project structure
print_status $YELLOW "Step 4: Checking project structure..."
required_files=(
    "backend/pom.xml"
    "backend/src/main/java/com/loganalyzer/Application.java"
    "backend/src/main/resources/application.yml"
)

missing_files=()
for file in "${required_files[@]}"; do
    if [ ! -f "$file" ]; then
        missing_files+=("$file")
    fi
done

if [ ${#missing_files[@]} -eq 0 ]; then
    print_status $GREEN "✓ All required project files present"
else
    print_status $RED "✗ Missing files:"
    for file in "${missing_files[@]}"; do
        print_status $RED "  - $file"
    done
    print_status $BLUE "Run './scripts/fix-build-issues.sh' to create missing files"
fi

# Step 5: Test basic compilation
print_status $YELLOW "Step 5: Testing basic compilation..."
cd backend

# Try with Maven wrapper first
if [ -x "../mvnw" ]; then
    print_status $BLUE "Attempting compilation with Maven wrapper..."
    if ../mvnw clean compile -q 2>/dev/null; then
        print_status $GREEN "✓ Compilation successful with Maven wrapper!"
        cd ..
        print_header "DIAGNOSIS COMPLETE - BUILD IS WORKING!"
        print_status $GREEN "🎉 Your build environment is working correctly!"
        print_status $BLUE "You can now run: ./mvnw clean package"
        exit 0
    else
        print_status $YELLOW "⚠ Compilation failed with Maven wrapper"
        print_status $BLUE "Error details:"
        ../mvnw clean compile 2>&1 | tail -10
    fi
fi

# Try with system Maven if available
if command -v mvn &> /dev/null; then
    print_status $BLUE "Attempting compilation with system Maven..."
    if mvn clean compile -q 2>/dev/null; then
        print_status $GREEN "✓ Compilation successful with system Maven!"
        cd ..
        print_header "DIAGNOSIS COMPLETE - BUILD IS WORKING!"
        print_status $GREEN "🎉 Your build environment is working correctly!"
        print_status $BLUE "You can now run: mvn clean package"
        exit 0
    else
        print_status $YELLOW "⚠ Compilation failed with system Maven"
        print_status $BLUE "Error details:"
        mvn clean compile 2>&1 | tail -10
    fi
fi

cd ..

# Step 6: Detailed error analysis
print_status $YELLOW "Step 6: Analyzing compilation errors..."

print_status $BLUE "Checking POM.xml for issues..."
if grep -q "spring-boot-starter-parent" backend/pom.xml; then
    print_status $GREEN "✓ POM has Spring Boot parent"
else
    print_status $RED "✗ POM missing Spring Boot parent"
fi

if grep -q "<java.version>17</java.version>" backend/pom.xml; then
    print_status $GREEN "✓ POM configured for Java 17"
else
    print_status $YELLOW "⚠ POM may not be configured for Java 17"
fi

# Step 7: Check for common issues
print_status $YELLOW "Step 7: Checking for common issues..."

# Check for proxy issues
if [ -n "$HTTP_PROXY" ] || [ -n "$HTTPS_PROXY" ]; then
    print_status $YELLOW "⚠ Proxy environment variables detected"
    print_status $BLUE "HTTP_PROXY: ${HTTP_PROXY:-not set}"
    print_status $BLUE "HTTPS_PROXY: ${HTTPS_PROXY:-not set}"
    print_status $BLUE "You may need to configure Maven proxy settings"
fi

# Check Maven local repository
if [ -d "$HOME/.m2/repository" ]; then
    repo_size=$(du -sh "$HOME/.m2/repository" 2>/dev/null | cut -f1)
    print_status $BLUE "Maven local repository size: $repo_size"
    
    # Check if repository is corrupted
    if find "$HOME/.m2/repository" -name "*.jar" -size 0 2>/dev/null | head -1 | grep -q .; then
        print_status $YELLOW "⚠ Found empty JAR files in Maven repository (may be corrupted)"
        print_status $BLUE "Consider running: rm -rf ~/.m2/repository"
    fi
else
    print_status $BLUE "Maven local repository not found (will be created on first run)"
fi

# Check disk space
available_space=$(df -h . | tail -1 | awk '{print $4}')
print_status $BLUE "Available disk space: $available_space"

# Step 8: Provide specific solutions
print_header "DIAGNOSIS RESULTS & SOLUTIONS"

print_status $RED "❌ Build is not working. Here are the most likely solutions:"
echo

print_status $BLUE "Solution 1: Fix Java/Maven configuration"
echo "export JAVA_HOME=\$(dirname \$(dirname \$(readlink -f \$(which java))))"
echo "export PATH=\$JAVA_HOME/bin:\$PATH"
echo "mvn -version  # Verify Maven uses Java 17"
echo

print_status $BLUE "Solution 2: Clean and rebuild"
echo "rm -rf ~/.m2/repository  # Clear Maven cache"
echo "./mvnw clean compile -U  # Force update dependencies"
echo

print_status $BLUE "Solution 3: Use local profile with embedded services"
echo "./mvnw clean compile -Dspring.profiles.active=local"
echo

print_status $BLUE "Solution 4: Fix project structure"
echo "./scripts/fix-build-issues.sh  # Auto-fix common issues"
echo

print_status $BLUE "Solution 5: Try with different Maven version"
echo "# If using system Maven, try wrapper: ./mvnw clean compile"
echo "# If using wrapper, try system: mvn clean compile"
echo

print_status $BLUE "Solution 6: Check for specific error patterns"
echo "# Run with verbose output to see exact error:"
echo "./mvnw clean compile -X -e"
echo

print_status $YELLOW "If none of these work, please share the exact error message you're seeing."

exit 1
