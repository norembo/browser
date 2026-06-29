# Architecture

## System diagram

```
┌─────────────────────────────────────────────┐
│            Android app (Kotlin/Compose)       │
│                                               │
│  UI (Compose screens)                         │
│      │  StateFlow                             │
│  ViewModels  ── feature/{onboarding,tracking, │
│      │          camera,fasting,water,insights}│
│  Repositories ─────────────┬──────────────┐   │
│      │                     │              │   │
│  Room (offline cache)   OpenFoodFacts   Backend│
│  DataStore (profile)    (direct, no key)  API  │
└───────────────────────────────────────────┼───┘
                                             │ HTTPS
                              ┌──────────────▼───────────────┐
                              │   Firebase Cloud Functions    │
                              │  snapIt → OpenAI Vision        │
                              │  barcode → OpenFoodFacts        │
                              │  recipeImport → JSON-LD scraper │
                              │  insights → patterns + LLM      │
                              └──────────────┬───────────────┘
                                             │
                       ┌─────────────────────┼───────────────┐
                       │ Firestore     Firebase Auth     FCM  │
                       └──────────────────────────────────────┘
```

## Layers (Android)

- **UI** — Jetpack Compose, stateless; renders `UiState` and emits events.
- **ViewModel** — owns `StateFlow<UiState>`, calls repositories, survives config changes.
- **Repository** — single source of truth per domain; merges Room (offline) with
  remote sources. The app is **offline-first**: writes hit Room immediately and
  sync to Firestore in the background.
- **Data sources** — Room/DataStore (local), Retrofit APIs (OFF + our backend).

## Why secrets stay server-side

`SnapItService` (device) only ever uploads a JPEG to our `snapIt` function. The
OpenAI key is a Functions secret. The same applies to any paid food API. Barcode
lookups use OpenFoodFacts, which needs no key, so the device calls it directly
for latency — with the backend `barcode` function available as a cacheable proxy
for future clients.

## Health sync (placeholder)

`androidx.health.connect:connect-client` reads steps + active calories. A
`HealthRepository` (to be added in Phase 5) exposes `observeTodayActivity()` and
writes an `activeCalories`/`steps` field onto the day's `dailySummaries` doc,
which the diary uses to show "calories remaining incl. exercise". On iOS the
same interface is backed by HealthKit.

## Module/package map

| Package | Responsibility |
|---------|----------------|
| `data.model` | Domain models (framework-agnostic) |
| `data.local` | Room entities, DAOs, mappers |
| `data.remote` | Retrofit APIs + DTOs |
| `data.repository` | Repositories |
| `domain.usecase` | Calorie/macro math, budget resolution |
| `feature.*` | Screen + ViewModel (+ service) per feature |
| `di` | Hilt modules |
| `ui` | Theme + navigation |
