import type { Metadata } from 'next'
import { UrlInput } from '@/components/UrlInput'

export const metadata: Metadata = {
  title: 'Free AI Conversion Audit — Kreis',
}

const TRUST_STATS = [
  { value: '10,000+', label: 'Audits run' },
  { value: '23%',     label: 'Avg. conversion uplift' },
  { value: '< 60s',   label: 'Time to first insight' },
]

const FEATURES = [
  {
    icon: '🔬',
    title: 'Deep page analysis',
    body:  'Playwright extracts your real DOM, CTAs, headings, images, and Core Web Vitals — not just a static HTML fetch.',
  },
  {
    icon: '🧠',
    title: 'LLM-powered diagnosis',
    body:  'Claude identifies every conversion barrier with severity, impact score, and an exact CSS or copy fix — no vague recommendations.',
  },
  {
    icon: '⚡',
    title: 'One-click auto-fix',
    body:  'Pro users inject a lightweight JS snippet. Kreis pushes live fixes to your store — no Shopify developer, no code deployments.',
  },
  {
    icon: '🔒',
    title: 'Safe & non-destructive',
    body:  'All fixes are CSS-only or text content changes. Zero eval(), zero innerHTML, fully reversible in one click.',
  },
]

export default function LandingPage() {
  return (
    <main className="min-h-screen bg-surface-900 overflow-x-hidden">
      {/* ── Gradient background ──────────────────────────────────────────── */}
      <div
        className="absolute inset-0 pointer-events-none"
        aria-hidden
        style={{
          background:
            'radial-gradient(ellipse 80% 50% at 50% -10%, rgba(99,102,241,0.18) 0%, transparent 70%)',
        }}
      />

      {/* ── Nav ─────────────────────────────────────────────────────────── */}
      <nav className="relative z-10 flex items-center justify-between px-6 py-5 max-w-6xl mx-auto">
        <div className="flex items-center gap-2">
          <span className="text-2xl select-none">◎</span>
          <span className="font-bold text-white text-lg tracking-tight">Kreis</span>
          <span className="text-[10px] text-brand-400 bg-brand-500/15 px-2 py-0.5 rounded-full font-semibold uppercase tracking-wider ml-1">
            Beta
          </span>
        </div>
        <a
          href="/pricing"
          className="text-sm text-slate-400 hover:text-white transition-colors"
        >
          Pricing
        </a>
      </nav>

      {/* ── Hero ─────────────────────────────────────────────────────────── */}
      <section className="relative z-10 flex flex-col items-center text-center px-4 pt-16 pb-20">
        <div className="inline-flex items-center gap-2 bg-brand-500/10 border border-brand-500/25 px-4 py-1.5 rounded-full text-sm text-brand-300 mb-8 font-medium">
          <span className="w-2 h-2 rounded-full bg-brand-400 animate-pulse-slow" />
          Free for Shopify stores · No account required
        </div>

        <h1 className="text-4xl sm:text-5xl md:text-6xl font-extrabold text-white max-w-3xl leading-tight text-balance mb-6">
          Your store is leaking{' '}
          <span className="text-transparent bg-clip-text bg-gradient-to-r from-brand-400 to-violet-400">
            conversions.
          </span>
          <br />
          We&apos;ll find every leak.
        </h1>

        <p className="text-lg text-slate-400 max-w-xl leading-relaxed mb-10 text-balance">
          Enter your store URL and get a free, AI-powered CRO audit in under 60 seconds.
          Every issue comes with a severity score and an exact, ready-to-apply fix.
        </p>

        <UrlInput />

        <p className="text-xs text-slate-600 mt-5">
          No sign-up required · Works on any e-commerce URL · Results saved for 30 days
        </p>
      </section>

      {/* ── Trust stats ──────────────────────────────────────────────────── */}
      <section className="relative z-10 flex justify-center gap-8 md:gap-16 flex-wrap px-4 pb-16">
        {TRUST_STATS.map(({ value, label }) => (
          <div key={label} className="text-center">
            <p className="text-3xl font-extrabold text-white">{value}</p>
            <p className="text-sm text-slate-500 mt-1">{label}</p>
          </div>
        ))}
      </section>

      {/* ── Features ─────────────────────────────────────────────────────── */}
      <section className="relative z-10 max-w-5xl mx-auto px-4 pb-24">
        <h2 className="text-center text-2xl font-bold text-white mb-10">
          What you get in your free audit
        </h2>

        <div className="grid sm:grid-cols-2 gap-4">
          {FEATURES.map(({ icon, title, body }) => (
            <div
              key={title}
              className="rounded-2xl border border-white/8 bg-surface-800 p-6 hover:border-brand-500/30 transition-colors"
            >
              <div className="text-3xl mb-3">{icon}</div>
              <h3 className="font-bold text-white mb-2">{title}</h3>
              <p className="text-sm text-slate-400 leading-relaxed">{body}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ── Footer ───────────────────────────────────────────────────────── */}
      <footer className="relative z-10 border-t border-white/5 py-8 text-center text-xs text-slate-600 px-4">
        © {new Date().getFullYear()} Kreis · Built for e-commerce founders who hate leaking money.
      </footer>
    </main>
  )
}
