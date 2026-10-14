# Android 4.2.0 verification

## Unified entry workflow
Fresh launch shows the shared first/home page. Two side-by-side expandable groups hold the existing unique input controls: accounting on the left; food, weight and exercise on the right. Food camera and gallery actions are adjacent and explicitly named. Report/trend/plan pages and historical-record editing remain. Contextual new-record actions return to the same home/form; deliberate continuation actions such as copying a saved meal to a favorite retain their task context.

Home date selectors expose accounting and food/exercise dates before opening inputs. Independent mode palettes use per-group contrast-safe tokens. Groups use aria-expanded/controls, keyboard focus, bounded columns and48px minimum controls; bottom navigation retains modal/keyboard/safe-area behavior. These are source/DOM checks, not visual rendering claims.

Draft baselines cover accounting, food, weight, exercise, favorites and profile forms. Declined Close/Back/Escape/switch preserves inputs; confirmed switches are explicit; repeated opens do not reset drafts. Modern AI callbacks retain validated delayed-result handling. Direct legacy food/bank callbacks cannot nest over unrelated manual dialogs or active settings edits; they show a local notice without introducing unverifiable deferred results. No callback writes records automatically.

## Gallery path
The previous version already contained chooseFoodPhoto and food_image native handling; the visible shortcut emphasized camera while gallery was deeper in the food log. This release makes gallery visible in the home health group. Native Java is byte-identical to4.1.0: ACTION_GET_CONTENT(image/*, CATEGORY_OPENABLE), no broad photo/storage permission, selected URI only, same compression/EXIF/size bounds, native consent, cancellation and request ownership. No live phone picker was exercised.

## Final passed checks
- 260 JavaScript/model/actual app.js jsdom tests:240 previous plus20 independent unified-entry cases.
- Native118 assertions:21 transport,66 updater,31 private provider.
- AI lifecycle1,244 JVM assertions and53 image +23 text request parsing assertions.
- Image/consent patch130 focused +176 independent checks.
- Health summary591 independent JVM assertions, with JS parity fixtures.
- Clean Android36 build after generated intermediate cleanup; same package/signature; asset/source parity checked.

Coverage includes mode/home routing, single gallery/camera source identity, repeated chooser cancellation and stale callbacks, declined/accepted owner change, dirty-form focus retention, new date binding, label shortcut focus, same-mode switch, single saves, contextual edit origin, shared-settings return, keyboard/modal navigation, modern deferred AI and both legacy callback guards. Three old test expectations were adjusted for moved camera control, new-entry home origin and refusal of legacy nested takeover; privacy and replacement assertions remain.

## Integrity
Package cn.dot.budget, version4.2.0/code13; original certificate SHA25642932ba6fc75b6e1e216b8def10e4af37bb73e6ebc9c3b94d5d6e7206b8bd5fb; APK v2/v3. Native Java and domain model.js match4.1.0; model SHA25668054188359ec92ea4faa849416440e144d9c4d1c528197058ad0178a6154077. Existing schema5 storage remains unchanged. Public source excludes private signing material, photos, personal records, tools/build caches and credentials.

## Limits
No rendered-pixel or Android-device validation, real photo-picker/camera execution, keyboard/insets/TalkBack hardware validation, installation/upgrade test or live DeepSeek request. All input fixtures are synthetic; no private photos/health/financial data were transmitted. DOM tests emulate dialogs and native bridges. GitHub publication is outside this build's completed actions.

## Reproduce
npm install; npm test. With JDK21 run bash tests/run-native-tests.sh, tests/run-ai-review-tests.sh, tests/run-v401-tests.sh, tests/run-v401-independent-review.sh and tests/run-v410-independent-review.sh. Runners accept JAVA_HOME. Use official Android SDK36 and your own signing credentials with build.sh; private keys are intentionally absent.
