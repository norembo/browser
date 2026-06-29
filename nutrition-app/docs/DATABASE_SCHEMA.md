# Database Schema (Firestore + local Room)

Firestore is the cloud source of truth; Room mirrors the hot data for offline
use. Collections are namespaced per user so security rules are trivial
(`request.auth.uid == uid`).

## Collections

```
users/{uid}                          ← profile, targets, fasting prefs
users/{uid}/foodLogs/{logId}         ← every logged item
users/{uid}/fastingSessions/{id}     ← fasting history
users/{uid}/waterLogs/{date}         ← one doc per day
users/{uid}/dailySummaries/{date}    ← rollup (written by trigger/client)
```

## `users/{uid}`

```jsonc
{
  "uid": "abc123",
  "email": "user@example.com",
  "profile": {
    "heightCm": 180, "weightKg": 82.5, "ageYears": 36,
    "sex": "male",                       // male | female
    "activityLevel": "moderate",         // sedentary|light|moderate|active|athlete
    "goal": "lose"                       // lose | maintain | gain
  },
  "targets": {
    "tdee": 2650,
    "dailyCalories": 2150,
    "macros": { "calories": 2150, "proteinG": 165, "carbsG": 195, "fatG": 62,
                "fiberG": 30, "sugarG": 50, "sodiumMg": 2300 },
    "calorieCyclingByWeekday": { "5": 2400, "6": 2400 }  // 0=Mon..6=Sun
  },
  "fasting": { "protocol": "16:8", "targetHours": 16, "eatingWindowStart": "12:00" },
  "createdAt": 1719600000000
}
```

## `users/{uid}/foodLogs/{logId}`

```jsonc
{
  "id": "log_001",
  "loggedAt": 1719603600000,
  "date": "2026-06-29",                  // denormalized for day queries
  "meal": "lunch",                       // breakfast|lunch|dinner|snack
  "source": "snap",                      // snap|barcode|manual|recipe
  "name": "Grilled chicken bowl",
  "servingQty": 1, "servingUnit": "bowl",
  "nutrition": { "calories": 540, "proteinG": 48, "carbsG": 35, "fatG": 18,
                 "fiberG": 6, "sugarG": 4, "sodiumMg": 690 },
  "aiConfidence": 0.82,                  // null for barcode/manual
  "barcode": null,
  "photoUrl": "gs://nutrisnap/uid/log_001.jpg"
}
```

**Indexes:** composite `(date ASC, loggedAt ASC)` for the diary; single-field on
`source` for analytics.

## `users/{uid}/fastingSessions/{id}`

```jsonc
{
  "id": "fast_001", "protocol": "16:8", "targetHours": 16,
  "startedAt": 1719550800000, "endedAt": 1719608400000,
  "completed": true, "actualHours": 16.0
}
```

## `users/{uid}/waterLogs/{date}`

```jsonc
{ "date": "2026-06-29", "goalMl": 2500, "totalMl": 1750,
  "entries": [ { "ml": 250, "at": 1719603600000 } ] }
```

## `users/{uid}/dailySummaries/{date}` (rollup)

Maintained by a Firestore trigger (or the client on write). Powers AI Insights
and the diary's activity-adjusted budget without re-reading every food log.

```jsonc
{
  "date": "2026-06-29",
  "totals": { "calories": 1820, "proteinG": 150, "carbsG": 160, "fatG": 55,
              "fiberG": 24, "sugarG": 40, "sodiumMg": 2100 },
  "proteinByMeal": { "breakfast": 12, "lunch": 48, "dinner": 70, "snack": 20 },
  "loggedAt": [1719603600000, 1719630000000],
  "steps": 8400, "activeCalories": 410,
  "withinFastingWindow": true
}
```

## Local Room tables

Room mirrors `food_logs`, `fasting_sessions`, and `water_logs` (see
`data/local/Entities.kt`). Each row carries a `synced` flag so a background
worker can push unsynced rows to Firestore and reconcile.
