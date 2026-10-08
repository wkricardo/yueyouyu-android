# Android GitHub update protocol

The native updater checks only the latest **public, non-draft, non-prerelease** release from `wkricardo/yueyouyu-android`. No GitHub token, user account, app ledger, image, or AI credential is sent. Requests expose standard network metadata to GitHub. Automatic startup checks are read-only; download and opening Android's installer each require a separate tap. Android requires the final installation confirmation.

## Release contract

- Tag `vMAJOR.MINOR.PATCH` (or the same numeric version without `v`), with the APK's versionName numerically equal to the tag.
- Asset `yueyouyu-android.apk`, with the GitHub asset's declared byte size matching its actual size.
- Asset `SHA256SUMS`, containing exactly one entry for that filename, e.g. `<64 hex SHA-256 characters>  yueyouyu-android.apk`.
- Both browser download URLs must identify those exact assets in that exact tag of the fixed repository.
- Package `cn.dot.budget`; archive versionCode must exceed the installed versionCode.
- Exactly one current signing certificate. Both the installed app and archive must match the pinned SHA-256 certificate `42932ba6fc75b6e1e216b8def10e4af37bb73e6ebc9c3b94d5d6e7206b8bd5fb`.

The release checksum is an integrity check, not independent proof of publisher identity. The certificate pin, package check, and Android's signature verification provide the installation trust boundary. Certificate rotation is intentionally rejected and would require a deliberate app update to this policy.

## Transport and lifecycle

HTTPS only. The metadata endpoint cannot redirect. Asset downloads start on the exact repository's `github.com` release path and follow at most four redirects to the same repository on github.com, release-assets.githubusercontent.com, or objects.githubusercontent.com. Other hosts, userinfo, fragments, custom ports, and HTTP are rejected. No authorization header, cookies, analytics, retries, or custom TLS trust override is used.

Metadata is capped at 512 KiB, checksums at 64 KiB, and APKs at 150 MiB. Socket connect/read timeouts are 15/30 seconds, with transfer-duration limits checked between reads. Downloads use private cache, and failed or canceled downloads are removed. The APK is hashed and inspected before the install offer, then checked again immediately before installer access. The installer receives a temporary, read-only `content://cn.dot.budget.files/apk/update.apk` URI grant. The provider must map that exact URI to cache/update.apk and deny writing.

If Android blocks installs from this app, the user can choose to open Android Settings. The app never changes the setting. After returning, the user must click Install again. Checks and downloads never write ledger data. Cancellation does not install anything. Startup connectivity/no-release failures are quiet; manual checks display a status message.

## Verification

Run `tests/run-updater-tests.sh` for pure-JVM mocked transport and trust-boundary checks. No network calls or installation occur in these tests. PackageManager archive parsing, runtime permission screens, and the final installer UI require Android-device validation; compilation and JVM tests do not establish those device-level behaviors.

References: https://docs.github.com/en/rest/releases/releases and https://developer.android.com/reference/android/content/pm/PackageManager
