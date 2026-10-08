# Android 1.2.0 verification

## Passed
- 52 model and real app.js/jsdom tests, covering old ledger/schema migrations, budget behavior, screenshot date review, food/weight CRUD, nutrition kJ/kcal conversion (4.184), estimated calorie ranges, local-only weight, explicit review/save, and mode switching.
- 21 mocked DeepSeek transport assertions. No real key, screenshot or paid request.
- 66 mocked GitHub updater assertions: fixed origins, redirects, response/download limits, malformed releases, checksums, cancellation, wrong package/signature/version and rollback rejection.
- 18 exact-path ContentProvider policy assertions: only one camera cache file and one read-only update APK can be shared; path traversal/arbitrary files/writable APK access rejected.
- Android API36 build produced real signed APK, package cn.dot.budget, versionName1.2.0, versionCode5. APK v2/v3 signature verified and signer matches previous releases and pinned updater certificate.
- Packaged assets byte-for-byte match tested source. Static DOM IDs and JavaScript syntax checked.
- Separate public source staging excludes signing keys, API keys, SDK/JDK, caches, actual user photos and databases.

## Limits
jsdom is DOM emulation, not Android WebView/device/visual validation. No Android device or emulator was available. System camera, native Keystore, picture picker, notifications, unknown-source permission and installer flow need device testing. No real DeepSeek API call or real future GitHub update installation was performed. Network/update behavior was tested with mocks; published release availability is verified separately during publication.

## Reproduce
Run `npm install && npm test`, then `JAVA_HOME=/path/to/jdk21 bash tests/run-native-tests.sh`. Build with official Android SDK36 and your own securely held signing key using build.sh. Build/test logs are included in this directory. No signing private material is included.
