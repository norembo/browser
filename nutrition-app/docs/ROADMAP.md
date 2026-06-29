# Implementation Roadmap (MVP-first)

Each phase ships something usable. Dependencies dictate the order: the logging
loop must exist before AI/insights have data to work with.

## Phase 0 — Foundations (week 1)
- Android project (this scaffold), Firebase project, Auth (Google + email).
- Firestore schema + security rules. CI: lint + unit tests on PR.
- **Done when:** a user can sign in and an empty diary renders.

## Phase 1 — MVP logging loop (weeks 2–3)  ← the real MVP
- Onboarding → `CalorieTargetCalculator` (Mifflin-St Jeor) → targets.
- Manual food entry + daily diary with macro progress bars (`DiaryScreen`).
- Offline-first via Room.
- **Done when:** a user can set a goal and hit a daily calorie/macro target by
  hand. *This validates the core habit before any AI spend.*

## Phase 2 — Barcode scanning (week 4)
- CameraX + ML Kit (`BarcodeScannerService`) → OpenFoodFacts (`FoodRepository`).
- **Done when:** scanning a packaged product logs it in 2 taps. *Fastest "wow".*

## Phase 3 — AI "Snap It" (weeks 5–6)
- `SnapItService` (device capture) → `snapIt` Cloud Function → OpenAI Vision.
- Confirmation/edit screen before logging (estimates are never auto-trusted).
- Cost controls: low-detail images, per-user daily cap.
- **Done when:** a photo produces editable macro estimates.

## Phase 4 — Lifestyle features (weeks 7–8)
- Intermittent fasting timer (`FastingScreen`, 16:8 default, configurable).
- Water tracking + WorkManager reminders (`WaterReminderWorker`).
- Calorie cycling (per-weekday budgets via `calorieCyclingByWeekday`,
  resolved by `CalorieTargetCalculator.budgetFor`).
- **Done when:** all three run without backend dependencies.

## Phase 5 — Integrations (weeks 9–10)
- Health Connect (steps + active calories) → activity-adjusted budget.
- Recipe import (`recipeImport` function) → ingredients → macros/serving.
- **Done when:** exercise adjusts the budget and a recipe URL logs a serving.

## Phase 6 — AI Insights (week 11+)
- `insightsEngine`: deterministic pattern detection + LLM phrasing over
  `dailySummaries`. Ships last because it needs accumulated data.
- **Done when:** a user with ≥1 week of logs sees ≥3 grounded, useful tips.

## Cross-cutting (continuous)
- Analytics + crash reporting from Phase 1.
- Cost dashboards for OpenAI from Phase 3.
- iOS app reuses backend + schema once Android retention is proven.

## Build sequencing rationale
1. **Habit before magic** — manual logging proves retention cheaply.
2. **Barcode before AI** — free, fast, high trust; warms users to the camera.
3. **AI after the loop** — vision/insights only add value with real data and a
   place to put results.
