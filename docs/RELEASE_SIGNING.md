# Release signing

**No key, keystore, password or credential belongs in this repository.** The release build reads
its signing config from Gradle properties, and with none of them set it simply produces an unsigned
artifact — which is what CI builds.

## One-time setup, on the machine that releases

Create the upload key (keep the keystore file outside the repository, and back it up — losing it
means you can no longer update the app on Play without Google's key reset process):

```
keytool -genkeypair -v \
  -keystore ~/keys/darkframe-upload.jks \
  -alias darkframe \
  -keyalg RSA -keysize 4096 -validity 10000
```

Put the four values in `~/.gradle/gradle.properties`, which is outside the project:

```
DARKFRAME_STORE_FILE=/home/you/keys/darkframe-upload.jks
DARKFRAME_STORE_PASSWORD=…
DARKFRAME_KEY_ALIAS=darkframe
DARKFRAME_KEY_PASSWORD=…
```

## Building

```
./gradlew bundleRelease     # app/build/outputs/bundle/release/app-release.aab
./gradlew assembleRelease   # APK, for sideload testing
```

With the properties set the artifact is signed; without them it is unsigned and Play will reject
it, which is the intended failure mode rather than a silent debug-signed upload.

## Play App Signing

Enrol in Play App Signing and upload the `.aab`. Google then holds the app signing key and your
upload key is only an identity — if it is ever compromised, it can be rotated without the app
losing its identity on devices.

## Checklist before an upload

- [ ] `versionCode` incremented in `app/build.gradle.kts`
- [ ] `./gradlew testDebugUnitTest assembleDebug lintDebug bundleRelease` all green
- [ ] Release build installed and opened once on a real device
- [ ] Billing tested through Play **Internal Testing**, not a sideloaded APK — a debug-signed build
      cannot complete a real purchase, and DarkFrame has no fake Pro path to stand in for one
- [ ] Data Safety form matches `docs/PACKAGE_VISIBILITY.md`
- [ ] No `QUERY_ALL_PACKAGES` in the merged manifest
