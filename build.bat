@echo off
setlocal

echo [ParkEase] Checking Java compiler...
set JAVAC_BIN=javac
where javac >nul 2>nul
if %errorlevel% neq 0 (
    if exist "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\javac.exe" (
        set "JAVAC_BIN=C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\javac.exe"
    ) else (
        echo [ERROR] javac could not be found. Please ensure JDK 17+ is installed.
        exit /b 1
    )
)

echo [ParkEase] Compiling sources to bin/...
if not exist "bin" mkdir "bin"

"%JAVAC_BIN%" -encoding UTF-8 -d bin src\com\parkease\billing\*.java src\com\parkease\exception\*.java src\com\parkease\model\*.java src\com\parkease\service\*.java src\com\parkease\util\*.java src\com\parkease\ui\*.java src\com\parkease\test\*.java

if %errorlevel% equ 0 (
    echo [ParkEase] Compilation SUCCESS! Binaries placed in bin/
) else (
    echo [ParkEase] Compilation FAILED.
    exit /b %errorlevel%
)
