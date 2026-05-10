# Last Season — Boxing Game (Desktop)

Ported from Android Studio to a standalone desktop Java app.  
All game logic, assets, audio, and sprites from the original are preserved.

---

## Requirements

- **Java 11 or newer** (JRE is enough to run the JAR)
- **Maven 3.6+** (only needed to build from source)

Check your Java version:
```bash
java -version
```

---

## Quick Start (run the pre-built JAR)

```bash
java -jar boxing-game-1.0.jar
```

---

## Build from Source

```bash
git clone <your-repo-url>
cd BoxingGameDesktop
mvn package
java -jar target/boxing-game-1.0.jar
```

Maven will compile all sources and bundle every asset (PNGs, WAVs) into a single fat JAR.

---

## Controls

Controls work via the on-screen buttons **or** keyboard shortcuts:

| Key | Action |
|-----|--------|
| `P` | Punch |
| `C` | Charged Punch (power shot) |
| `B` | Block |
| `L` | Dodge Left |
| `R` | Dodge Right |

---

## What was changed (Android → Desktop)

| Android | Desktop replacement |
|---|---|
| `Activity` / `Intent` stack | `JFrame` + `CardLayout` (App.java) |
| `android.os.Handler` / `Looper` | `SwingUtilities.invokeLater()` |
| `SharedPreferences` | `java.util.prefs.Preferences` (saves to OS user profile) |
| `MediaPlayer` / `SoundPool` | `javax.sound.sampled.Clip` (SoundManager.java) |
| `Canvas` / `View` drawing | `JPanel` + `Graphics2D` (FightView.java) |
| `R.drawable.*` (int IDs) | `String` resource names loaded by `Assets.java` |
| Android vector XML drawables | Java2D procedural fallback in `Assets.generateFallback()` |
| `R.raw.*` WAV references | `String` names loaded from `/raw/*.wav` in JAR |
| Gradle Android plugin | Maven + maven-shade-plugin (fat JAR) |

**Unchanged:** `GameConstants`, `GameState`, `FightEngine`, `IdleEngine`,  
`EnemyRoster`, all PNG sprites, all WAV audio files.

---

## Save Data

Progress is stored using `java.util.prefs.Preferences`:
- **Windows:** Registry under `HKCU\Software\JavaSoft\Prefs\com\boxergame`
- **macOS:** `~/Library/Preferences/com.apple.java.util.prefs.plist`
- **Linux:** `~/.java/.userPrefs/com/boxergame/prefs.xml`

To reset all progress, delete those entries or call `SaveManager.reset()`.

---

## Project Structure

```
BoxingGameDesktop/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/boxergame/
    │   ├── App.java              ← Entry point / screen router
    │   ├── LobbyScreen.java      ← Main menu (replaces MainActivity)
    │   ├── FightScreen.java      ← Fight UI (replaces FightActivity)
    │   ├── FightView.java        ← Canvas rendering (replaces Android FightView)
    │   ├── ShopScreen.java       ← Shop (replaces ShopActivity)
    │   ├── ResultScreen.java     ← Result (replaces ResultActivity)
    │   ├── Assets.java           ← Resource loader (replaces R.drawable/R.raw)
    │   ├── SoundManager.java     ← Audio (replaces Android MediaPlayer/SoundPool)
    │   ├── SaveManager.java      ← Persistence (replaces SharedPreferences)
    │   ├── FightEngine.java      ← [UNCHANGED] All fight logic + AI
    │   ├── GameState.java        ← [UNCHANGED] Central game state
    │   ├── IdleEngine.java       ← [UNCHANGED] Muscle Memory / idle
    │   ├── GameConstants.java    ← [UNCHANGED] All tunable constants
    │   ├── EnemyData.java        ← [MODIFIED] String resource names instead of int IDs
    │   ├── EnemyRoster.java      ← [MODIFIED] String resource names instead of R.drawable.*
    └── resources/
        ├── drawable/             ← All 33 original PNG sprites
        └── raw/                  ← All 15 original WAV audio files
```
