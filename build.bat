@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo [ParkEase] Compiling sources to bin\...
if not exist "bin" mkdir bin

dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -cp "lib/*" -d bin @sources.txt
del sources.txt

if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed!
    exit /b %errorlevel%
)
echo [ParkEase] Compilation SUCCESS! Binaries placed in bin\
