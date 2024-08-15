#!/bin/bash

OUTPUT_DIR="./artifacts"

rm -f $OUTPUT_DIR/*

mkdir -p $OUTPUT_DIR

# Maven Profiles

for p in java8 java11 java16 java17 java21; do
    # Skip tests
    mvn clean package -f pom.multiple.xml -DskipTests -P$p
    cp target/PotatoPeeler*java*.jar $OUTPUT_DIR
done
