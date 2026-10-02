# Privacy

DarkFrame is an appearance app. It does not have accounts, analytics, crash reporting, advertising
or a backend.

## What it reads

| Data | Why | Where it goes |
| --- | --- | --- |
| The list of apps that have a launcher icon, with their labels and icons | To theme them. This is the product | Stays on the device |
| Which launcher is your home screen, and whether Theme Park / Good Lock are installed | To offer the apply step that exists on your device | Stays on the device |
| Your chosen look, favourites and onboarding flag | To remember what you picked | Device, plus Android's own backup if you have it on |
| Rendered icons and wallpaper thumbnails | Cache, so the app is not redrawing constantly | Device cache directory; excluded from backup because they are specific to this device's apps |

## What it does not do

- **No network code**, except Google Play Billing for the one-time Pro purchase. Billing talks to
  Google Play, not to us; we never see a payment detail and there is no server to receive one.
- **No background service, job or alarm.** Nothing runs when the app is closed. The widgets are
  drawn by the launcher in its own process and redraw only on real system events.
- **No `QUERY_ALL_PACKAGES`.** DarkFrame declares the narrowest package visibility that answers its
  question — see `PACKAGE_VISIBILITY.md`.
- **No location, contacts, camera, microphone, storage or identifier access.**

## Permissions in the manifest

| Permission | Used for |
| --- | --- |
| `SET_WALLPAPER` | Setting a DarkFrame wallpaper when you ask it to |

That is the whole list.

## Play Data Safety answers

- **Does your app collect or share any of the required user data types?** No.
- **Is all user data encrypted in transit?** Not applicable — no user data is transmitted.
- **Do you provide a way for users to request data deletion?** Not applicable — no data leaves the
  device. Uninstalling removes everything, and Settings → Rebuild icon cache clears the cache at any
  time.
- **Installed apps**: read on device, not collected and not transmitted. Declare accordingly if the
  console asks about the app-list permission group.
