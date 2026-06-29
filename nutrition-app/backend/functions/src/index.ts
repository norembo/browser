import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { onRequest } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import OpenAI from "openai";

import { analyzeFoodImage } from "./ai/visionService";
import { lookupBarcode } from "./food/barcodeService";
import { importRecipe } from "./recipe/recipeImport";
import { generateInsights, DayLog } from "./insights/insightsEngine";

initializeApp();
const db = getFirestore();

// OpenAI key is stored as a Functions secret, never shipped to clients.
const OPENAI_API_KEY = defineSecret("OPENAI_API_KEY");
const openaiClient = () => new OpenAI({ apiKey: OPENAI_API_KEY.value() });

/**
 * In production every endpoint should verify the Firebase ID token in the
 * Authorization header. Stubbed here for brevity.
 */
async function requireUid(req: { headers: Record<string, unknown> }): Promise<string> {
  // const token = (req.headers.authorization as string)?.replace("Bearer ", "");
  // return (await getAuth().verifyIdToken(token)).uid;
  return (req.headers["x-debug-uid"] as string) ?? "demo-uid";
}

/** POST /snapIt  { imageBase64, hintMeal } -> SnapResponse */
export const snapIt = onRequest(
  { secrets: [OPENAI_API_KEY], cors: true, memory: "512MiB", timeoutSeconds: 60 },
  async (req, res) => {
    if (req.method !== "POST") { res.status(405).send("Method not allowed"); return; }
    const { imageBase64, hintMeal } = req.body ?? {};
    if (!imageBase64) { res.status(400).json({ error: "imageBase64 required" }); return; }
    try {
      const result = await analyzeFoodImage(imageBase64, hintMeal, openaiClient());
      res.json(result);
    } catch (e) {
      console.error("snapIt failed", e);
      res.status(500).json({ items: [], confidence: 0 });
    }
  },
);

/** GET /barcode?code=XXXX -> ProductResult | 404 */
export const barcode = onRequest({ cors: true }, async (req, res) => {
  const code = String(req.query.code ?? "");
  if (!code) { res.status(400).json({ error: "code required" }); return; }
  const product = await lookupBarcode(code);
  if (!product) { res.status(404).json({ error: "not found" }); return; }
  res.json(product);
});

/** POST /recipeImport { url } -> ParsedRecipe | 404 */
export const recipeImport = onRequest({ cors: true }, async (req, res) => {
  const url = String(req.body?.url ?? "");
  if (!url) { res.status(400).json({ error: "url required" }); return; }
  const recipe = await importRecipe(url);
  if (!recipe) { res.status(404).json({ error: "could not parse recipe" }); return; }
  res.json(recipe);
});

/** GET /insights?days=14 -> { insights: string[] } */
export const insights = onRequest(
  { secrets: [OPENAI_API_KEY], cors: true },
  async (req, res) => {
    const uid = await requireUid(req);
    const days = Math.min(30, Number(req.query.days ?? 14));

    // Read pre-aggregated daily summaries the client/triggers maintain.
    const since = new Date(Date.now() - days * 86_400_000).toISOString().slice(0, 10);
    const snap = await db
      .collection(`users/${uid}/dailySummaries`)
      .where("date", ">=", since)
      .get();
    const dayLogs = snap.docs.map((d) => d.data() as DayLog);

    try {
      const result = await generateInsights(dayLogs, openaiClient());
      res.json({ insights: result });
    } catch (e) {
      console.error("insights failed", e);
      res.json({ insights: [] });
    }
  },
);
