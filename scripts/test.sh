#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/build.sh
mkdir -p target/test-classes
find src/test/java -name '*.java' -print > target/tests.txt
javac --release 21 -cp target/classes -d target/test-classes @target/tests.txt
java -cp target/classes:target/test-classes com.iamreview.AuditTests
