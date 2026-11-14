# 0004: File dialogs on desktop with FileKit

Date: 2026-11-14. Status: accepted by the owner on 2026-11-14.

- **Problem.** The first manual test (M1) showed that AWT `FileDialog` gives the old Windows dialog: no pasted paths, and it forgets the last folder.
- **Decision: FileKit `io.github.vinceglb:filekit-dialogs` 0.17.0 in `desktopApp` only**, behind the existing `FileChooser` interface (`FileKitFileChooser`). It shows the native Windows dialog, uses the XDG desktop portal on Linux and falls back to AWT when no portal runs. No Compose artifact is used.
- **Fits the stack.** Built with Kotlin 2.4.21 and coroutines 1.11.0, the same as this project. Pulls in JNA and dbus-java.
- **License.** MIT (also dbus-java and slf4j-api, which it brings). "MIT License" was added to `config/allowed-licenses.json` after review: permissive, no copyleft.
- **Packaging.** The jlink runtime needs `java.instrument`, `jdk.security.auth`, `jdk.unsupported` (from `:desktopApp:suggestRuntimeModules`), set in `desktopApp/build.gradle.kts`. The Windows package was built and started (smoke test); the Linux package and the Linux dialogs are verified by CI only.
- **Last folder.** FileKit does not remember it, so `LastFolder` stores it in `<data>/last-folder.txt` and passes it as the start folder.
- **Not decided here.** Android keeps the system pickers (`ActivityResultContracts`, task 6). FileKit also supports Android and could be used there later; that would be a new decision.
