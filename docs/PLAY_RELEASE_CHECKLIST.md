# Google Play release checklist

- [ ] CI debug build and lint green
- [ ] Unit tests green
- [ ] Release AAB builds
- [ ] Owner creates and secures production signing key
- [ ] Final package/application ID owner review
- [ ] Adaptive icon and splash reviewed on physical devices
- [ ] Privacy policy URL supplied and owner-reviewed
- [ ] Data Safety form owner-reviewed against shipped behavior
- [ ] Open-source notices reviewed
- [ ] Real Play Billing product configured and tested
- [ ] Restore-purchases behavior tested
- [ ] Store screenshots/feature graphic
- [ ] Fold cover/inner display QA, including a fold while a screen is open
- [ ] No debug UI or fake purchase state. The Visual QA matrix lives in `src/debug`,
      so confirm it is absent from the release manifest rather than merely unreachable:
      `./gradlew bundleRelease` then check the merged manifest for `QaMatrixActivity`
- [ ] Pending-purchase state tested with a Play test card that settles slowly

## Claims review

Store listing and screenshots must not promise what Android does not allow. Specifically:

- [ ] No claim that DarkFrame replaces icons system-wide, automatically, or without user steps
- [ ] Icon-pack support described as applying to launchers that support icon packs, not to all
- [ ] Pinned shortcuts described as adding a themed shortcut, not as replacing an app's icon
- [ ] No implication that Samsung One UI Home or Pixel Launcher can be themed by DarkFrame directly
- [ ] Screenshots that show a fully themed home screen state which launcher produced it
- [ ] Coverage described as "every app your device exposes", with the documented gaps not hidden

## Permissions review

- [ ] No `QUERY_ALL_PACKAGES` in the merged manifest
- [ ] `<queries>` limited to `CATEGORY_LAUNCHER` and `CATEGORY_HOME`
- [ ] Data Safety form states that installed-app metadata is read on device and never transmitted
