<div align="center">

# ⚡ SleeperBuild

**All-in-One High-Discipline Self-Improvement & Habit Tracker for Android**

<!-- GitHub Shields / Badges Card -->
<p>
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Language-Kotlin%20100%25-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Language Kotlin"></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20(M3)-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"></a>
  <a href="https://developer.android.com/"><img src="https://img.shields.io/badge/Platform-Android%20Native-3DDC84?style=for-the-badge&logo=android&logoColor=black" alt="Android Platform"></a>
  <a href="https://m3.material.io/"><img src="https://img.shields.io/badge/Design-Glassmorphic%20%7C%20Neon-00B0FF?style=for-the-badge&logo=material-design&logoColor=white" alt="UI Style"></a>
  <a href="https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences"><img src="https://img.shields.io/badge/Security-AES256--GCM-FF5722?style=for-the-badge&logo=securityscorecard&logoColor=white" alt="AES256 Encrypted"></a>
  <a href="https://aladhan.com/prayer-times-api"><img src="https://img.shields.io/badge/API-Aladhan%20Precise-00E676?style=for-the-badge&logo=googlemaps&logoColor=black" alt="Aladhan API"></a>
  <a href="https://t.me/sr7mods"><img src="https://img.shields.io/badge/Dev-SR7%20Mods-9C27B0?style=for-the-badge&logo=telegram&logoColor=white" alt="Developer Handle"></a>
</p>

</div>

---

## 🌟 Key Features

* **🕌 Precise Salah & Mosque Jam'ah Hub**:
  * **Aladhan API Engine**: Real-time astronomical calculation with international calculation method presets (*Karachi Hanafi, ISNA, MWL, Makkah Umm Al-Qura, Egyptian General Authority, Diyanet Turkey, Dubai*).
  * **Juristic School Precision**: Configurable Asr shadow ratios between **Hanafi (2x)** and **Standard/Shafi'i (1x)**.
  * **Manual Jam'ah Time Overrides**: Never miss congregation in the mosque! Easily adjust prayer times with direct minute inputs, `+1m`/`-1m` buttons, or quick offset chips (`-15m` to `+20m`) with visual `🕌 JAM'AH TIME` badges.
  * **Exact Wake-Up Alarms & TTS**: Custom advance notice buffers (*At Salah time*, *5m prior*, *10m walk buffer*, *15m Jam'ah prep*) with Arabic & English TTS voice callouts.

* **🏋️ Guided Workout Coach & Fitbit Telemetry**:
  * **Real-time Voice Coaching**: Audio cues, rep pacing, countdown interval chimes, and form guidance.
  * **Fitbit-Style 3-Ring Activity Rings**: Real-time tracking of active minutes, estimated calories burned, and completed exercise sets.
  * **Customizable Routine Programs**: Bodyweight, Simple Equipment, Full Equipment, HIIT Fat Burn, and Core Shred presets.
  * **Body Metrics & BMI Engine**: Live BMI calculation with auto-categorization and athletic rank progression (*Sleeper Initiate Lv 1* to *Sleeper God Lv 5*).
  * **Music Player Direct Trigger**: Floating draggable action button that directly launches **Ongaku7** or your configured player, with one-tap GitHub release download links when not installed.

* **📚 Schedules & Focus Timers (Study & Others)**:
  * Dedicated session scheduling with exact `AlarmManager` reminders.
  * Interactive circular focus countdown timer with completion voice announcements.
  * 7-day consistency activity heatmaps and focus goal compliance metrics.

* **🧠 Mental & Physical Discipline (NoFap & No Smoke)**:
  * **NoFap Tracker**: Millisecond live counter, warrior rank badges, milestone progress rings, and relapse journaling with recovery quotes.
  * **No Smoke Dual-Mode**: Switch between **Angelic Mode** (health restoration metrics, lung repair milestones, money saved) and **Demonic Mode** (hardcore gothic aesthetic, skull chimes, tough-love discipline).
  * **Instant Habit Logger**: Quick counter to log or undo cigarettes with daily and all-time tracking.

* **🔊 Universal Alarm & TTS Voice Audio Engine**:
  * Flexible audio modes: **Voice Only (TTS)**, **Ringtone Alert**, or **Both (Chime + Voice Announcement)**.
  * Pitch and timbre profiles: **Cute Girl (~1.35x)**, **Cute Boy (~0.90x)**, and **Calm Voice (~1.0x)**.
  * System-level exact alarms with wake locks, custom vibration patterns, and notification deep-linking.

* **🔐 Security & Privacy Vault**:
  * Hardware-backed **AES-256-GCM / AES-256-SIV** encryption via AndroidX Security Crypto.
  * Protected by PIN, Pattern, or Password with salted SHA-256 hash verification and security question recovery.
  * Flexible privacy options: Lock entire app on startup or selectively protect sensitive sections.

* **💾 Offline Data Sovereignty & Encrypted Backup**:
  * 100% offline-first architecture — your data never leaves your device.
  * One-tap encrypted JSON backup and restore to safeguard your streaks and logs.

---

## 📊 Tech Stack Breakdown

| Layer | Technology | Details |
| :--- | :--- | :--- |
| **Language** | **Kotlin (100%)** | Idiomatic Kotlin coroutines, StateFlows, and modern clean architecture |
| **UI Framework** | **Jetpack Compose (Material 3)** | Declarative UI, glassy cards, glowing neon accents, and smooth transitions |
| **Prayer Engine** | **Aladhan API & Solar Astronomy** | Real-time REST endpoints + astronomical equation fallback with Hanafi/Shafi'i shadow offsets |
| **Audio & TTS** | **Android TextToSpeech & AudioEngine** | Multi-profile pitch-shifted voice synthesis, ToneGenerator, and RingtoneManager |
| **Alarms & Scheduling** | **AlarmManager & BroadcastReceivers** | `setExactAndAllowWhileIdle` wake-up alarms with Android 13+ permission compliance |
| **Security & Crypto** | **AndroidX EncryptedSharedPreferences** | AES-256-GCM encrypted key-value store with MasterKey Keystore hardware security |
| **Telemetry & Telematics** | **Google Play Services Location** | FusedLocationProviderClient for automatic GPS coordinate resolution |
| **Serialization** | **Moshi (Kotlin Reflection & KSP)** | Fast and type-safe JSON serialization for local backup and restore |

---

## 📁 Project Structure

```text
SleeperBuild/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/sleeper/build7/
│   │       │   ├── MainActivity.kt               # Main entry point & deep-link route dispatcher
│   │       │   ├── audio/
│   │       │   │   └── SleeperAudioEngine.kt     # Multi-profile TTS voice synthesis, chimes & vibration
│   │       │   ├── data/
│   │       │   │   ├── Models.kt                 # Moshi data classes (Prayers, Workouts, Gains, Streaks)
│   │       │   │   ├── PrayerRepository.kt       # Aladhan API client & offline astronomical calculation
│   │       │   │   └── SleeperRepository.kt      # Encrypted storage, reactive flows & backup manager
│   │       │   ├── receiver/
│   │       │   │   ├── AlarmReceiver.kt          # System exact alarm handler & TTS trigger
│   │       │   │   └── BootReceiver.kt           # Device reboot alarm restoration
│   │       │   └── ui/
│   │       │       ├── MainContainer.kt          # Dynamic Hub navigation, security lock & sidebar
│   │       │       ├── components/               # GlassCard, AnimatedRing, Heatmap, SecurityDialogs
│   │       │       ├── screens/
│   │       │       │   ├── PrayerScreen.kt       # Aladhan API, Jam'ah overrides & prayer alarms
│   │       │       │   ├── WorkoutScreen.kt      # Guided coach, 3-ring telemetry, routines & hydration
│   │       │       │   ├── StudyScreen.kt        # Focus session schedules, focus timer & alerts
│   │       │       │   ├── GainsScreen.kt        # Daily gains markdown note logger
│   │       │       │   ├── NoFapScreen.kt        # Clean day counter, ranks & relapse journal
│   │       │       │   ├── NoSmokeScreen.kt      # Angelic vs Demonic tracker & cigarette counter
│   │       │       │   ├── SettingsScreen.kt     # Global Audio Hub, theme & security lock
│   │       │       │   └── BackupScreen.kt       # Encrypted JSON export & restore
│   │       │       └── theme/                    # Material 3 neon color palette & typography
│   │       ├── res/
│   │       │   ├── values/                       # Strings, theme definitions & dimensions
│   │       │   └── drawable/                     # Custom vector icons & adaptive launcher assets
│   │       └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
├── metadata.json
└── README.md
```

---

## 👨‍💻 Developer & Community

Developed with ❤️ by **Alex Sifat Rayhan (SR7MODS)**.

* 📢 **Telegram Channel**: [@sr7mods](https://t.me/sr7mods)
* 👤 **Facebook**: [Alex Sifat Rayhan](https://m.facebook.com/sifatrayhan2007)
* 💬 **WhatsApp**: [+8801318930997](https://wa.me/+8801318930997)
* 🐙 **GitHub**: [sr7mods](https://github.com/sr7mods)
* 📦 **Source Repository**: [sr7mods/SleeperBuild](https://github.com/sr7mods/SleeperBuild)
