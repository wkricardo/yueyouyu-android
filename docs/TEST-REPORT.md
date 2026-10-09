# Android 3.0.0 verification

## Twenty real iterations
Budget route architecture, shared icons, glass navigation, safe-area/keyboard handling, period header, monthly ring, transaction metrics, category warnings, daily caps, quick entries, ledger filters, grouped timeline, cashflow chart, signed category chart, plan editor, shared settings, transaction editor, screenshot review, accessible narrow-screen styling, and interruption hardening were implemented sequentially. Each has a distinct archive, SHA256 and passing test transcript: see ITERATIONS-V3.md. Only final publication is intended.

## Passed
136 model and actual app.js/jsdom tests pass, including21 independently written unified-shell integration tests. 105 native mock assertions pass:21 transport,66 updater,18 private-file policy. model.js, wellness.js/css and native source remain byte-identical to v2.0.0; existing schema5 and storage semantics are preserved.

New tests cover four budget routes, cross-mode return, exact integer-cent transaction/refund/income/transfer/repayment totals,80%/over-budget boundaries, monthly signed ring and zero target, selected-day caps, quick entries, filters/group subtotals, full-month refund-aware cashflow/real calendar spacing, signed category drilldown, budget draft validation, full-screen modal opener focus/repeated submits, review replacement/cancellation, dynamic nested select sheets, keyboard versus ordinary resize, modal-local error visibility, true last-opened-first Back handling, invalid month/day recovery and leap bounds. Existing115 tests cover health/food, import dates, backup migration and prior privacy rules.

## Integrity
Package cn.dot.budget, version3.0.0/code9, original pinned certificate, APK v2/v3 signatures. build-verification.txt contains package/signature/checksum evidence. Independent checks compare final APK assets, source stage, all20 snapshots/manifests and public privacy. No signing private material, actual user photos, records, health profiles or credentials belong in public artifacts.

## Limits
Source UX and DOM emulation are not pixel-rendered or Android device validation. Supported cloud-browser preview remained unavailable; blocked routes were not retried or bypassed. Real glass blur appearance,360/390/tablet layouts, safe areas, Android keyboard and predictive Back require device validation. No actual API credentials or user images were used. Camera,Keystore,picker,notifications,installer and automatic update installation remain untested on hardware. GitHub publication is verified separately.

## Reproduce
npm install; npm test. JAVA_HOME=/path/to/jdk21 bash tests/run-native-tests.sh. Build with official Android SDK36 and own securely held signing credentials using build.sh. Source excludes private keys.
