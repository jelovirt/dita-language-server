#!/usr/bin/env bash

gradle shadowJar

#sdk use java 25.0.1-graalce
native-image -jar build/libs/*.jar -o build/libs/dls
#sdk env