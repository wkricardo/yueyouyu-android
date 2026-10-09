#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_HOME:=$PWD/tools/sdk}"
: "${JAVA_HOME:=$PWD/tools/jdk-21.0.2}"
export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"
BT="$ANDROID_HOME/build-tools/36.0.0"
JAR="$ANDROID_HOME/platforms/android-36/android.jar"
mkdir -p build/classes build/generated build/dex dist
"$BT/aapt2" compile --dir app/src/main/res -o build/resources.zip
"$BT/aapt2" link -o build/base.apk -I "$JAR" --manifest app/src/main/AndroidManifest.xml --java build/generated -A app/src/main/assets build/resources.zip
javac -source 8 -target 8 -encoding UTF-8 -classpath "$JAR" -d build/classes $(find app/src/main/java build/generated -name '*.java')
"$BT/d8" --lib "$JAR" --min-api 26 --output build/dex $(find build/classes -name '*.class')
cp build/base.apk build/unsigned.apk
(cd build/dex && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
if [[ -z "${SIGNING_KEYSTORE:-}" ]]; then
  echo 'Set SIGNING_KEYSTORE, SIGNING_ALIAS and SIGNING_PASSWORD for your own key.' >&2; exit 1
fi
"$BT/apksigner" sign --ks "$SIGNING_KEYSTORE" --ks-key-alias "${SIGNING_ALIAS:-budget}" --ks-pass env:SIGNING_PASSWORD --out dist/yueyouyu-3.0.0.apk build/aligned.apk
"$BT/apksigner" verify --verbose --print-certs dist/yueyouyu-3.0.0.apk
"$BT/aapt2" dump badging dist/yueyouyu-3.0.0.apk
sha256sum dist/yueyouyu-3.0.0.apk
