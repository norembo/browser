/**
 * Kreis Fix Applicator — snippet.js
 * Version: 1.0.0
 *
 * Installation (Shopify):
 *   1. In Shopify Admin → Online Store → Themes → Edit Code → theme.liquid
 *   2. Paste this script tag just before </body>:
 *
 *      <script
 *        src="https://your-kreis-domain.com/snippet.js"
 *        data-kreis-token="YOUR_64_CHAR_TOKEN"
 *        async
 *        defer
 *      ></script>
 *
 * Security design:
 *   - NO eval() — all CSS is injected via <style> elements
 *   - NO innerHTML — all copy changes use .textContent (XSS-safe)
 *   - Token is store-specific, low-entropy only for this store's fixes
 *   - Full HTTPS; HTTP requests are blocked at the origin
 *   - Mutations are idempotent and tracked by fix ID
 */

;(function (global) {
  'use strict'

  var KREIS_VERSION = '1.0.0'
  var API_BASE      = 'https://your-kreis-domain.com'  // Replace with your actual domain
  var ATTR_TOKEN    = 'data-kreis-token'
  var APPLIED_KEY   = '__kreis_applied__'

  // ── 1. Extract token from the script element itself ──────────────────────

  function getToken () {
    var scripts = document.querySelectorAll('script[' + ATTR_TOKEN + ']')
    for (var i = 0; i < scripts.length; i++) {
      var t = scripts[i].getAttribute(ATTR_TOKEN)
      if (t && /^[0-9a-f]{64}$/.test(t)) return t
    }
    return null
  }

  // ── 2. Fetch active fixes from the Kreis API ──────────────────────────────

  function fetchFixes (token, callback) {
    var xhr = new XMLHttpRequest()
    xhr.open('GET', API_BASE + '/api/snippet?token=' + token, true)
    xhr.timeout = 5000

    xhr.onload = function () {
      if (xhr.status === 200) {
        try {
          callback(JSON.parse(xhr.responseText))
        } catch (e) {
          // Malformed JSON — silently abort
        }
      }
    }

    xhr.onerror  = function () { /* network error — silent fail */ }
    xhr.ontimeout = function () { /* timeout — silent fail */ }

    xhr.send()
  }

  // ── 3. Apply a CSS fix — injects a deduplicated <style> tag ───────────────

  function applyCSSFix (fix) {
    var styleId = 'kreis-fix-' + fix.id
    if (document.getElementById(styleId)) return  // idempotent

    var style = document.createElement('style')
    style.id              = styleId
    style.setAttribute('data-kreis', KREIS_VERSION)
    style.setAttribute('data-fix-id', fix.id)
    style.textContent     = fix.css                // CSS, never JS — safe
    document.head.appendChild(style)
  }

  // ── 4. Apply a copy fix — replaces text content of a matched element ──────

  function applyCopyFix (fix) {
    if (!fix.selector || !fix.text) return

    var markerAttr = 'data-kreis-copy-' + fix.id
    var el = document.querySelector(fix.selector)

    if (!el) return
    if (el.getAttribute(markerAttr)) return   // idempotent

    el.textContent = fix.text                // textContent, never innerHTML — XSS-safe
    el.setAttribute(markerAttr, '1')
  }

  // ── 5. Main ───────────────────────────────────────────────────────────────

  function init () {
    var token = getToken()
    if (!token) {
      // Script loaded but no valid token attribute — nothing to do
      return
    }

    fetchFixes(token, function (fixes) {
      if (!Array.isArray(fixes)) return

      for (var i = 0; i < fixes.length; i++) {
        var fix = fixes[i]
        try {
          if (fix.type === 'css'  && fix.css)      applyCSSFix(fix)
          if (fix.type === 'copy' && fix.selector)  applyCopyFix(fix)
        } catch (e) {
          // Never let a single broken fix crash the rest
        }
      }

      // Surface applied count for debugging (only in non-production)
      if (typeof console !== 'undefined' && global.location.hostname === 'localhost') {
        console.log('[Kreis] Applied ' + fixes.length + ' fix(es)')
      }
    })
  }

  // Run after DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init)
  } else {
    init()
  }

})(window)
