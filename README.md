# skyWash Cleaning — On-Demand Laundry Pickup

A mobile-first, installable web app prototype for on-demand laundry pickup and delivery in any city, modeled on request → choose partner → track → rate flow.

## Docs
- [`DOCUMENTATION.md`](DOCUMENTATION.md) — complete product & platform documentation
- [`API.md`](API.md) — extracted backend API contract
- [`backend/README.md`](backend/README.md) — Spring Boot API run/deploy guide

## What's included
- **Frontend (Vercel):** `index.html`, `css/`, `js/`, PWA files
- **Backend (Spring Boot):** `backend/` — Java 21 API on port `8080`
- `manifest.json` + `sw.js` — installable PWA
- `icons/` — app icons
- `vercel.json` — static hosting config (frontend only)

## Backend (Spring Boot)

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"
cd backend
mvn -DskipTests package
java -jar target/skywash-api-0.1.0.jar
```

API: `http://localhost:8080/api/health`  
Deploy the JAR to Railway/Render/Fly — **not** Vercel (JVM long-running process).

## Deploy frontend to Vercel

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
