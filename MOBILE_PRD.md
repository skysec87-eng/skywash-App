# skyWash Cleaning — Mobile Product Requirements & Architecture

**Version:** 1.1  
**Date:** 11 Sep 2026  
**Status:** Spec for native apps. Client = Kotlin/Compose (Android). Behaviour = live web + API. No implementation in this document.  
**Audience:** Founders, mobile engineers, designers, QA, store reviewers.

This is the source of truth for **what the native apps must do**. Behavior is taken from the **live pilot** (Vercel web + Fly API + Neon), not from stale sections of `DOCUMENTATION.md` that still describe a localStorage-only prototype.

| Companion | Role |
|---|---|
| Interactive canvas | Visual PRD (architecture, screens, build plan) |
| `DOCUMENTATION.md` | Brand, positioning, Uber analogy — treat §11.4 as historical |
| Live web | `index.html` + `js/app.js` — customer UX to match |
| Live API | `https://skywash-api.fly.dev` — contract to consume |
| Ops | `/ops/` — stays web in v1 |

---

## 0. How to read this

1. **Parity first.** Customer v1 is the web product on Android (Kotlin), plus push, GPS, and Play install. Do not invent a different marketplace.
2. **Reuse the API.** Do not add Firebase Auth, a second database, or a Node BFF. Kotlin is the client language only.
3. **Pilot constraints are product constraints.** Empty Neon, Places billing, and Resend domain verification are launch blockers, not “mobile bugs.”
4. **v1 vs later.** Anything marked **v1.1 / future** is specified so it is not accidentally built now, and so partner/courier work has a contract when it starts.

---

## 1. Product thesis

skyWash Cleaning is an on-demand laundry marketplace. Customers request a wash the way they request a ride: set pickup, see nearby shops, **choose one** (inDrive-style, not Uber auto-dispatch), pay, track, rate.

| Dimension | Uber | skyWash |
|---|---|---|
| Request | Trip A → B | Pickup → wash → delivery |
| Supply | Drivers | Laundry partners (couriers later) |
| Matching | Platform-assigned | Customer picks from nearby offers |
| Unit | Ride | Order |
| Tracking | Driver GPS | Stage stepper + simulated courier pin (real GPS future) |

**Promise:** Book laundry pickup wherever you are.  
**Not:** A single-shop website, or a Lagos-only tool.

### Brand

| Element | Value |
|---|---|
| Brand | skyWash |
| Full name | skyWash Cleaning |
| UI tagline | On-demand cleaning |
| Onboarding kicker | Fresh laundry, on demand |
| Onboarding line | Book a pickup in minutes — partners come to you. |
| Pitch line | The on-demand laundry network for Nigerian cities |
| Promo | `SKY10` — 10% off subtotal |
| Theme | Lagoon navy `#08283a`, sun yellow `#ffc93c` / `#f5a623` |
| Fonts | Fraunces (display), Inter (body) |
| Support | `isaac.arinze.dev@gmail.com` |

Locales today: **en, fr, yo, ha, ig**. Theme: **light / dark / system**.

---

## 2. Decision

| Choice | Decision |
|---|---|
| Client | **Kotlin + Jetpack Compose** (Android). Behaviour is the API + this PRD, not the UI toolkit. |
| Flavors | **Customer** (v1 Play Store) · **Partner** (v1.1, same Android project or `:partner` module) |
| iOS | **Not Kotlin.** Web PWA until a later Swift/Compose Multiplatform client. Same API. |
| Ops | **Web only** at `/ops/` until volume needs a tablet app |
| Backend | Existing Spring Boot on Fly.io. Small new endpoints only. |
| Web | Remains the fallback PWA, iOS path, and GITEX demo |

The language of the phone app does **not** change product behaviour. Booking, auth, quote math, 25 km nearby, Paystack, status machine, confirm-receipt, Assist, and ratings stay 1:1 with the web. Kotlin only replaces *how pixels and HTTP are implemented*. Do not invent Android-only flows.

**Android stack (v1)**

| Layer | Choice |
|---|---|
| UI | Jetpack Compose, Material 3 themed to lagoon/sun (not default purple) |
| Nav | Navigation Compose — same IA as §5 |
| HTTP | Retrofit + OkHttp against `https://skywash-api.fly.dev` |
| JSON | Kotlinx Serialization or Moshi — field names match the API (`snake_case`) |
| Session | EncryptedSharedPreferences / DataStore for `sw_` token (equivalent of SecureStore) |
| Map | Google Maps SDK for Android (or OSM if billing still off) |
| Auth | Credential Manager / Google Sign-In; OTP screens as specified |
| Pay | Custom Tabs / WebView on Paystack `authorization_url`; then `POST /payments/verifications` |
| Push | FCM (Phase 5) |
| i18n | `values` / `values-yo` / `values-ha` / `values-ig` / `values-fr` — same keys as web |

**Why behaviour stays the same:** the server owns quote totals, order status ticks, nearby radius, OTP TTL, and Paystack verification. The Kotlin app is a client: it must not recompute fees, invent partners, or auto-mark delivered.

### Goals / non-goals

| Goal | In scope | Out of scope |
|---|---|---|
| Book in ~90 seconds | GPS or typed address, nearby list, quote, Paystack, track | Courier hardware GPS, multi-stop routes |
| Same account as web | Google + email OTP vs existing `users` table | New identity provider |
| Partner can run a job | v1.1: accept, status ticks, notify | POS, inventory, staff roster, payouts |
| Trust | Ratings, history, Assist, support email | Voice, KYC, insurance claims |
| Store | Privacy labels, location while booking | Cookie banner (web-only concern) |

---

## 3. Current platform (do not duplicate)

```
Customer web PWA (Vercel)     Partner Ops web (/ops/)
        │                              │
        └──────── HTTPS + Bearer ──────┘
                         │
              skyWash API  Fly.io  Java 21
                         │
     ┌─────────┬─────────┼─────────┬──────────┬─────────┐
     ▼         ▼         ▼         ▼          ▼         ▼
   Neon PG   Google    Overpass  Paystack   Resend    Groq
   users     OAuth     nearby    cards      OTP       Assist
   orders    Maps      OSM       webhook    partner   Llama
   partners  Places                         email
                                            WhatsApp
```

| Layer | Today |
|---|---|
| API | `https://skywash-api.fly.dev` — `GET /api/health` → `{ ok, service: "skywash-api" }` |
| Data | Neon Postgres (eu-west-2). Railway history was **not** imported. |
| Web | Vercel static customer app + `/ops/` |
| Auth | Google ID token + email OTP (Resend). Also legacy phone+password. |
| Session | Token `sw_<uuid>`, 30 days, table `auth_sessions` |
| Match | Google Places (needs Cloud billing) then Overpass 8 km / 20 km rings. Cap **25 km**. |
| Pay | Paystack initialize / verify / webhook |
| Chat | Per-order Assist (Groq Llama). **In-memory — lost on API restart.** |
| Notify | Customer: OTP email only. Partner: email + WhatsApp on **delivery confirm**. No push. |

**Auth header:** `Authorization: Bearer sw_...` (raw token also accepted).  
**Error envelope:** `{ error, status, timestamp }`.  
**There is no `/api/me`.** Use `GET /api/account`. Guest returns `{ id: "anon", demo: true }`.

---

## 4. Personas and roles

### Customer (primary, v1)

Urban resident. Pickup at home or office. Chooses a nearby laundry. Pays NGN via Paystack (card default; transfer and cash exist). Needs push for “partner on the way” and “out for delivery.” Speaks EN / Yorùbá / Hausa / Igbo / French.

**Can do today on web (must do on mobile):** onboard, book, choose partner, pay, track, confirm receipt, rate, history, Assist, theme, language, logout.

### Laundry partner (v1.1 app; today: ops web)

Shop owner or counter staff. Sees incoming jobs, advances status, gets alert when customer confirms receipt.

**Today:** no partner login. Discovered via OSM/Google into `partners`. Alerts on delivery confirm. Ops PIN dashboard is **read-only**.

**Accept stub exists:** `POST /api/orders/{id}/acceptances` → `confirmed`. Unused by customer web.

### Courier / rider (future)

Pickup dirty bag → laundry → return clean bag. Live GPS. Backend `POST /api/couriers/{id}/location` is a **demo stub** (no persistence). Map courier pin today is **interpolated** during `enroute` / `delivering`.

### Ops / admin (web)

PIN-gated **Partner Ops** — monitor orders and whether email/WhatsApp alerts sent. **Not** city launch, KYC, refunds, or fraud.

Demo PIN (documented in `.env.example`): `skywash-ops`. Header: `X-Ops-Pin`.

---

## 5. Information architecture — customer app

Match web shells, not website chrome.

### Onboarding (`obStep`)

`welcome` → `email` → `otp` → `profile` (if `registration_required`) → home.

| Screen | Actions |
|---|---|
| Welcome | Google · Create account · Sign in · Forgot / reset access |
| Email | Address; intent `start` \| `signin` \| `reset` |
| OTP | 6 digits; resend (45s cooldown) |
| Profile | Name + phone (required). Google may prefill name. |

### Main tabs (bottom nav)

| Tab | Web panel | Notes |
|---|---|---|
| Book | `#panelBooking` | Default after login |
| Map / Browse | `#panelBrowse` | Search partners; native map |
| Orders | `#panelHistory` | History + reopen / book again |
| Account | `#panelAccount` | Web hides this in avatar menu — **put it on the tab bar** on mobile |

### Booking sub-steps (same machine as web)

`form` → `matching` → `offers` → `trip` → `rating` → `form`

### Overlays

Paystack WebView/SDK · Assist sheet · Confirm-receipt dialog · Native permission sheets (location, notifications).

---

## 6. Auth (must be identical to web)

### Google

1. `GET /api/auth/google/config` → `{ enabled, client_id }`
2. Native Google Sign-In SDK (new iOS/Android OAuth clients; web client ID is not enough)
3. `POST /api/auth/google` `{ id_token }`
4. Existing user → `{ token, user, registration_required: false }`
5. New user → `{ registration_token, email, name, registration_required: true }` → `POST /api/auth/registrations` `{ registration_token, name, phone }`

Verified email required. Store token in **EncryptedSharedPreferences** (or DataStore). Never plain SharedPreferences.

### Email OTP

1. `POST /api/auth/verification-codes` `{ email }` or `{ email, purpose: "reset" }`
2. TTL **10 min**, resend cooldown **45 s**, max **5 attempts**
3. `POST /api/auth/verification-codes/confirmations` `{ email, code }`
4. Signup path: `registration_required` → profile → `POST /api/auth/registrations`

**Launch constraint:** Until a real domain is verified on Resend, OTP only delivers to the Resend account inbox. Copy must recommend Google until then.

### Password (legacy, keep for parity)

`POST /api/auth/password-registrations` `{ name, phone, password }`  
`POST /api/auth/password-sessions` `{ phone, password }`  
Password min 4 chars. Phone unique.

### Session

| Action | Endpoint |
|---|---|
| Profile | `GET /api/account` (optional auth; guest if none) |
| Update | `PATCH /api/account` `{ name?, phone?, payment_preference? }` |
| Logout this device | `DELETE /api/auth/sessions/current` |
| Logout everywhere | `DELETE /api/auth/sessions` |

User shape: `{ id, name, phone?, email?, payment_preference: { key, name } }`.  
Payment keys: `card` | `transfer` | `cash`.

**Apple Sign-In:** not needed for Android v1. Add `POST /auth/apple` only when an iOS app ships.

---

## 7. Booking — end-to-end (v1 must-ship)

This is the acceptance test. A stranger on a physical phone completes it without coaching.

### 7.1 Schedule

- **Now** (default) or **Later**
- Later: datetime, step 15 min, min now+15 min, max +30 days, default ~+1 h
- Payload: `scheduled_at` ISO-8601 or `null`
- Scheduled orders **do not auto-progress** until `scheduled_at` is in the past

### 7.2 Location

- Typed address → `POST /api/geocode` `{ address }`
- GPS → `POST /api/geocode` `{ lat, lng }`
- Fallback if API fails: Nominatim (web does this; native may call API only)
- Success: pickup `{ lat, lng, address }`. Request button disabled until set.
- Permission copy: location is **only used to find nearby laundries and set pickup**.

### 7.3 Services (multi-select, min 1)

| type | label | rate (NGN) | unit |
|---|---|---|---|
| `wash` | Wash & Fold | 500 | kg |
| `dry` | Dry Cleaning | 1500 | item |
| `iron` | Iron Only | 300 | kg |
| `express` | Express (4h) | 900 | kg |

Loaded from `GET /api/services` (also returns `base_fee`, `platform_fee_rate`, `currency`).  
Qty **1–20**, default **3**, same qty applied to each selected line (web behavior).

### 7.4 Quote (live, debounce ~200 ms)

`POST /api/pricing/quote` `{ services: [{ type, qty }], promo_code? }`

```
service_cost  = Σ(qty × rate)
platform_fee  = round(service_cost × 0.10)
subtotal      = 500 + service_cost + platform_fee
discount      = round(subtotal × promo_rate)   # SKY10 → 0.10
total         = subtotal − discount
```

Show: base pickup fee, service lines, 10% platform fee, promo row, **total estimate**.

Promo: `POST /api/promos/validate` `{ code }`. Only **SKY10** is valid. Invalid: “Invalid code — try SKY10”.

### 7.5 Payment method (preference, not always charge)

Grid: **card** (Visa, Mastercard, Verve) · **transfer** · **cash** (“Pay the courier directly”).  
Persist locally + `PATCH /api/account`. Charge Paystack **only when `card`** on confirm (web behavior).

### 7.6 Matching

- CTA: “Request pickup”
- UI: “Looking nearby…” radar
- `GET /api/partners/nearby?lat=&lng=&limit=8&service={primary}&qty={weight}`
- Client does **not** send `radius_km`. Server cap: **25 km** (docs still say 40 — **25 is live**).
- Empty: stay on offers screen, “No partners nearby”, retry + Assist. Never invent far-away cities.
- Failure: alert and return to form.

### 7.7 Choose partner (inDrive)

Offer row: initials, name, rating, city · area, ETA minutes, distance km.  
Map: inactive store pins vs selected (sun).  
Confirm CTA: “Confirm pickup”. Cancel returns to form.

Offer object includes `partner`, `distance_km`, `eta_minutes`, `total_eta_minutes`, `eta_breakdown`.

### 7.8 Create order

`POST /api/orders` **requires auth**.

```json
{
  "partner_id": "osm-… or gplace-…",
  "pickup": { "lat": 0, "lng": 0, "address": "…" },
  "services": [{ "type": "wash", "qty": 3 }],
  "promo_code": "SKY10",
  "payment_method": "card",
  "scheduled_at": null
}
```

Then `GET /api/orders/{id}`, load messages, start poll.

Partner IDs are **dynamic** (OSM/Google). Always nearby-fetch before booking. Do not hardcode partner catalogs.

---

## 8. Payments (Paystack)

| Step | Endpoint |
|---|---|
| Public key | `GET /api/payments/config` `{ provider, public_key, configured }` |
| Checkout | `POST /api/payments/checkouts` `{ order_id, amount (NGN int), email? }` |
| Verify | `POST /api/payments/verifications` `{ reference }` |
| Return | `GET /api/payments/callback` → 302 to frontend `?reference=` |
| Webhook | `POST /api/payments/webhook` — server-only |

Min amount ₦100. Reference `SKY_` + hex. Amount to Paystack in **kobo**.  
Order payment_status: `unpaid` → `pending` → `paid`. Timeline key `paid` / “Payment received”.

**v1 mobile:** Paystack WebView with `authorization_url` is acceptable. Native SDK later.

If keys unset, API returns `demo: true` stub. Production app must fail closed with “Payments not configured,” not fake success.

Deep link restore: after checkout, app must verify reference and reopen the order (web uses `?payment=callback&reference=`).

---

## 9. Live trip

### Status machine (UI keys — match web 1:1)

| key | label |
|---|---|
| `confirmed` | Request confirmed |
| `enroute` | Partner heading to you |
| `pickedup` | Picked up from you |
| `washing` | Washing at the laundromat |
| `delivering` | Out for delivery |
| `delivered` | Delivered |

Also: `cancelled`, `rated`.  
When auto-progress would hit delivered, status **stays `delivering`**, label **“Arrived — confirm receipt”**, `awaiting_delivery_confirmation: true`.

**Auto-progress** (server, every 2s): `confirmed → enroute → pickedup → washing → delivering` then **stall**. Never auto-completes delivery.

**Cancel:** `POST /api/orders/{id}/cancellations`. Blocked if `delivered` | `rated` | `cancelled`.

**Rate:** `POST /api/orders/{id}/ratings` `{ rating: 1–5 }`. Allowed on `delivered` or `rated`. Sets `rated`.

**Confirm receipt:** `POST /api/orders/{id}/delivery-confirmations` only when `delivering`. Triggers partner email + WhatsApp. Ops chip becomes “Customer confirmed.”

**PATCH status / acceptances:** exist for partner/ops later; customer app should **not** call them in v1.

### Polling vs push

- Web polls **every 1.5 s**. Mobile v1: poll **8–15 s** plus push (Phase 5).
- Stop poll on `delivered` | `rated` | `cancelled`.
- Resume active order from encrypted prefs (web uses `skywash_active_order`).

### Map during trip

- Pickup pin, partner pin, simulated courier during `enroute` / `delivering` (`courier_lat` / `courier_lng`).
- ETA banner: `eta_label`, `eta_minutes`, `eta_phase`.
- Breakdown: pickup min / wash min / delivery min.
- Auto-open map on `enroute` and `delivering`.

### Rating screen

Default 5 stars pre-selected (web). Submit → history refresh → back to book form.

---

## 10. skyWash Assist (not partner DM)

Chat is **AI + system**, not a live laundry inbox.

| sender | UI |
|---|---|
| `customer` | Me |
| `assist` | Them (Llama if `LLAMA_API_KEY`, else rules) |
| `system` | Status announcements |

`GET/POST /api/orders/{id}/messages` `{ text }`. **No auth today** (harden later).

**Ephemeral:** messages are in-memory on the API. Do not promise durable chat history.

Quick replies (English today): Ring the bell · Leaving with security · How much longer? · Handle with care · Contact support (message + mailto).

Support mailto: `isaac.arinze.dev@gmail.com`, subject `skyWash support — order {id}`.

Call icon on partner card is **decorative** on web — do not ship a fake tap-to-call unless `partner.phone` is real and you mean it.

---

## 11. Browse, history, account

### Browse / Map

`GET /api/partners` and `GET /api/partners?q=&lat=&lng=`.  
Empty: “No partners match that search.”  
`GET /api/cities` exists but **web does not call it** — discovery is pin-based, not a city picker. Do not lock the app to Lagos/Abuja/PH lists.

### History

`GET /api/orders?limit=50` — **empty for guests**.  
Card: provider, date, service label, total, payment · status, stars, **Book again**.  
Empty: “No orders yet — your completed pickups will show up here.” (Neon may be empty.)

### Account

Name, phone, email (read-only), default payment, language, theme, reset access, logout, logout everywhere.  
Link to Partner Ops is **web-only**; omit from customer store app.

---

## 12. Screen inventory (customer v1)

| Screen | Purpose | Done when |
|---|---|---|
| Splash | Restore `sw_` token, `GET /api/account` | Home or Welcome |
| Welcome | Google + Create + Sign in | Matches web intents |
| Email / OTP / Profile | Passwordless + name/phone | Same user as web |
| Home / Book | Map, services, qty, schedule, pay, promo, quote | Location required to request |
| Matching | Radar | Nearby returns or empty copy |
| Choose partner | Offer list + markers | Customer selects, not auto-dispatch |
| Checkout | Paystack if card | Verify reference, reopen order |
| Live job | Stepper, ETA, map, confirm receipt, cancel | Status 1:1 with web |
| Assist | Thread + quick replies + support | System + assist + customer |
| Rating | 1–5 | Status `rated` |
| Orders | History | Book again |
| Account | Profile, i18n, theme, logout | Token cleared |
| Empty nearby | Honest empty | Retry, no fake shops |
| Offline / API down | Block booking | Copy names Fly URL, retry |

Partner v1.1 screens: login · job queue · job detail · status actions · earnings lite (count only).

---

## 13. API contract the apps call

Base: `https://skywash-api.fly.dev` · prefix `/api`.

### Health / catalog / geo

| Method | Path | Auth |
|---|---|---|
| GET | `/health` | No |
| GET | `/services` | No |
| GET | `/partners` | No |
| GET | `/partners/nearby` | No |
| GET | `/cities` | No |
| POST | `/geocode` | No |
| POST | `/pricing/quote` | No |
| POST | `/promos/validate` | No |

### Auth / account

| Method | Path | Auth |
|---|---|---|
| POST | `/auth/password-registrations` | No |
| POST | `/auth/password-sessions` | No |
| POST | `/auth/verification-codes` | No |
| POST | `/auth/verification-codes/confirmations` | No |
| POST | `/auth/registrations` | No |
| GET | `/auth/google/config` | No |
| POST | `/auth/google` | No |
| DELETE | `/auth/sessions/current` | Optional |
| DELETE | `/auth/sessions` | Yes |
| GET | `/account` | Optional |
| PATCH | `/account` | Yes |

### Orders

| Method | Path | Auth | Customer v1? |
|---|---|---|---|
| POST | `/orders` | Yes | Yes |
| GET | `/orders` | Optional | Yes |
| GET | `/orders/{id}` | No | Yes |
| POST | `/orders/{id}/cancellations` | No | Yes |
| POST | `/orders/{id}/delivery-confirmations` | No | Yes |
| POST | `/orders/{id}/ratings` | No | Yes |
| GET | `/orders/{id}/messages` | No | Yes |
| POST | `/orders/{id}/messages` | No | Yes |
| POST | `/orders/{id}/acceptances` | No | Partner v1.1 |
| PATCH | `/orders/{id}/status` | No | Partner v1.1 (`delivered` internally confirms) |

### Payments

| Method | Path | Notes |
|---|---|---|
| GET | `/payments/config` | Public key |
| POST | `/payments/checkouts` | WebView URL |
| POST | `/payments/verifications` | After return |
| GET | `/payments/callback` | Browser 302 — configure app URL |
| POST | `/payments/webhook` | Server only |

### Ops / stub

| Method | Path | Auth |
|---|---|---|
| GET | `/ops/partners` | `X-Ops-Pin` |
| GET | `/ops/orders?partner_id=` | `X-Ops-Pin` |
| POST | `/couriers/{id}/location` | None — demo, do not use in customer app |

### Endpoints to **add** on Spring (small, not a new platform)

| Need | Endpoint idea | Phase |
|---|---|---|
| Push | `POST /account/device-tokens` `{ platform, token }` | 5 |
| Apple Sign-In | `POST /auth/apple` | Only when an iOS app ships |
| Partner session | `POST /auth/partner-sessions` + partner-scoped order list | 7 |
| Harden | Auth on get/cancel/rate/chat by order owner | anytime |

CORS: native apps do not need origin expansion the way browsers do. Deep links need `PAYSTACK_FRONTEND_CALLBACK_URL` pointed at the app or a universal-link landing page.

---

## 14. Data model (Neon)

**users** — id, name, phone (unique), email (unique), google_sub, password_hash, payment_key, payment_name  

**auth_sessions** — token `sw_…`, user_id, created_at, expires_at (30d)  

**otp_challenges** — email, code_hash, purpose signup|login, pending name/phone/google_sub, attempts (max 5), expires 10 min, consumed, email_verified. Id doubles as `registration_token`.  

**partners** — id (`osm-*` / `gplace-*` / seed), name, city, area, address, lat, lng, rating, phone, email, is_active  

**service_types** — type PK, label, unit, icon, rate  

**orders** — status, labels, pickup geo+address, scheduled_at, partner_id, user_id, provider_name, quantity, payment_method/display, promo, fee breakdown, rating, service_label, courier_lat/lng, payment_status/reference/channel, paid_at, timestamps. Embedded: `order_services`, `order_timeline`. **No Payment table.**  

**partner_notifications** — order_id, partner_id, event `delivery_confirmed`, channel email|whatsapp, status sent|queued|failed, target, payload, wa.me deeplink  

Chat messages are **not** a table.

---

## 15. Discovery rules

1. If `GOOGLE_MAPS_API_KEY` set → Places Nearby (up to 25 km).
2. Then Overpass rings **8 km** and **20 km**.
3. Upsert into `partners` (`osm-{type}-{id}` or `gplace-{place_id}`).
4. Toggle `LAUNDRY_DISCOVERY` (default true).
5. Nearby default limit **8**. Max local **25 km** even if client sends a larger radius.
6. Default ETA inputs if omitted: service `wash`, qty `2`.
7. OSM ratings are synthetic 4.2–4.9; Google uses real rating when present.
8. Missing partner email → `ops+{slug}@partner.skywash.app`.

**Places `REQUEST_DENIED`** = Google Cloud billing off. Nearby then depends on Overpass (slow from Fly). This is an infra SLO, not a client bug.

---

## 16. Partner app (v1.1) and Ops (web)

### Partner app — specified so v1 customer does not pretend it exists

- Login: new partner token (not ops PIN in the store app).
- Queue: incoming + in progress, filter by status.
- Detail: address, services, customer phone, deep link from push.
- Actions: accept → picked up → washing → delivering. **Do not** mark delivered; customer confirms receipt.
- Earnings lite: today’s completed count. No payouts, no POS.

Reuse: `POST .../acceptances`, `PATCH .../status`, ops order JSON as a shape reference.

### Ops web — out of the stores

- PIN → `X-Ops-Pin` → list partners → orders for partner, poll 4 s.
- Shows “Customer confirmed” vs “awaiting confirm”, notification badges, WhatsApp `wa.me` when Cloud API unset.
- **Cannot** accept, advance, cancel, refund, or edit partners.

---

## 17. Notifications

| Audience | Channel | When |
|---|---|---|
| Customer | Email OTP | Login / signup / reset |
| Customer | In-app Assist system messages | Status changes |
| Customer | Push FCM/APNs | **New in mobile Phase 5** |
| Customer | Human email | Support escalation |
| Partner | Email | Delivery confirmed |
| Partner | WhatsApp Cloud or `wa.me` queued | Delivery confirmed |
| Partner | Push | **v1.1** |

No SMS. No customer transactional email beyond OTP. Pitch “pushes status updates” is **in-app chat today**, not device push.

---

## 18. Design system (parity)

| Token | Light | Dark |
|---|---|---|
| Lagoon 900 | `#08283a` | `#041018` |
| Sun 400 / 500 | `#ffc93c` / `#f5a623` | same |
| Ink / foam / card | see `css/styles.css` | dark variants |
| Good / warn | `#2f9e6e` / `#c0503a` | brighter |

Primary CTA: sun yellow, lagoon text. Ghost cancel: warn.  
Native map: Google Maps or Mapbox acceptable; web uses Leaflet + OSM tiles. Pins: store lagoon, selected sun, pickup dark, courier sun border.  
Frame: 390px first. Bottom nav. Portrait. Safe area.

**Legal gap:** web has **no Terms or Privacy Policy**. Stores require a public privacy URL before submit. Write it in Phase 6 (location, email, identifiers, Paystack as processor). NDPR-minded.

---

## 19. Error and empty states (copy to ship)

| Scenario | UX |
|---|---|
| API down at boot | Block; “Cannot reach skyWash API” + retry |
| No partners nearby | Offers empty; “No laundry within 25 km”; retry; Assist |
| Nearby API failure | Alert; back to form |
| Browse no matches | “No partners match that search.” |
| History empty | “No orders yet…” |
| Geo denied | Explain why location is needed; typed address still works |
| Google unavailable | Hide Google button; email path remains |
| OTP fail | Inline error; cooldown; Google recommended until domain verified |
| Invalid promo | Red “✕ {message}” |
| Payment setup fail | Alert; do not create a silent unpaid trip as “success” |
| Chat fail | Alert; mailto still available |
| Request disabled | “Set a pickup location first” |

Do not invent shops in other cities. Do not log OTP codes or card PAN.

---

## 20. Non-functional

| SLO | Target |
|---|---|
| Nearby p95 | < 8 s with Places billed; < 12 s OSM-only |
| Auth + home | < 3 s on mid-range Android |
| API | Keep Fly min machine running; cold start is not UX |
| Crash-free | > 99% sessions |
| Location | While using for booking / trip map only |
| Chat | Treat as best-effort; lost on API restart |

---

## 21. Doc vs live — discrepancies the PRD resolves

| Topic | Docs / pitch | Live code | Mobile follows |
|---|---|---|---|
| Nearby radius | 40 km | **25 km cap** | **25 km** |
| Prototype storage | localStorage partners | Neon + API | **API** |
| Chat | Partner chat | Assist + system | **Assist** |
| Courier GPS | Uber-like | Simulated interpolation | **Simulated until Phase 7+** |
| Ops | Full admin | Read-only PIN dashboard | **Web ops** |
| Delivery complete | Vague | **Customer confirm only** | Same |
| Cities | Lagos / Abuja / PH seed | Pin-based discovery worldwide | **Pin-based** |
| Push | Pitch “pushes updates” | In-app messages | **In-app + add device push** |
| Native apps | DOCUMENTATION Phase 4 | This PRD | **Now** |

---

## 22. Out of scope (do not build in customer v1)

- Partner POS, capacity, hours, staff, KYC, payouts  
- Real courier GPS / rider app / commission rider  
- City admin live-toggle, disputes, refunds, fraud console  
- Surge / distance-based courier fee / VAT  
- B2B (hotels, salons, short-lets)  
- Referrals, subscriptions  
- Two-way ratings  
- Durable chat DB (unless you add it for a reason)  
- Firebase / second Postgres / Node BFF  
- Embedding ops PIN in the customer store app  

---

## 23. Journeys (test scripts)

**J1 First-time customer (must ship)**  
Location → Google or OTP → services + qty + now/later → nearby + price → pick partner → Paystack if card → timeline → confirm receipt → rate.

**J2 Returning**  
Token restore. Home = map + last pickup + Book again. Orders list. Assist from trip.

**J3 Email OTP**  
6-digit via Resend. Profile if new. Google recommended until sending domain verified.

**J4 Empty / sparse area**  
`offers: []` → honest empty. Infra: Places billing + Overpass.

**J5 Card payment restore**  
Checkout → leave app → return with reference → verify → land on live job.

**J6 Scheduled**  
Later time → order sits until `scheduled_at` → then auto-progress.

**J7 Cancel**  
Cancel before picked up → status `cancelled`. Cannot cancel after delivered.

**J8 Partner job (v1.1)**  
Push → accept → picked up → washing → out for delivery → wait for customer confirm → see notify in ops.

---

## 24. Build plan (no code in this spec)

Unblock billing and email **in parallel** with the app shell.

| Phase | When | Work | Done when |
|---|---|---|---|
| **0 Platform** | Week 0 | Places billing; Resend real domain; keep Neon + Fly; Google iOS/Android OAuth clients | Nearby shops in Lekki/Sangotedo; OTP to a second Gmail |
| **1 Repo** | Week 1 | Android app in `/android` (Kotlin, Compose), `API_BASE`, encrypted token store, string resources | Device hits `/api/health` |
| **2 Auth** | Week 2 | Google Sign-In, OTP, profile, logout, session restore | Same user on web and Android |
| **3 Book** | Weeks 3–4 | Map, geocode, services, nearby, quote, SKY10 | Sangotedo shows real partners on a physical phone |
| **4 Pay & track** | Weeks 5–6 | Create order, Paystack Custom Tab/WebView, timeline, confirm receipt, rate, orders | Paid test order → delivered → rated |
| **5 Push & Assist** | Week 7 | FCM device tokens, status push, Assist | Kill app, status still notifies |
| **6 Store** | Week 8 | Icons, privacy URL, Play internal testing | Review-ready Android build |
| **7 Partner** | Weeks 9–12 | Partner auth, queue, status actions, notify reuse | Shop completes a job without `/ops/` |

### Immediate (this week)

1. Enable Google Cloud billing for Places.  
2. Verify a real domain on Resend; then change `MAIL_FROM`.  
3. Create a Google OAuth Android client (package name + SHA-1). iOS client only when a Swift app starts.  
4. Keep Fly API pointed at Neon.  
5. Then start Phase 1 in `/android` (Kotlin).

---

## 25. Success metrics (30 days after store)

| Metric | Target |
|---|---|
| Install → first paid order (test cohort) | ≥ 25% |
| Nearby empty rate in launch city | < 15% of book attempts |
| OTP delivery (verified domain) | > 95% |
| Crash-free sessions | > 99% |
| Assist chats that escalate to email | < 20% |

---

## 26. Store & compliance checklist

- [ ] Privacy policy URL (location, email, identifiers, payments via Paystack)  
- [ ] Account deletion or support path (Play Console)  
- [ ] Location purpose string (booking + map only)  
- [ ] Push purpose string (FCM)  
- [ ] Google Android OAuth client (package + SHA-1)  
- [ ] Paystack callback / app links  
- [ ] No OTP or PAN in logs/crash reports  
- [ ] NDPR-minded retention of orders and sessions  

---

## 27. Glossary

| Term | Meaning |
|---|---|
| Offer | A nearby partner shown for customer selection |
| Partner | Laundry fulfilling wash/fold/etc. |
| Order | One job from request to rating |
| Assist | In-trip AI + system messages, not the shop |
| Confirm receipt | Customer action that completes delivery and alerts the partner |
| Ops | PIN web monitor, not the customer app |
| `sw_` token | Session in `auth_sessions` |

---

*skyWash Cleaning — laundry, on demand. Native apps consume the same marketplace the web already runs.*
