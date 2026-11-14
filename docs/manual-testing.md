# GlyphDraft: manual testing

Steps for the owner to click through the app. Automated tests cover the logic (`.\gradlew jvmTest :desktopApp:test testAndroidHostTest`); this file covers what only a person can judge: the real file dialogs, the look and feel, the layout at different sizes.

Status of this file: written during M1 after task 5 (desktop file dialogs). Android still uses placeholder picker and saver until task 6; the notes in each section say what to expect. Update the "Known gaps" lists as tasks land.

While reviewing, write down every change you want (wording, layout, flow, missing screen). M1 is done when each one is done or filed as a GitHub issue.

## Before you start

Temporary files for manual testing live under **`C:\Vibe\z_tmp\GlyphDraftTests`** (decision 0003), outside the repo, so nothing from testing can end up in a commit. This applies to manual testing only; do not use `%TEMP%` for it.

- **Run commands from the repo root** (`C:\Vibe\GlyphDraft`): `gradlew` and the relative paths below need it.
- **Keep test files in the `files` folder**, never in the repo. Pick files from there in the dialogs and save "Save as…" output there too.
- Write your results next to them, for example `C:\Vibe\z_tmp\GlyphDraftTests\m1.txt`.

```powershell
$root = "C:\Vibe\z_tmp\GlyphDraftTests"

# data folder for the app (keeps the test run away from your real data)
$env:GLYPHDRAFT_DATA_DIR = "$root\data"

# test files
$files = "$root\files"
New-Item -ItemType Directory $files -Force | Out-Null
New-Item "$files\empty.png" -Force | Out-Null                     # empty file
fsutil file createnew "$files\big.png" 62914560 | Out-Null         # 60 MB of zeros (optional); needs an admin shell, skip if it fails
Copy-Item "C:\path\to\any\photo.jpg" "$files\photo.jpg"            # any PNG or JPEG you have
```

The environment variable lasts for that PowerShell window only; set it again in a new window. If you created a test file in the repo by mistake, delete it (`git status` shows stray files as untracked).

## Desktop (Windows, Linux)

Start: `.\gradlew :desktopApp:run` (first start compiles, about a minute).

### 1. Home screen

| Do | Expect |
|---|---|
| Look at the window after start | Title "GlyphDraft", "Settings" top right; card "1. Image" says "No image chosen yet."; language shows "Auto-detect"; "Recognize text" is greyed out |
| Check `%GLYPHDRAFT_DATA_DIR%\logs\glyphdraft.log` | One line "GlyphDraft dev starting" |

### 2. Choose image (real file dialog)

| Do | Expect |
|---|---|
| Click "Choose image…", then Cancel | Nothing changes, no message |
| Click it again, pick your photo | Card shows file name and size in bytes; button now says "Choose another image…"; "Recognize text" is enabled |
| Click "Choose another image…", pick a different file | The name changes |
| Pick `empty.png` from the test-files folder, then "Recognize text" | Red banner "The image 'empty.png' is empty."; you stay on Home; "Dismiss" removes the banner |
| Pick `big.png` (60 MB; the limit is 50 MB, a file of exactly 50 MB is still accepted), if you made it | Red banner: file too large, with the limit (the file is not read) |

Note: on Windows the dialog shows all files, not only images (the extension filter is ignored by the Windows dialog).

### 3. Language and recognize

| Do | Expect |
|---|---|
| Open the language dropdown | "Auto-detect" first, then English, Russian, Hebrew, German, French, Spanish, Chinese, Japanese |
| Leave "Auto-detect", click "Recognize text" | Spinner and "Cancel" for under a second, then the Result screen: "Language: English (detected)" and fixed sample text naming your file |
| Back, choose "Russian", recognize again | "Language: Russian (chosen)" |
| Click "Recognize text" and quickly "Cancel" | Back to idle on Home, no result screen. The stub only takes 0.8 s, so you may need a few tries |
| While the spinner shows, look at "Choose another image…", the dropdown, "Settings" | All disabled |

### 4. Result screen

| Do | Expect |
|---|---|
| Select text with the mouse | It highlights and can be copied with Ctrl+C |
| Click "Copy", paste into Notepad | The full text is pasted; a blue banner "Copied to the clipboard." |
| Click "Save as…" | Save dialog with the suggested name `<image name>.txt` |
| Save it, open the file | Content equals the shown text; banner "Saved." |
| Click "Save as…" and cancel | No banner, nothing written |
| Click "Save as…" and save into a protected folder such as `C:\Windows` (Windows may refuse or ask for admin rights), or over a file that another program has open and locked | Red banner "Cannot save …", and a line with `ERROR Cannot write` in the log file |
| Click "← Back" | Home, with your image and language still chosen |

### 5. Settings

| Do | Expect |
|---|---|
| Click "Settings" | Theme (System, Light, Dark), Default language, Engine ("Stub …"), version line ("GlyphDraft dev" when started with gradle; the packaged app shows the real version, Android shows it too) |
| Choose Dark, then Light | The whole window switches at once; "System" follows Windows |
| Set Default language to "Hebrew", go Back | Home dropdown shows Hebrew |
| Restart the app | Settings are back to defaults. This is expected in M1 (they live in memory; persistence comes with M2) |

### 6. Window size

| Do | Expect |
|---|---|
| Drag the window narrower than about 600 px | The three steps stack in one column |
| Maximise the window | Content stays centred, at most 720 px wide; nothing stretches across the screen |
| Make it very small | Content scrolls; nothing is cut off with no way to reach it |

### Known gaps (desktop)

- No image preview, only file name and size.
- Nothing is remembered between runs.
- Fixed sample text: the stub engine ignores the picture.

## Android

Needs the emulator or a real device with USB debugging. Paths below are for this machine.

```powershell
$sdk = "C:\Users\Admin\AppData\Local\Android\Sdk"
# emulator (leave it running; remove -no-window to see the screen):
& "$sdk\emulator\emulator.exe" -avd Medium_Phone_API_37.0 -no-audio
# in another terminal:
.\gradlew :androidApp:assembleDebug
& "$sdk\platform-tools\adb.exe" install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk
& "$sdk\platform-tools\adb.exe" shell am start -n com.glyphdraft.app/.MainActivity
```

### Checks

| Do | Expect |
|---|---|
| Start the app | Status bar clock and icons are dark on the light background (this was a bug in v0.0.1) |
| Switch Settings → Theme → Dark | Status bar icons turn light |
| Tap "Choose image…" | **Placeholder for now:** it picks a fake `sample.png` without opening any picker (real system picker arrives in task 6) |
| Tap "Recognize text" | Result screen with the language line and sample text |
| Tap "Copy" | Banner "Copied to the clipboard."; Android shows its clipboard preview |
| Tap "Save as…" | **Placeholder for now:** red banner "Saving is not wired yet." |
| Press the system Back button on Result or Settings | Currently exits the app; the on-screen "← Back" is the way back. Tell me if the system Back should also navigate (likely a change request) |
| Rotate the device | **Known gap until task 6:** state is lost and the app returns to the empty Home screen |
| Check the top and bottom edges | Content is not hidden under the status bar or the gesture bar |
| Try a small and a large phone, and a tablet if you have one | Layout stays readable; the tablet shows the two-column Home |

### Known gaps (Android)

- Picker and saver are placeholders until task 6.
- State does not survive rotation until task 6.
- Not tried on a real device yet; the emulator is API 37.

## When something looks wrong

- Desktop log: `<data folder>\logs\glyphdraft.log`.
- Android log: `& "$sdk\platform-tools\adb.exe" logcat -s AndroidRuntime` right after a crash.
- Send me the step number, what you saw and what you expected, or file a GitHub issue.
