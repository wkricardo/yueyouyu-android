# Android 2.0.0 verification

## Ten substantive iterations
Information architecture, budget ring, concise metrics/disclosures, dedicated quick-entry flows, favorite strip, meal timeline, exercise flow, actual weight graph, standalone plan/onboarding, and accessibility/interruption/narrow-screen polish were implemented sequentially. Every step has a distinct source archive, SHA256 and passing test transcript; see ITERATIONS-V2.md. Only the final version is released.

## Passed
115 model and real app.js/jsdom tests pass, including15 independently written wellness integration tests. 105 native mock assertions pass:21 DeepSeek transport,66 GitHub updater,18 private-file policy. The audited v1.4 model bytes remain unchanged; schema5 and data persistence are preserved.

New tests cover four routes and budget isolation, visible settings Back return, nested selector/dialog cancellation, ring selected-date ranges/zero/negative/no-plan states, favorites explicit save/history independence, real SVG chronological spacing and latest14/full-history text, weight cancel/repeated submit, five modal focus restoration/repeated draft-open handling, delayed AI result replacement confirmation, blank eligibility-gated plan, and exercise active-calorie/date/no duplicate submit rules.

## Integrity
Package cn.dot.budget, version2.0.0/code8, original pinned certificate and APK v2/v3. Build signature/version/checksum are in build-verification.txt. Final APK assets and public source tree are independently compared. Signing private keys, actual photos, health/account data and credentials are excluded from public source and artifacts.

## Limits
jsdom is DOM emulation, not Android WebView/device or pixel-rendered visual validation. Source-level UX review verified hierarchy, responsive rules and accessibility contracts. Supported cloud browser had no usable local preview bridge; file/data protocols were unsupported and loopback preview inaccessible. No alternative route was used to bypass those restrictions. No genuine screenshot visual validation, Android device/emulator or real API call is claimed. Camera, Keystore, picker, notifications, installer, predictive Back and actual update installation still need device testing. GitHub publication is verified separately.

## Reproduce
npm install; npm test. JAVA_HOME=/path/to/jdk21 bash tests/run-native-tests.sh. Build using official Android SDK36 and your own securely held signing key with build.sh. Private signing material is never included.
