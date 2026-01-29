#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.
# ----------------------------------------------------------------------------

# OS specific support
cygwin=false
mingw=false
darwin=false
case "$(uname)" in
    CYGWIN*) cygwin=true;;
    MINGW*) mingw=true;;
    Darwin*) darwin=true;;
esac

# Find Maven project base dir
if [ -z "$MAVEN_PROJECTBASEDIR" ]; then
    MAVEN_PROJECTBASEDIR=$(cd "$(dirname "$0")" && pwd)
fi

MAVEN_OPTS="${MAVEN_OPTS:-"-Xmx512m"}"

# Download wrapper jar if needed
wrapperJar="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
if [ ! -f "$wrapperJar" ]; then
    if command -v wget > /dev/null; then
        wget -q -O "$wrapperJar" "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
    elif command -v curl > /dev/null; then
        curl -s -o "$wrapperJar" "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
    fi
fi

# Run Maven with wrapper
exec java $MAVEN_OPTS \
    -Dmaven.multiModuleProjectDirectory="$MAVEN_PROJECTBASEDIR" \
    -classpath "$wrapperJar" \
    org.apache.maven.wrapper.MavenWrapperMain "$@"
