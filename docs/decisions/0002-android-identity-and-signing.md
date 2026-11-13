# 0002: Android identity and signing

Date: 2026-11-13. Status: M0 part accepted; release-key part proposed (owner to confirm before v0.1.0).

- **applicationId and namespace: `com.glyphdraft.app`.** Chosen without owning a matching domain; it only has to be unique and stable. It cannot change once users have installed the app, and a Play Store listing would fix it for good. Change it now if you prefer another (for example `io.github.<owner>.glyphdraft`).
- **M0 (v0.0.1): the release APK is signed with the debug key** (`androidApp/build.gradle.kts`), so it installs for testing. It is not a real release key: a debug-keystore APK cannot be updated by an APK signed with a different key.
- **Proposal for v0.1.0 and later:** generate one release keystore locally (`keytool`), keep the original and its password outside the repo and outside any folder Claude works in (as with the GitHub recovery codes), and give CI the keystore as base64 plus passwords in GitHub Actions secrets. The release workflow signs only on `v*` tags. If the key is lost, installed users must uninstall and reinstall.
- Windows and Linux stay unsigned (decision 0001).
