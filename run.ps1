$ErrorActionPreference = "Stop"

$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
    $defaultJdk = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\java.exe"
    if (Test-Path $defaultJdk) {
        $javaPath = $defaultJdk
    } else {
        Write-Error "java could not be found. Please ensure JDK 17+ is installed."
        exit 1
    }
} else {
    $javaPath = $java.Source
}

if (-not (Test-Path "bin\com\parkease\ui\ParkEaseApp.class")) {
    Write-Host "[ParkEase] Binaries not found. Building first..." -ForegroundColor Yellow
    & ".\build.ps1"
}

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
& $javaPath "-Dfile.encoding=UTF-8" -cp bin com.parkease.ui.ParkEaseApp
