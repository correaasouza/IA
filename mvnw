#!/bin/sh
# ----------------------------------------------------------------------------
# Minimal Maven Wrapper launcher (POSIX)
# ----------------------------------------------------------------------------

set -e

BASEDIR=$(cd "$(dirname "$0")" && pwd)

WRAPPER_JAR="$BASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$BASEDIR/.mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Maven wrapper jar not found at $WRAPPER_JAR" >&2
  exit 1
fi
if [ ! -f "$WRAPPER_PROPERTIES" ]; then
  echo "Maven wrapper properties not found at $WRAPPER_PROPERTIES" >&2
  exit 1
fi

JAVA_EXE="java"
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA_EXE="$JAVA_HOME/bin/java"
fi

exec "$JAVA_EXE" -classpath "$WRAPPER_JAR" -Dmaven.multiModuleProjectDirectory="$BASEDIR" org.apache.maven.wrapper.MavenWrapperMain "$@"

