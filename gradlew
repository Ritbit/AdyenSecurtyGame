#!/bin/sh
# Gradle wrapper launch script for Unix
# Generated for Gradle 8.4

##############################################################################
# Determine the Java command to use to start the JVM.
##############################################################################
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found."
fi

##############################################################################
# Determine the application home directory.
##############################################################################
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec "$JAVACMD" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain "$@"
