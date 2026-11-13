# GlyphDraft

AI-powered media translator: give it an image, get the text in it, in another language if you want. Runs on-device. For Windows, Linux and Android.

**Status: planning.** Nothing is built yet; see [docs/status.md](docs/status.md) and [docs/plan.md](docs/plan.md).

## Planned features (in order)

1. Text from an image (OCR); the language is detected if you do not give it.
2. Translation into a language you choose.
3. Handwritten text.
4. Audio to text.
5. Android: point the camera at text and read it.

## Build

Needs JDK 21 and, for Android, the Android SDK (Android Studio installs it). Then:

```powershell
.\scripts\build.ps1                  # licenses, build, test, package, checksums, smoke test (this OS)
.\scripts\build.ps1 -Target android  # APK
```

Packages are written to `dist/`. The app, Gradle wrapper and tests arrive in milestone M0.

## Releases

Releases are not signed. Windows SmartScreen will warn ("More info", then "Run anyway"); Android asks to allow installs from your browser or file manager. Check downloads against `SHA256SUMS.txt`.

## License

[Apache-2.0](LICENSE). Third-party models downloaded by the app have their own licenses.
