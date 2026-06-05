/**
 * POST /api/audit
 *
 * Orchestrates the full audit pipeline:
 *   1. Validate URL
 *   2. Create audit row (status: 'pending')
 *   3. Scrape the URL with Playwright
 *   4. Analyze scraped data with LLM
 *   5. Persist results + create ai_fixes rows
 *   6. Return { auditId, healthScore, summary, issues }
 *
 * This is synchronous for the MVP (~25–40 s per request).
 * For production scale: move scrape+analyze into a job queue (BullMQ / Inngest)
 * and return the auditId immediately, then poll GET /api/audit/[auditId].
 */

import { NextRequest, NextResponse } from 'next/server'
import { z } from 'zod'
import { createAdminClient } from '@/lib/supabase'
import { scrapeUrl }         from '@/lib/scraper'
import { analyze }           from '@/lib/analyzer'

const schema = z.object({
  url: z.string().url('Please enter a valid URL (include https://)'),
})

export const maxDuration = 60  // Vercel Pro / Edge config max request duration (seconds)

export async function POST(req: NextRequest) {
  // ── 1. Validate input ──────────────────────────────────────────────────────
  const body   = await req.json().catch(() => null)
  const parsed = schema.safeParse(body)

  if (!parsed.success) {
    return NextResponse.json(
      { error: parsed.error.errors[0].message },
      { status: 400 }
    )
  }

  const { url } = parsed.data
  const db      = createAdminClient()

  // TODO: Replace 'anonymous' with real user ID from session once auth is wired:
  //   const { data: { session } } = await createCookieClient().auth.getSession()
  //   const userId = session?.user.id ?? 'anonymous'
  const userId = 'anonymous'

  // ── 2. Create pending audit row ────────────────────────────────────────────
  const { data: audit, error: insertError } = await db
    .from('audits')
    .insert({ url, user_id: userId, status: 'pending' })
    .select('id')
    .single()

  if (insertError || !audit) {
    console.error('[audit] insert failed:', insertError)
    return NextResponse.json({ error: 'Failed to create audit record' }, { status: 500 })
  }

  const auditId = audit.id

  try {
    // ── 3. Scrape ──────────────────────────────────────────────────────────
    await db.from('audits').update({ status: 'scraping' }).eq('id', auditId)

    const scraped = await scrapeUrl(url)

    // ── 4. Analyze ─────────────────────────────────────────────────────────
    await db
      .from('audits')
      .update({ status: 'analyzing', scraped_data: scraped })
      .eq('id', auditId)

    const result = await analyze(scraped)

    // ── 5. Persist results ─────────────────────────────────────────────────
    await db
      .from('audits')
      .update({
        status:       'complete',
        health_score: result.health_score,
        summary:      result.summary,
        issues:       result.issues,
        completed_at: new Date().toISOString(),
      })
      .eq('id', auditId)

    // Materialise each issue as its own ai_fixes row (enables per-fix application tracking)
    if (result.issues.length > 0) {
      await db.from('ai_fixes').insert(
        result.issues.map((issue) => ({
          audit_id:           auditId,
          issue_id:           issue.id,
          issue_type:         issue.issue_type,
          severity:           issue.severity,
          impact_score:       issue.impact_score,
          description:        issue.description,
          suggested_fix_css:  issue.suggested_fix_css,
          suggested_fix_copy: issue.suggested_fix_copy,
          selector_hint:      issue.selector_hint,
        }))
      )
    }

    // ── 6. Return ──────────────────────────────────────────────────────────
    return NextResponse.json({
      auditId,
      healthScore: result.health_score,
      summary:     result.summary,
      issues:      result.issues,
    })
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    console.error('[audit] pipeline failed:', message)

    await db
      .from('audits')
      .update({ status: 'failed', error_message: message })
      .eq('id', auditId)

    return NextResponse.json(
      { error: `Audit failed: ${message}` },
      { status: 500 }
    )
  }
}
