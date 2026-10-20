@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul

if not exist "bin\com\parkease\demo\LiveDemonstrator.class" (
    echo [ParkEase] Compiled binaries not found. Invoking build.bat first...
    call build.bat
    if errorlevel 1 exit /b 1
)

java -Dfile.encoding=UTF-8 -cp "bin;lib/*" com.parkease.demo.LiveDemonstrator %*
