@echo off
title LearnAI - Build

set "BASE=%~dp0"

echo ============================================
echo   LearnAI - Build (Backend + Frontend)
echo ============================================
echo.

echo [1/2] Building Spring Boot Backend...
echo.
cd /d "%BASE%backend"
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Backend build FAILED!
    pause
    exit /b 1
)
echo.
echo [OK] Backend built: backend\target\learn-backend-1.0.0.jar

echo.
echo [2/2] Building Vue Frontend...
echo.
cd /d "%BASE%frontend"
call npm run build
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Frontend build FAILED!
    pause
    exit /b 1
)
echo.
echo [OK] Frontend built: frontend\dist\

echo.
echo ============================================
echo   Build Complete!
echo     Backend  : backend\target\learn-backend-1.0.0.jar
echo     Frontend : frontend\dist\
echo ============================================
echo.
echo Press any key to close this window
pause > nul
