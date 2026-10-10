#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
JAVA_HOME="${JAVA_HOME:-$PWD/tools/jdk-21.0.2}"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
"$JAVA_HOME/bin/javac" -d "$OUT" app/src/main/java/cn/dot/budget/{AiJson,AiRequestContract,AiRequestSession,AiRememberedConsent,AiSourceCopy,AiSensitiveText,AiBoundedBytes,AiConsentSnapshot,AiPayloadBuilder,AiImagePolicy,AiImageEncoding,DeepSeekTransport}.java tests/java/cn/dot/budget/AiV401Test.java
"$JAVA_HOME/bin/java" -Xmx512m -cp "$OUT" cn.dot.budget.AiV401Test
