# Android 4.0.1 verification

## Problem and scope
Source inspection verified that 4.0.0 rejected original images over10MiB before compression and performed original-image loading/decoding on the UI thread. Single-pass encoding could also fail without a smaller encoded attempt. These are reproduced policy/source defects; the user's exact camera failure has not been reproduced on their phone.

4.0.1 streams originals to private bounded cache on a shared single worker, keeps camera descriptor ownership across unlink, normalizes EXIF and samples before allocation. Encoded image maximum is9,999,999 bytes; the actual UTF8 request maximum is15,000,000 bytes, including Base64. Originals allow up to128MiB,65535 pixels per edge and268435456 total pixels, subject to decoded-memory limits. Food uses a bounded12-attempt quality/size ladder; bills use lossless-first and up to3 same-size JPEG alternatives, refusing unsafe alpha conversion. Bills needing too much downsampling must be cropped; no oversized-original fallback exists.

Four native versioned consent scopes are opt-in, unchecked initially and never inferred from old per-use acceptance. Verified writes and revocation fail closed; revocation invalidates requests and delayed results. Saved-scope explicit retries remain charge-labeled without a repeated consent dialog. Redacted bills retain an actual-current-image task preview and recognition action. Text credential checks are supplemental, not a guaranteed detector. No background collection or automatic retries were added.

## Passed
- 213 model and actual app.js/jsdom tests, including two new settings/consent-copy integration cases.
- 130 focused native policy, injected-codec ladder, source-stream, descriptor ownership, cancellation and consent checks.
- 176 independent pure-JVM boundary/persistence/revocation/orientation checks.
- Existing native tests:21 transport +66 updater +31 provider =118 assertions.
- Existing AI JVM:1,244 assertions; independent production request parsing:53 image +23 text assertions.
- Clean Android36 compilation after clearing generated class/Java/dex directories; signed package cn.dot.budget, version4.0.1/code11, APK v2/v3.

Certificate SHA256 remains42932ba6fc75b6e1e216b8def10e4af37bb73e6ebc9c3b94d5d6e7206b8bd5fb. Domain model.js remains unchanged (SHA25668054188359ec92ea4faa849416440e144d9c4d1c528197058ad0178a6154077), as does schema5 storage. Final source/stage/APK assets are checked byte-for-byte. Signing credentials, actual user images and private records are excluded from archives.

## Not executed
No real Android device, camera/Bitmap codec, HEIF capability, TalkBack, actual keyboard/insets or rendered-pixel validation. Encoding tests inject a codec and assert the production retry policy; they do not prove Android JPEG visual quality. No real API keys, user screenshots or paid service calls were used. Native view/accessibility/lifecycle behavior has source review, not hardware runtime verification. The exact reported camera failure remains unconfirmed without device/error evidence.

## Reproduce
npm install; npm test. With JDK21 run bash tests/run-native-tests.sh, bash tests/run-ai-review-tests.sh, bash tests/run-v401-tests.sh and bash tests/run-v401-independent-review.sh. Runners accept JAVA_HOME. Build with official Android SDK36 using build.sh and your own securely held signing credentials. Private keys are intentionally absent. Earlier50-iteration evidence describes v4.0.0; this patch has its own regression transcripts.
