# Android 1.3.0 verification

## Scope
Local food favorites saved from logged meals, editable names/default portions/calorie ranges, duplicate-name handling, quick meal prefill with explicit save, proportional portion scaling from a stable baseline, and user-controlled calorie overrides. Existing ledger, nutrition and weight records migrate to schema4 and remain separate from favorite templates. Favorite changes never alter historical meals.

## Passed
71 model and real app.js/jsdom regression tests pass, including repeated closed-dialog submit prevention and nested cancel behavior. 105 native mock assertions pass (21 transport, 66 updater, 18 private-file policy).

## Evidence
Final model/actual app.js DOM results are in model-dom-results.txt. Native mock results are in native-results.txt. APK build, package/version, checksum and signer results are in build-verification.txt. Independent review checks final APK assets against the reviewed source and audits the clean public staging tree.

Regression scope includes old schema1/2/3 migration, backup/import roundtrip, normalized duplicate names, cancel/no-write, favorite editing/deletion, re-logging date/portion/calories, repeated portion changes without compounding, manual overrides including label calculation, invalid zero/negative/nonfinite weights and valid zero-calorie foods. Food favorites do not access network or native AI interfaces.

## Native tests
21 mocked DeepSeek transport assertions, 66 mocked GitHub updater assertions, and 18 exact-path private ContentProvider assertions. No real credentials, external API calls or user images used.

## Release integrity
Package cn.dot.budget, version1.3.0/code6, same pinned signer as prior Android versions; APK v2/v3 signatures. New source/release staging directories are distinct from pending1.2.0 upload artifacts. Public source excludes signing keys, SDKs/caches, actual user photos, databases and API secrets.

## Limits
jsdom is DOM emulation, not Android WebView/device/visual validation. No Android device or emulator was available. Real camera, Keystore, picture picker, notification, installer and future update installation still need device testing. No real DeepSeek API call was performed. GitHub release availability is verified separately by publication.

## Reproduce
Run npm install and npm test. Run JAVA_HOME=/path/to/jdk21 bash tests/run-native-tests.sh. Build with official Android SDK36 and your own securely held signing key using build.sh. No signing private material is included.
