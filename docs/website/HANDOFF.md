# Nyx web — handoff for local Claude Code

> Goal: a showcase site **and** the real web app, served from `nyx.technowolf.in` on Cloudflare
> Workers. This doc is the brief. Open it in a local Claude Code session (kickoff prompt at the
> bottom) and work through the phases in order.

## TL;DR

- **Two things, one Worker.** A static showcase site (Astro) at `/`, plus the existing Compose/Wasm
  app (`:webApp`) at `/app/`. One Cloudflare Worker with static assets only, no server code. It
  binds `nyx.technowolf.in` as a Worker Custom Domain and is deployed separately from the
  technowolf.in Worker.
- **Don't build the showcase in Compose.** Compose/Wasm draws everything on a `<canvas>`. Search
  engines, link previews and screen readers see an empty page, and the first load is several MB.
  Use it for the app only.
- **Make the web build's trust story checkable.** Ship a strict CSP on `/app/*` so the browser
  itself enforces "no third-party network calls", and tell visitors how to verify it.
- **Deploy from CI, not a laptop.** Push to `master` deploys to production. Each PR gets a preview
  URL.

## Three uncomfortable truths first

1. **The web build weakens the "network-free" promise unless you design for it.** On Android and
   desktop the user installs a binary once. On the web, the server sends the crypto code again on
   every visit, so anyone who gets into the Cloudflare account or CI can ship a build that leaks
   passwords. Mitigations, all in this plan:
   - a strict `connect-src 'self'` CSP;
   - deploys only from tagged/`master` CI;
   - published bundle hashes;
   - a security page that says this plainly.

   Your README's honesty about the security model is a selling point. Keep that tone on the site.
2. **The web vault is session-only** (`webApp/.../InMemoryVaultSource.kt`). Visitors lose their
   vault on reload. The site must say so next to the "Try in browser" button. Otherwise the first
   bug report will be "Nyx deleted my images". Persistent web storage (OPFS/IndexedDB) is a
   separate project. Don't mix it into this one.
3. **`/app` currently shows a blank screen on unsupported browsers.**
   `webApp/src/wasmJsMain/resources/index.html` loads `composeApp.js` with no fallback. Kotlin/Wasm
   needs WasmGC [Likely: Chrome 119+, Firefox 120+, Safari 18.2+]. Older Safari, which is common on
   iPhones that haven't updated, gets a dead page. Add feature detection and a fallback message
   before launch.

## What's verified in the repo (as of `eb445e7`)

| Fact | Where |
|---|---|
| Web target is Kotlin/Wasm, output module `composeApp`, entry `composeApp.js` | `webApp/build.gradle.kts` |
| Production bundle task: `./gradlew :webApp:wasmJsBrowserDistribution` | CI `_build.yml` → `web` job |
| Output dir (expected, confirm locally): `webApp/build/dist/wasmJs/productionExecutable/` | Gradle default |
| Bare `index.html` (no meta, no OG tags, no fallback), `styles.css` hides overflow | `webApp/src/wasmJsMain/resources/` |
| Brand tokens: 5 palettes; default dark = **Nardo** (`bg #1E2020`, brand `#BFDCC9`), default light = **Cream** (`bg #F3EFE4`, brand `#4E7761`) | `shared/design-library/.../tokens/NxColors.kt` |
| Typeface: JetBrains Mono, 5 weights (~275 KB TTF each) | `shared/design-library/src/commonMain/composeResources/font/` |
| Tokens were copied from a SlothieSmooth `colors_and_type.css` | comment at top of `NxColors.kt` |
| Logos / hero art: `images/nyx-logo.png`, `applogo.png`, `app-featured-image.jpg`, `.ai` sources | repo root |
| 85 Paparazzi goldens (components × 5 themes), not full screens | `shared/design-library/snapshot/src/test/snapshots/images/` |
| Releases: tag on `master` == `nyx-version`. Assets: `Nyx-<tag>.apk`, `.deb`, `.dmg` (unsigned, arm64), `.exe`, `.msi`, `SHA256SUMS.txt` | `.github/workflows/release.yml` |
| Branching: PRs target `develop`; `master` is release | `CONTRIBUTING.md` |
| Current version `1.0.1` | `gradle/libs.versions.toml` |

**Not verified (couldn't build in the cloud session, Google Maven was blocked):**
- the real Wasm bundle size;
- whether the output filenames are content-hashed;
- whether Compose resources load correctly from a sub-path.

Phase 0 settles all three.

## Cloudflare facts used (checked Nov 2026)

- Workers static assets: **25 MiB max per file**; **20,000 files per version on Free**, 100,000 on
  paid ([limits](https://developers.cloudflare.com/workers/platform/limits)). The Wasm files must
  each be under 25 MiB. Measure in Phase 0.
- `assets.not_found_handling`: `"404-page"` | `"single-page-application"` | `"none"`. Use
  `"404-page"` for a static site
  ([docs](https://developers.cloudflare.com/workers/static-assets/routing/static-site-generation/)).
- [Likely] Custom domain via `routes: [{ pattern: "nyx.technowolf.in", custom_domain: true }]`.
  Wrangler creates the DNS record and certificate itself, so there must be **no existing DNS
  record** for `nyx`.
- [Likely] `_headers` / `_redirects` files in the assets dir are honoured by Workers static assets.
- [Guessing] Cloudflare brotli-compresses `application/wasm` automatically. Check with
  `curl -sI -H 'Accept-Encoding: br' https://nyx.technowolf.in/app/<file>.wasm`.

**Check before Phase 3:** does the technowolf.in Worker have a wildcard route like
`*.technowolf.in/*`? If so, it may intercept the new subdomain. Look in the dashboard under
Workers → technowolf → Settings → Domains & Routes.

## Architecture decision

```
nyx.technowolf.in  ──►  Worker "nyx-web" (static assets only, no script)
                         ├── /             Astro static site (showcase)
                         ├── /security     threat model, verify-the-build instructions
                         ├── /download     platform matrix → GitHub Releases
                         ├── /app/         Compose/Wasm app (copied from :webApp dist)
                         └── 404.html
```

**Why a separate Worker from technowolf.in:** independent deploys, separate failure domain, and the
Nyx deploy token can't touch the main site.

**Why `/app/` rather than `app.nyx.technowolf.in`:** one origin and one deploy. It also avoids
second-level-subdomain certificate questions. [Guessing] Universal SSL only covers
`*.technowolf.in`. Workers Custom Domains may issue their own certificate, but `/app/` removes the
question entirely.

**Why Astro:** zero JS by default, so the pages are fast, crawlable and work without JS. It has a
mature static output and Markdown pages for docs. The Kotlin-native option is Kobweb. Its ecosystem
is smaller, and the showcase gains nothing from Kotlin, so skip it.

### Proposed layout (in this repo, so the site and app version together)

```
website/
  astro.config.mjs        # output: 'static'
  package.json
  wrangler.jsonc
  public/
    _headers
    og-image.png
    fonts/                # JetBrains Mono woff2 subsets (convert from the TTFs)
  src/
    styles/tokens.css     # NxColors → CSS custom properties, 5 palettes
    pages/ index.astro security.astro download.astro 404.astro
    components/ …
scripts/
  build-web.sh            # gradle wasm dist → astro build → copy into website/dist/app/
.github/workflows/website.yml
```

### `wrangler.jsonc` sketch

```jsonc
{
  "name": "nyx-web",
  "compatibility_date": "2026-11-01",
  "assets": {
    "directory": "./dist",
    "not_found_handling": "404-page",
    "html_handling": "auto-trailing-slash"   // /app → /app/ so relative wasm paths resolve
  },
  "routes": [{ "pattern": "nyx.technowolf.in", "custom_domain": true }]
}
```

### `_headers` sketch. Iterate in Phase 1 until the browser console shows zero CSP violations.

```
/*
  X-Content-Type-Options: nosniff
  Referrer-Policy: no-referrer
  Permissions-Policy: camera=(), microphone=(), geolocation=()

/app/*
  Content-Security-Policy: default-src 'self'; script-src 'self' 'wasm-unsafe-eval'; connect-src 'self'; img-src 'self' blob: data:; style-src 'self' 'unsafe-inline'; font-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'none'
  Cache-Control: no-cache          # switch to immutable only for content-hashed filenames

/_astro/*
  Cache-Control: public, max-age=31536000, immutable
```

`connect-src 'self'` is the important line. The browser blocks requests to any other origin, so
secrets cannot be exfiltrated to a third party even by a compromised dependency. It still allows
same-origin fetches for Compose resources. Put this on the security page as a claim visitors can
check themselves: open DevTools, then the Network tab, and watch.

## Showcase ideas, ranked by impact ÷ effort

| # | Idea | Why it lands | Effort |
|---|---|---|---|
| 1 | **"Spot the difference" slider.** Original vs stego image, then an *Amplify LSBs* toggle that shows the bit-noise map | Shows the magic visually. Use **real Nyx output**: generate the images with a small JVM task over `:steganography`, not a fake | Low |
| 2 | **Try it in your browser** → `/app/`, with honest caveats (session-only vault, desktop browser recommended) | Turns visitors into users with no install | Low (Phase 1) |
| 3 | **Pipeline walkthrough:** message → AES-GCM ciphertext (hex) → bits → pixels, step by step on scroll | Teaches the README's diagram interactively | Medium |
| 4 | **Theme switcher with all 5 palettes.** The site itself re-skins using the app's own tokens | Shows the design system and makes the site feel like the app | Low |
| 5 | **Security page:** threat model, "verify this build" (CSP, SHA256SUMS, link to the CI run) | Builds credibility with people who care about crypto | Low |
| 6 | **Download matrix:** OS detection highlights the right installer. Version and links fetched **at build time** from the GitHub Releases API (no runtime calls) | Converts visitors | Low |
| 7 | **"Where it breaks":** WhatsApp/Instagram recompression destroys the payload. Show a before/after | Heads off the #1 support issue | Low |
| 8 | **For developers:** architecture (module graph from README), KMP story, link to CONTRIBUTING | Recruits contributors; portfolio value | Low |
| 9 | Stretch: a small `@JsExport` Kotlin/Wasm build of only `:crypto` + `:steganography` (no Compose) that drives an inline demo on the landing page | Real engine with no canvas UI. Payload is likely much smaller than the full app (unmeasured) | High, after launch |

## Phased plan (each phase ≈ one Claude Code session, each ends with a PR into `develop`)

### Phase 0: measure (30 min, no code)
- `./gradlew :webApp:wasmJsBrowserDistribution`. Record every file, its raw size and its brotli
  size (`brotli -c f | wc -c`), and whether the names are content-hashed.
- Serve it from a sub-path: `mkdir -p /tmp/s/app && cp -r <dist>/* /tmp/s/app/ && npx serve /tmp/s`,
  then open `/app/`. Do encrypt → download → decrypt work, and do the fonts load?
- **Exit:** a short `docs/website/measurements.md`. If any file is over 25 MiB, stop and rethink.

### Phase 1: harden the app shell (`webApp/src/wasmJsMain/resources/`)
- `index.html`: title, description, theme-color (`#1E2020`), favicon, a loading state, and
  WasmGC/feature detection with a friendly fallback that links to `/download`.
- Add the `/app/*` CSP. Use Playwright to load `/app/`, run encrypt/decrypt and assert zero console
  CSP violations.
- **Exit:** the app works from `/app/` with a strict CSP, and an unsupported browser sees a message
  instead of a blank page.

### Phase 2: site skeleton + design tokens
- `website/` with Astro. Write `tokens.css` from `NxColors.kt`: all 5 palettes as
  `[data-theme=…]` blocks, plus `prefers-color-scheme` defaults (Nardo / Cream).
- Self-host JetBrains Mono as woff2 (subset Latin). No Google Fonts call, to keep the zero-third-party
  story intact.
- Pages: `/`, `/security`, `/download`, `/404`. Add meta/OG tags and an OG image made from
  `app-featured-image.jpg`.
- `scripts/build-web.sh` builds both and puts the app in `website/dist/app/`.
- **Exit:** `npx wrangler dev` serves the full site and app locally, and Lighthouse scores ≥ 95 on
  `/`.

### Phase 3: CI deploy + domain
- Cloudflare API token scoped to **Workers Scripts: Edit** on this account only. Add GitHub secrets
  `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID`.
- `.github/workflows/website.yml`:
  - PRs touching `website/**` or `webApp/**` → `wrangler versions upload` (preview URL posted on
    the PR).
  - Push to `master` → `wrangler deploy`.
  - Reuse the JDK/Gradle setup steps from `_build.yml`. Add Node for Astro.
- After the deploy, publish `sha256sum` of the `/app/` files as a build artifact. The security
  page links to it.
- **Exit:** `https://nyx.technowolf.in` is live from CI, a PR shows a working preview URL, and the
  `curl` checks pass (wasm `content-type`, `content-encoding: br`, CSP header present).

### Phase 4: showcase content (ideas 1, 3–8)
- A Gradle task or one-off JVM script under `:steganography` generates the before/after/LSB-map
  images for idea 1. Commit the outputs into `website/public/demo/`.
- Copy: reuse the README wording, which is already good and honest.
- **Exit:** the content is reviewed on mobile and desktop, and the site works with JS disabled
  (except the slider and theme toggle).

### Phase 5: polish + launch
- Accessibility pass (keyboard, contrast; the tokens are already WCAG-AA per `NxColorsTest`),
  `sitemap.xml`, `robots.txt`.
- Update the README badge/link, the GitHub repo "Website" field, and the Play listing if one
  exists.

## Decisions only you can make (my recommendation first)

1. **App location:** `/app/` (rec) vs `app.nyx.technowolf.in`.
2. **Analytics:** none (rec; it fits "network-free") vs Cloudflare Web Analytics on marketing pages
   only, **never** on `/app/*`.
3. **Play Store:** is Nyx listed? If yes, the download page leads with the Play badge on Android.
4. **SlothieSmooth `colors_and_type.css`:** do you have it? If yes, drop it into `website/` as the
   token source instead of re-deriving from Kotlin.
5. **Deploy trigger:** every push to `master` (rec, since `master` == released) vs only on version
   tags.

## Kickoff prompt for local Claude Code

```text
Read docs/website/HANDOFF.md fully. We're doing Phase 0 now. Run the wasm production build,
measure the output as described, test serving it from /app/ (encrypt → download → decrypt in
Playwright), and write docs/website/measurements.md. Don't start Phase 1 until I've reviewed
the numbers. Branch off develop as feature/website-phase-0.
```

For later phases: "Read docs/website/HANDOFF.md and docs/website/measurements.md. Do Phase N.
Stop at its exit criteria and show me evidence (screenshots, curl output, test results)."
