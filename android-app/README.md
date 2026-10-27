# Gyaanly: a playful daily-learning Android app

Bite-sized daily lessons with quizzes, streaks, XP and levels. Fully **offline**: no account, no API key.

| Path | Lessons | What you get each day |
|---|---|---|
| 🧠 **Human Edge in AI** | 100 lessons on the edges people keep even if AI becomes overpowering: Edge in an AI-Run World (trust, presence, care, ownership, accountability, meaning…) → Mindset → Skills → Work & Career → Future-proofing → Industry Playbooks | One idea, why humans win, how to apply it, a deeper dive, *today's mission*, 3-question quiz |
| 🤝 **People Skills at Work** | 100 lessons for corporate people skills & communication: Self-awareness & First Impressions → Working with Your Manager → Peers & Teamwork → Meetings → Emails, Chat & Writing → Influence & Stakeholders → Feedback & Difficult Conversations → Conflict, Politics & Power → Visibility, Presentations & Executive Presence → Leading Others | The situation, what skilled people do, **say-it-like-this scripts**, what to avoid, a speak-it drill, *practise at the office today*, 3-question quiz. Also a built-in **e-book** (📖 Read the e-book), see [`ebook/`](ebook/README.md) |
| 🇮🇳 **Smart Citizen India** | 200 everyday Indian problems in 10 categories: roads, air & water (NCR focus), health, money, cyber fraud, documents & rights, home, work, safety & climate, consumer rights | The problem, your human edge, how AI & tech help, rights/helplines/portals, *today's mission*, 3-question quiz |
| 🗣️ **English Speaking** | 100 lessons, CEFR **A1 → C2** (~17 per level), plus **Learn with Videos** (10 shadowing lessons with a ▶ button to the *Speak English With Class* YouTube channel) and a **15-day Interview Prep course** (Day 1 job research → Day 15 follow-up) | Grammar/phrases, workplace usage, common mistakes, a dialogue, a **speak-it drill** (text-to-speech + speech-recognition score), 3-question quiz |
| 🇪🇸 **Spanish** | 120 lessons in **12 Duolingo-style units** (A1 → B1), each grammar point taught completely in one unit, then reviewed: Hello & Café → Ser → Estar → Family & Tener → Everyday Verbs → Likes & Plans → Irregular Present → Daily Routine → Preterite → Imperfect → Future & Experiences → Commands & Subjunctive | Grammar + pronunciation tips, a dialogue, **speak-it drill in Spanish** (`es-ES`), today's mission, 3-question quiz |
| 🏢 **Finance Shared Services** | 110 lessons, one key word each, for 11 teams: AP, O2C Collections, Master Creation, Billing, ES (payroll & employee services), CRT (central revenue), Banking & Treasury, Taxation, Central MIS, IC & Softex, Fixed Assets. Each team ends with its reconciliation. **Finish-by-31-Dec-2026 pace tracker** | Explain like I'm 20, what the team actually does, a real example, what to watch out for, worked calculations, 3-question quiz |
| 🧩 **Mind & Words** | 100 topics shown in **random order**: cognitive biases, manipulation tactics (gaslighting, triangulation, covert narcissism, double bind…), psychology effects (halo effect…), money & modern life (guilt tipping, dark patterns…), manifesting & mindset (the honest, evidence-based version) and trending words | Explain like I'm 20, a real-life example, how to spot it, what to do, 3-question quiz |
| 💰 **Corporate Finance** | 100 lessons: Basics → Intermediate → Advanced | Concept, word of the day, real example, **solve-it-yourself calculation** with step-by-step reveal, 3-question quiz |

Plus a 🧰 **Finance toolbox**: NPV, IRR, WACC, CAPM, CAGR, EMI, break-even, payback and key ratios, all showing their working.

**🎙️ Talk: spoken conversation practice.** The Talk tab runs real, hands-free conversations: the app speaks, you answer out loud (no script to read), it listens automatically and carries on. 40 built-in role-plays cover daily life, office, interviews and cold sales calls, and you can paste your own script (`Them:` / `Me:` lines; your lines become the model answers). At the end you get a report: overall score, fluency (pace, filler words, hedging), key points covered, common grammar slips (e.g. "discuss about", "since 5 years"), answer length, questions asked and the next step on sales calls, plus a turn-by-turn review with a strong sample answer. Optional AI (Settings → AI): with a **Sarvam AI** or **Claude** API key, the partner improvises and replies to what you actually say, writes any scenario you describe, and an AI coach gives detailed feedback. Keys stay on the phone and are never included in backups. For better recognition of Indian accents, Settings → Speech recognition → **Sarvam speech-to-text** records each answer on the phone (pauses allowed), sends it to Sarvam's model, and falls back to the phone's recogniser with one tap if the network or key fails. Settings → Partner's voice → **Sarvam Bulbul** gives the partner a natural, human-sounding Indian voice (choose the voice name and speed); lines are fetched while you answer, and the phone voice steps in if Sarvam fails.

**Listen, share and e-books, for every course:** in any lesson tap **🔊 Listen** to hear the whole lesson (Spanish lines in a Spanish voice) or **📤 Share** to send it as text. In any path, **📖 Read, listen or share as e-book** turns the course into a book you can read in the app, listen to chapter by chapter, share (WhatsApp, Drive, email) or save as EPUB. Each topic also has a **📖 Book** button for just that topic.

**Too much at once?** Learn → ⚙️ Choose (or Me → Choose courses & topics) lets you switch whole courses or single topics off. They vanish from Today and Learn, and your progress is kept. Tapping a level chip inside a path jumps straight to that level's lessons.

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
| `order` | Position in lists (built-ins use 1-6) |
| `levelPicker` | `true` lets learners choose their starting level |
| `tools` | Optional, e.g. `["calculators"]` or `["book"]` |
| `shuffle` | `true` gives a random unfinished lesson each day |
| `deadline` | Optional `"yyyy-MM-dd"`; the path shows how many lessons a day you need to finish on time |

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
