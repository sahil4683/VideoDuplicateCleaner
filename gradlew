#!/bin/sh
##############################################################################
# Gradle start up script for UN*X
##############################################################################
# Attempt to set APP_HOME
SAVED="`pwd`"
cd "`dirname \"$0\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null
APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
exec "$JAVACMD" "${JVM_OPTS[@]}" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
