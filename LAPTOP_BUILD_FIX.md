# 💻 Laptop Build Fix Guide

Since you have Java 17 and Maven installed but the build is still not working, let's fix this step by step.

## 🔍 **Step 1: Run Diagnosis**

First, let's see exactly what's wrong:

```bash
# Clone your repo (if not already done)
git clone https://github.com/avis-enna/log-analysis-system.git
cd log-analysis-system

# Run the diagnosis script
chmod +x scripts/diagnose-local-build.sh
./scripts/diagnose-local-build.sh
```

This will tell us exactly what's failing.

## 🛠️ **Step 2: Most Common Fixes**

### **Fix A: Java/Maven Version Mismatch**

```bash
# Check what Java version Maven is using
mvn -version

# If Maven is not using Java 17, set JAVA_HOME:
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
export PATH=$JAVA_HOME/bin:$PATH

# Verify the fix
mvn -version
java -version
```

### **Fix B: Use Maven Wrapper Instead**

```bash
# Make sure Maven wrapper is executable
chmod +x mvnw

# Try building with wrapper instead of system Maven
./mvnw clean compile
```

### **Fix C: Clear Maven Cache**

```bash
# Sometimes Maven cache gets corrupted
rm -rf ~/.m2/repository

# Try building again
./mvnw clean compile -U
```

### **Fix D: Fix Project Structure**

```bash
# Auto-fix any missing files or configuration issues
./scripts/fix-build-issues.sh

# Then try building
./mvnw clean compile
```

## 🎯 **Step 3: Try These Commands in Order**

Run these commands one by one until one works:

### **Command 1: Basic Build**
```bash
./mvnw clean compile
```

### **Command 2: Force Update Dependencies**
```bash
./mvnw clean compile -U
```

### **Command 3: Use Local Profile**
```bash
./mvnw clean compile -Dspring.profiles.active=local
```

### **Command 4: Skip Tests Temporarily**
```bash
./mvnw clean compile -DskipTests
```

### **Command 5: Verbose Output (to see exact error)**
```bash
./mvnw clean compile -X -e
```

## 🚨 **Step 4: If Still Failing, Try This**

### **Option A: Use Embedded Services Only**

Create a minimal `application-local.yml`:

```bash
# Create simplified config
cat > backend/src/main/resources/application-local.yml << 'EOF'
spring:
  profiles:
    active: local
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
EOF

# Try building with this profile
./mvnw clean compile -Dspring.profiles.active=local
```

### **Option B: Minimal POM.xml**

If POM.xml is causing issues, replace it:

```bash
# Backup current POM
cp backend/pom.xml backend/pom.xml.backup

# Create minimal working POM
cat > backend/pom.xml << 'EOF'
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
    
    <properties>
        <java.version>17</java.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
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

# Try building with minimal POM
./mvnw clean compile
```

## 📋 **Step 5: Test the Application**

Once compilation works, test the application:

```bash
# Start the application
./mvnw spring-boot:run -Dspring.profiles.active=local

# In another terminal, test it
curl http://localhost:8080/actuator/health
```

You should see: `{"status":"UP"}`

## 🔧 **Step 6: Common Error Solutions**

### **Error: "Could not find or load main class"**
```bash
# Make sure Application.java exists
ls -la backend/src/main/java/com/loganalyzer/Application.java

# If missing, create it:
mkdir -p backend/src/main/java/com/loganalyzer
cat > backend/src/main/java/com/loganalyzer/Application.java << 'EOF'
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
```

### **Error: "Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin"**
```bash
# Check Java version in POM matches your Java
grep -A 5 -B 5 "java.version" backend/pom.xml

# Should show: <java.version>17</java.version>
# If not, edit the POM or set JAVA_HOME correctly
```

### **Error: "Cannot resolve dependencies"**
```bash
# Clear Maven cache and try again
rm -rf ~/.m2/repository
./mvnw clean compile -U

# If behind corporate firewall, you may need proxy settings
```

## 🎯 **Step 7: Quick Success Test**

Try this minimal test to confirm everything works:

```bash
# 1. Clean build
./mvnw clean compile

# 2. Run tests
./mvnw test

# 3. Package application
./mvnw package -DskipTests

# 4. Run the JAR
java -jar backend/target/*.jar --spring.profiles.active=local
```

## 📞 **Step 8: Get Help**

If you're still having issues, please share:

1. **The exact error message** you're seeing
2. **Output of these commands**:
   ```bash
   java -version
   mvn -version
   ./mvnw -version
   echo $JAVA_HOME
   ```
3. **Your operating system** (Windows, macOS, Linux)
4. **The specific command that's failing**

## 🚀 **Success Indicators**

You'll know it's working when:
- ✅ `./mvnw clean compile` completes without errors
- ✅ `./mvnw test` runs successfully
- ✅ `./mvnw spring-boot:run` starts the application
- ✅ `curl http://localhost:8080/actuator/health` returns `{"status":"UP"}`

## 💡 **Pro Tips**

1. **Always use the Maven wrapper** (`./mvnw`) instead of system Maven
2. **Use the local profile** (`-Dspring.profiles.active=local`) for development
3. **Clear Maven cache** (`rm -rf ~/.m2/repository`) when in doubt
4. **Check JAVA_HOME** is pointing to Java 17+
5. **Use verbose output** (`-X -e`) to see detailed error messages

Once the backend builds successfully, we can move on to the frontend and full system testing!
