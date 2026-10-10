# Android 4.0.0 verification

## Passed
50 sequential implementation checkpoints each contain a distinct production-source delta, SHA256 manifest and successful test transcript. See ITERATIONS-V4.md and the separate evidence archive.

211 JavaScript tests pass, including 75 independently written AI contract and actual app.js/jsdom cases. Pure JVM tests pass 1,244 AI assertions. Independent Node JSON.parse checks pass 53 image-payload and 23 text-payload assertions. Original native mock tests pass 118 assertions: 21 transport, 66 updater and 31 exact private-file provider cases.

Coverage includes immutable exact consent payloads; no hidden history/profile context; stale request, input revision, cancellation, lifecycle and key-clear rejection; strict JSON/schema/UTF8 limits; image allocation and output limits; one-use picker codes and unique camera URI mapping; typed error/manual retry; exact-cents financial repair and duplicates; year provenance; batch-save idempotency; food unknown values, immutable portion scaling, label takeover, date binding and canonical-only storage; deferred result viewing, visible shared status and modal Cancel accessibility; literal-text offline handoff. Existing accounting, budget80% boundaries, health arithmetic, favorites, backups and navigation regressions remain covered.

## Integrity
Package cn.dot.budget, version4.0.0/code10, original certificate SHA256 42932ba6fc75b6e1e216b8def10e4af37bb73e6ebc9c3b94d5d6e7206b8bd5fb, APK v2/v3 signatures. Domain model.js and schema5 remain unchanged. Source, final checkpoint and APK assets are compared byte-for-byte. Signing keys, actual photos, personal records and credentials are excluded from distributable source.

## Limitations
Source review and emulated DOM are not rendered visual or Android device tests. Pixel appearance, device keyboard/safe areas, TalkBack, camera bitmap/EXIF processing, process-death behavior, Keystore, notifications, system installer and live API calls remain unverified on hardware. All network tests use mocks and synthetic data. No claim of server zero-retention is made. Publication status is separate from build verification.

## Reproduce
Install jsdom26.1.0 using npm install, then npm test. With JDK21, run bash tests/run-ai-review-tests.sh and bash tests/run-native-tests.sh. Build using official Android SDK36 and your own signing credentials with build.sh. Test runners also accept JAVA_HOME. Private signing material is not included.
