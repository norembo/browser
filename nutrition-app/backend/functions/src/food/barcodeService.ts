import { EMPTY_NUTRITION, Nutrition } from "../lib/types";

/**
 * Server-side OpenFoodFacts lookup. The Android client scans barcodes directly,
 * but this proxy exists for: (a) future iOS/web clients, (b) caching popular
 * products in Firestore, and (c) augmenting OFF gaps with a paid fallback
 * (Nutritionix) without shipping that key to devices.
 */

const OFF_BASE = "https://world.openfoodfacts.org/api/v2";

interface OffNutriments {
  [key: string]: number | undefined;
}

export interface ProductResult {
  barcode: string;
  name: string;
  servingUnit: string;
  nutrition: Nutrition;
}

function pick(n: OffNutriments, serving: string, per100: string): number {
  return n[serving] ?? n[per100] ?? 0;
}

export async function lookupBarcode(barcode: string): Promise<ProductResult | null> {
  const url = `${OFF_BASE}/product/${encodeURIComponent(barcode)}.json` +
    `?fields=product_name,brands,serving_size,nutriments`;
  const res = await fetch(url, { headers: { "User-Agent": "NutriSnap/0.1 (support@nutrisnap.app)" } });
  if (!res.ok) return null;

  const data = (await res.json()) as {
    status?: number;
    product?: { product_name?: string; brands?: string; serving_size?: string; nutriments?: OffNutriments };
  };
  if (data.status !== 1 || !data.product?.nutriments) return null;

  const n = data.product.nutriments;
  const nutrition: Nutrition = {
    ...EMPTY_NUTRITION,
    calories: Math.round(pick(n, "energy-kcal_serving", "energy-kcal_100g")),
    proteinG: pick(n, "proteins_serving", "proteins_100g"),
    carbsG: pick(n, "carbohydrates_serving", "carbohydrates_100g"),
    fatG: pick(n, "fat_serving", "fat_100g"),
    fiberG: pick(n, "fiber_serving", "fiber_100g"),
    sugarG: pick(n, "sugars_serving", "sugars_100g"),
    sodiumMg: pick(n, "sodium_serving", "sodium_100g") * 1000,
  };
  if (nutrition.calories === 0) return null;

  const name = [data.product.brands?.split(",")[0], data.product.product_name]
    .filter(Boolean)
    .join(" ")
    .trim() || "Scanned product";

  return { barcode, name, servingUnit: data.product.serving_size ?? "serving", nutrition };
}
