# NutriSnap — AI-Powered Calorie & Nutrition Tracker

> Reference architecture and foundational code for a comprehensive, AI-powered
> nutrition tracking mobile app.

This package contains the **design** (this file + `/docs`) and a **runnable
foundational scaffold** for the **native Android client** (`/android`) and the
serverless backend (`/backend`).

> **Build order (per request): Android first.** We ship a native Android app
> now, then add iOS later. Native Kotlin is chosen over React Native/Flutter for
> the first release because the camera-heavy, AI-heavy core (CameraX + ML Kit
> on-device barcode scanning, Health Connect, fast photo capture) is best served
> by first-party Android APIs. The domain layer (calorie math, repositories,
> models) is framework-agnostic, so a future iOS app or RN layer can reuse the
> same backend and schema.

---

## 1. Tech Stack Recommendation

| Layer | Choice | Why |
|-------|--------|-----|
| **Frontend (Android, first target)** | **Kotlin + Jetpack Compose** | Modern declarative UI, first-class **CameraX** + **ML Kit** for instant on-device barcode scanning, **Health Connect** for steps/active-calories, and **Hilt** for DI. Best performance for the camera/AI flows that define this app. *React Native/Flutter remain the recommended path if/when a single iOS+Android codebase becomes the priority.* |
| **Backend** | **Firebase (Firestore + Auth + Cloud Functions + FCM)** | Serverless, scales to zero, real-time sync out of the box, and offline persistence for free — critical for a food-logging app used in spotty connectivity (restaurants, gyms). Cloud Functions host the secrets (OpenAI key) so they never touch the device. FCM handles water/fasting reminders. |
| **AI Vision** | **OpenAI Vision (`gpt-4o`)** for "Snap It" | Best zero-shot food recognition + structured macro estimation in one call via JSON mode. Google Cloud Vision is a fallback for pure label detection but doesn't estimate macros. |
| **Barcode → Food** | **OpenFoodFacts API** (primary), Nutritionix (paid fallback) | Free, open, 3M+ products, no per-call cost. Nutritionix fills gaps for US-packaged/branded items. |
| **Recipe parsing** | **Cloud Function scraper** using `schema.org/Recipe` JSON-LD + LLM fallback | Most recipe sites expose structured `Recipe` JSON-LD; we parse that first and only fall back to the LLM for messy sites. |
| **Health sync** | `react-native-health` (HealthKit) + `react-native-health-connect` (Google Fit / Health Connect) | Native step + active-energy data; abstracted behind one `HealthService` interface. |
| **Android architecture** | **MVVM + Clean-ish layers, Room + Retrofit** | `ViewModel` + `StateFlow` for UI state; **Room** for offline-first food logs; **Retrofit/OkHttp** for OpenFoodFacts and our backend; **DataStore** for prefs. |

**Key principle:** *All third-party API keys live in Cloud Functions, never on the
device.* The app calls our functions; our functions call OpenAI / Nutritionix.

---

## 2. App Architecture & Folder Structure

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the diagram and the
full folder tree. High level:

```
nutrition-app/
├── android/                        # Native Android client (Kotlin + Compose)
│   ├── app/build.gradle.kts
│   └── app/src/main/java/com/nutrisnap/app/
│       ├── NutriSnapApp.kt         # @HiltAndroidApp
│       ├── MainActivity.kt
│       ├── di/                     # Hilt modules (network, db, repos)
│       ├── data/
│       │   ├── model/              # Domain models (Nutrition, FoodLog…)
│       │   ├── remote/             # Retrofit APIs (OpenFoodFacts, backend) + DTOs
│       │   ├── local/              # Room entities + DAOs
│       │   └── repository/         # FoodRepository, etc.
│       ├── domain/usecase/         # CalorieTargetCalculator (Mifflin-St Jeor)…
│       ├── feature/
│       │   ├── onboarding/         # Goals → calorie target
│       │   ├── tracking/           # Daily diary, macro rings
│       │   ├── camera/             # BarcodeScannerService + SnapItService ← core
│       │   ├── fasting/            # 16:8 timer
│       │   ├── water/              # Intake + reminders
│       │   └── insights/           # AI behavioural feedback
│       └── ui/                     # theme/ + navigation/
└── backend/                        # Firebase (shared by Android + future iOS)
    └── functions/src/
        ├── ai/                     # visionService (Snap It), insightsEngine
        ├── food/                   # barcodeService (OpenFoodFacts proxy)
        ├── recipe/                 # recipeImport (JSON-LD scraper)
        ├── health/                 # aggregation of synced activity
        └── lib/                    # macro math, auth guards, schemas
```

The Android app follows **MVVM with feature packages**: each `feature/` package
owns its Compose screens, `ViewModel`, and any service. The `data/` and
`domain/` layers are framework-agnostic so they can back a future iOS client.
Domain models mirror the Firestore schema.

---

## 3. Database Schema

Full schema with indexes and security notes in
[`docs/DATABASE_SCHEMA.md`](docs/DATABASE_SCHEMA.md). Summary (Firestore NoSQL):

```jsonc
// users/{uid}
{
  "uid": "abc123",
  "email": "user@example.com",
  "profile": {
    "heightCm": 180, "weightKg": 82.5, "birthDate": "1990-04-12",
    "sex": "male", "activityLevel": "moderate",   // sedentary..athlete
    "goal": "lose"                                  // lose | maintain | gain
  },
  "targets": {
    "tdee": 2650,                 // computed maintenance
    "dailyCalories": 2150,        // default daily budget
    "macros": { "proteinG": 165, "carbsG": 195, "fatG": 62,
                "fiberG": 30, "sugarG": 50, "sodiumMg": 2300 },
    "calorieCycling": {           // optional per-weekday override
      "enabled": true,
      "byWeekday": { "0": 2400, "6": 2400 }  // 0=Sun..6=Sat (weekend higher)
    }
  },
  "fasting": { "protocol": "16:8", "eatingWindowStart": "12:00" },
  "createdAt": 1719600000000
}

// users/{uid}/foodLogs/{logId}
{
  "id": "log_001",
  "loggedAt": 1719603600000,
  "date": "2026-06-29",          // denormalized for daily queries
  "meal": "lunch",               // breakfast|lunch|dinner|snack
  "source": "snap",              // snap | barcode | manual | recipe
  "name": "Grilled chicken bowl",
  "servingQty": 1, "servingUnit": "bowl",
  "nutrition": {
    "calories": 540, "proteinG": 48, "carbsG": 35, "fatG": 18,
    "fiberG": 6, "sugarG": 4, "sodiumMg": 690
  },
  "aiConfidence": 0.82,          // null for barcode/manual
  "barcode": null,
  "photoUrl": "gs://.../log_001.jpg"
}

// users/{uid}/fastingSessions/{sessionId}
{
  "id": "fast_001",
  "protocol": "16:8",
  "targetHours": 16,
  "startedAt": 1719550800000,
  "endedAt": 1719608400000,      // null while active
  "completed": true,
  "actualHours": 16.0
}

// users/{uid}/waterLogs/{date}   (one doc per day)
{ "date": "2026-06-29", "goalMl": 2500, "entries": [
    { "ml": 250, "at": 1719603600000 } ], "totalMl": 1750 }

// users/{uid}/dailySummaries/{date}  (rollup written by trigger)
{ "date": "2026-06-29", "calories": 1820, "macros": {...},
  "steps": 8400, "activeCalories": 410, "withinFastingWindow": true }
```

---

## 4. Core Code Examples

The two most complex features are scaffolded as real services:

- **AI "Snap It"** — on-device photo capture + backend vision call:
  - Android: [`android/.../feature/camera/SnapItService.kt`](android/app/src/main/java/com/nutrisnap/app/feature/camera/SnapItService.kt)
  - Server: [`backend/functions/src/ai/visionService.ts`](backend/functions/src/ai/visionService.ts)
- **Barcode scanning** — ML Kit on-device decode + OpenFoodFacts lookup:
  - Android: [`android/.../feature/camera/BarcodeScannerService.kt`](android/app/src/main/java/com/nutrisnap/app/feature/camera/BarcodeScannerService.kt)
  - Repo/API: [`android/.../data/repository/FoodRepository.kt`](android/app/src/main/java/com/nutrisnap/app/data/repository/FoodRepository.kt)

Key excerpts are reproduced inline in the chat reply; the files contain the
complete, commented implementations.

---

## 5. Implementation Roadmap

Detailed in [`docs/ROADMAP.md`](docs/ROADMAP.md). TL;DR — ship an MVP that
proves the logging loop, then layer AI and lifestyle features:

1. **Phase 0 — Foundations (week 1):** Expo app, Firebase project, Auth,
   Firestore schema, CI. *Done when a user can sign in.*
2. **Phase 1 — MVP logging loop (weeks 2–3):** Onboarding → calorie target,
   manual food log, daily macro rings. *Done when a user can hit a daily goal.*
3. **Phase 2 — Barcode (week 4):** Camera + OpenFoodFacts. Fastest "magic" win.
4. **Phase 3 — Snap It AI (weeks 5–6):** OpenAI Vision behind a Cloud Function.
5. **Phase 4 — Lifestyle (weeks 7–8):** Fasting timer, water + reminders,
   calorie cycling.
6. **Phase 5 — Integrations (weeks 9–10):** HealthKit/Google Fit, recipe import.
7. **Phase 6 — AI Insights (week 11+):** Behavioural analysis on accumulated
   logs. Needs data, so it ships last.

See the docs for the dependency rationale and per-phase acceptance criteria.

---

## Running the scaffold

```bash
# Android — open ./android in Android Studio (Hedgehog+), or:
cd android && gradle wrapper   # one-time: generates gradlew + wrapper jar
./gradlew :app:assembleDebug
#   Add your backend base URL in local.properties:  BACKEND_BASE_URL=...
#   Add Firebase google-services.json under android/app/ and enable the plugin.

# Backend
cd backend/functions && npm install && npm run build
firebase emulators:start          # local Firestore + Functions
```

> This is a foundational scaffold: services, types, and one representative
> screen/hook per feature are implemented; UI polish and remaining screens are
> intentionally left as the build-out described in the roadmap.
