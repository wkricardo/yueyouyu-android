#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
JAVA_HOME="${JAVA_HOME:-$PWD/tools/jdk-21.0.2}"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
BASE=tests/v431-review/baseline/DeepSeekTransport.java
printf '%s  %s\n' e1b0279599332fea47654f1f5a22bde4e653529d91163af4d2ecb576c81b3c67 "$BASE" | sha256sum -c -
for variant in baseline patched; do
  SOURCE="$BASE"; if [[ "$variant" == patched ]]; then SOURCE=app/src/main/java/cn/dot/budget/DeepSeekTransport.java; fi
  mkdir -p "$OUT/$variant"
  "$JAVA_HOME/bin/javac" -d "$OUT/$variant" "$SOURCE" tests/v431-review/WaitComparison.java
  "$JAVA_HOME/bin/java" -cp "$OUT/$variant" cn.dot.budget.WaitComparison "$variant"
done
