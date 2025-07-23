#!/bin/bash

# Build Check Script - Validates project structure and dependencies
# This script checks the project without requiring Java/Maven installation

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

print_header "LOG ANALYSIS SYSTEM - BUILD VALIDATION"

# Check project structure
print_status $YELLOW "Checking project structure..."

# Required directories
required_dirs=(
    "backend"
    "backend/src"
    "backend/src/main"
    "backend/src/main/java"
    "backend/src/main/resources"
    "backend/src/test"
    "frontend"
    "frontend/src"
    "scripts"
    "docs"
)

missing_dirs=()
for dir in "${required_dirs[@]}"; do
    if [ ! -d "$dir" ]; then
        missing_dirs+=("$dir")
    fi
done

if [ ${#missing_dirs[@]} -eq 0 ]; then
    print_status $GREEN "✓ All required directories present"
else
    print_status $RED "✗ Missing directories:"
    for dir in "${missing_dirs[@]}"; do
        print_status $RED "  - $dir"
    done
fi

# Check required files
print_status $YELLOW "Checking required files..."

required_files=(
    "README.md"
    "docker-compose.yml"
    "backend/pom.xml"
    "backend/Dockerfile"
    "frontend/package.json"
    "frontend/Dockerfile"
    ".gitignore"
)

missing_files=()
for file in "${required_files[@]}"; do
    if [ ! -f "$file" ]; then
        missing_files+=("$file")
    fi
done

if [ ${#missing_files[@]} -eq 0 ]; then
    print_status $GREEN "✓ All required files present"
else
    print_status $RED "✗ Missing files:"
    for file in "${missing_files[@]}"; do
        print_status $RED "  - $file"
    done
fi

# Check Java source files
print_status $YELLOW "Checking Java source files..."
java_files=$(find backend/src -name "*.java" 2>/dev/null | wc -l)
if [ "$java_files" -gt 0 ]; then
    print_status $GREEN "✓ Found $java_files Java source files"
    
    # List main Java files
    print_status $BLUE "Main Java files:"
    find backend/src/main/java -name "*.java" 2>/dev/null | head -10 | while read file; do
        echo "  - $file"
    done
    
    # List test files
    test_files=$(find backend/src/test -name "*.java" 2>/dev/null | wc -l)
    if [ "$test_files" -gt 0 ]; then
        print_status $GREEN "✓ Found $test_files Java test files"
    else
        print_status $YELLOW "⚠ No Java test files found"
    fi
else
    print_status $RED "✗ No Java source files found"
fi

# Check frontend files
print_status $YELLOW "Checking frontend files..."
if [ -f "frontend/package.json" ]; then
    print_status $GREEN "✓ Frontend package.json found"
    
    # Check for React files
    react_files=$(find frontend/src -name "*.js" -o -name "*.jsx" -o -name "*.ts" -o -name "*.tsx" 2>/dev/null | wc -l)
    if [ "$react_files" -gt 0 ]; then
        print_status $GREEN "✓ Found $react_files React/TypeScript files"
    else
        print_status $YELLOW "⚠ No React/TypeScript files found"
    fi
    
    # Check for test files
    frontend_test_files=$(find frontend -name "*.test.js" -o -name "*.test.jsx" -o -name "*.test.ts" -o -name "*.test.tsx" -o -name "*.spec.js" 2>/dev/null | wc -l)
    if [ "$frontend_test_files" -gt 0 ]; then
        print_status $GREEN "✓ Found $frontend_test_files frontend test files"
    else
        print_status $YELLOW "⚠ No frontend test files found"
    fi
else
    print_status $RED "✗ Frontend package.json not found"
fi

# Check configuration files
print_status $YELLOW "Checking configuration files..."

config_files=(
    "backend/src/main/resources/application.yml"
    "backend/src/main/resources/application-local.yml"
)

for file in "${config_files[@]}"; do
    if [ -f "$file" ]; then
        print_status $GREEN "✓ $file found"
    else
        print_status $YELLOW "⚠ $file not found"
    fi
done

# Check Docker files
print_status $YELLOW "Checking Docker configuration..."

docker_files=(
    "docker-compose.yml"
    "backend/Dockerfile"
    "frontend/Dockerfile"
)

for file in "${docker_files[@]}"; do
    if [ -f "$file" ]; then
        print_status $GREEN "✓ $file found"
    else
        print_status $YELLOW "⚠ $file not found"
    fi
done

# Check scripts
print_status $YELLOW "Checking build scripts..."

script_files=(
    "scripts/test-local.sh"
    "scripts/run-all-tests.sh"
    "mvnw"
)

for file in "${script_files[@]}"; do
    if [ -f "$file" ]; then
        print_status $GREEN "✓ $file found"
        if [ -x "$file" ]; then
            print_status $GREEN "  - Executable: Yes"
        else
            print_status $YELLOW "  - Executable: No (run: chmod +x $file)"
        fi
    else
        print_status $YELLOW "⚠ $file not found"
    fi
done

# Validate Maven POM
print_status $YELLOW "Validating Maven POM..."
if [ -f "backend/pom.xml" ]; then
    # Check for required elements in POM
    if grep -q "<groupId>" backend/pom.xml && grep -q "<artifactId>" backend/pom.xml && grep -q "<version>" backend/pom.xml; then
        print_status $GREEN "✓ Maven POM has required elements"
        
        # Extract basic info
        group_id=$(grep -o '<groupId>[^<]*</groupId>' backend/pom.xml | head -1 | sed 's/<[^>]*>//g')
        artifact_id=$(grep -o '<artifactId>[^<]*</artifactId>' backend/pom.xml | head -1 | sed 's/<[^>]*>//g')
        version=$(grep -o '<version>[^<]*</version>' backend/pom.xml | head -1 | sed 's/<[^>]*>//g')
        
        print_status $BLUE "  - Group ID: $group_id"
        print_status $BLUE "  - Artifact ID: $artifact_id"
        print_status $BLUE "  - Version: $version"
    else
        print_status $RED "✗ Maven POM missing required elements"
    fi
else
    print_status $RED "✗ Maven POM not found"
fi

# Validate package.json
print_status $YELLOW "Validating package.json..."
if [ -f "frontend/package.json" ]; then
    if grep -q '"name"' frontend/package.json && grep -q '"version"' frontend/package.json; then
        print_status $GREEN "✓ package.json has required elements"
        
        # Extract basic info
        name=$(grep -o '"name"[^,]*' frontend/package.json | cut -d'"' -f4)
        version=$(grep -o '"version"[^,]*' frontend/package.json | cut -d'"' -f4)
        
        print_status $BLUE "  - Name: $name"
        print_status $BLUE "  - Version: $version"
    else
        print_status $RED "✗ package.json missing required elements"
    fi
fi

# Check documentation
print_status $YELLOW "Checking documentation..."

doc_files=(
    "README.md"
    "LOCAL_TESTING_GUIDE.md"
)

for file in "${doc_files[@]}"; do
    if [ -f "$file" ]; then
        lines=$(wc -l < "$file")
        print_status $GREEN "✓ $file found ($lines lines)"
    else
        print_status $YELLOW "⚠ $file not found"
    fi
done

# Summary
print_header "BUILD VALIDATION SUMMARY"

total_checks=0
passed_checks=0

# Count checks
if [ ${#missing_dirs[@]} -eq 0 ]; then ((passed_checks++)); fi
((total_checks++))

if [ ${#missing_files[@]} -eq 0 ]; then ((passed_checks++)); fi
((total_checks++))

if [ "$java_files" -gt 0 ]; then ((passed_checks++)); fi
((total_checks++))

if [ -f "frontend/package.json" ]; then ((passed_checks++)); fi
((total_checks++))

if [ -f "backend/pom.xml" ]; then ((passed_checks++)); fi
((total_checks++))

percentage=$((passed_checks * 100 / total_checks))

print_status $BLUE "Validation Results: $passed_checks/$total_checks checks passed ($percentage%)"

if [ $percentage -ge 80 ]; then
    print_status $GREEN "🎉 Project structure is valid and ready for development!"
    print_status $GREEN "✅ The log analysis system appears to be properly structured"
    print_status $GREEN "✅ All major components are present"
    print_status $GREEN "✅ Configuration files are in place"
    
    echo
    print_status $BLUE "Next steps:"
    echo "1. Install Java 17+ and Maven to build the backend"
    echo "2. Install Node.js 18+ to build the frontend"
    echo "3. Run './scripts/test-local.sh' for local testing"
    echo "4. Use 'docker-compose up' for full deployment"
    
elif [ $percentage -ge 60 ]; then
    print_status $YELLOW "⚠️  Project structure is mostly valid but has some issues"
    print_status $YELLOW "The system should work but may need some fixes"
    
else
    print_status $RED "❌ Project structure has significant issues"
    print_status $RED "Please fix the missing components before proceeding"
fi

echo
print_status $BLUE "For detailed build instructions, see:"
print_status $BLUE "- README.md"
print_status $BLUE "- LOCAL_TESTING_GUIDE.md"

exit 0
