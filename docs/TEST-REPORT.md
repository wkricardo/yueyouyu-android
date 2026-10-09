# Android 1.4.0 verification

100 model and actual app.js/jsdom tests pass. Native mocks pass 105 assertions: 21 DeepSeek transport, 66 GitHub updater, 18 private-file policy.

New coverage: schema5 migration and backup preservation, blank local body profile, conservative editable planning, low-activity starter assumptions/manual maintenance, no double-counting exercise, date/CRUD/duplicate-submit guards, unsafe/inapplicable planning suppression while logs remain usable, remaining range versus whole-day projected deficit, explicit completion, local-only privacy; custom select keyboard/disabled/dynamic options/cancel/focus/reopen flows; independent saved mode colors and text contrast (4096 sampled colors).

The custom select routes Android Back through legacy and API33+ OnBackInvokedCallback. Native compilation verifies SDK API use; actual predictive Back gesture requires device validation.

Evidence: model-dom-results.txt, native-results.txt and build-verification.txt. Release package cn.dot.budget version1.4.0/code7 is signed with the original pinned certificate, APK v2/v3. Independent reviewer compares source/public tree/APK assets and scans staging for accidental private content.

## Limits
jsdom is DOM emulation, not Android WebView/device/visual validation. Supported cloud browser could not access the app preview: file scheme is unsupported and isolated loopback preview was unreachable. No visual screenshot verification is claimed. No Android device/emulator or real API credentials were used. Camera, Keystore, picker, notifications, installer, predictive Back and actual update installation still need device testing. GitHub publication is verified separately.

## Reproduce
npm install; npm test. JAVA_HOME=/path/to/jdk21 bash tests/run-native-tests.sh. Build with official Android SDK36 and your own securely held signing key using build.sh. Private signing material is excluded.
