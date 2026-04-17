#!/bin/sh
# Helper: bootstraps the Gradle wrapper JAR.
# Run this once after unzipping, then use ./gradlew build normally.

GRADLE_VERSION="8.8"
WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"

if [ -f "$WRAPPER_JAR" ]; then
    echo "[OK] gradle-wrapper.jar already present."
    exit 0
fi

if command -v gradle > /dev/null 2>&1; then
    echo "Found system Gradle — generating wrapper..."
    gradle wrapper --gradle-version $GRADLE_VERSION
    echo "[OK] Gradle wrapper ready. You can now run: ./gradlew build"
    exit 0
fi

echo "[ERROR] Could not find system Gradle to bootstrap the wrapper."
echo ""
echo "Options:"
echo "  1) Install Gradle 8.8+ then re-run this script."
echo "  2) Open the project in IntelliJ IDEA — it auto-configures the wrapper."
echo "  3) Download gradle-wrapper.jar from:"
echo "     https://github.com/gradle/gradle/raw/v8.8.0/gradle/wrapper/gradle-wrapper.jar"
echo "     and place it in: gradle/wrapper/"
exit 1
