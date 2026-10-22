#!/bin/bash
cd "$(dirname "$0")"
javac -cp ".:*" *.java
java -cp ".:*" Main
