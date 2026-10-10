#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
JAVA_HOME="${JAVA_HOME:-$PWD/tools/jdk-21.0.2}"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
"$JAVA_HOME/bin/javac" -d "$OUT" \
  app/src/main/java/cn/dot/budget/{DeepSeekTransport,AiJson,AiRequestContract,AiRequestSession,AiRememberedConsent,AiBoundedBytes,AiPayloadBuilder,AiConsentSnapshot,AiImagePolicy,AiSourceCopy}.java \
  tests/v401-review/IndependentReview.java
"$JAVA_HOME/bin/java" -Xmx256m -cp "$OUT" cn.dot.budget.IndependentReview
