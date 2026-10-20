$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

if (-not (Test-Path -Path "bin/com/parkease/demo/LiveDemonstrator.class")) {
    Write-Host "[ParkEase] Compiled binaries not found. Invoking build.ps1 first..."
    & .\build.ps1
}

& java -Dfile.encoding=UTF-8 -cp "bin;lib/*" com.parkease.demo.LiveDemonstrator $args
