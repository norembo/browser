import OpenAI from "openai";
import { EMPTY_NUTRITION, Nutrition, SnapItem, SnapResponse } from "../lib/types";

/**
 * Core of the AI "Snap It" feature. Receives a base64 JPEG from the device and
 * asks an OpenAI vision model to (1) identify each distinct food on the plate
 * and (2) estimate its serving size and macros. We force JSON output so the
 * result is machine-parseable, then clamp/sanitize before returning.
 *
 * The OpenAI key lives only here (Cloud Functions secret), never on the device.
 */

const SYSTEM_PROMPT = `You are a nutrition vision assistant.
Identify each distinct food/drink item visible in the photo.
For EACH item, estimate a realistic single-serving portion and its nutrition.
Respond with STRICT JSON ONLY in this shape:
{
  "items": [
    {
      "name": "string",
      "servingQty": number,
      "servingUnit": "string (e.g. 'bowl', 'g', 'piece')",
      "nutrition": {
        "calories": number, "proteinG": number, "carbsG": number,
        "fatG": number, "fiberG": number, "sugarG": number, "sodiumMg": number
      }
    }
  ],
  "confidence": number   // 0..1, your overall confidence in the estimate
}
Be conservative; if unsure, lower the confidence. Do not include commentary.`;

function clampNutrition(raw: Partial<Nutrition> | undefined): Nutrition {
  const n = raw ?? {};
  const num = (v: unknown) => (typeof v === "number" && isFinite(v) && v >= 0 ? v : 0);
  return {
    calories: Math.round(num(n.calories)),
    proteinG: num(n.proteinG),
    carbsG: num(n.carbsG),
    fatG: num(n.fatG),
    fiberG: num(n.fiberG),
    sugarG: num(n.sugarG),
    sodiumMg: num(n.sodiumMg),
  };
}

export async function analyzeFoodImage(
  imageBase64: string,
  hintMeal: string | undefined,
  openai: OpenAI,
): Promise<SnapResponse> {
  const userText = hintMeal
    ? `This is likely a ${hintMeal}. Analyze the photo.`
    : "Analyze the photo.";

  const completion = await openai.chat.completions.create({
    model: "gpt-4o",
    temperature: 0.2,
    response_format: { type: "json_object" },
    messages: [
      { role: "system", content: SYSTEM_PROMPT },
      {
        role: "user",
        content: [
          { type: "text", text: userText },
          {
            type: "image_url",
            image_url: { url: `data:image/jpeg;base64,${imageBase64}`, detail: "low" },
          },
        ],
      },
    ],
  });

  const content = completion.choices[0]?.message?.content ?? "{}";
  let parsed: { items?: unknown[]; confidence?: number };
  try {
    parsed = JSON.parse(content);
  } catch {
    return { items: [], confidence: 0 };
  }

  const items: SnapItem[] = (parsed.items ?? []).map((raw) => {
    const it = raw as Partial<SnapItem>;
    return {
      name: typeof it.name === "string" && it.name.trim() ? it.name.trim() : "Unknown food",
      servingQty: typeof it.servingQty === "number" && it.servingQty > 0 ? it.servingQty : 1,
      servingUnit: typeof it.servingUnit === "string" ? it.servingUnit : "serving",
      nutrition: clampNutrition(it.nutrition ?? EMPTY_NUTRITION),
    };
  });

  const confidence =
    typeof parsed.confidence === "number" ? Math.min(1, Math.max(0, parsed.confidence)) : 0.5;

  return { items, confidence };
}
