# Android release signing

Why and what was decided: `docs/decisions/0002-android-identity-and-signing.md`. This file is the how-to. Only the owner runs these steps; the key never goes into the repo or into a folder Claude works in.

## How the build picks the key

`androidApp/build.gradle.kts` reads four environment variables: `GLYPHDRAFT_KEYSTORE_FILE`, `GLYPHDRAFT_KEYSTORE_PASSWORD`, `GLYPHDRAFT_KEY_ALIAS`, `GLYPHDRAFT_KEY_PASSWORD`. If all four are set, `assembleRelease` signs with that key. If any is missing, it signs with the debug key and says so in the Gradle output.

In CI (`.github/workflows/ci.yml`, job `build-android`), a run for a `v*` tag or a manual run (`workflow_dispatch`, the release dry run) decodes the keystore from the secrets below, sets the variables, and fails if a secret is missing. After the build it checks the APK with `apksigner verify --print-certs` and fails if the certificate is the debug one. Pushes and pull requests build with the debug key and never see the secrets.

## One-time setup

1. Make a folder for the key outside the repository and outside any folder Claude works in, for example `C:\Users\Admin\GlyphDraftKeys`, and generate the keystore (JDK 21 `keytool`; it asks for a password and keeps the key password equal to it for PKCS12):

   ```powershell
   $keys = "$env:USERPROFILE\GlyphDraftKeys"
   New-Item -ItemType Directory -Force $keys | Out-Null
   keytool -genkeypair -v -keystore "$keys\glyphdraft-release.jks" -alias glyphdraft -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=GlyphDraft"
   ```

2. **Back up** `glyphdraft-release.jks` and the password in your password manager (and a second place). If the key is lost, installed users must uninstall and reinstall, because Android refuses an update signed with a different key.

3. Write the fingerprint down; it is what the checks below compare with:

   ```powershell
   keytool -list -v -keystore "$keys\glyphdraft-release.jks" -alias glyphdraft | Select-String "SHA256"
   ```

4. Give CI the four secrets. Run these in your own terminal (they prompt for the values; the base64 line reads the file and sends it through the pipe):

   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("$keys\glyphdraft-release.jks")) | gh secret set ANDROID_KEYSTORE_BASE64 --repo shaulk80/GlyphDraft
   gh secret set ANDROID_KEYSTORE_PASSWORD --repo shaulk80/GlyphDraft
   gh secret set ANDROID_KEY_ALIAS --repo shaulk80/GlyphDraft      # glyphdraft
   gh secret set ANDROID_KEY_PASSWORD --repo shaulk80/GlyphDraft   # the same password as the keystore
   ```

## Checks

- Dry run: `gh workflow run release.yml --ref main`. The log of "Check the APK signature" shows the certificate; its SHA-256 must equal the one from step 3. The `release-preview` artifact holds the signed APK.
- On a device: `apksigner verify --print-certs GlyphDraft-<version>-android.apk` (in `build-tools` of the Android SDK) prints the same fingerprint.

## Building a signed APK locally (rarely needed)

```powershell
$env:GLYPHDRAFT_KEYSTORE_FILE = "$keys\glyphdraft-release.jks"
$env:GLYPHDRAFT_KEYSTORE_PASSWORD = Read-Host "Keystore password"
$env:GLYPHDRAFT_KEY_ALIAS = "glyphdraft"
$env:GLYPHDRAFT_KEY_PASSWORD = $env:GLYPHDRAFT_KEYSTORE_PASSWORD
.\scripts\build.ps1 -Target android
```

## Upgrading from v0.0.1

The v0.0.1 APK was signed with the debug key, so the first release with the real key (v0.1.0) cannot install over it: uninstall v0.0.1 first. The release notes of v0.1.0 say so.
