@echo off
title LearnAI - Launcher

set "BASE=%~dp0"

echo ============================================
echo   LearnAI - Multi-Agent Learning System
echo ============================================
echo.

echo [1/3] Starting Python PPT Service (port 5050)...
start "Python-PPT" cmd /k "cd /d "%BASE%ppt-service" && python main.py"

echo [2/3] Starting Spring Boot Backend (port 6060)...
start "Java-Backend" cmd /k "cd /d "%BASE%backend" && mvn spring-boot:run"

echo [3/3] Starting Vue Frontend (port 5173)...
start "Vue-Frontend" cmd /k "cd /d "%BASE%frontend" && npm run dev"

echo.
echo ============================================
echo   All services started!
echo     Python PPT : http://localhost:5050
echo     Backend    : http://localhost:6060
echo     Frontend   : http://localhost:5173
echo ============================================
echo.
echo Press any key to close this window (services keep running)
pause > nul
