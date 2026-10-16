@echo off
setlocal

set JAVA_BIN=java
where java >nul 2>nul
if %errorlevel% neq 0 (
    if exist "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\java.exe" (
        set "JAVA_BIN=C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\java.exe"
    ) else (
        echo [ERROR] java could not be found. Please ensure JDK 17+ is installed.
        exit /b 1
    )
)

if not exist "bin\com\parkease\ui\ParkEaseApp.class" (
    echo [ParkEase] Compiled binaries not found. Invoking build.bat first...
    call build.bat
    if %errorlevel% neq 0 exit /b %errorlevel%
)

chcp 65001 >nul
"%JAVA_BIN%" -Dfile.encoding=UTF-8 -cp bin com.parkease.ui.ParkEaseApp
