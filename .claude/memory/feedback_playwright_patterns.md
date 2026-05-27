---
name: feedback_playwright_patterns
description: Playwright e2e pitfalls learned in this project — LIFO routing, ng-reflect-*, macOS Chromium path
type: feedback
---

## 1. Route registration order is LIFO — wildcards must be registered first

When using `page.route()` with multiple patterns where one is a wildcard (`*`), register the wildcard **first** (lowest priority) and specific patterns **last** (highest priority, wins).

**Why:** Playwright matches routes in LIFO order. If `stores/*` is registered after `stores/@staff`, the wildcard intercepts the specific URL first and returns the wrong mock.

**How to apply:** In every `mockXxxApi()` helper in `e2e/mocks.ts`, always put the `*` wildcard route call first, then the `@view*`/`@named-path` routes. Comments already document this order:
```typescript
// Register wildcard fallback first (lower LIFO priority), specific patterns last (win)
await page.route(`${API}/stores/*`, route => route.fulfill({ json: MOCK_STORE_DETAIL }));
await page.route(`${API}/stores/@sales-by-store`, route => route.fulfill({ json: MOCK_SALES_BY_STORE }));
```

## 2. Do not test `ng-reflect-*` attributes — use CSS or behavior instead

`ng-reflect-router-link` (and all `ng-reflect-*` attributes) are Angular development-mode-only debug reflection attributes. Angular omits them in any production/optimized build, so tests that assert their presence fail intermittently.

**Why:** Angular strips reflection attributes in non-dev mode; `toHaveAttribute('ng-reflect-router-link')` passes in dev but fails otherwise.

**How to apply:** Use `toHaveCSS('cursor', 'pointer')` to verify clickability (rows that navigate set `style="cursor:pointer"` explicitly). For navigation itself, test the actual click behavior with `toHaveURL()`.

## 3. macOS Chromium cache path differs from Linux

macOS: `~/Library/Caches/ms-playwright/chromium-NNNN/chrome-mac-x64/Google Chrome for Testing.app/…`
Linux: `~/.cache/ms-playwright/chromium-NNNN/chrome-linux64/chrome`

**How to apply:** `playwright.config.ts` already has a `process.platform === 'darwin'` conditional — keep it. Override path with env var `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH` on unusual setups.
