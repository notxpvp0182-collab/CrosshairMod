#!/bin/sh
#
# Gradle startup script for UNIX
# (Standard Gradle wrapper bootstrap script)
#

APP_HOME=$( cd "${0%"${0##*/}"}." && pwd -P ) || exit
APP_NAME="Gradle"
APP_BASE_NAME=${0##*/}

DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

# Determine JAVA command
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    fi
else
    JAVACMD="java"
    if ! command -v java >/dev/null 2>&1
    then
        die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
    fi
fi

# Check if gradle-wrapper.jar exists
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ] ; then
    echo "ERROR: gradle-wrapper.jar not found!"
    echo ""
    echo "To set up the Gradle wrapper, run ONE of these:"
    echo "  Option 1 (if Gradle is installed):  gradle wrapper --gradle-version 8.8"
    echo "  Option 2 (IntelliJ IDEA):            Open project in IDEA — it auto-downloads."
    echo "  Option 3:                            Run ./setup-wrapper.sh"
    echo ""
    exit 1
fi

CLASSPATH=$WRAPPER_JAR

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
