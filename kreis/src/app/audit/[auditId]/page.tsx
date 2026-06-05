/**
 * /audit/[auditId]  — Server Component
 *
 * Fetches the audit row from Supabase server-side and renders the results.
 * Because the POST /api/audit route is synchronous, by the time the user
 * arrives here the audit should always be 'complete' or 'failed'.
 */

import type { Metadata } from 'next'
import { notFound }      from 'next/navigation'
import { createAdminClient } from '@/lib/supabase'
import { AuditResults }      from '@/components/AuditResults'
import type { AuditRow }     from '@/lib/types'

interface Props {
  params: { auditId: string }
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  return {
    title: `Audit ${params.auditId.slice(0, 8)}… | Kreis`,
  }
}

export default async function AuditPage({ params }: Props) {
  const { auditId } = params

  if (!auditId || auditId.length < 10) notFound()

  const db = createAdminClient()

  const { data, error } = await db
    .from('audits')
    .select('id, url, status, health_score, summary, issues, error_message, created_at, completed_at')
    .eq('id', auditId)
    .single<AuditRow>()

  if (error || !data) notFound()

  // ── Failed state ────────────────────────────────────────────────────────────
  if (data.status === 'failed') {
    return (
      <div className="min-h-screen bg-surface-900 flex items-center justify-center px-4">
        <div className="max-w-md text-center space-y-4">
          <div className="text-4xl">⚠️</div>
          <h1 className="text-xl font-bold text-white">Audit Failed</h1>
          <p className="text-slate-400 text-sm">{data.error_message ?? 'An unexpected error occurred.'}</p>
          <a
            href="/"
            className="inline-block px-6 py-2.5 bg-brand-600 hover:bg-brand-500 text-white font-bold rounded-xl transition-colors"
          >
            Try Again
          </a>
        </div>
      </div>
    )
  }

  // ── Still processing (shouldn't normally happen, but handle gracefully) ─────
  if (data.status !== 'complete') {
    return (
      <div className="min-h-screen bg-surface-900 flex items-center justify-center px-4">
        <div className="text-center space-y-4">
          <div className="w-10 h-10 border-2 border-brand-500 border-t-transparent rounded-full animate-spin mx-auto" />
          <p className="text-slate-400">Still processing your audit…</p>
          <p className="text-xs text-slate-600">Refresh in a few seconds.</p>
        </div>
      </div>
    )
  }

  // ── Complete ─────────────────────────────────────────────────────────────────
  return (
    <AuditResults
      auditId={data.id}
      url={data.url}
      healthScore={data.health_score ?? 0}
      summary={data.summary ?? ''}
      issues={data.issues ?? []}
      completedAt={data.completed_at}
      isPro={false}
    />
  )
}
