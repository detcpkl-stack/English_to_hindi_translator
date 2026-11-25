# English Hindi Voice Translator (Android / Java)

Android 12+ voice translator with:
- English speech input -> Hindi translation -> Hindi speech output
- Hindi speech input -> English translation -> English speech output
- Typed text translation
- Swap direction, replay output
- On-device ML Kit translation models after initial download

## Build APK using GitHub Actions (no Android Studio required)

1. Create a GitHub repository named `EnglishHindiTranslator`.
2. Upload all files and folders from this project to the repository root. Keep `.github/workflows/android.yml` in that exact location.
3. Commit to `main` (or `master`) or open **Actions** and run **Build Android APK** using **Run workflow**.
4. Open the successful workflow run. Under **Artifacts**, download `English-Hindi-Translator-APK`.
5. Extract the downloaded artifact ZIP. It contains `app-debug.apk`.
6. Transfer the APK to your Android phone and install it. You may need to allow installation from that file manager.

## Important usage notes

- Allow microphone permission when prompted.
- Speech recognition uses the Android device's speech recognition service; availability and offline support depend on the device and installed speech services.
- The first translation for each language direction downloads the ML Kit language model. Internet is required for that initial download; subsequent translations can run on-device.
- Android Text-to-Speech voices depend on the voice data installed on the phone. If a voice is unavailable, install Hindi and English voice data in Android Settings > Text-to-speech.
- The app is configured for Android 6.0+ technically (minSdk 23), and is suitable for Android 12+ devices.

## Project settings

- Java 17
- Android Gradle Plugin 8.7.3
- Gradle 8.9
- compileSdk 35 / targetSdk 35
- ML Kit Translate 17.0.3
