'use client'

import { useState } from 'react'
import { clsx } from 'clsx'
import { ScoreGauge } from './ScoreGauge'
import { IssueCard }  from './IssueCard'
import type { AuditIssue, Severity } from '@/lib/types'

interface Props {
  auditId:     string
  url:         string
  healthScore: number
  summary:     string
  issues:      AuditIssue[]
  completedAt: string | null
  isPro?:      boolean
}

const SEVERITY_ORDER: Severity[] = ['high', 'medium', 'low']

export function AuditResults({
  auditId,
  url,
  healthScore,
  summary,
  issues,
  completedAt,
  isPro = false,
}: Props) {
  const [filter, setFilter] = useState<Severity | 'all'>('all')

  const grouped = SEVERITY_ORDER.reduce<Record<Severity, AuditIssue[]>>(
    (acc, sev) => {
      acc[sev] = issues.filter((i) => i.severity === sev)
      return acc
    },
    { high: [], medium: [], low: [] }
  )

  const visible = filter === 'all'
    ? issues
    : grouped[filter]

  const counts = {
    high:   grouped.high.length,
    medium: grouped.medium.length,
    low:    grouped.low.length,
  }

  function handleApplyFix(issueId: string) {
    // POST /api/fix/[fixId]  — for MVP we use issue.id as the fix ID
    return fetch(`/api/fix/${issueId}`, { method: 'POST' })
  }

  return (
    <div className="min-h-screen bg-surface-900 text-white">
      <div className="max-w-4xl mx-auto px-4 py-10 space-y-10">

        {/* ── Page header ─────────────────────────────────────────────────── */}
        <div className="flex items-center justify-between gap-4 flex-wrap">
          <div>
            <p className="text-xs text-slate-500 uppercase tracking-widest mb-1 font-semibold">
              Conversion Audit
            </p>
            <h1 className="text-2xl font-bold text-white truncate max-w-lg">{url}</h1>
            {completedAt && (
              <p className="text-xs text-slate-500 mt-1">
                {new Date(completedAt).toLocaleString()}
              </p>
            )}
          </div>
          <button
            onClick={() => { window.location.href = '/' }}
            className="text-sm text-slate-400 hover:text-white border border-white/10 hover:border-white/30 px-4 py-2 rounded-lg transition-colors"
          >
            ← New Audit
          </button>
        </div>

        {/* ── Score + summary card ─────────────────────────────────────────── */}
        <div className="rounded-2xl border border-white/8 bg-surface-800 p-6 md:p-8">
          <div className="flex flex-col md:flex-row items-center gap-8">
            <ScoreGauge score={healthScore} size={200} />

            <div className="flex-1 space-y-4">
              <div>
                <h2 className="text-lg font-bold text-white mb-2">Conversion Health Score</h2>
                <p className="text-slate-300 leading-relaxed">{summary}</p>
              </div>

              {/* Quick-count breakdown */}
              <div className="grid grid-cols-3 gap-3">
                {(['high', 'medium', 'low'] as Severity[]).map((sev) => (
                  <div
                    key={sev}
                    className={clsx(
                      'rounded-xl p-3 text-center cursor-pointer transition-all',
                      sev === 'high'   && 'bg-red-500/10    border border-red-500/20   hover:border-red-500/50',
                      sev === 'medium' && 'bg-orange-500/10 border border-orange-500/20 hover:border-orange-500/50',
                      sev === 'low'    && 'bg-yellow-500/10 border border-yellow-500/20 hover:border-yellow-500/50',
                    )}
                    onClick={() => setFilter(filter === sev ? 'all' : sev)}
                  >
                    <p className={clsx(
                      'text-2xl font-bold',
                      sev === 'high'   && 'text-red-400',
                      sev === 'medium' && 'text-orange-400',
                      sev === 'low'    && 'text-yellow-400',
                    )}>
                      {counts[sev]}
                    </p>
                    <p className="text-xs text-slate-500 capitalize mt-0.5">{sev}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* ── Filter tabs ──────────────────────────────────────────────────── */}
        <div className="flex items-center gap-2 overflow-x-auto pb-1">
          {(['all', 'high', 'medium', 'low'] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setFilter(tab)}
              className={clsx(
                'px-4 py-1.5 rounded-full text-sm font-medium transition-all shrink-0',
                filter === tab
                  ? 'bg-brand-600 text-white'
                  : 'text-slate-400 hover:text-white hover:bg-surface-700'
              )}
            >
              {tab === 'all' ? `All Issues (${issues.length})` : `${tab.charAt(0).toUpperCase() + tab.slice(1)} (${counts[tab]})`}
            </button>
          ))}
        </div>

        {/* ── Issue list ───────────────────────────────────────────────────── */}
        <div className="space-y-3">
          {visible.length === 0 ? (
            <p className="text-slate-500 text-sm py-8 text-center">No issues at this severity level.</p>
          ) : (
            visible.map((issue, i) => (
              <IssueCard
                key={issue.id}
                issue={issue}
                index={i}
                onApplyFix={handleApplyFix}
                isPro={isPro}
              />
            ))
          )}
        </div>

        {/* ── Upgrade CTA (free tier) ──────────────────────────────────────── */}
        {!isPro && (
          <div className="rounded-2xl border border-brand-500/30 bg-gradient-to-br from-brand-900/40 to-surface-800 p-6 md:p-8 text-center space-y-3">
            <p className="text-xl font-bold">Auto-fix every issue in one click</p>
            <p className="text-slate-400 max-w-md mx-auto">
              Upgrade to Pro and Kreis will automatically apply CSS fixes, rewrite
              conversion copy, and push changes live to your store — no developer needed.
            </p>
            <a
              href="/pricing"
              className="inline-block mt-2 px-8 py-3 bg-brand-600 hover:bg-brand-500 text-white font-bold rounded-xl transition-colors"
            >
              Upgrade to Pro →
            </a>
          </div>
        )}

        {/* ── Audit ID (for support) ────────────────────────────────────────── */}
        <p className="text-[11px] text-slate-600 text-center">
          Audit ID: <code className="font-mono">{auditId}</code>
        </p>
      </div>
    </div>
  )
}
