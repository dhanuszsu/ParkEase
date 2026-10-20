$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot

Write-Host "[ParkEase] Compiling sources to bin/..."
if (-not (Test-Path -Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

$sources = Get-ChildItem -Path "src" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
& javac -encoding UTF-8 -cp "lib/*" -d bin $sources
Write-Host "[ParkEase] Compilation SUCCESS! Binaries placed in bin/"
