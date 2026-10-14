# Android4.3.0 verification

## Workflow
Two compact home corner controls replace the large input groups: accounting at lower left, health at lower right. Only AI and Manual appear in each first expansion. Secondary choices retain the unique original controls and dates. Health AI explicitly offers camera, gallery and text. Reports/trends/plans and contextual historical edits remain.

Reference pixels were inspected; implementation adapts a small radial fan with central close affordance. Source/DOM checks cover outside/Back/Escape dismissal, keyboard focus recovery/exit, one open group, minimum targets, reduced motion, distinct palettes, bottom-navigation clearance and safe areas. Draft protection and deferred AI receipt validation remain authoritative. No automatic record writes.

## Native behavior
Gallery remains ACTION_GET_CONTENT(image/*,CATEGORY_OPENABLE), camera uses its separate source. No broad storage permission. Native Java and domain model are unchanged from4.2.0: bounded compression, EXIF handling, scoped consent, private camera URI ownership, cancellation, health privacy and signed updater checks remain. Schema5 data unchanged.

## Checks
275 JavaScript/model/actual app.js jsdom tests pass, including15 independent corner-entry cases. They cover24 dismissal combinations,8 late-result chooser races, dirty drafts, gallery source identity, keyboard focus-exit dismissal, favorites-management mode change and pending-owner decline/accept. Native reruns pass118 original assertions,1244 AI lifecycle assertions,53 image and23 text payload assertions,130 image/consent checks,176 independent image-policy checks and591 health JVM assertions with synthetic parity fixtures.

Build cleans generated class/resource/dex intermediates. Package cn.dot.budget,4.3.0/code14; original certificate SHA25642932ba6fc75b6e1e216b8def10e4af37bb73e6ebc9c3b94d5d6e7206b8bd5fb with v2/v3 APK signing. Public source excludes signing keys, photos, user records, tools/build caches and credentials.

## Limits
No rendered-pixel or real Android camera/gallery, keyboard/insets, TalkBack, installation/upgrade or live DeepSeek test. DOM dialogs/native bridges are emulated. All data fixtures are synthetic; no private photos/health/financial data transmitted. Publication is separately verified.

## Reproduce
npm install; npm test. With JDK21 run bash tests/run-native-tests.sh, tests/run-ai-review-tests.sh, tests/run-v401-tests.sh, tests/run-v401-independent-review.sh and tests/run-v410-independent-review.sh. Runners accept JAVA_HOME. Use official Android SDK36 and your own signing credentials with build.sh; private keys are intentionally absent.
