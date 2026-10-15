#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
JAVA_HOME="${JAVA_HOME:-$PWD/tools/jdk-21.0.2}"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
"$JAVA_HOME/bin/javac" -d "$OUT" app/src/main/java/cn/dot/budget/DeepSeekTransport.java tests/v431-review/IndependentWaitReview.java
"$JAVA_HOME/bin/java" -cp "$OUT" cn.dot.budget.IndependentWaitReview
NODE_PATH="$PWD/tools/dom-test/node_modules${NODE_PATH:+:$NODE_PATH}" node --test tests/ai-wait-independent.test.js
