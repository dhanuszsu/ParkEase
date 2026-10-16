$ErrorActionPreference = "Stop"

$javac = Get-Command javac -ErrorAction SilentlyContinue
if (-not $javac) {
    $defaultJdk = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\javac.exe"
    if (Test-Path $defaultJdk) {
        $javacPath = $defaultJdk
    } else {
        Write-Error "javac could not be found. Please ensure JDK 17+ is installed."
        exit 1
    }
} else {
    $javacPath = $javac.Source
}

Write-Host "[ParkEase] Compiling sources with $javacPath..." -ForegroundColor Cyan

if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

$sources = Get-ChildItem -Path "src" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
& $javacPath -encoding UTF-8 -d bin $sources

if ($LASTEXITCODE -eq 0) {
    Write-Host "[ParkEase] Compilation SUCCESS! Binaries placed in bin/" -ForegroundColor Green
} else {
    Write-Host "[ParkEase] Compilation FAILED." -ForegroundColor Red
    exit $LASTEXITCODE
}
