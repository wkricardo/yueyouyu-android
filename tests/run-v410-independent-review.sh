#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
JAVA_HOME="${JAVA_HOME:-$PWD/tools/jdk-21.0.2}"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
node - "$OUT/fixtures.jsonl" <<'JS'
const fs=require('node:fs'),H=require('./app/src/main/assets/health-trend.js');let lines=[];
for(const days of [7,14,30])for(const variant of [0,1,2,3,4]){const state={weights:[],nutrition:[],exercises:[],completedFoodDays:[]};for(let i=0;i<days;i++){const date=H.offset('2026-10-12',1-days+i);if(variant&&i%(variant===1?1:3)===0)state.weights.push({date,kg:80-i*(variant===2?-.3:.1)});if(variant>=3){state.nutrition.push({date,lowKcal:1000+i,highKcal:1400+i});if(i%2===0)state.completedFoodDays.push(date);state.exercises.push({date,name:'步行',activeKcal:100});}}const summary=H.summary(state,days,'2026-10-12');lines.push(JSON.stringify({summary,stats:H.stats(summary)}));}fs.writeFileSync(process.argv[2],lines.join('\n'));
JS
"$JAVA_HOME/bin/javac" -d "$OUT" app/src/main/java/cn/dot/budget/{AiJson,AiHealthHistory,AiRequestContract,AiRequestSession,AiRememberedConsent,AiBoundedBytes,AiPayloadBuilder,AiConsentSnapshot,AiRetryStore,AiImagePolicy,AiResponseParser,DeepSeekTransport}.java tests/v410-review/IndependentHealthReview.java
"$JAVA_HOME/bin/java" -Xmx256m -cp "$OUT" cn.dot.budget.IndependentHealthReview "$OUT/fixtures.jsonl"
node --test tests/health-trend-independent.test.js
