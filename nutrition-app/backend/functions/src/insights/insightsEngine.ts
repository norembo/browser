import OpenAI from "openai";
import { Nutrition } from "../lib/types";

/**
 * AI Insights: detects behavioural patterns from the user's recent logs, then
 * uses the LLM only to phrase them as short, friendly, actionable tips. We do
 * the statistics deterministically (cheap, explainable) and let the model handle
 * natural language — this keeps insights grounded in the user's real data.
 */

export interface DayLog {
  date: string;
  totals: Nutrition;
  proteinByMeal: { breakfast: number; lunch: number; dinner: number; snack: number };
  loggedAt: number[]; // epoch millis of each entry
}

interface Signal {
  key: string;
  detail: string;
}

/** Pure pattern detection — returns plain-language signals for the LLM to polish. */
export function detectSignals(days: DayLog[]): Signal[] {
  const signals: Signal[] = [];
  if (days.length < 3) return signals;

  const avg = (xs: number[]) => (xs.length ? xs.reduce((a, b) => a + b, 0) / xs.length : 0);

  // 1. Protein distribution: low breakfast protein.
  const breakfastProtein = avg(days.map((d) => d.proteinByMeal.breakfast));
  const dinnerProtein = avg(days.map((d) => d.proteinByMeal.dinner));
  if (breakfastProtein < 20 && dinnerProtein > breakfastProtein * 1.8) {
    signals.push({
      key: "protein_breakfast",
      detail: `Average breakfast protein is only ${breakfastProtein.toFixed(0)}g vs ${dinnerProtein.toFixed(0)}g at dinner.`,
    });
  }

  // 2. Late-night eating (entries after 21:00).
  const lateDays = days.filter((d) =>
    d.loggedAt.some((t) => new Date(t).getHours() >= 21),
  ).length;
  if (lateDays / days.length > 0.4) {
    signals.push({
      key: "late_eating",
      detail: `Eating after 9pm on ${lateDays} of the last ${days.length} days.`,
    });
  }

  // 3. Fiber consistently under target.
  const avgFiber = avg(days.map((d) => d.totals.fiberG));
  if (avgFiber < 22) {
    signals.push({ key: "low_fiber", detail: `Average fiber ${avgFiber.toFixed(0)}g/day (target ~30g).` });
  }

  // 4. Weekend calorie spikes.
  const weekend = days.filter((d) => [0, 6].includes(new Date(d.date).getDay()));
  const weekday = days.filter((d) => ![0, 6].includes(new Date(d.date).getDay()));
  const weAvg = avg(weekend.map((d) => d.totals.calories));
  const wdAvg = avg(weekday.map((d) => d.totals.calories));
  if (weAvg > wdAvg * 1.2 && weekend.length >= 1) {
    signals.push({
      key: "weekend_spike",
      detail: `Weekend intake (~${weAvg.toFixed(0)} kcal) runs well above weekdays (~${wdAvg.toFixed(0)} kcal).`,
    });
  }

  return signals;
}

export async function generateInsights(days: DayLog[], openai: OpenAI): Promise<string[]> {
  const signals = detectSignals(days);
  if (signals.length === 0) return [];

  const completion = await openai.chat.completions.create({
    model: "gpt-4o-mini",
    temperature: 0.5,
    messages: [
      {
        role: "system",
        content:
          "You are a supportive nutrition coach. Turn each data signal into ONE short, " +
          "specific, encouraging insight (max 22 words). Reference the behaviour and suggest a " +
          "concrete tweak. Return a JSON object: { \"insights\": string[] }.",
      },
      { role: "user", content: JSON.stringify({ signals }) },
    ],
    response_format: { type: "json_object" },
  });

  try {
    const parsed = JSON.parse(completion.choices[0]?.message?.content ?? "{}");
    return Array.isArray(parsed.insights) ? parsed.insights.slice(0, 5) : [];
  } catch {
    return signals.map((s) => s.detail);
  }
}
