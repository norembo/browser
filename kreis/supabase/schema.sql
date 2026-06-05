-- ============================================================
-- Kreis — Supabase / PostgreSQL Schema
-- Run this in the Supabase SQL editor (Dashboard → SQL Editor)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ──────────────────────────────────────────────────────────────
-- 1. USERS  (shadow table extending auth.users)
-- ──────────────────────────────────────────────────────────────
CREATE TABLE public.users (
  id             UUID        REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
  email          TEXT        NOT NULL,
  plan           TEXT        NOT NULL DEFAULT 'free'
                             CHECK (plan IN ('free', 'pro', 'agency')),
  audit_credits  INT         NOT NULL DEFAULT 3,    -- free-tier monthly allowance
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
CREATE POLICY "users_select_own" ON public.users FOR SELECT USING (auth.uid() = id);
CREATE POLICY "users_update_own" ON public.users FOR UPDATE USING (auth.uid() = id);

-- ──────────────────────────────────────────────────────────────
-- 2. STORES  (one user → many stores)
-- ──────────────────────────────────────────────────────────────
CREATE TABLE public.stores (
  id                UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id           UUID        REFERENCES public.users(id) ON DELETE CASCADE NOT NULL,
  domain            TEXT        NOT NULL,
  platform          TEXT        NOT NULL DEFAULT 'shopify'
                                CHECK (platform IN ('shopify', 'woocommerce', 'magento', 'custom')),
  snippet_installed BOOLEAN     NOT NULL DEFAULT FALSE,
  -- 64-char hex token embedded in the client JS snippet; never the user's API key
  snippet_token     TEXT        NOT NULL UNIQUE DEFAULT encode(gen_random_bytes(32), 'hex'),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (user_id, domain)
);

CREATE INDEX idx_stores_user_id       ON public.stores (user_id);
CREATE INDEX idx_stores_snippet_token ON public.stores (snippet_token);

ALTER TABLE public.stores ENABLE ROW LEVEL SECURITY;
CREATE POLICY "stores_owner_all" ON public.stores USING (auth.uid() = user_id);

-- ──────────────────────────────────────────────────────────────
-- 3. AUDITS  (one store → many audits; anonymous audits allowed)
-- ──────────────────────────────────────────────────────────────
CREATE TABLE public.audits (
  id             UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
  store_id       UUID        REFERENCES public.stores(id) ON DELETE SET NULL,
  user_id        UUID        REFERENCES public.users(id) ON DELETE CASCADE NOT NULL,
  url            TEXT        NOT NULL,
  status         TEXT        NOT NULL DEFAULT 'pending'
                             CHECK (status IN ('pending', 'scraping', 'analyzing', 'complete', 'failed')),
  health_score   SMALLINT    CHECK (health_score BETWEEN 0 AND 100),
  summary        TEXT,
  scraped_data   JSONB,      -- raw playwright output (kept for re-analysis without re-scraping)
  issues         JSONB,      -- AuditIssue[] array — GIN-indexed for fast filtering
  error_message  TEXT,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  completed_at   TIMESTAMPTZ
);

CREATE INDEX idx_audits_user_id    ON public.audits (user_id);
CREATE INDEX idx_audits_store_id   ON public.audits (store_id);
CREATE INDEX idx_audits_status     ON public.audits (status);
CREATE INDEX idx_audits_created_at ON public.audits (created_at DESC);
CREATE INDEX idx_audits_issues_gin ON public.audits USING GIN (issues);

ALTER TABLE public.audits ENABLE ROW LEVEL SECURITY;
CREATE POLICY "audits_owner_all" ON public.audits USING (auth.uid() = user_id);

-- ──────────────────────────────────────────────────────────────
-- 4. AI_FIXES  (one audit → many fixes; tracks application state)
-- ──────────────────────────────────────────────────────────────
CREATE TABLE public.ai_fixes (
  id                  UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
  audit_id            UUID        REFERENCES public.audits(id)  ON DELETE CASCADE NOT NULL,
  store_id            UUID        REFERENCES public.stores(id)  ON DELETE CASCADE,
  issue_id            TEXT        NOT NULL,   -- stable snake_case id from LLM output
  issue_type          TEXT        NOT NULL,
  severity            TEXT        NOT NULL CHECK (severity IN ('high', 'medium', 'low')),
  impact_score        SMALLINT    NOT NULL CHECK (impact_score BETWEEN 1 AND 10),
  description         TEXT        NOT NULL,
  suggested_fix_css   TEXT,       -- injected via <style> tag by snippet.js
  suggested_fix_copy  TEXT,       -- applied via textContent by snippet.js (XSS-safe)
  suggested_fix_js    TEXT,       -- future: safe DOM mutations only, never eval()
  selector_hint       TEXT,       -- CSS selector for the target element
  status              TEXT        NOT NULL DEFAULT 'pending'
                                  CHECK (status IN ('pending', 'applied', 'reverted', 'dismissed')),
  applied_at          TIMESTAMPTZ,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_fixes_audit_id ON public.ai_fixes (audit_id);
CREATE INDEX idx_ai_fixes_store_id ON public.ai_fixes (store_id);
CREATE INDEX idx_ai_fixes_status   ON public.ai_fixes (status);

ALTER TABLE public.ai_fixes ENABLE ROW LEVEL SECURITY;
-- Access is granted via ownership of the parent audit
CREATE POLICY "ai_fixes_owner_all" ON public.ai_fixes
  USING (
    EXISTS (
      SELECT 1 FROM public.audits a
      WHERE a.id = audit_id AND a.user_id = auth.uid()
    )
  );

-- ──────────────────────────────────────────────────────────────
-- TRIGGERS
-- ──────────────────────────────────────────────────────────────

-- Auto-bump updated_at on every UPDATE
CREATE OR REPLACE FUNCTION public.fn_set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_users_updated_at
  BEFORE UPDATE ON public.users
  FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();

CREATE TRIGGER trg_stores_updated_at
  BEFORE UPDATE ON public.stores
  FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at();

-- Auto-create public.users row when a Supabase Auth user is created
CREATE OR REPLACE FUNCTION public.fn_handle_new_auth_user()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
  INSERT INTO public.users (id, email)
  VALUES (NEW.id, NEW.email)
  ON CONFLICT (id) DO NOTHING;
  RETURN NEW;
END;
$$;

CREATE TRIGGER trg_on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.fn_handle_new_auth_user();
