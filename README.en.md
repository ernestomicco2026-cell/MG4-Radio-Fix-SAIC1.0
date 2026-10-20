# MG4 Radio Fix — SAIC 1.0 (EH32 EU)

🇬🇧 English · [🇮🇹 Italiano](README.md)

Fixes for the **original radio service** of the MG4 infotainment with the **SAIC 1.0** system
(Comfort, Luxury, Trophy, XPower trims).

> ⚠️ **Not for "Color Radio" versions** and not for the **EH32 LL** firmware
> (where the radio is a single app, `com.saicmotor.radio`: for that one use
> [MG4-Radio-DABplus-Fix](https://github.com/Skittle6938/MG4-Radio-DABplus-Fix)).

> 🛑 **The ready-made APK is ONLY for infotainment SWI68-29958-1300R69** (radio service
> `com.saicmotor.service.radio` version **`1.0.0_EH32_SOP_2502201114`**). You can find your version in
> the car's system information.
> It has **only been tested on an MG4 Luxury**. On other releases (R59, R63, R67, R71…) **do not install it**:
> build the APK for your own version with the [build script](#building-the-apk-for-your-version).
> If you already installed it by mistake, see [Going back to the original](#going-back-to-the-original):
> it can be removed **without a computer**.

---

## What it fixes

| Problem | Cause in the original firmware | Fix |
|---|---|---|
| **FM stuck on a single station** (e.g. stays on 90.90 and next/previous do nothing) | Every received frequency, even outside a search (e.g. after a seek), is put into a list that **replaces** the whole station list 3 s later | The list is only updated during a real scan |
| **FM not changing frequency when the signal fades** (v1.1) | When reception gets weak the tuner switches by itself to another frequency of the same station (AF/RDS), but the service stays on the old one | The service follows the frequency change (requires **AF enabled** in the radio settings) |
| **Radio doesn't resume when the car starts** | When the car turns off, the radio loses audio focus and saves a "paused" state | If you were listening to the radio, it resumes at start-up; if you had paused it yourself or were using another source (CarPlay, Bluetooth…), it stays off |
| **DAB logos not showing** | Bug in the logo/station matching loop (`size() - 1`): the last received station is never checked | Loop fixed |
| **DAB logos discarded** | Only perfectly square `logo_unrestricted` logos are accepted | Rectangular logos are accepted too and made square (uniform borders cropped, or centred) |
| **No DAB+ images on the home screen** | The slideshow only goes to the radio app | Slideshow images are also sent to the home screen |
| **Waiting for the image at every station change** | No slideshow memory | The last image of each station is saved and shown immediately |
| **Memory growing over time** | Slideshow, logos and station info pile up without limit | Last 4 images kept, lists de-duplicated and capped |

**The graphics don't change**: the app with the radio screen (`com.saicmotor.hmi.radio`) stays the factory one.
**Reception doesn't improve** (weak signal, unstable DAB): that depends on the tuner and its firmware.

---

## Compatibility

| Firmware | Status |
|---|---|
| SAIC 1.0, SWI68-29958-1300**R69**, service `1.0.0_EH32_SOP_2502201114` | ✅ [Ready-made APK in Releases](https://github.com/ernestomicco2026-cell/MG4-Radio-Fix-SAIC1.0/releases/latest), **tested on MG4 Luxury** (Comfort, Trophy and XPower not tested yet) |
| SAIC 1.0, other releases (R59, R63, R67, R71…) | ⚠️ **Do not use the ready-made APK.** Use the **build script** on your own APK: it checks by itself whether the code is compatible |
| EH32 LL (`com.saicmotor.radio`) | ❌ Different firmware → [MG4-Radio-DABplus-Fix](https://github.com/Skittle6938/MG4-Radio-DABplus-Fix) |
| Color Radio | ❌ Not supported |

Check the version of your radio service:

```bash
adb shell dumpsys package com.saicmotor.service.radio | grep versionName
```

If it is **`1.0.0_EH32_SOP_2502201114`** you can use the ready-made APK: **[download it here](https://github.com/ernestomicco2026-cell/MG4-Radio-Fix-SAIC1.0/releases/latest)** (SHA-256 listed in the release). Otherwise build your own (below).

---

## Installation

**No computer needed.** A USB stick is enough: the APK is installed from the car's file manager,
like a normal app.

### Why it can be installed from a USB stick

The radio service is a system app that is already on the car. This APK has **the same package name**
(`com.saicmotor.service.radio`) and **the same digital signature** as the original. That's why Android
doesn't treat it as a new app but as an **update** of the existing one, exactly as it would with an
official update.

In practice:
- **no** new icon appears: the radio service runs in the background and is used by the usual Radio app;
- the graphics **don't** change: the radio screen stays the factory one;
- **no** root needed, and the system partition is **not** touched;
- the original stays intact in `/system` and can be restored at any time.

### Before you start: check the version

The ready-made APK is built **only** for radio service **`1.0.0_EH32_SOP_2502201114`**, found on
infotainment **SWI68-29958-1300R69** (you can see it in the car's system information), and it has been
tested on an **MG4 Luxury**.
**If your version is not R69, do not install it**: the radio may stop working.
Build the APK for your version instead (see [below](#building-the-apk-for-your-version)).

### Option A — From a USB stick (recommended)

1. Download **[RadioService_radiofix2_1.0.0_EH32_SOP_2502201114.apk](https://github.com/ernestomicco2026-cell/MG4-Radio-Fix-SAIC1.0/releases/latest)** from the **Assets** section of the latest release.
2. Copy it to a USB stick formatted as **FAT32** or **exFAT** (the root folder is fine).
3. With the car on, plug the stick into the USB port.
4. Open the infotainment **file manager** and go to the USB stick.
5. Tap the APK: you'll be asked to **update** an existing app. Confirm with **Install**.
6. When it's done, **restart the unit**: hold the **Home button for about 20 seconds** until the screen
   turns off and on again.
7. Open the radio and **run a new FM scan**, so the station list is complete again.

> ⚠️ **The restart is mandatory.** The radio service is always in memory: until you restart, the old
> version keeps running and it looks like the update "did nothing".

### Option B — With ADB (from a computer)

You need ADB enabled on the unit (for the MG4: [ADB_util by Leon Kerman, XDA thread](https://xdaforums.com/t/mg4-electric-aaos-9-playing-and-possibly-other-mg-models.4697712/post-90591053)).

```bash
adb install -r RadioService_radiofix2_1.0.0_EH32_SOP_2502201114.apk
adb reboot
```

With ADB you can also check the exact service version before installing:

```bash
adb shell dumpsys package com.saicmotor.service.radio | grep versionName
```

### If something goes wrong

| Message / situation | What it means |
|---|---|
| **"App not installed"** or **"conflicts with an existing package"** | Your car's radio service is signed with a different key or is from a different firmware (e.g. LL or Color Radio). This APK is not for your car. |
| The file manager **won't open** APKs | On some versions installing from the file manager is blocked: use Option B. |
| **Nothing changes** after installing | You didn't restart the unit (step 6). |
| **The radio stops working** after installing | Your version is probably not R69. Remove the patch: see [Going back to the original](#going-back-to-the-original). |
| The FM list is **empty or incomplete** | Run a new FM scan from the Radio app (step 7). |

### Going back to the original

You also need this if you installed the APK on the wrong version (not R69) and the radio no longer works.
The factory original is still intact in `/system`: you just need to remove the update and restart.

**1. From Settings (no computer):** car Settings → app list → show system apps → radio service
(`com.saicmotor.service.radio`, sometimes "RadioService") → ⋮ menu → **Uninstall updates**, then
restart. Not every firmware version shows this option: if yours doesn't, use option 2.

**2. With Radio Fix Remover, from a USB stick (no computer, no ADB):**

1. Download **`RadioFixRemover_1.0.apk`** from the **Assets** section of the [latest release](https://github.com/ernestomicco2026-cell/MG4-Radio-Fix-SAIC1.0/releases/latest)
   and copy it to a **FAT32** or **exFAT** USB stick.
2. Plug in the stick, open the car's **file manager**, tap the APK and confirm **Install**
   (same as for the patch).
3. Open **Radio Fix Remover** and tap **Remove Radio Fix**. If Android asks
   *"Replace this app with the factory version?"*, confirm.
4. When the screen says the factory radio service is active, tap **Restart head unit**
   (or hold Home for about 20 seconds).
5. After the restart, open it again and tap **Uninstall this helper app** to remove it.

The app is signed with the same system key as the patch, which is why it can remove the update just like
`adb uninstall`. It works on any SAIC 1.0 version with an AOSP-signed radio service, not just R69.
The code is in [`remover/`](remover/).

**3. With ADB:**

```bash
adb uninstall com.saicmotor.service.radio
adb reboot
```

In every case the factory version comes back automatically, since it is still intact in `/system`.

**Last resort:** a factory reset of the infotainment also removes the patch, but it erases settings,
profiles, paired phones and any other installed apps.

> ⚠️ Don't install an original APK taken from another car over it: if its version differs from yours,
> you'd again have a radio service that doesn't match your system.

---

## Building the APK for your version

Requirements (Linux/macOS/WSL): Java 11+, git, curl, unzip, zip, python3, openssl.

```bash
# 1. pull the original radio service from your car
adb shell pm path com.saicmotor.service.radio
adb pull /system/priv-app/<folder>/<file>.apk RadioService_original.apk   # use the path shown above

# 2. build
cd patcher
./build.sh ../RadioService_original.apk
```

The script downloads the tools it needs, then **stops without producing anything** if:
- the APK is not the EH32 radio service (`com.saicmotor.service.radio`);
- the original is not signed with the AOSP platform key (the update would not install);
- your firmware's code differs from what's expected (one of the patch points can't be found).

If it stops at the third point, open an *issue* attaching your original APK and your SWI68 version.

---

## Checking that it works

```bash
adb logcat -s RadioFix
```

Typical messages: `scan start` / `scan over` (FM scan), `ignored program info outside scan`
(a station that would previously have "locked" the list), `AF follow` (frequency change followed), `auto resume`, `restored art`.

---

## How it works (technical)

The original service is decompiled with apktool. A call to a new Java class,
[`RadioFix`](patcher/src/com/saicmotor/service/radio/fix/RadioFix.java), is inserted at 14 points of the smali
code; the class is added to the APK as a separate dex. Every `RadioFix` method is wrapped in `try/catch`:
on error it falls back to the original behaviour.

| Point (smali) | Hook |
|---|---|
| `RadioService$6.onProgramInfoChanged` | `shouldCollect()` — ignores stations outside a scan |
| `RadioTunerExt.scan` / `RadioService.amOrFmScanOver` | `onScanStart()` / `onScanOver()` |
| `RadioService$7.onDabServiceLogoChanged` | loop `i < size()` instead of `size() - 1` |
| `RadioService$7.onDabServiceInformationListChanged` | `acceptLogo()` |
| `BitmapFactory.decodeByteArray` (logos) | `decodeLogo()` — square image |
| `List.add` on `mLogoList` / `mServiceInformation` | `addCapped()` |
| `RadioService$7.onDabSlideShowChanged` | `onSlide()` — limit, home screen, per-station memory |
| `RadioService$5.onCurrentProgramChange` | `onStationChanged()` — saved image |
| `AudioController.onAudioFocusChange` | `onFocusChange()` |
| `RadioService.lambda$initRadioListener$0` | `onProgram()` — remembers whether you were listening |
| `LastMode.tuneDefault` | `resumeState()` |
| `RadioService.onCreate` | `scheduleBootResume()` |

The APK is signed with the **public AOSP platform key** (`test-key`), the same one SAIC used
for this firmware (SHA-256 fingerprint `C8:A2:E9:BC:…:2A:B8`).

---

## How releases are made

Releases are **built and signed automatically by GitHub Actions**
([`.github/workflows/release.yml`](.github/workflows/release.yml)) from the code in this repository:
every published APK matches exactly the sources you see here.

Building requires the original radio service APK. The repository only contains it **encrypted**
(`firmware/*.apk.enc`, AES-256): it is decrypted only during the build, with a key kept in the repository's
private *Secrets*, and is never published in clear.

---

## Disclaimer

Unofficial project, not affiliated with SAIC Motor or MG. You modify your car's software at your own risk.
The original radio service code belongs to SAIC Motor. The source code in this repository contains only the
changes (scripts, hooks, `RadioFix` class, MIT licence); the APK in Releases includes the modified SAIC service.

Thanks to [Skittle6938](https://github.com/Skittle6938/MG4-Radio-DABplus-Fix) for the idea of DAB+ artwork on the
home screen and of automatic resume (LL firmware).
