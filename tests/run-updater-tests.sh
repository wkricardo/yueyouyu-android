#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
JAVA_HOME="${JAVA_HOME:-$PWD/tools/jdk-21.0.2}"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
"$JAVA_HOME/bin/javac" -d "$OUT" app/src/main/java/cn/dot/budget/GitHubUpdateTransport.java tests/java/cn/dot/budget/UpdaterTest.java
"$JAVA_HOME/bin/java" -cp "$OUT" cn.dot.budget.UpdaterTest
