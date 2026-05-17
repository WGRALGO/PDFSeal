# Releasing PDFSeal

This document explains how to produce a **signed release APK**, publish it on
GitHub Releases, and why the signing key must be protected.

> **The private signing key is NEVER committed to this repository.**
> `.gitignore` excludes `*.jks`, `*.keystore`, and `key.properties`.

## 1. Generate a release keystore (one time)

The keystore lives **outside** the repository. On the maintainer machine it is at:

```
/home/noneya/.pdfseal-keystore/release-keystore.jks
```

Generate it with `keytool` (JDK):

```bash
keytool -genkeypair \
  -keystore /home/noneya/.pdfseal-keystore/release-keystore.jks \
  -alias pdfseal \
  -keyalg RSA -keysize 4096 \
  -validity 36500 \
  -storetype PKCS12 \
  -dname "CN=PDFSeal, OU=PDFSeal, O=The Wealth Gap Resolution Algorithm, L=, S=, C=US"
```

- `-validity 36500` ≈ 100 years, so the key does not expire before the app's life.
- Use a strong store/key password. On the maintainer machine the generated
  password is saved (plaintext, `chmod 600`, outside the repo) at
  `/home/noneya/.pdfseal-keystore.txt` — the same pattern used for other apps on
  this machine. **Back this file up.**

## 2. Back up the keystore securely

If you lose the signing key you **cannot ship updates that install over an
existing PDFSeal install** — Android rejects an update signed with a different
key. Users would have to uninstall (losing app data) and reinstall.

Therefore:

- Copy `release-keystore.jks` **and** the password file to at least one
  offline / encrypted backup (encrypted USB, password manager attachment, etc.).
- Never put either in the git repo, an issue, a PR, a screenshot, or a cloud
  drive that is publicly shared.

## 3. Configure `key.properties`

Copy the example and fill in real values (this file is gitignored):

```bash
cp key.properties.example key.properties
```

```properties
storeFile=/home/noneya/.pdfseal-keystore/release-keystore.jks
storePassword=<real store password>
keyAlias=pdfseal
keyPassword=<real key password>
```

`app/build.gradle.kts` reads this file if present and configures the release
signing config. If the file is absent, the release build falls back gracefully
(unsigned / debug) so CI and fresh clones still build.

## 4. Build the signed release APKs (dual-APK flow)

Since v1.0.0 a release ships **two** APKs, built with the `-PabiSplit` flag:

| File | ABIs | For |
|------|------|-----|
| `PDFSeal-<ver>-arm64-v8a.apk` | `arm64-v8a` only | most modern phones/tablets (smallest) |
| `PDFSeal-<ver>-universal.apk` | `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` | unsure / older / emulator / multi-ABI |

> Provenance rule (mandatory): build from a **clean tree at the exact release
> tag** so AGP embeds the right commit into the APK (see §6.5). Tag *before*
> building: `HEAD` must equal the `vX.Y.Z` tag commit.

```bash
export JAVA_HOME=/home/noneya/jdk-portable/jdk-17.0.19+10
export GRADLE_USER_HOME=/home/noneya/.gradle   # exFAT workspace caveat

# HEAD must already be the release commit AND be tagged.
git status --porcelain   # must be empty
git rev-parse HEAD        # must equal git rev-parse vX.Y.Z^{commit}

sh ./gradlew --gradle-user-home /home/noneya/.gradle \
  clean :app:assembleRelease -PabiSplit
```

Outputs in `app/build/outputs/apk/release/`:

- `app-arm64-v8a-release.apk`   → rename `PDFSeal-<ver>-arm64-v8a.apk`
- `app-universal-release.apk`   → rename `PDFSeal-<ver>-universal.apk`
- `app-armeabi-v7a-release.apk` → **not shipped** since v1.0.0 (universal covers it)

```bash
cd release
R=../app/build/outputs/apk/release
cp "$R/app-arm64-v8a-release.apk" PDFSeal-1.0.0-arm64-v8a.apk
cp "$R/app-universal-release.apk" PDFSeal-1.0.0-universal.apk
```

### Versioning rule (mandatory)

Every rebuilt APK must bump **all three**: `versionCode`, `versionName`, and the
distributed file names. Android rejects an install if `versionCode` did not
increase. Both APKs in a release share the same `versionCode`/`versionName`.

## 5. Verify the signature and capture the fingerprint

Verify **both** shipped APKs:

```bash
AS=$(ls -d $ANDROID_HOME/build-tools/*/ | sort -V | tail -1)
for f in release/PDFSeal-1.0.0-arm64-v8a.apk release/PDFSeal-1.0.0-universal.apk; do
  "${AS}apksigner" verify --print-certs "$f" | grep -i SHA-256
done
```

Both must report the same digest as the documented fingerprint below
(`f8d74e09…356137ed5`). Record the **SHA-256** certificate fingerprint. Put it in:

- the GitHub Release notes,
- the in-app **About** screen (placeholder until first real release),
- this file below.

```
Signing certificate SHA-256 fingerprint (public — safe to publish):
  F8:D7:4E:09:42:74:10:8F:B9:EB:A8:06:AE:61:0B:39:BA:E0:9F:39:F6:C9:F0:41:25:4E:38:03:56:13:7E:D5
```

> This is the **public** certificate fingerprint of the release keystore created
> on 2026-05-15. It is safe to publish and lets users verify a downloaded APK was
> signed with the genuine PDFSeal key. The matching **private** key
> (`/home/noneya/.pdfseal-keystore/release-keystore.jks`) and its password file
> (`/home/noneya/.pdfseal-keystore.txt`) are stored outside the repository and
> must never be committed or shared.

## 6. Compute the APK checksums (one per shipped APK)

```bash
cd release
sha256sum PDFSeal-1.0.0-arm64-v8a.apk > PDFSeal-1.0.0-arm64-v8a.apk.sha256
sha256sum PDFSeal-1.0.0-universal.apk > PDFSeal-1.0.0-universal.apk.sha256
sha256sum -c PDFSeal-1.0.0-arm64-v8a.apk.sha256 PDFSeal-1.0.0-universal.apk.sha256
```

## 6.5. Verify build provenance (mandatory since v1.0.0)

AGP embeds the build's git HEAD SHA into the APK at
`META-INF/version-control-info.textproto`. It **must** equal the release tag
commit, or the public source does not correspond to the binary (AGPL).

```bash
EXPECT=$(git rev-parse v1.0.0^{commit})
for f in release/PDFSeal-1.0.0-arm64-v8a.apk release/PDFSeal-1.0.0-universal.apk; do
  unzip -p "$f" META-INF/version-control-info.textproto | grep revision
done
echo "expected: $EXPECT"
```

Also confirm ABIs and version:

```bash
AS=$(ls -d $ANDROID_HOME/build-tools/*/ | sort -V | tail -1)
unzip -l release/PDFSeal-1.0.0-arm64-v8a.apk | grep -oE 'lib/[^/]+' | sort -u  # arm64-v8a only
unzip -l release/PDFSeal-1.0.0-universal.apk | grep -oE 'lib/[^/]+' | sort -u  # 4 ABIs
"${AS}aapt" dump badging release/PDFSeal-1.0.0-arm64-v8a.apk | grep versionName
```

If the embedded revision is wrong, the APK was built from a dirty tree or
pre-tag commit. Fix: ensure `HEAD == vX.Y.Z` and the tree is clean, then
rebuild from §4 (`clean` is required).

## 7. Publish the GitHub Release

1. Tag was created and pushed in §4 / before building:
   `git tag -a v1.0.0 -m "…" && git push origin main && git push origin v1.0.0`
2. Create the release with all **four** assets via `gh`:

   ```bash
   GH_TOKEN=$(gh auth token) gh release create v1.0.0 \
     --repo WGRALGO/WGRALGO-PDFSeal \
     --title "PDFSeal v1.0.0 — First Stable GitHub Sideload Release" \
     --notes-file <notes.md> \
     release/PDFSeal-1.0.0-arm64-v8a.apk \
     release/PDFSeal-1.0.0-arm64-v8a.apk.sha256 \
     release/PDFSeal-1.0.0-universal.apk \
     release/PDFSeal-1.0.0-universal.apk.sha256
   ```

   Re-upload after a rebuild: `gh release upload v1.0.0 <files> --clobber`.
3. Release notes must include:
   - what changed,
   - **both** APK SHA-256 sums,
   - the exact build commit + the verify command from §6.5,
   - the signing certificate SHA-256 fingerprint,
   - which APK to use (arm64-v8a recommended; universal for unsure/emulator),
   - honest limits (flattened export, not redaction, not certified signing),
   - reminder: local/offline, no ads/cloud/login/analytics/Play Services.

**Never** attach or paste the `.jks`, `key.properties`, or any password.

## 8. Pre-publish safety checklist

- [ ] `git status` clean; no `*.jks`, `key.properties` staged.
- [ ] `git log -p | grep -iE 'PRIVATE KEY|storePassword|keyPassword'` → no hits.
- [ ] `versionCode` increased vs. previous release (both APKs share it).
- [ ] `HEAD == vX.Y.Z` tag commit; tree clean; built with `clean … -PabiSplit`.
- [ ] Embedded `version-control-info.textproto` revision == tag commit (§6.5).
- [ ] arm64 APK has `lib/arm64-v8a` only; universal has all 4 ABIs.
- [ ] Both APKs signed with the genuine key (same cert SHA-256).
- [ ] Both `.sha256` files match their APKs; both in release notes.
- [ ] APK installs over the previous version on a device **without uninstall**.
