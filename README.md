# skyWash Cleaning — On-Demand Laundry Pickup

A mobile-first, installable web app prototype for on-demand laundry pickup and delivery in any city, modeled on request → choose partner → track → rate flow.

## Docs
- [`DOCUMENTATION.md`](DOCUMENTATION.md) — complete product & platform documentation (Uber-style: vision, flows, pricing, architecture, roadmap)
- `README.md` — deploy quickstart

## What's included
- `index.html` — app shell (booking flow, live map, order tracking, chat, order history)
- `css/styles.css` — styles
- `js/` — app logic (`app.js`, `register-sw.js`)
- `manifest.json` + `sw.js` — installable PWA with basic offline caching
- `icons/` — app icons for home-screen install
- `vercel.json` — static hosting config

## Deploy to Vercel

**Option A — Vercel dashboard (easiest)**
1. Go to https://vercel.com/new
2. Drag and drop this whole folder onto the page, or click "Upload" and select it
3. Vercel will detect it as a static site — no build step needed. Click **Deploy**.

**Option B — Vercel CLI**
```bash
npm i -g vercel
cd sudsnear-deploy
vercel
```
Follow the prompts (link/create a project, accept defaults). Then run `vercel --prod` to push it live.

**Option C — GitHub + Vercel**
1. Push this folder to a new GitHub repo
2. Import the repo at https://vercel.com/new
3. Framework preset: **Other** (no build command needed) — deploy as-is

## Notes for going from prototype → real product
This is a functioning demo, not a production backend. Before treating it as a real business:
- Laundry partner data is a static multi-city snapshot (Lagos, Abuja, Port Harcourt demo) — you'd want a real backend with live partner availability per city
- Users choose a nearby partner (inDrive-style) — a real dispatch system needs partner acceptance, capacity limits, and fallback matching
- Payment methods are UI-only — wire up Paystack or Flutterwave for real Naira transactions
- Rider/courier movement is a simulated straight-line animation, not real GPS tracking
- Order history is stored in the browser's `localStorage`, so it's per-device only — a real account system needs a backend + auth
