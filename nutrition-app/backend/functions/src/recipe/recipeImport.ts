import * as cheerio from "cheerio";

/**
 * Recipe import via URL. Strategy:
 *  1. Fetch the page and parse schema.org/Recipe JSON-LD (most sites expose it).
 *  2. Extract ingredients + yield (servings).
 *  3. (Caller) map ingredients to nutrition via OFF / an LLM and divide by servings.
 *
 * This file handles the scraping + structured extraction; macro resolution per
 * ingredient is delegated so it can be cached and reused.
 */

export interface ParsedRecipe {
  title: string;
  servings: number;
  ingredients: string[];
  sourceUrl: string;
}

function asArray<T>(v: T | T[] | undefined): T[] {
  if (v === undefined) return [];
  return Array.isArray(v) ? v : [v];
}

function findRecipeNode(json: unknown): Record<string, unknown> | null {
  const nodes: unknown[] = [];
  const visit = (n: unknown) => {
    if (Array.isArray(n)) n.forEach(visit);
    else if (n && typeof n === "object") {
      nodes.push(n);
      const graph = (n as Record<string, unknown>)["@graph"];
      if (graph) visit(graph);
    }
  };
  visit(json);
  return (
    (nodes.find((n) => {
      const type = (n as Record<string, unknown>)["@type"];
      return asArray(type as string).includes("Recipe");
    }) as Record<string, unknown>) ?? null
  );
}

function parseServings(node: Record<string, unknown>): number {
  const y = node.recipeYield ?? node.yield;
  const raw = asArray(y as string | number)[0];
  const match = String(raw ?? "").match(/\d+/);
  return match ? Math.max(1, parseInt(match[0], 10)) : 1;
}

export async function importRecipe(url: string): Promise<ParsedRecipe | null> {
  const res = await fetch(url, { headers: { "User-Agent": "NutriSnap/0.1" } });
  if (!res.ok) return null;
  const html = await res.text();
  const $ = cheerio.load(html);

  let recipeNode: Record<string, unknown> | null = null;
  $('script[type="application/ld+json"]').each((_, el) => {
    if (recipeNode) return;
    try {
      const node = findRecipeNode(JSON.parse($(el).contents().text()));
      if (node) recipeNode = node;
    } catch {
      /* ignore malformed JSON-LD blocks */
    }
  });

  if (!recipeNode) return null;
  const node = recipeNode as Record<string, unknown>;

  const ingredients = asArray(node.recipeIngredient as string | string[])
    .map((s) => String(s).trim())
    .filter(Boolean);
  if (ingredients.length === 0) return null;

  return {
    title: String(node.name ?? "Imported recipe"),
    servings: parseServings(node),
    ingredients,
    sourceUrl: url,
  };
}
