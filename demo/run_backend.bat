@echo off
title Spring Boot Backend (MongoDB)
echo ========================================================
echo   Setting up Wireless ADB Port Forwarding (Port 8080)
echo ========================================================
"C:\Users\HP\AppData\Local\Android\Sdk\platform-tools\adb.exe" reverse tcp:8080 tcp:8080
echo.
echo ========================================================
echo   Starting Spring Boot Server + MongoDB on Port 8080
echo ========================================================
cd /d "%~dp0"
if exist "C:\Users\HP\.jdks\ms-17.0.20\bin\java.exe" (
    "C:\Users\HP\.jdks\ms-17.0.20\bin\java.exe" -jar "build\libs\demo-0.0.1-SNAPSHOT.jar"
) else (
    "C:\Program Files\Android\Android Studio1\jbr\bin\java.exe" -jar "build\libs\demo-0.0.1-SNAPSHOT.jar"
)
pause
