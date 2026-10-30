#!/bin/sh
# Host checks use test-only org.json/kxml2; neither JAR is packaged into the launcher.
set -eu
cd "$(dirname "$0")/.."
: "${JAVA_HOME:?set JAVA_HOME to JDK 17}"
: "${ANDROID_HOME:?set ANDROID_HOME to Android SDK}"
: "${TEST_JSON_JAR:?set TEST_JSON_JAR to org.json 20250517}"
: "${TEST_XML_JAR:?set TEST_XML_JAR to kxml2 2.3.0}"
CP="build/classes:$TEST_JSON_JAR:$TEST_XML_JAR:$ANDROID_HOME/platforms/android-34/android.jar"
mkdir -p checks/scratch/classes
"$JAVA_HOME/bin/javac" --release 11 -cp "$CP" -d checks/scratch/classes checks/MemoryPreferences.java checks/RegionalChecks.java
"$JAVA_HOME/bin/java" -cp "checks/scratch/classes:$CP" com.petrobot.hume.RegionalChecks "$@"
