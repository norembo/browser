// ─────────────────────────────────────────────────────────────────────────────
// Kreis — shared TypeScript types
// ─────────────────────────────────────────────────────────────────────────────

export type Plan     = 'free' | 'pro' | 'agency'
export type Platform = 'shopify' | 'woocommerce' | 'magento' | 'custom'
export type AuditStatus = 'pending' | 'scraping' | 'analyzing' | 'complete' | 'failed'
export type Severity    = 'high' | 'medium' | 'low'
export type FixStatus   = 'pending' | 'applied' | 'reverted' | 'dismissed'

export type IssueType =
  | 'hero_copy'
  | 'cta_weakness'
  | 'trust_signals'
  | 'page_speed'
  | 'mobile_ux'
  | 'navigation'
  | 'social_proof'
  | 'checkout_friction'
  | 'value_proposition'
  | 'visual_hierarchy'
  | 'accessibility'

// ─── Scraper output ───────────────────────────────────────────────────────────

export interface CTAButton {
  text: string
  tagName: string
  backgroundColor: string
  color: string
  fontSize: string
  href?: string
}

export interface ScrapedData {
  url: string
  title: string
  metaDescription: string | null
  hasViewportMeta: boolean
  headings: {
    h1: string[]
    h2: string[]
    h3: string[]
  }
  ctaButtons: CTAButton[]
  images: {
    total: number
    withoutAlt: number
  }
  performance: {
    loadTimeMs: number
    lcpMs: number | null
    tbtMs: number | null
  }
  wordCount: number
  navLinksCount: number
  screenshotBase64?: string
}

// ─── LLM analysis output ──────────────────────────────────────────────────────

export interface AuditIssue {
  id: string
  issue_type: IssueType
  severity: Severity
  impact_score: number          // 1–10
  description: string
  suggested_fix_css: string | null
  suggested_fix_copy: string | null
  selector_hint: string | null
}

export interface AnalysisResult {
  health_score: number          // 0–100
  summary: string
  issues: AuditIssue[]
}

// ─── Database row shapes ──────────────────────────────────────────────────────

export interface AuditRow {
  id: string
  store_id: string | null
  user_id: string
  url: string
  status: AuditStatus
  health_score: number | null
  summary: string | null
  scraped_data: ScrapedData | null
  issues: AuditIssue[] | null
  error_message: string | null
  created_at: string
  completed_at: string | null
}

export interface AiFixRow {
  id: string
  audit_id: string
  store_id: string | null
  issue_id: string
  issue_type: IssueType
  severity: Severity
  impact_score: number
  description: string
  suggested_fix_css: string | null
  suggested_fix_copy: string | null
  suggested_fix_js: string | null
  selector_hint: string | null
  status: FixStatus
  applied_at: string | null
  created_at: string
}

// ─── API payloads ─────────────────────────────────────────────────────────────

export interface CreateAuditPayload {
  url: string
}

export interface CreateAuditResponse {
  auditId: string
  healthScore: number
  summary: string
  issues: AuditIssue[]
}

// Payload returned to the client-side JS snippet installed on merchant stores
export interface SnippetFix {
  id: string
  type: 'css' | 'copy'
  css?: string
  selector?: string
  text?: string
}
