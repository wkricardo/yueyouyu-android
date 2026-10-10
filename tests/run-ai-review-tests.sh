#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
JAVA_HOME="${JAVA_HOME:-$PWD/tools/jdk-21.0.2}"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
"$JAVA_HOME/bin/javac" -d "$OUT" \
  app/src/main/java/cn/dot/budget/AiRequestContract.java \
  app/src/main/java/cn/dot/budget/AiRequestSession.java \
  app/src/main/java/cn/dot/budget/AiConsentSnapshot.java \
  app/src/main/java/cn/dot/budget/DeepSeekTransport.java \
  app/src/main/java/cn/dot/budget/AiPayloadBuilder.java \
  app/src/main/java/cn/dot/budget/AiJson.java \
  app/src/main/java/cn/dot/budget/AiResponseParser.java \
  app/src/main/java/cn/dot/budget/AiKeyDeletion.java \
  app/src/main/java/cn/dot/budget/AiErrors.java \
  app/src/main/java/cn/dot/budget/AiImagePolicy.java \
  app/src/main/java/cn/dot/budget/AiBoundedBytes.java \
  app/src/main/java/cn/dot/budget/AiDisclosure.java \
  app/src/main/java/cn/dot/budget/AiConsentSelection.java \
  app/src/main/java/cn/dot/budget/AiCaptureCleanup.java \
  app/src/main/java/cn/dot/budget/AiRetryStore.java \
  app/src/main/java/cn/dot/budget/AiDeliveryGate.java \
  app/src/main/java/cn/dot/budget/AiSourceRegistry.java \
  app/src/main/java/cn/dot/budget/AiCapturePath.java \
  tests/java/cn/dot/budget/AiLifecycleReviewTest.java
"$JAVA_HOME/bin/java" -cp "$OUT" cn.dot.budget.AiLifecycleReviewTest
"$JAVA_HOME/bin/java" -cp "$OUT" cn.dot.budget.AiLifecycleReviewTest --payload-fixtures > "$OUT/ai-payloads.jsonl"
node - "$OUT/ai-payloads.jsonl" <<'JS'
const fs=require('node:fs');let checks=0;const assert=new Proxy(require('node:assert/strict'),{get:(target,key)=>typeof target[key]==='function'?(...args)=>{checks++;return target[key](...args);}:target[key]});
const lines=fs.readFileSync(process.argv[2],'utf8').trim().split('\n');assert.equal(lines.length,3);
for(const [i,line]of lines.entries()){
 const p=JSON.parse(line);assert.deepEqual(Object.keys(p).sort(),['max_tokens','messages','model','response_format','stream','thinking']);
 assert.equal(p.model,'deepseek-flash');assert.equal(p.stream,false);assert.equal(p.max_tokens,6000);assert.deepEqual(p.thinking,{type:'disabled'});assert.deepEqual(p.response_format,{type:'json_object'});
 assert.equal(p.messages.length,1);assert.deepEqual(Object.keys(p.messages[0]).sort(),['content','role']);assert.equal(p.messages[0].role,'user');const c=p.messages[0].content;assert.equal(c.length,2);assert.deepEqual(Object.keys(c[0]).sort(),['text','type']);assert.equal(c[0].type,'text');assert.equal(typeof c[0].text,'string');assert.ok(c[0].text.length>0);assert.deepEqual(c[1],{type:'image_url',image_url:{url:'data:image/png;base64,AAECA/8=',detail:'original'}});assert.equal(line.includes('do-not-send-correlation'),false);
 if(i===0)assert.match(c[0].text,/逐笔交易/);else assert.match(c[0].text,/食物/);
}
assert.equal(lines[1],lines[2]);console.log('PASS: '+checks+' parsed production-payload assertions; exact approved bytes, no hidden context.');
JS

"$JAVA_HOME/bin/java" -cp "$OUT" cn.dot.budget.AiLifecycleReviewTest --text-payload-fixtures > "$OUT/ai-text-payloads.jsonl"
node - "$OUT/ai-text-payloads.jsonl" <<'JS'
const fs=require('node:fs'),assert=require('node:assert/strict');const lines=fs.readFileSync(process.argv[2],'utf8').trim().split('\n');assert.equal(lines.length,2);
for(const line of lines){const p=JSON.parse(line);assert.deepEqual(Object.keys(p).sort(),['max_tokens','messages','model','response_format','stream','thinking']);assert.equal(p.model,'deepseek-flash');assert.equal(p.stream,false);assert.deepEqual(p.thinking,{type:'disabled'});assert.deepEqual(p.response_format,{type:'json_object'});assert.equal(p.messages.length,2);assert.deepEqual(Object.keys(p.messages[0]).sort(),['content','role']);assert.equal(p.messages[0].role,'system');assert.equal(typeof p.messages[0].content,'string');assert.deepEqual(p.messages[1],{role:'user',content:'  合成午餐 12.30 元\n"quoted" \\ end\t  '});assert.equal(line.includes('do-not-send-correlation'),false);}
console.log('PASS: 23 independent Node text-payload assertions; exact Unicode/quote/control text and fixed prompt only.');
JS
