# DualMode Lock — Milestone 1

A native Android Kotlin + Jetpack Compose prototype. It has:
- First-run setup for a main PIN and a separate decoy PIN
- Salted PBKDF2-HMAC-SHA256 PIN verifiers (raw PINs are not stored)
- Main PIN launches the official WhatsApp app (or its download page if missing)
- Decoy PIN opens fictional, local sample conversations
- Arbitrary incorrect PINs do not unlock either environment

## Requirements
- Android Studio Ladybug or newer
- JDK 17
- Android SDK Platform 35 installed
- Android phone or emulator running Android 8.0 (API 26) or newer

## Open and run
1. Extract `DualModeLock.zip`.
2. In Android Studio, choose **Open** and select the extracted `DualModeLock` folder.
3. Allow Gradle sync to finish. If prompted, install Android SDK Platform 35.
4. Connect an Android phone with USB debugging enabled, or create an emulator.
5. Press **Run** for the `app` configuration.

To build a debug APK: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
The APK is normally produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Manual test checklist
1. First launch shows setup.
2. Enter a 4–8 digit main PIN twice and a different 4–8 digit decoy PIN twice.
3. Save; app returns to the lock screen.
4. Enter a random wrong PIN: neither environment opens and an error appears.
5. Enter the decoy PIN: the sample chat list opens.
6. Open a conversation and press Back; chat list returns.
7. Press the lock icon to return to the lock screen.
8. Enter the main PIN: installed WhatsApp or WhatsApp Business opens. If neither is installed, a download page opens.
9. Force-stop and reopen the app: PIN setup should not repeat.
10. Uninstalling the app removes its local PIN data.

## Important limitations
- This is an app-level launcher, not a system-wide lock. It cannot prevent WhatsApp from being opened through its own icon, notifications, share sheets, or other entry points.
- The contact picker and importing selected real contacts are not implemented in Milestone 1. The sample names and chats are fictional. Contact import is the next milestone.
- Decoy messages are read-only and are not sent to anyone.
- PIN hashing is a useful prototype baseline, not a complete production security design. Add throttling, secure settings storage, security review, and careful lifecycle handling before real-world use.
- Android does not let an ordinary app read WhatsApp's private message database.

## Reset
Use **Reset / change PINs** on the lock screen to configure new PINs. In this prototype, saving new PINs overwrites the old verifier hashes.
