/**
 * GET /api/audit/[auditId]
 * Returns the current state of an audit — used for polling and deep-linking.
 */

import { NextRequest, NextResponse } from 'next/server'
import { createAdminClient } from '@/lib/supabase'

export async function GET(
  _req: NextRequest,
  { params }: { params: { auditId: string } }
) {
  const { auditId } = params

  if (!auditId || auditId.length < 10) {
    return NextResponse.json({ error: 'Invalid audit ID' }, { status: 400 })
  }

  const db = createAdminClient()

  const { data, error } = await db
    .from('audits')
    .select('id, url, status, health_score, summary, issues, error_message, created_at, completed_at')
    .eq('id', auditId)
    .single()

  if (error || !data) {
    return NextResponse.json({ error: 'Audit not found' }, { status: 404 })
  }

  return NextResponse.json(data)
}
