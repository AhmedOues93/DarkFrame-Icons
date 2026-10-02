# Package visibility and Google Play

## What DarkFrame declares

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
    </intent>
    <package android:name="com.samsung.android.themedesigner" />
    <package android:name="com.samsung.app.goodlock" />
    <package android:name="com.samsung.android.goodlock" />
    <package android:name="com.sec.android.app.samsungapps" />
</queries>
```

That is the entire declaration: two intent filters, four named packages, no permissions.

1. **`CATEGORY_LAUNCHER`** — the apps that have a launcher icon. This is exactly the subject of the
   product: DarkFrame themes launcher icons, so it asks about apps that have one.
2. **`CATEGORY_HOME`** — the home launchers, so DarkFrame can identify the active launcher and offer
   only the apply mechanism that launcher actually supports.
3. **Four named Samsung packages** — Theme Park, Good Lock (both package names it has used) and the
   Galaxy Store. Naming a package is the narrowest form of visibility there is: it answers "is this
   one thing installed?" and reveals nothing else. The Samsung flow needs it to offer the right step
   instead of guessing, and to disable the button when nothing can serve it.

## Why not `QUERY_ALL_PACKAGES`

`QUERY_ALL_PACKAGES` would make development marginally easier and is not used, for two reasons.

**It is broader than the feature needs.** The `<queries>` declaration above already returns the
user's whole app drawer. `QUERY_ALL_PACKAGES` would additionally expose packages with no launcher
entry — background services, libraries, carrier components — none of which DarkFrame can theme,
because they have no icon on the home screen. There is no app DarkFrame could theme with it that it
cannot theme without it.

**Google Play restricts it.** The permission is governed by Play's sensitive-permissions policy and
is approved only for a narrow set of declared use cases (antivirus, device management, banking
fraud prevention, file managers, and a few others). Icon customisation is not among them. Declaring
it would therefore put the listing at risk in exchange for no capability.

The same reasoning applies to the broad-visibility alternatives: DarkFrame does not request
`ACCESS_SUPERUSER`, does not look for root, and does not use `LauncherApps` cross-profile APIs that
are reserved for the active home launcher.

## Documented gaps, and why they exist

A narrow declaration means the catalog is "every app Android is willing to tell us about". On a
normal device that is the user's whole app drawer. It is not literally every installed package, and
the product does not imply otherwise — the guided setup screen states this to the user directly.

| Not visible | Reason | Could DarkFrame fix it? |
| --- | --- | --- |
| Apps with no launcher activity | They have no launcher icon to theme | Nothing to fix; out of scope by definition |
| Work-profile and Secure Folder apps | Enumerating another profile requires being the active home launcher (`LauncherApps` with a cross-profile user handle) | No. Not available to a non-launcher app at any permission level |
| Packages hidden by device policy | Enterprise/MDM suppression | No, and should not be worked around |
| Instant apps | Not installed | Not applicable |

`AppIdentity` carries an `isWorkProfile` flag and the catalog builder keeps work-profile entries
distinct from their personal-profile twins, so the model is already correct for the day a launcher
integration makes those entries reachable. It is deliberately not presented as working today.

## Behaviour on Android 10 and below

`<queries>` is ignored on API < 30, where `queryIntentActivities` already returns all launcher
activities. The same code path serves both; `minSdk` is 26.

## Play Store declarations this implies

- **Sensitive permissions**: none to declare. No `QUERY_ALL_PACKAGES`, no location, no contacts,
  no storage permission (icon export writes to our own cache and shares a scoped `FileProvider`
  URI).
- **Data safety**: DarkFrame reads installed-app metadata on-device to render icons. Nothing is
  transmitted; there is no network code in the engine at all.
