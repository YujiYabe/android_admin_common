# android_admin_common

Android apps in `private_dev` share parent/admin screens through this Compose
library.

## Usage

Add the included build to each app's `settings.gradle.kts`:

```kotlin
includeBuild("../android_admin_common")
```

Then depend on the library from the app module:

```kotlin
implementation("com.yuji:android-admin-common:0.1.0")
```

## Current Shared APIs

- `AdminLoginScreen`: full-screen admin login UI.
- `AdminUnlockPanel`: embedded parent/admin unlock panel.
- `AdminPinDisplay`, `AdminNumericKeypad`: screen-rendered PIN input controls
  used by admin entry screens, avoiding the Android software keyboard.
- `AdminWifiSettingsSection`: shared WiFi SSID picker, password input,
  connection test, save/update, and saved-network list UI.
- `AdminWifiNetwork`: shared WiFi credential model for app boundary mapping.
- `hasFineLocationPermission()`, `currentWifiSsid()`, `availableWifiSsids()`,
  `requestAdminWifiConnection()`: Android WiFi helpers used by the shared UI.
- `YoutubePlayerScreen`: shared in-app YouTube WebView screen aligned with
  `school_kanji_game` behavior: Back button, remaining time header, WiFi-gated
  loading, playback progress bridge, and lifecycle-stop close handling.
- `rememberYoutubeWifiStatus()`: shared YouTube WiFi candidate connection state.
- `toYoutubeResumeUrl()`, `normalizedYoutubeUrl()`, `isYoutubeVideoUrl()`:
  shared YouTube URL helpers.
- `adminSha256()`: legacy compatible SHA-256 helper.
- `digitsOnly()`: numeric password input normalization.
- `AdminPasswordHasher`: salted hash creation and verification for new PINs.

App-specific settings such as kanji grade filters, YouTube reward rules, and WiFi
connection behavior should stay in each app and call these shared components.
