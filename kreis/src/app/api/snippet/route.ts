/**
 * GET /api/snippet?token=<snippet_token>
 *
 * Called by the client-side snippet.js installed on merchant Shopify stores.
 * Returns JSON array of active fixes the snippet should apply.
 *
 * Security model:
 *   — token is a 64-char random hex string (256-bit entropy), NOT user credentials
 *   — only returns fixes that have status='applied' for the matched store
 *   — responses are safe-to-cache for 60 s (CDN edge caching keeps origin load minimal)
 *   — CORS: allow all origins so cross-origin Shopify stores can fetch freely
 */

import { NextRequest, NextResponse } from 'next/server'
import { createAdminClient } from '@/lib/supabase'
import type { SnippetFix } from '@/lib/types'

export async function GET(req: NextRequest) {
  const token = req.nextUrl.searchParams.get('token') ?? ''

  // 64-char hex = 32 bytes = 256-bit token; reject anything else immediately
  if (!/^[0-9a-f]{64}$/.test(token)) {
    return NextResponse.json({ error: 'Invalid token' }, { status: 401 })
  }

  const db = createAdminClient()

  // Look up the store by its snippet token
  const { data: store } = await db
    .from('stores')
    .select('id')
    .eq('snippet_token', token)
    .single()

  if (!store) {
    return NextResponse.json({ error: 'Store not found' }, { status: 401 })
  }

  // Fetch all currently-active fixes for this store
  const { data: fixes } = await db
    .from('ai_fixes')
    .select('id, suggested_fix_css, suggested_fix_copy, selector_hint, issue_type')
    .eq('store_id', store.id)
    .eq('status', 'applied')

  // Split each fix into typed SnippetFix entries (CSS and copy are separate operations)
  const snippetFixes: SnippetFix[] = (fixes ?? []).flatMap((fix) => {
    const out: SnippetFix[] = []

    if (fix.suggested_fix_css) {
      out.push({ id: `${fix.id}-css`, type: 'css', css: fix.suggested_fix_css })
    }
    if (fix.suggested_fix_copy && fix.selector_hint) {
      out.push({
        id:       `${fix.id}-copy`,
        type:     'copy',
        selector: fix.selector_hint,
        text:     fix.suggested_fix_copy,
      })
    }

    return out
  })

  return NextResponse.json(snippetFixes, {
    headers: {
      'Access-Control-Allow-Origin': '*',
      'Cache-Control':               'public, s-maxage=60, stale-while-revalidate=300',
    },
  })
}

// Pre-flight for browser CORS
export async function OPTIONS() {
  return new Response(null, {
    headers: {
      'Access-Control-Allow-Origin':  '*',
      'Access-Control-Allow-Methods': 'GET, OPTIONS',
    },
  })
}
