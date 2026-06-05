import { createClient } from '@supabase/supabase-js'
import { createServerClient, type CookieOptions } from '@supabase/ssr'
import { cookies } from 'next/headers'

const url  = process.env.NEXT_PUBLIC_SUPABASE_URL!
const anon = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!
const svc  = process.env.SUPABASE_SERVICE_ROLE_KEY!

// Browser client — RLS enforced, safe to expose via NEXT_PUBLIC_ vars
export const createBrowserClient = () => createClient(url, anon)

// Server client — reads the Auth session from cookies, RLS enforced
export const createCookieClient = () => {
  const jar = cookies()
  return createServerClient(url, anon, {
    cookies: {
      get:    (name: string)                           => jar.get(name)?.value,
      set:    (name: string, value: string, opts: CookieOptions) => jar.set({ name, value, ...opts }),
      remove: (name: string, opts: CookieOptions)     => jar.set({ name, value: '', ...opts }),
    },
  })
}

// Admin client — bypasses RLS; use only inside server-side API routes
export const createAdminClient = () =>
  createClient(url, svc, { auth: { persistSession: false } })
