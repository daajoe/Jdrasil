#!/bin/bash
export JAVA_COMPILER=javac

cd jdrasil/src
$JAVA_COMPILER -encoding utf8 -cp ../../lib/glucose.jar:../../lib/glucosep.jar:../../lib/pblib.jar:. de/uniluebeck/tcs/App.java


