/**
 * Playwright-based scraper for Kreis CRO audits.
 *
 * Production deployment note:
 *   Vercel Serverless Functions do not support Playwright out of the box.
 *   Options for production:
 *     a) Deploy this API route to a VPS / Fly.io / Railway (recommended for MVP)
 *     b) Use @sparticuz/chromium + playwright-core for AWS Lambda / Vercel Pro
 *     c) Proxy to a dedicated scraping microservice
 */

import { chromium } from 'playwright'
import type { ScrapedData, CTAButton } from './types'

// Keywords that strongly signal a CTA element
const CTA_KEYWORDS = [
  'buy', 'add to cart', 'shop now', 'order', 'get started', 'try',
  'subscribe', 'checkout', 'purchase', 'claim', 'download', 'sign up',
  'start free', 'book', 'reserve', 'grab',
]

export async function scrapeUrl(targetUrl: string): Promise<ScrapedData> {
  const browser = await chromium.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage'],
  })

  try {
    const context = await browser.newContext({
      viewport: { width: 1440, height: 900 },
      userAgent:
        'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 ' +
        '(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    })

    const page = await context.newPage()

    const navigationStart = Date.now()

    await page.goto(targetUrl, {
      waitUntil: 'networkidle',
      timeout: 30_000,
    })

    const loadTimeMs = Date.now() - navigationStart

    // Collect Core Web Vitals by observing 2 seconds after load
    const vitals = await page.evaluate(() =>
      new Promise<{ lcpMs: number | null; tbtMs: number | null }>((resolve) => {
        let lcpMs: number | null = null
        let totalBlockingTime    = 0

        try {
          new PerformanceObserver((list) => {
            const entries = list.getEntries()
            lcpMs = entries[entries.length - 1].startTime
          }).observe({ type: 'largest-contentful-paint', buffered: true })

          new PerformanceObserver((list) => {
            for (const e of list.getEntries()) {
              // TBT = sum of (long-task duration − 50ms threshold)
              totalBlockingTime += Math.max(0, e.duration - 50)
            }
          }).observe({ type: 'longtask', buffered: true })
        } catch {
          // PerformanceObserver not supported — degrade gracefully
        }

        setTimeout(() => resolve({ lcpMs, tbtMs: totalBlockingTime || null }), 2000)
      })
    )

    // Extract all structural page data in a single evaluate call (minimises IPC)
    const pageData = await page.evaluate((keywords: string[]) => {
      const getText  = (el: Element) => (el.textContent ?? '').trim()
      const getStyle = (el: Element, prop: string) =>
        window.getComputedStyle(el).getPropertyValue(prop)

      const title          = document.title
      const metaDescription =
        document.querySelector('meta[name="description"]')?.getAttribute('content') ?? null
      const hasViewportMeta = !!document.querySelector('meta[name="viewport"]')

      const h1 = Array.from(document.querySelectorAll('h1')).map(getText).filter(Boolean)
      const h2 = Array.from(document.querySelectorAll('h2')).map(getText).filter(Boolean).slice(0, 10)
      const h3 = Array.from(document.querySelectorAll('h3')).map(getText).filter(Boolean).slice(0, 10)

      const ctaElements = Array.from(
        document.querySelectorAll('button, a[href], input[type="submit"], [role="button"]')
      )

      const ctaButtons: CTAButton[] = ctaElements
        .filter((el) => {
          const lower = getText(el).toLowerCase()
          return keywords.some((kw) => lower.includes(kw))
        })
        .slice(0, 12)
        .map((el) => ({
          text:            getText(el),
          tagName:         el.tagName.toLowerCase(),
          backgroundColor: getStyle(el, 'background-color'),
          color:           getStyle(el, 'color'),
          fontSize:        getStyle(el, 'font-size'),
          href:            el.getAttribute('href') ?? undefined,
        }))

      const images      = document.querySelectorAll('img')
      const withoutAlt  = Array.from(images).filter(
        (img) => !(img.getAttribute('alt') ?? '').trim()
      ).length

      // Strip non-content nodes before counting words
      const clone = document.body.cloneNode(true) as HTMLElement
      clone.querySelectorAll('script, style, noscript, svg, iframe').forEach((n) => n.remove())
      const wordCount = (clone.textContent ?? '').split(/\s+/).filter(Boolean).length

      const navLinksCount = document.querySelectorAll('nav a, header a').length

      return {
        title,
        metaDescription,
        hasViewportMeta,
        headings: { h1, h2, h3 },
        ctaButtons,
        images:   { total: images.length, withoutAlt },
        wordCount,
        navLinksCount,
      }
    }, CTA_KEYWORDS)

    // Above-the-fold screenshot (1440×900)
    const screenshotBuffer = await page.screenshot({ type: 'png', fullPage: false })
    const screenshotBase64 = screenshotBuffer.toString('base64')

    return {
      url: targetUrl,
      ...pageData,
      performance: {
        loadTimeMs,
        lcpMs: vitals.lcpMs,
        tbtMs: vitals.tbtMs,
      },
      screenshotBase64,
    }
  } finally {
    await browser.close()
  }
}
