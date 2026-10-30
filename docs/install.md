# Install and set up

This is the Spain/Madrid fork. It uses its own signing key and must not overwrite an
upstream-signed installation by uninstalling it without the owner's consent. Keep signing
material private and stable for future fork updates. Build/check requirements are in
[.agents/validation.md](../.agents/validation.md).

## Requirements

- An Android TV box on Android 7.0+ (tested on a Xiaomi Mi Box, `MIBOX4`, Android 12).
- Developer options and **network debugging** turned on (Settings → Device Preferences → About →
  click *Build* seven times).
- A computer with `adb` (Android SDK platform-tools) on the same network.

## Build

```sh
./build.sh                        # uses JDK 17 and Android build-tools 34
VERSION_CODE=62 VERSION_NAME=6.9 ./build.sh
```

The script compiles resources (`aapt2`), Java (`javac`), dexes (`d8`), aligns and signs. Development
builds use a local debug key; official releases are signed by the maintainers with a private key.
Android only installs updates signed with the same key, so switching between a release and a
development build needs an uninstall first.

## Install and make it the home screen

```sh
TV=192.168.1.13:5555
adb connect $TV
adb -s $TV install -r build/tvos-home.apk

adb -s $TV shell pm set-home-activity com.petrobot.hume/.MainActivity
```

Press Home on the remote — Hume Launcher opens.

**TVs that keep their own launcher in front (Google TV, many TCL/Sony/Philips):** no need to disable
it. Turn on **Hume's switch** (setup offers it, or Settings → Home → *Home button opens Hume*, or
the TV's Settings → Accessibility → *Hume Launcher*). Whenever the TV's own home screen appears,
Hume opens instead. The stock launcher stays installed, so the TV's quick settings, account
screens and updates keep working. This fork's installation keeps Google TV Home and the setup
wizard enabled; no debloating, DNS changes or system-package disabling is required.

For wireless debugging, pair using the pairing address/port shown on the TV, then connect using
its separate connection address/port. Pairing codes and connection ports are temporary; do not
publish them. Turn debugging off after setup/verification if no longer needed.

## One-time permissions

Hume asks for these **on screen** (setup's last page and Settings → Permissions): Android's prompts,
the system's own settings screens, or Hume's accessibility switch. The commands below are only a
last resort for TVs that have no such screen.

| Feature | Command |
|---|---|
| Home button + Start with the TV (Hume's switch) | `adb shell settings put secure enabled_accessibility_services com.petrobot.hume/com.petrobot.hume.PowerService` then `adb shell settings put secure accessibility_enabled 1` (replaces any other enabled accessibility services) |
| Continue Watching (apps' Watch Next lists) | `adb shell pm grant com.petrobot.hume android.permission.READ_TV_LISTINGS` |
| Do Not Disturb | `adb shell cmd notification allow_listener com.petrobot.hume/com.petrobot.hume.MediaWatchService` |
| Power Off + Sleep Timer | `adb shell dpm set-active-admin com.petrobot.hume/.SleepReceiver\$Admin` |
| Wi-Fi name in Control Center | `adb shell pm grant com.petrobot.hume android.permission.ACCESS_FINE_LOCATION` |
| Search dictation (microphone) | `adb shell pm grant com.petrobot.hume android.permission.RECORD_AUDIO` — not normally needed: without it, Search uses the TV's own voice prompt |

## Optional: recommendation source

Recommendations use JustWatch by default (no setup). **Settings → Top Shelf → Source** also offers
**Stremio** (Cinemeta's popular movies and series, no setup; titles open in the Stremio app, or in
Nuvio when only Nuvio is installed) and **TMDB**. For TMDB, enter a free API key from themoviedb.org (Settings → API). The short v3 *API Key* is
easiest to type with a remote. It can also be set from a computer:

```sh
adb shell am start -n com.petrobot.hume/.SettingsActivity --es tmdb_token <token>
```

## Optional: AirPlay receiver

To AirPlay from a Mac, iPhone or iPad to the TV, install the open-source
[AirPlay Server](https://github.com/jqssun/android-airplay-server) app (GPL-3.0; a separate app, not
part of Hume). Download `app-release.apk` from its
[latest release](https://github.com/jqssun/android-airplay-server/releases/latest) and install it:

```sh
adb install -r app-release.apk
```

Open **Settings → AirPlay receiver** in Hume (or the receiver's app tile), choose its name and
approve its own permission prompts. Configure start-on-boot/background operation in that app.
The fork targets the separate official 0.0.36 APK; verify its published digest/signature and ABI.
No GPL receiver code is bundled into Apache-licensed Hume.

Hume does not request DUMP or silently restart/reconfigure the receiver: 0.0.36 protects those
settings broadcasts. Test real mirroring/audio/video from an Apple client, including disconnect,
wake and reboot. Advertisement alone is not playback proof; DRM compatibility is not promised.

## Undo

Turn off Hume Madrid in the TV's Accessibility settings to stop its Home override. Google TV
Home remains installed/enabled. If desired, uninstall Hume Madrid through the ordinary app
settings; this deletes its local settings. The separate AirPlay receiver can be removed separately.
Never remove a replacement Home while the stock Home is disabled. This fork does not disable it.
