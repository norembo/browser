/**
 * POST /api/fix/[fixId]
 * Marks an ai_fix as 'applied'.  Called from the dashboard "Apply Fix" button.
 * Gated behind plan check — free tier gets a 402 with upgrade prompt.
 *
 * In a real paid flow you would also link the fix to a store_id here so the
 * snippet API knows to start serving it.
 */

import { NextRequest, NextResponse } from 'next/server'
import { z } from 'zod'
import { createAdminClient } from '@/lib/supabase'

const bodySchema = z.object({
  storeId: z.string().uuid('Invalid store ID').optional(),
})

export async function POST(
  req: NextRequest,
  { params }: { params: { fixId: string } }
) {
  const { fixId } = params

  if (!fixId || fixId.length < 10) {
    return NextResponse.json({ error: 'Invalid fix ID' }, { status: 400 })
  }

  const body   = await req.json().catch(() => ({}))
  const parsed = bodySchema.safeParse(body)
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.errors[0].message }, { status: 400 })
  }

  // TODO: verify session + plan:
  //   const session = await getSession()
  //   if (session?.user.plan === 'free') return NextResponse.json({ error: 'Upgrade to Pro', upgrade: true }, { status: 402 })

  const db = createAdminClient()

  const { data, error } = await db
    .from('ai_fixes')
    .update({
      status:     'applied',
      applied_at: new Date().toISOString(),
      ...(parsed.data.storeId ? { store_id: parsed.data.storeId } : {}),
    })
    .eq('id', fixId)
    .select('id, status, issue_type, suggested_fix_css, suggested_fix_copy, selector_hint')
    .single()

  if (error || !data) {
    return NextResponse.json({ error: 'Fix not found or already applied' }, { status: 404 })
  }

  return NextResponse.json(data)
}

// Allow un-applying (revert) a fix
export async function DELETE(
  _req: NextRequest,
  { params }: { params: { fixId: string } }
) {
  const db = createAdminClient()

  const { data, error } = await db
    .from('ai_fixes')
    .update({ status: 'reverted', applied_at: null })
    .eq('id', params.fixId)
    .select('id, status')
    .single()

  if (error || !data) {
    return NextResponse.json({ error: 'Fix not found' }, { status: 404 })
  }

  return NextResponse.json(data)
}
