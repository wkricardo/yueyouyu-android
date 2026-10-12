# Android 4.1.0 verification

## New health-history workflow
A prominent trend action previews7/14/30 local-calendar days, default30. A separate native health_history grant discloses exact fields/range/recipient/costs; prior food/bill grants do not authorize it. Remembering is unchecked initially and revocable. No background sending or scheduled upload exists.

Strict daily summaries exclude profile, bank data, meal names, notes, identifiers, arbitrary exercise names and conversation history. Missing observations remain null; completion with no meal rows is separately flagged as unknown. Same-day weights use daily arithmetic means and original counts. Calendar spacing, recent observed averages, count/span, completeness and regression rate are calculated locally. Rate requires at least4 observed dates across14 days; this is a cautious product rule, not a clinical standard.

AI returns closed advisory codes checked against the local evidence. Arbitrary text, contradictory claims, numerical prescriptions and extra fields are rejected. No ledger, diet, weight, exercise or plan writes occur. Changed records/ranges/midnight invalidate pending results; retained results keep original evidence with an outdated label. Native request ownership, revocation, frozen retry and five-second actual-send spacing are enforced; health output max_tokens600. The summary is bounded16KiB, bridge envelope32KiB, and one request owns the workflow at a time.

## Final passed checks
- 240 JavaScript/model/actual app.js jsdom tests, including15 new health DOM flows and12 independent health adversarial cases.
- 591 independent health JVM assertions, including15 JS-generated parity fixtures.
- Existing AI lifecycle1,244 JVM assertions,53 image and23 text production-payload parsing assertions.
- Existing native118 assertions:21 transport,66 updater,31 private provider.
- Prior image/consent patch130 targeted and176 independent checks remain passing.
- Clean Android36 compilation after deleting generated classes/Java/dex; independent fresh compilation matches all72 class files.

Tests cover timezone/calendar/leap boundaries, same-day measurements, sparse observations, no-record versus zero, complete-but-empty days, immutable summary/retry, isolated persistent grants and revoke, unsafe/contradictory replies, range/record/midnight change, mode/Back/cancel, native denied delivery, session reset and no automatic writes. Legacy DOM helpers now disconnect real observers before teardown; callbacks and assertions are unchanged.

## Integrity
cn.dot.budget, version4.1.0/code12, original certificate SHA25642932ba6fc75b6e1e216b8def10e4af37bb73e6ebc9c3b94d5d6e7206b8bd5fb, APK v2/v3. Domain model.js remains SHA25668054188359ec92ea4faa849416440e144d9c4d1c528197058ad0178a6154077 and schema5 is unchanged. Source/stage/APK asset parity and source archive privacy are verified separately. No private signing material, real health data, photos or credentials belong in release archives.

## Limits
No real Android hardware, rendered-pixel, TalkBack, keyboard/insets, camera codec, upgrade-installation or live DeepSeek call was tested. All health fixtures are synthetic; no private health data or paid API calls were transmitted. JVM/DOM tests and source review do not establish clinical accuracy, real-world model quality or device behavior. The app offers limited record-based advice, not medical safety certification or personalized prescriptions.

## Reproduce
npm install; npm test. With JDK21 run tests/run-native-tests.sh, tests/run-ai-review-tests.sh, tests/run-v401-tests.sh, tests/run-v401-independent-review.sh and tests/run-v410-independent-review.sh using bash. Runners accept JAVA_HOME. Build with official Android SDK36 and your own signing credentials using build.sh. Source intentionally excludes private keys.
