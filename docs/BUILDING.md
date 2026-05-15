# Building PDFSeal

PDFSeal is a standard Kotlin + Gradle Android project. It builds in Android Studio
or from the command line. No NDK build is required: MuPDF and Tesseract are pulled
as prebuilt artifacts.

## Requirements

| Tool | Version | Notes |
|------|---------|-------|
| JDK | **17** | Required by Android Gradle Plugin 8.x. JDK 11 will not work. |
| Android SDK | Platform 34, Build-Tools 34.0.0+ | `compileSdk`/`targetSdk` = 34, `minSdk` = 24 |
| Gradle | Wrapper-provided | Use `./gradlew`, do not install Gradle globally |
| NDK | **not required** | Native libs come prebuilt in the AAR dependencies |

### Reference local setup (maintainer machine)

- JDK 17: `/home/noneya/jdk-17.0.19+10`
- Android SDK: `/home/noneya/Android/Sdk`

```bash
export JAVA_HOME=/home/noneya/jdk-17.0.19+10
export ANDROID_HOME=/home/noneya/Android/Sdk
```

`local.properties` (gitignored) must point at the SDK:

```properties
sdk.dir=/home/noneya/Android/Sdk
```

## Building

Debug APK (debug-signed, installable for testing):

```bash
./gradlew :app:assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
```

Release APK (requires signing config — see RELEASING.md):

```bash
./gradlew :app:assembleRelease
# output: app/build/outputs/apk/release/app-release.apk
```

Run unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

## exFAT / external-disk workspace caveat

This project may be developed on an exFAT external disk (e.g.
`/media/noneya/300/PDFSeal/repo`). exFAT has no Unix permissions and no symlinks,
which can break Gradle's cache and the `gradlew` executable bit.

Mitigations:

1. **Keep the Gradle cache on an internal ext4 disk.** Either:
   - run with `--gradle-user-home`:
     ```bash
     ./gradlew --gradle-user-home /home/noneya/.gradle :app:assembleDebug
     ```
   - or `export GRADLE_USER_HOME=/home/noneya/.gradle` before building.
2. If `./gradlew` reports "Permission denied", invoke it via the JVM directly or
   re-run after `chmod +x gradlew` (the bit may not persist on exFAT — prefer
   `sh gradlew :app:assembleDebug`).
3. Build outputs (`app/build/`) on exFAT are fine but slower; they are gitignored.

## App icon regeneration

The launcher icon is generated from the source artwork
`PDFSeal Logo.png` (kept one directory above the repo, not committed at that path).
Icons are regenerated with **Pillow** (Python). Example:

```bash
python3 - <<'PY'
from PIL import Image, ImageDraw
src = Image.open("/media/noneya/300/PDFSeal/PDFSeal Logo.png").convert("RGBA")
sizes = {"mdpi":48,"hdpi":72,"xhdpi":96,"xxhdpi":144,"xxxhdpi":192}
for name, px in sizes.items():
    img = src.resize((px, px), Image.LANCZOS)
    img.save(f"app/src/main/res/mipmap-{name}/ic_launcher.png")
    # round variant
    mask = Image.new("L", (px, px), 0)
    ImageDraw.Draw(mask).ellipse((0,0,px,px), fill=255)
    rnd = img.copy(); rnd.putalpha(mask)
    rnd.save(f"app/src/main/res/mipmap-{name}/ic_launcher_round.png")
# adaptive foreground 432x432 with safe-zone padding
fg = Image.new("RGBA",(432,432),(0,0,0,0))
seal = src.resize((300,300), Image.LANCZOS)
fg.paste(seal,(66,66),seal)
fg.save("app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png")
PY
```

ImageMagick (`magick`) works too if installed; Pillow is the documented path
because it is already present on the maintainer machine.

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `Unsupported class file major version` | Wrong JDK. Use JDK 17, set `JAVA_HOME`. |
| `SDK location not found` | Create `local.properties` with `sdk.dir=...`. |
| `Could not resolve com.artifex.mupdf:fitz` | Confirm `mavenCentral()` is in `settings.gradle.kts` repositories; check the artifact version in `app/build.gradle.kts`. |
| Tesseract crashes / `eng.traineddata not found` | The trained data is copied from assets to `filesDir` on first OCR run. Confirm `app/src/main/assets/tessdata/eng.traineddata` exists in the build. |
| `gradlew: Permission denied` (exFAT) | Run `sh ./gradlew ...` or set the executable bit; see exFAT section above. |
| Gradle cache corruption on exFAT | Use `GRADLE_USER_HOME=/home/noneya/.gradle`. |
| Build very slow | Build output on exFAT is slow; consider building into an internal-disk worktree. |

## Sideloading the built APK

See the "Sideloading" section in [../README.md](../README.md) and
[RELEASING.md](RELEASING.md) for signed releases.
