'use client'

import { useState } from 'react'
import { clsx } from 'clsx'
import type { AuditIssue } from '@/lib/types'

interface Props {
  issue: AuditIssue
  index: number
  onApplyFix?: (issueId: string) => void
  isPro?: boolean
}

const SEVERITY_CONFIG = {
  high:   { label: 'HIGH',   bg: 'bg-red-500/15',    text: 'text-red-400',    border: 'border-red-500/30',    dot: 'bg-red-400' },
  medium: { label: 'MEDIUM', bg: 'bg-orange-500/15', text: 'text-orange-400', border: 'border-orange-500/30', dot: 'bg-orange-400' },
  low:    { label: 'LOW',    bg: 'bg-yellow-500/15', text: 'text-yellow-400', border: 'border-yellow-500/30', dot: 'bg-yellow-400' },
}

const ISSUE_TYPE_LABEL: Record<string, string> = {
  hero_copy:          'Hero Copy',
  cta_weakness:       'CTA Weakness',
  trust_signals:      'Trust Signals',
  page_speed:         'Page Speed',
  mobile_ux:          'Mobile UX',
  navigation:         'Navigation',
  social_proof:       'Social Proof',
  checkout_friction:  'Checkout Friction',
  value_proposition:  'Value Proposition',
  visual_hierarchy:   'Visual Hierarchy',
  accessibility:      'Accessibility',
}

export function IssueCard({ issue, index, onApplyFix, isPro = false }: Props) {
  const [expanded,  setExpanded]  = useState(false)
  const [applying,  setApplying]  = useState(false)
  const [applied,   setApplied]   = useState(false)
  const [showGate,  setShowGate]  = useState(false)

  const sev = SEVERITY_CONFIG[issue.severity]

  async function handleApply() {
    if (!isPro) { setShowGate(true); return }

    setApplying(true)
    await onApplyFix?.(issue.id)
    setApplied(true)
    setApplying(false)
  }

  const hasFixPreview = issue.suggested_fix_css || issue.suggested_fix_copy

  return (
    <div
      className={clsx(
        'rounded-xl border bg-surface-700 transition-all duration-200',
        'hover:border-brand-500/40 hover:bg-surface-600/80',
        sev.border,
        'animate-fade-up'
      )}
      style={{ animationDelay: `${index * 60}ms`, animationFillMode: 'both', opacity: 0 }}
    >
      {/* ── Header row ───────────────────────────────────────────────────────── */}
      <div
        className="flex items-start gap-3 p-4 cursor-pointer select-none"
        onClick={() => setExpanded((v) => !v)}
      >
        {/* Impact score bar */}
        <div className="flex flex-col items-center gap-1 shrink-0 mt-0.5">
          <div className="w-8 h-8 rounded-lg bg-surface-600 flex items-center justify-center">
            <span className={clsx('text-sm font-bold', sev.text)}>{issue.impact_score}</span>
          </div>
          <span className="text-[9px] text-slate-500 uppercase tracking-wider">impact</span>
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap mb-1.5">
            {/* Severity badge */}
            <span className={clsx('inline-flex items-center gap-1.5 text-[10px] font-bold tracking-wider px-2 py-0.5 rounded-full uppercase', sev.bg, sev.text)}>
              <span className={clsx('w-1.5 h-1.5 rounded-full', sev.dot)} />
              {sev.label}
            </span>
            {/* Issue type chip */}
            <span className="text-[11px] text-slate-400 bg-surface-600 px-2 py-0.5 rounded-full">
              {ISSUE_TYPE_LABEL[issue.issue_type] ?? issue.issue_type}
            </span>
          </div>

          <p className="text-sm text-slate-200 leading-snug">{issue.description}</p>
        </div>

        {/* Chevron */}
        <span className={clsx('text-slate-500 transition-transform duration-200 shrink-0 mt-1', expanded && 'rotate-180')}>
          ▾
        </span>
      </div>

      {/* ── Expanded detail ────────────────────────────────────────────────── */}
      {expanded && hasFixPreview && (
        <div className="border-t border-white/5 px-4 pb-4 pt-3 space-y-3">
          {/* CSS fix preview */}
          {issue.suggested_fix_css && (
            <div>
              <p className="text-[11px] uppercase tracking-wider text-slate-500 mb-1.5 font-semibold">
                CSS Fix
              </p>
              <pre className="bg-surface-900 rounded-lg p-3 text-[11px] text-brand-400 font-mono overflow-x-auto leading-relaxed whitespace-pre-wrap">
                {issue.suggested_fix_css}
              </pre>
            </div>
          )}

          {/* Copy fix preview */}
          {issue.suggested_fix_copy && (
            <div>
              <p className="text-[11px] uppercase tracking-wider text-slate-500 mb-1.5 font-semibold">
                Copy Fix
              </p>
              <div className="bg-surface-900 rounded-lg p-3 text-sm text-emerald-300 italic border border-emerald-500/20">
                &ldquo;{issue.suggested_fix_copy}&rdquo;
              </div>
            </div>
          )}

          {/* Selector hint */}
          {issue.selector_hint && (
            <p className="text-[11px] text-slate-500">
              Target: <code className="text-slate-400 bg-surface-900 px-1.5 py-0.5 rounded font-mono">{issue.selector_hint}</code>
            </p>
          )}

          {/* Apply button */}
          <div className="pt-1">
            {applied ? (
              <div className="flex items-center gap-2 text-sm text-emerald-400 font-medium">
                <span>✓</span> Fix applied — changes live on your store
              </div>
            ) : (
              <>
                <button
                  onClick={handleApply}
                  disabled={applying}
                  className={clsx(
                    'inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold transition-all',
                    isPro
                      ? 'bg-brand-600 hover:bg-brand-500 text-white active:scale-95'
                      : 'bg-surface-600 text-slate-400 hover:bg-surface-500 cursor-pointer',
                    applying && 'opacity-60 cursor-wait'
                  )}
                >
                  {applying ? (
                    <><Spinner /> Applying…</>
                  ) : isPro ? (
                    '⚡ Apply Fix Automatically'
                  ) : (
                    '🔒 Apply Fix  ·  Upgrade to Pro'
                  )}
                </button>

                {showGate && (
                  <p className="text-xs text-slate-400 mt-2">
                    Auto-applying fixes requires a <a href="/pricing" className="text-brand-400 underline">Pro plan</a>.
                    Your suggested fix is shown above — you can apply it manually.
                  </p>
                )}
              </>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

function Spinner() {
  return (
    <svg className="animate-spin h-3.5 w-3.5" viewBox="0 0 24 24" fill="none">
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
    </svg>
  )
}
