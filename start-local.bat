@echo off
REM Log Analysis System - Local Development Startup Script for Windows
REM This script helps start the system locally without Docker

setlocal enabledelayedexpansion

REM Colors (limited in Windows batch)
set "INFO=[INFO]"
set "SUCCESS=[SUCCESS]"
set "WARNING=[WARNING]"
set "ERROR=[ERROR]"

echo.
echo 🚀 Log Analysis System - Local Development (Windows)
echo =====================================================
echo.

REM Check if Java is installed
:check_java
echo %INFO% Checking Java installation...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo %ERROR% Java is not installed or not in PATH
    echo Please install Java 17+ from: https://openjdk.org/
    pause
    exit /b 1
)

for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
    set JAVA_VERSION=!JAVA_VERSION:"=!
)

echo %SUCCESS% Java is installed: !JAVA_VERSION!

REM Check if Node.js is installed
:check_node
echo %INFO% Checking Node.js installation...
node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo %ERROR% Node.js is not installed or not in PATH
    echo Please install Node.js 18+ from: https://nodejs.org/
    pause
    exit /b 1
)

for /f %%i in ('node --version') do set NODE_VERSION=%%i
echo %SUCCESS% Node.js is installed: !NODE_VERSION!

REM Check if ports are available
:check_ports
echo %INFO% Checking if ports 8080 and 3000 are available...
netstat -an | findstr ":8080" >nul
if %errorlevel% equ 0 (
    echo %ERROR% Port 8080 is already in use
    echo Please stop the process using this port
    netstat -an | findstr ":8080"
    pause
    exit /b 1
)

netstat -an | findstr ":3000" >nul
if %errorlevel% equ 0 (
    echo %ERROR% Port 3000 is already in use
    echo Please stop the process using this port
    netstat -an | findstr ":3000"
    pause
    exit /b 1
)

echo %SUCCESS% Ports 8080 and 3000 are available

REM Create logs directory
if not exist logs mkdir logs

REM Setup backend
:setup_backend
echo %INFO% Setting up backend...
cd backend
echo %INFO% Installing backend dependencies...
call mvnw.cmd clean install -DskipTests
if %errorlevel% neq 0 (
    echo %ERROR% Backend setup failed
    pause
    exit /b 1
)
echo %SUCCESS% Backend setup completed
cd ..

REM Setup frontend
:setup_frontend
echo %INFO% Setting up frontend...
cd frontend
echo %INFO% Installing frontend dependencies...
call npm install --legacy-peer-deps
if %errorlevel% neq 0 (
    echo %ERROR% Frontend setup failed
    pause
    exit /b 1
)
echo %SUCCESS% Frontend setup completed
cd ..

REM Start backend
:start_backend
echo %INFO% Starting backend server...
cd backend
start "Backend Server" cmd /c "mvnw.cmd spring-boot:run > ..\logs\backend.log 2>&1"
cd ..

echo %SUCCESS% Backend started
echo %INFO% Backend logs: logs\backend.log

REM Wait for backend to start
echo %INFO% Waiting for backend to start...
set /a counter=0
:wait_backend
timeout /t 2 /nobreak >nul
curl -f http://localhost:8080/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo %SUCCESS% Backend is ready!
    goto start_frontend
)
set /a counter+=1
if %counter% lss 30 (
    echo|set /p="."
    goto wait_backend
)
echo %ERROR% Backend failed to start within 60 seconds
pause
exit /b 1

REM Start frontend
:start_frontend
echo %INFO% Starting frontend server...
cd frontend
start "Frontend Server" cmd /c "npm start > ..\logs\frontend.log 2>&1"
cd ..

echo %SUCCESS% Frontend started
echo %INFO% Frontend logs: logs\frontend.log

REM Wait for frontend to start
echo %INFO% Waiting for frontend to start...
set /a counter=0
:wait_frontend
timeout /t 2 /nobreak >nul
curl -f http://localhost:3000 >nul 2>&1
if %errorlevel% equ 0 (
    echo %SUCCESS% Frontend is ready!
    goto show_success
)
set /a counter+=1
if %counter% lss 30 (
    echo|set /p="."
    goto wait_frontend
)
echo %ERROR% Frontend failed to start within 60 seconds
pause
exit /b 1

:show_success
echo.
echo %SUCCESS% 🎉 System is ready!
echo.
echo 🌐 Frontend: http://localhost:3000
echo 🔧 Backend API: http://localhost:8080/api/v1
echo ❤️ Health Check: http://localhost:8080/actuator/health
echo 📚 API Documentation: http://localhost:8080/swagger-ui.html
echo.
echo Press any key to open the application in your browser...
pause >nul

REM Open browser
start http://localhost:3000

echo.
echo The system is now running!
echo.
echo To stop the services:
echo 1. Close the Backend Server and Frontend Server command windows
echo 2. Or run: taskkill /f /im java.exe ^& taskkill /f /im node.exe
echo.
echo Logs are available in the 'logs' directory
echo.
pause

endlocal
