#!/bin/bash
cd /Users/imac/Documents/code/github/msll
mvn -q test-compile
java -cp "target/test-classes:target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)" org.twelve.msll.evaluation.SimpleJSONTest
