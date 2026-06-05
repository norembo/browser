/**
 * LLM-powered CRO analyzer.
 * Supports Anthropic (default) and OpenAI — set LLM_PROVIDER env var to switch.
 */

import Anthropic from '@anthropic-ai/sdk'
import OpenAI    from 'openai'
import type { ScrapedData, AnalysisResult, AuditIssue } from './types'

// ─────────────────────────────────────────────────────────────────────────────
// System prompt — instructs the LLM to act as a CRO expert and return strict JSON
// ─────────────────────────────────────────────────────────────────────────────

const SYSTEM_PROMPT = `\
You are a world-class Conversion Rate Optimization (CRO) specialist with 15+ years of \
e-commerce experience across Shopify, DTC brands, and 8-figure stores.

Your task: analyze the provided page data and identify every conversion barrier present.

CRITICAL OUTPUT RULE: respond with ONLY a valid JSON object — no prose, no markdown \
fences, no explanation text. Any non-JSON output will break the pipeline.

Required output schema:
{
  "health_score": <integer 0–100>,
  "summary": <string ≤200 chars, direct and data-driven — cite specific numbers>,
  "issues": [
    {
      "id": <unique snake_case string e.g. "missing_trust_badges">,
      "issue_type": <one of: "hero_copy" | "cta_weakness" | "trust_signals" | "page_speed" | "mobile_ux" | "navigation" | "social_proof" | "checkout_friction" | "value_proposition" | "visual_hierarchy" | "accessibility">,
      "severity": <"high" | "medium" | "low">,
      "impact_score": <integer 1–10>,
      "description": <string ≤300 chars — reference actual content/numbers from the input>,
      "suggested_fix_css": <valid CSS string targeting real elements via selector_hint, or null>,
      "suggested_fix_copy": <exact replacement text string, not a description of what to write, or null>,
      "selector_hint": <best CSS selector to target the problematic element, or null>
    }
  ]
}

Scoring weights for health_score:
  Page speed  25% | CTA clarity    25% | Value proposition 20%
  Trust signals 15% | Mobile UX   15%

Issue count: return 5–12 issues, sorted by impact_score descending.

Severity thresholds:
  "high"   — directly kills conversions, >20% estimated impact; fix this week
  "medium" — significant friction, 10–20% impact; fix this sprint
  "low"    — polish / micro-optimization, <10% impact; backlog

CSS fix rules:
  — MUST use selector_hint as the target selector
  — MUST be valid, parseable CSS (no placeholders like "YOUR_COLOR")
  — Use specific properties: font-size, color, background-color, padding, border-radius, etc.
  — Never use blanket * selector or !important unless necessary

Copy fix rules:
  — Write the EXACT replacement string, ready to drop in
  — Keep the same approximate length as original unless length is itself the problem`

// ─────────────────────────────────────────────────────────────────────────────
// Build the user message from scraped data
// ─────────────────────────────────────────────────────────────────────────────

function buildPrompt(data: ScrapedData): string {
  return JSON.stringify(
    {
      url:                data.url,
      title:              data.title,
      meta_description:   data.metaDescription,
      has_viewport_meta:  data.hasViewportMeta,
      headings:           data.headings,
      cta_buttons:        data.ctaButtons,
      images:             data.images,
      performance: {
        load_time_ms:             data.performance.loadTimeMs,
        lcp_ms:                   data.performance.lcpMs,
        total_blocking_time_ms:   data.performance.tbtMs,
      },
      word_count:       data.wordCount,
      nav_links_count:  data.navLinksCount,
    },
    null,
    2
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// Parse + validate LLM JSON output
// ─────────────────────────────────────────────────────────────────────────────

function parseResult(raw: string): AnalysisResult {
  // Strip any accidental markdown fences the model might emit
  const clean = raw.replace(/^```(?:json)?\s*/i, '').replace(/\s*```$/i, '').trim()

  const parsed = JSON.parse(clean) as {
    health_score: unknown
    summary: unknown
    issues: unknown[]
  }

  if (typeof parsed.health_score !== 'number' || !Array.isArray(parsed.issues)) {
    throw new Error(`Unexpected LLM schema: ${JSON.stringify(parsed).slice(0, 200)}`)
  }

  return {
    health_score: Math.max(0, Math.min(100, Math.round(parsed.health_score))),
    summary: String(parsed.summary ?? ''),
    issues: (parsed.issues as AuditIssue[]).map((issue) => ({
      id:                  String(issue.id),
      issue_type:          issue.issue_type,
      severity:            issue.severity,
      impact_score:        Math.max(1, Math.min(10, Number(issue.impact_score))),
      description:         String(issue.description),
      suggested_fix_css:   issue.suggested_fix_css  ?? null,
      suggested_fix_copy:  issue.suggested_fix_copy ?? null,
      selector_hint:       issue.selector_hint      ?? null,
    })),
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Provider implementations
// ─────────────────────────────────────────────────────────────────────────────

async function analyzeWithAnthropic(data: ScrapedData): Promise<AnalysisResult> {
  const client = new Anthropic({ apiKey: process.env.ANTHROPIC_API_KEY })

  const response = await client.messages.create({
    model:      'claude-sonnet-4-6',
    max_tokens: 4096,
    system:     SYSTEM_PROMPT,
    messages:   [{ role: 'user', content: buildPrompt(data) }],
  })

  const block = response.content[0]
  const text  = block.type === 'text' ? block.text : ''
  return parseResult(text)
}

async function analyzeWithOpenAI(data: ScrapedData): Promise<AnalysisResult> {
  const client = new OpenAI({ apiKey: process.env.OPENAI_API_KEY })

  const response = await client.chat.completions.create({
    model:           'gpt-4o',
    response_format: { type: 'json_object' },
    max_tokens:      4096,
    messages: [
      { role: 'system', content: SYSTEM_PROMPT },
      { role: 'user',   content: buildPrompt(data) },
    ],
  })

  return parseResult(response.choices[0].message.content ?? '{}')
}

// ─────────────────────────────────────────────────────────────────────────────
// Public API — picks provider from environment
// ─────────────────────────────────────────────────────────────────────────────

export async function analyze(data: ScrapedData): Promise<AnalysisResult> {
  const provider = (process.env.LLM_PROVIDER ?? 'anthropic').toLowerCase()

  if (provider === 'openai') return analyzeWithOpenAI(data)
  return analyzeWithAnthropic(data)
}
