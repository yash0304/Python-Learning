# Daily Edge — Android learning app

A daily micro-learning app with three tracks, one lesson per track per day:

| Track | What you get each day | Content |
|---|---|---|
| **Human Edge in AI** | One topic on how humans stay ahead of AI + a concrete action for today | 34 lessons: Mindset → Skills → Work & Career → Future-proofing |
| **English Speaking** | Phrase/grammar point, workplace usage, dialogue, speaking drill with text-to-speech and speech recognition scoring | 36 lessons, CEFR A1 → C2 |
| **Corporate Finance** | One concept + key term + real example + step-by-step calculation | 36 lessons: Basics → Intermediate → Advanced |

Every lesson ends with a quiz. The app tracks streaks, scores and progress, and can send a daily reminder.
It also has 9 interactive finance calculators: NPV, IRR, WACC, CAPM, CAGR, EMI, break-even, payback, key ratios.

Everything works **offline**: no account, no API key.

## Architecture

```
ui (Jetpack Compose, Material 3)
 ├─ TodayScreen      today's 3 lessons, streak
 ├─ TrackScreen      all lessons per track, English level picker
 ├─ LessonScreen     content blocks, speaking practice, quiz
 ├─ Calculator*      finance calculators
 └─ ProfileScreen    progress, reminder settings
        │
   AppViewModel (StateFlow)
        │
 ┌──────┴────────────────────┐
 domain (pure Kotlin)         data
 ├─ DailyPlanner  (pick/streak)  ├─ ContentRepository  ← assets/content/*.json
 ├─ Calculators                  ├─ ContentParser
 └─ SpeechMatch                  └─ ProgressStore (SharedPreferences)
 notify: ReminderScheduler + ReminderWorker (WorkManager)
 speech: Speaker (TextToSpeech) + system RecognizerIntent
```

**Daily engine.** On the first open each day, the app picks for each track the first unfinished lesson (for English, starting at your chosen CEFR level). That lesson stays fixed for the day. When everything is done, it cycles lessons for revision.

## Adding lessons

Lessons are plain JSON in `app/src/main/assets/content/{ai,english,finance}.json`. Add an object to `lessons`:

```json
{
  "id": "fin-37", "level": "Advanced", "title": "…", "summary": "…",
  "sections": [{"h": "Concept", "b": "…"}],
  "term": {"word": "…", "meaning": "…"},           // finance
  "example": "…",
  "calc": {"problem": "…", "steps": ["…"], "answer": "…"},  // finance
  "calculator": "npv",                              // optional: npv irr wacc capm cagr emi breakeven payback ratios
  "speak": ["Sentence to practise"],                // English
  "action": "Something to do today",                // Human Edge
  "quiz": [{"q": "…", "options": ["a","b","c","d"], "answer": 0, "why": "…"}]
}
```

`ContentTest` validates all packs on every build: unique ids, valid levels, 4 options per question and track-specific blocks.

## Build & install

* **Download:** every push runs the *Android app* GitHub Action. Open the run → *Artifacts* → `daily-edge-debug-apk`, unzip, and install `app-debug.apk` on your phone (allow "install unknown apps").
* **Locally:** open `android-app/` in Android Studio, or run `./gradlew assembleDebug` (requires the Android SDK, JDK 17).

Requirements: Android 8.0+ (API 26). Speech practice uses the device's speech recognition service (Google app / Speech Services).
