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

## 4. Build the signed release APK

```bash
export JAVA_HOME=/home/noneya/jdk-17.0.19+10
./gradlew :app:assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

Rename per the versioning rule (see below), e.g. `PDFSeal-0.1.0.apk`.

### Versioning rule (mandatory)

Every rebuilt APK must bump **all three**: `versionCode`, `versionName`, and the
distributed file name. Android may reject an install if `versionCode` did not
increase. This matches the established workflow for other apps on this machine.

## 5. Verify the signature and capture the fingerprint

```bash
$ANDROID_HOME/build-tools/35.0.0/apksigner verify --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

Record the **SHA-256** certificate fingerprint. Put it in:

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

## 6. Compute the APK checksum

```bash
sha256sum PDFSeal-0.1.0.apk > PDFSeal-0.1.0.apk.sha256
cat PDFSeal-0.1.0.apk.sha256
```

## 7. Publish the GitHub Release

1. Tag the commit: `git tag v0.1.0 && git push origin v0.1.0`
2. Create the release at
   <https://github.com/WGRALGO/PDFSeal/releases/new> (or `gh release create`).
3. Attach:
   - `PDFSeal-0.1.0.apk`
   - `PDFSeal-0.1.0.apk.sha256`
4. In the release notes include:
   - what changed,
   - the APK SHA-256,
   - the signing certificate SHA-256 fingerprint,
   - a reminder that the build is local/offline with no ads/cloud/login.

**Never** attach or paste the `.jks`, `key.properties`, or any password.

## 8. Pre-publish safety checklist

- [ ] `git status` clean; no `*.jks`, `key.properties` staged.
- [ ] `git log -p | grep -iE 'PRIVATE KEY|storePassword|keyPassword'` → no hits.
- [ ] `versionCode` increased vs. previous release.
- [ ] APK installs over the previous version on a device **without uninstall**.
- [ ] SHA-256 in release notes matches the attached APK.
