# Gyaanly: a playful daily-learning Android app

Bite-sized daily lessons with quizzes, streaks, XP and levels. Fully **offline**: no account, no API key.

| Path | Lessons | What you get each day |
|---|---|---|
| 🧠 **Human Edge in AI** | 100 lessons on the edges people keep even if AI becomes overpowering: Edge in an AI-Run World (trust, presence, care, ownership, accountability, meaning…) → Mindset → Skills → Work & Career → Future-proofing → Industry Playbooks | One idea, why humans win, how to apply it, a deeper dive, *today's mission*, 3-question quiz |
| 🇮🇳 **Smart Citizen India** | 200 everyday Indian problems in 10 categories: roads, air & water (NCR focus), health, money, cyber fraud, documents & rights, home, work, safety & climate, consumer rights | The problem, your human edge, how AI & tech help, rights/helplines/portals, *today's mission*, 3-question quiz |
| 🗣️ **English Speaking** | 100 lessons, CEFR **A1 → C2** (~17 per level), plus **Learn with Videos** (10 shadowing lessons with a ▶ button to the *Speak English With Class* YouTube channel) and a **15-day Interview Prep course** (Day 1 job research → Day 15 follow-up) | Grammar/phrases, workplace usage, common mistakes, a dialogue, a **speak-it drill** (text-to-speech + speech-recognition score), 3-question quiz |
| 🇪🇸 **Spanish** | 100 lessons, CEFR **A1 → C1**: sounds, survival Spanish, all main tenses and the subjunctive, work emails, meetings, interviews, finance vocabulary, idioms, Spain vs Latin America | Grammar + pronunciation tips, a dialogue, **speak-it drill in Spanish** (`es-ES`), today's mission, 3-question quiz |
| 💰 **Corporate Finance** | 100 lessons: Basics → Intermediate → Advanced | Concept, word of the day, real example, **solve-it-yourself calculation** with step-by-step reveal, 3-question quiz |

Plus a 🧰 **Finance toolbox**: NPV, IRR, WACC, CAPM, CAGR, EMI, break-even, payback and key ratios, all showing their working.

## Look & feel

A vibrant, game-like theme: gradient cards per path, chunky 3D "pop" buttons, Duolingo-style quiz tiles, a winding lesson path, XP ⚡, streaks 🔥, learner levels 🏆 and a celebration screen after every lesson. Light and dark modes are both supported.

## Architecture

```
ui (Jetpack Compose, Material 3)
 ├─ Today      today's lesson for every path, streak, XP
 ├─ Learn      grid of all paths (auto-discovered)
 ├─ Track      lesson path per level, level picker, tools
 ├─ Lesson     content blocks, speaking drill, quiz, celebration
 ├─ Tools      finance calculators
 └─ Me         level/XP, per-path progress, reminder settings
        │
   AppViewModel (StateFlow)
        │
 ┌──────┴──────────────────────┐
 domain (pure Kotlin)           data
 ├─ DailyPlanner (pick, streak)  ├─ ContentRepository ← every assets/content/*.json
 ├─ Calculators                  ├─ ContentParser
 └─ SpeechMatch                  └─ ProgressStore (SharedPreferences)
 notify: ReminderScheduler + ReminderWorker (WorkManager)
 speech: Speaker (TextToSpeech, any language) + system speech recognizer
```

Each path is **one self-describing JSON file**. The app discovers every `*.json` in `app/src/main/assets/content/`, so adding a path needs **no code changes**.

## ➕ Add your own path (e.g. a language)

1. Copy [`content-template/language-template.json`](content-template/language-template.json) to `app/src/main/assets/content/<your-key>.json`.
2. Edit the `track` block:

| Field | Meaning |
|---|---|
| `key` | Unique id, e.g. `"spanish"` |
| `title`, `tagline`, `emoji` | Shown on cards |
| `color` | Hex colour for the path's gradient, e.g. `"#E11D48"` |
| `order` | Position in lists (built-ins use 1-3) |
| `levelPicker` | `true` lets learners choose their starting level |
| `tools` | Optional, e.g. `["calculators"]` |

3. List your `levels` in order, then add lessons. Every block except `id`, `level`, `title`, `sections` and `quiz` is optional:

```json
{
  "id": "es-02", "level": "A1", "title": "…", "summary": "…",
  "term": {"word": "…", "meaning": "…"},
  "sections": [{"h": "Learn", "b": "…"}],
  "example": "…",
  "calc": {"problem": "…", "steps": ["…"], "answer": "…"},
  "calculator": "npv",
  "speak": ["Sentence to practise"],
  "speakLang": "es-ES",
  "action": "Something to do today",
  "link": {"label": "Open the video", "url": "https://…"},
  "quiz": [{"q": "…", "options": ["a", "b", "c", "d"], "answer": 0, "why": "…"}]
}
```

`speakLang` (a BCP-47 tag such as `es-ES`, `fr-FR`, `de-DE`, `hi-IN`, `ja-JP`) sets the voice for text-to-speech and the language for speech recognition, so pronunciation drills work for any language your phone supports. Lesson ids must be unique across all paths.

4. Push. `ContentTest` validates every pack on each build (unique ids, valid levels, 4 distinct options per question, valid answer index, known calculator ids). The GitHub Action then builds a new APK.

## Build & install

* **Download:** every push runs the *Android app* GitHub Action. Open the run → *Artifacts* → `gyaanly-debug-apk`, unzip and install `app-debug.apk` (allow "install unknown apps").
* **Updating:** every build is signed with the same committed key (`keystore/gyaanly-debug.keystore`) and has a higher version number, so a new APK installs straight over the old one and keeps your progress. (Builds made before this change used a random key per build. If Android says *"package conflicts with an existing package"*, uninstall that old build once, then install the new one.)
* **Backup:** Me → 💾 *Back up progress* saves a JSON file (e.g. to Google Drive); *Restore from backup* brings it back on a new phone or after a reinstall.
* **Locally:** open `android-app/` in Android Studio or run `./gradlew assembleDebug` (Android SDK + JDK 17).

Requires Android 8.0+ (API 26). Speaking drills use the device's speech services (Google app / Speech Services). For Spanish (or any new language), install the voice once: Settings → Speech → Text-to-speech → Google → Install voice data → Español; and add the language to Google voice typing for offline recognition.
