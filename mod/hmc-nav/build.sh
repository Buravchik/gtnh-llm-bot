#!/bin/bash
set -e
cd "$(dirname "$0")"
JDK=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/bin
SPEC="$HOME/gtnh-bot/jars/hmc-specifics-1.7.10-forge-latest.jar"
rm -rf build && mkdir -p build/stubs build/classes
$JDK/javac --release 8 -nowarn -d build/stubs $(find stubs -name '*.java')
$JDK/javac --release 8 -nowarn -Xlint:-options -proc:none -cp "build/stubs:$SPEC" -d build/classes $(find src -name '*.java')
cp -R res/* build/classes/
(cd build/classes && $JDK/jar --create --file ../hmc-nav-0.1.0.jar .)
echo built build/hmc-nav-0.1.0.jar
