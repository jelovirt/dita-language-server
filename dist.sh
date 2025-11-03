#!/bin/bash

gradle fatJar
cp build/libs/dita-language-server-1.0.0-all.jar ../dita-vscode-extension/server/

