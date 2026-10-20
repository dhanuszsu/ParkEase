#!/bin/bash
set -e

# Resolve directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Locate JDK if not on PATH or if stub fails
if ! javac -version >/dev/null 2>&1; then
    for candidate in /Library/Java/JavaVirtualMachines/*/Contents/Home/bin /opt/homebrew/opt/openjdk*/bin /usr/local/opt/openjdk*/bin; do
        if [ -x "$candidate/javac" ]; then
            export PATH="$candidate:$PATH"
            break
        fi
    done
fi

if ! javac -version >/dev/null 2>&1; then
    if [ -x "/usr/libexec/java_home" ]; then
        JAVA_HOME_PATH="$(/usr/libexec/java_home 2>/dev/null || true)"
        if [ -n "$JAVA_HOME_PATH" ] && [ -x "$JAVA_HOME_PATH/bin/javac" ]; then
            export PATH="$JAVA_HOME_PATH/bin:$PATH"
        fi
    fi
fi

if ! javac -version >/dev/null 2>&1; then
    echo "[ERROR] javac could not be found. Please ensure JDK 17+ is installed and on your PATH." >&2
    exit 1
fi

echo "[ParkEase] Compiling sources to bin/ using $(javac -version 2>&1)..."
mkdir -p bin

# Find all java files and compile with lib/* classpath
JAVA_SOURCES=$(find src -name "*.java")
javac -encoding UTF-8 -cp "lib/*" -d bin $JAVA_SOURCES

echo "[ParkEase] Compilation SUCCESS! Binaries placed in bin/"
