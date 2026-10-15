# Android4.3.1 verification

## Common waiting defect
The released4.3.0 APK hash and source were verified before editing. Native transport is identical4.0.1–4.3.0; request/bridge/delivery classes identical4.1–4.3.0. No new4.3-specific cause was established. The old client has connect/read-idle timeouts, but continuous keepalive bytes and blocked uploads can avoid a terminal result for a long time. Official DeepSeek docs explicitly describe blank-line keepalive while queued. See AI-WAIT-DIAGNOSIS.md.

## Repair
The shared transport bounds upload at30seconds and the whole network request at120seconds. Completion is independent of potentially blocking socket cleanup; I/O and cleanup pools are each limited to2 workers. Timeout/cancel selects one terminal outcome, and late success cannot replace it. No automatic retries, endpoint changes, model changes, extra data or credential logging.

The page also bounds missing native startup acknowledgment at10seconds and missing terminal delivery after Sending at130seconds. Timers are attempt-bound, do not restart on repeated phase events, clear on terminal/cancel/reset, and cancel only the matching native attempt. Photo picking and user consent are not timed out as network work. Status text shows the limits and preserves manual entry; a new request/retry remains explicit and may incur a new charge.

## Validation
Frozen source passes312 JavaScript/model/actual DOM tests, including37 independent waiting regressions;99 independent Java transport assertions pass. Prior suites pass118 native,1244 lifecycle,53 image+23 text payload,130 image/consent,176 independent image-policy and591 health JVM assertions. Synthetic native tests exercise keepalive, blocked connection acquisition/upload/headers/read/cleanup, cancellation, late completion and bounded resources. Actual app.js DOM fixtures cover food camera/gallery/text, bill image/text and health_history, including error/cancel/success and stale-result isolation. Prior financial, food, health, consent, update and provider regression suites remain included.

Clean Android36 build removes generated intermediates. Package cn.dot.budget,4.3.1/code15, original certificate SHA25642932ba6fc75b6e1e216b8def10e4af37bb73e6ebc9c3b94d5d6e7206b8bd5fb, v2/v3 signatures. Public source excludes private signing credentials, photos, user records and build/tool caches. Domain model and schema5 remain unchanged.

## Limits
No real API call or private user data was used. The original incident’s precise server/network cause is unverified. No promise of seconds-long server response. No actual Android installation/upgrade, networking hardware, rendered-pixel, TalkBack or camera execution test. DOM bridges/dialogs and network connections are mocked. Publication is separately verified.

## Reproduce
npm install; npm test. With JDK21 run bash tests/run-native-tests.sh, tests/run-ai-review-tests.sh, tests/run-v401-tests.sh, tests/run-v401-independent-review.sh, tests/run-v410-independent-review.sh and tests/run-v431-independent-review.sh and tests/run-v431-wait-comparison.sh. Runners accept JAVA_HOME. Build with official AndroidSDK36 and your own signing credentials; private signing files are intentionally absent.
