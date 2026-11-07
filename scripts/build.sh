#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p target/classes
find src/main/java -name '*.java' -print > target/sources.txt
javac --release 21 -d target/classes @target/sources.txt
jar --create --file target/iam-review.jar --main-class com.iamreview.Main -C target/classes .
echo 'Built target/iam-review.jar'
