#!/bin/bash
set -e

# Resolve directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Locate JRE if not on PATH or if stub fails
if ! java -version >/dev/null 2>&1; then
    for candidate in /Library/Java/JavaVirtualMachines/*/Contents/Home/bin /opt/homebrew/opt/openjdk*/bin /usr/local/opt/openjdk*/bin; do
        if [ -x "$candidate/java" ]; then
            export PATH="$candidate:$PATH"
            break
        fi
    done
fi

if ! java -version >/dev/null 2>&1; then
    if [ -x "/usr/libexec/java_home" ]; then
        JAVA_HOME_PATH="$(/usr/libexec/java_home 2>/dev/null || true)"
        if [ -n "$JAVA_HOME_PATH" ] && [ -x "$JAVA_HOME_PATH/bin/java" ]; then
            export PATH="$JAVA_HOME_PATH/bin:$PATH"
        fi
    fi
fi

if ! java -version >/dev/null 2>&1; then
    echo "[ERROR] java could not be found. Please ensure JDK 17+ is installed." >&2
    exit 1
fi

if [ ! -f "bin/com/parkease/ui/ParkEaseApp.class" ]; then
    echo "[ParkEase] Compiled binaries not found. Invoking ./build.sh first..."
    ./build.sh
fi

java -Dfile.encoding=UTF-8 -cp "bin:lib/*" com.parkease.ui.ParkEaseApp "$@"
