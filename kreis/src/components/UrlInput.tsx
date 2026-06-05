'use client'

import { useState, useTransition } from 'react'
import { useRouter } from 'next/navigation'
import { clsx } from 'clsx'

const PROGRESS_STEPS = [
  { label: 'Launching headless browser…',   pct: 15 },
  { label: 'Scraping page structure…',       pct: 35 },
  { label: 'Measuring Core Web Vitals…',     pct: 55 },
  { label: 'Running AI CRO analysis…',       pct: 80 },
  { label: 'Generating fix recommendations…',pct: 95 },
]

export function UrlInput() {
  const router = useRouter()
  const [, startTransition] = useTransition()

  const [url,         setUrl]         = useState('')
  const [loading,     setLoading]     = useState(false)
  const [stepIndex,   setStepIndex]   = useState(0)
  const [error,       setError]       = useState<string | null>(null)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)

    let cleanUrl = url.trim()
    if (!cleanUrl) return

    // Auto-prepend https:// if the user didn't type a scheme
    if (!/^https?:\/\//i.test(cleanUrl)) cleanUrl = `https://${cleanUrl}`

    setLoading(true)
    setStepIndex(0)

    // Cycle through progress messages while the request is in-flight
    const interval = setInterval(() => {
      setStepIndex((prev) => Math.min(prev + 1, PROGRESS_STEPS.length - 1))
    }, 6000)

    try {
      const res = await fetch('/api/audit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: cleanUrl }),
      })

      const data = await res.json()

      if (!res.ok) {
        throw new Error(data.error ?? `Request failed (${res.status})`)
      }

      startTransition(() => {
        router.push(`/audit/${data.auditId}`)
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong')
      setLoading(false)
    } finally {
      clearInterval(interval)
    }
  }

  const currentStep = PROGRESS_STEPS[Math.min(stepIndex, PROGRESS_STEPS.length - 1)]

  return (
    <div className="w-full max-w-2xl mx-auto space-y-4">
      <form onSubmit={handleSubmit} className="relative">
        <div className={clsx(
          'flex items-center rounded-2xl border bg-surface-800 overflow-hidden transition-all duration-300',
          loading
            ? 'border-brand-500/60 shadow-[0_0_30px_rgba(99,102,241,0.25)]'
            : 'border-white/10 hover:border-brand-500/40 focus-within:border-brand-500/70 focus-within:shadow-[0_0_30px_rgba(99,102,241,0.15)]'
        )}>
          {/* Globe icon */}
          <span className="pl-4 text-slate-500 text-lg select-none">🌐</span>

          <input
            type="text"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            placeholder="yourstore.myshopify.com"
            disabled={loading}
            className={clsx(
              'flex-1 bg-transparent px-3 py-4 text-base text-white placeholder-slate-600',
              'outline-none disabled:opacity-60'
            )}
            aria-label="Store URL"
            autoFocus
            autoComplete="off"
            spellCheck={false}
          />

          <button
            type="submit"
            disabled={loading || !url.trim()}
            className={clsx(
              'mx-2 px-6 py-2.5 rounded-xl font-bold text-sm transition-all duration-200 shrink-0',
              loading || !url.trim()
                ? 'bg-surface-600 text-slate-500 cursor-not-allowed'
                : 'bg-brand-600 hover:bg-brand-500 text-white active:scale-95 shadow-lg shadow-brand-600/30'
            )}
          >
            {loading ? <Spinner /> : 'Run Free Audit →'}
          </button>
        </div>
      </form>

      {/* Progress feedback */}
      {loading && (
        <div className="bg-surface-800 border border-white/8 rounded-xl p-4 space-y-2.5 animate-fade-up">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>{currentStep.label}</span>
            <span className="font-mono tabular-nums">{currentStep.pct}%</span>
          </div>
          <div className="h-1.5 bg-surface-600 rounded-full overflow-hidden">
            <div
              className="h-full bg-gradient-to-r from-brand-600 to-brand-400 rounded-full transition-all duration-[2000ms] ease-out"
              style={{ width: `${currentStep.pct}%` }}
            />
          </div>
          <p className="text-[11px] text-slate-600">
            This takes ~30–45 s on the first run. Please keep this tab open.
          </p>
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="bg-red-500/10 border border-red-500/30 rounded-xl px-4 py-3 text-sm text-red-400 animate-fade-up">
          {error}
        </div>
      )}
    </div>
  )
}

function Spinner() {
  return (
    <svg className="animate-spin h-4 w-4 mx-2" viewBox="0 0 24 24" fill="none">
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
    </svg>
  )
}
