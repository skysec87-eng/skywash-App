# skyWash Cleaning — Complete Product & Platform Documentation

**Version:** 1.0 (Prototype)  
**Product:** skyWash Cleaning  
**Category:** On-demand laundry marketplace (mobility-style logistics)  
**Document type:** Product, operations, and technical reference  

---

## 1. Executive summary

**skyWash Cleaning** is an on-demand laundry pickup and delivery platform. Customers request a wash the same way they request a ride: set a location, choose a service, pick a nearby laundry partner, track the job live, then rate the experience.

The product borrows the **request → match/choose → track → complete → rate** loop popularized by Uber, with a **customer-chooses-partner** interaction closer to **inDrive**, applied to laundry instead of passenger transport.

| Dimension | Uber (rides) | skyWash (laundry) |
|---|---|---|
| What is requested | A trip from A → B | Pickup → wash → delivery |
| Supply side | Drivers | Laundry partners (+ couriers) |
| Matching | Platform-assigned (classic Uber) | Customer picks from nearby offers (inDrive-style) |
| Geography | Multi-city / multi-country | Multi-city by design (not locked to one city) |
| Unit of work | Ride | Laundry order |
| Live tracking | Driver GPS | Courier / trip stage simulation (prototype) |

This document describes **what the product is**, **how it behaves today**, and **what a production Uber-class version requires**.

---

## 2. Vision & positioning

### 2.1 Vision

Make laundry as easy as ordering a ride — available in every city we operate, on demand or scheduled, with transparent pricing and live status.

### 2.2 Positioning

- **Category:** Local logistics + service marketplace  
- **Promise:** “Book laundry pickup wherever you are.”  
- **Not:** A single-shop laundry website, or a Lagos-only tool  
- **Like Uber:** Location-based, multi-city, request → fulfill → rate  
- **Like inDrive:** You see nearby partners and choose who to book  

### 2.3 Brand

| Element | Value |
|---|---|
| Brand name | skyWash |
| Full product name | skyWash Cleaning |
| Tagline (UI) | On-demand cleaning |
| Promo code (demo) | `SKY10` (10% off) |

---

## 3. How skyWash relates to Uber

### 3.1 Shared platform principles

1. **Demand is location-based** — the map and GPS define who can serve you.  
2. **Supply is a marketplace** — independent laundry partners, not only company-owned shops.  
3. **The job has a lifecycle** — confirmed → en route → in progress → delivered.  
4. **Trust is rated** — stars after completion shape future selection.  
5. **Cities are markets** — launch city by city; the app is the same, inventory differs.  
6. **Mobile-first** — installable web app (PWA), designed for phone use.

### 3.2 Where skyWash intentionally differs

| Uber pattern | skyWash choice | Why |
|---|---|---|
| Auto-dispatch one driver | Show nearby partners; user selects | More control, clearer for laundry quality/brand preference |
| Instant trip only | Pickup now **or** schedule later | Laundry is often planned, not only impulse |
| Per-km fare | Weight/item + fees + promo | Laundry pricing is service-based |
| Two parties (rider, driver) | Customer, laundry, (later) courier | Wash happens at a facility |
| Seconds of matching | Short “looking nearby” then offer list | Simpler UX for demo; production can add partner accept |

### 3.3 Analogy map (Uber vocabulary → skyWash)

| Uber term | skyWash term |
|---|---|
| Rider | Customer |
| Driver | Laundry partner / courier |
| Trip | Order |
| Pickup | Laundry bag pickup |
| Drop-off | Clean laundry delivery |
| Surge | (Future) peak / express premium |
| Vehicle type | Service type (Wash & Fold, Dry Clean, Iron, Express) |
| City / region | Operating city (Lagos, Abuja, Port Harcourt, …) |
| ETA | Estimated pickup / delivery time |
| Receipt | Order total + history |

---

## 4. Market model (multi-city)

skyWash is **not tied to one city**. Like Uber:

- The **product** is global/national in design.  
- The **supply** (laundry partners) is local.  
- The **app** discovers partners near the customer’s pickup pin.

### 4.1 Current demo cities

| City | Role in prototype |
|---|---|
| Lagos | Dense partner seed data |
| Abuja | Multi-city proof |
| Port Harcourt | Multi-city proof |

### 4.2 Nearby radius

Partners are offered only if they are within **`NEARBY_RADIUS_KM = 40`** of the customer’s pickup location. If none qualify, the UI explains that no partners are nearby.

### 4.3 Expanding to a new city (production playbook)

Same pattern Uber uses when entering a market:

1. Onboard laundry partners (KYC, rates, capacity, hours).  
2. Define city pricing / fees.  
3. Recruit or assign couriers for pickup/delivery.  
4. Soft launch + support.  
5. Turn the city “live” in the backend so partners appear in nearby search.

---

## 5. Users & roles

### 5.1 Customer (implemented in prototype)

- Sets pickup location  
- Chooses service, weight/items, payment method, promo  
- Requests pickup, chooses a partner, confirms  
- Tracks order stages, chats with partner (quick replies)  
- Rates and sees order history  

### 5.2 Laundry partner (future production)

- Receives / accepts jobs  
- Confirms bag received, wash started, ready for delivery  
- Manages capacity and hours  
- Earns payouts  

### 5.3 Courier / rider (future production)

- Picks up dirty laundry from customer  
- Delivers to laundry, then returns clean laundry  
- Live GPS (Uber-like tracking)  

### 5.4 Operations / admin (future production)

- City launches, partner vetting, disputes, refunds, fraud  

---

## 6. Core customer journey

```text
Open app
  → Set pickup (GPS or address)
  → Choose service + weight + payment (+ optional schedule / promo)
  → Request pickup
  → See nearby partner offers (sorted by distance)
  → Select partner → Confirm
  → Live trip stages (map + status stepper)
  → Optional in-trip chat
  → Delivered → Rate
  → Order saved to history
```

This mirrors Uber’s trip loop with laundry-specific stages in the middle.

---

## 7. Features (current product)

### 7.1 Book pickup

| Feature | Behavior |
|---|---|
| Pickup now / Schedule later | Toggle; schedule shows day + time slots |
| Location | GPS button or typed address |
| Service types | Wash & Fold, Dry Cleaning, Iron Only, Express (4h) |
| Weight / items | Stepper 1–20; unit follows service (kg or item) |
| Payment method | Card, bank transfer, cash on pickup (UI preference) |
| Promo | `SKY10` → 10% off |
| Estimate box | Base fee + service + 10% service fee − promo = total |

### 7.2 Choose a partner (inDrive-style)

After a short “Looking nearby…” state:

- Up to **8** partners within **40 km**, closest first  
- Each offer shows: name, rating, city · area, ETA, distance  
- User taps one offer, then **Confirm pickup**  
- Map highlights the selected partner  

### 7.3 Live order tracking

Status machine (prototype timings are demo-short):

| Status key | Label | Demo duration |
|---|---|---|
| `confirmed` | Request confirmed | 2.5s |
| `enroute` | Partner heading to you | 5s (+ rider animation) |
| `pickedup` | Picked up from you | 2.5s |
| `washing` | Washing at the laundromat | 5s |
| `delivering` | Out for delivery | 5s (+ rider animation) |
| `delivered` | Delivered | → rating screen |

### 7.4 Map & browse

- Leaflet + OpenStreetMap tiles  
- Partner pins on the map  
- Browse tab: search by name, area, or **city**  
- Mobile: bottom nav + map FAB for full-screen map  

### 7.5 In-trip chat

Quick-message chips to the partner; demo auto-replies after a short delay.

### 7.6 Rating & history

- 1–5 stars after delivery  
- Orders stored in browser `localStorage` (`skywash_orders`)  
- History panel lists past pickups; “Book again” returns to booking form  

### 7.7 PWA

- `manifest.json` + service worker (`sw.js`)  
- Installable to home screen; basic offline shell caching  

---

## 8. Pricing model (prototype)

### 8.1 Formula

```text
base_pickup_fee     = ₦500
service_cost        = weight_or_items × rate
platform_fee        = service_cost × 10%
subtotal            = base + service_cost + platform_fee
discount            = subtotal × promo_rate   (0 or 0.10 for SKY10)
total               = subtotal − discount
```

### 8.2 Service rates (demo)

| Service | Rate | Unit |
|---|---|---|
| Wash & Fold | ₦500 | kg |
| Dry Cleaning | ₦1,500 | item |
| Iron Only | ₦300 | kg |
| Express (4h) | ₦900 | kg |

### 8.3 Production pricing (Uber-like roadmap)

- City-specific base fees and rates  
- Distance-based courier fee (pickup + delivery legs)  
- Peak / express multipliers  
- Partner take-rate vs platform commission  
- Taxes and receipting (e.g. VAT where required)  
- Real payments via **Paystack** or **Flutterwave**  

---

## 9. Matching & dispatch

### 9.1 Prototype algorithm

1. Compute Haversine distance from customer to every partner.  
2. Keep partners with `distance ≤ 40 km`.  
3. Sort ascending by distance.  
4. Take top 8.  
5. Customer selects one; order “starts” on confirm.

### 9.2 Production dispatch (Uber-class)

| Capability | Description |
|---|---|
| Availability | Only online / open partners |
| Capacity | Bags in progress vs daily limit |
| Accept / decline | Partner must accept (with timeout) |
| Cascade | Offer next partner if declined / timed out |
| Courier assignment | Separate matching for pickup rider |
| Fairness / quality | Rating, cancel rate, SLA |
| Batching | Optional multi-stop courier routes |

---

## 10. Order lifecycle (state machine)

```text
DRAFT (form)
  → REQUESTED (looking nearby)
  → OFFERED (list shown)
  → CONFIRMED (customer picked partner)
  → EN_ROUTE_PICKUP
  → PICKED_UP
  → WASHING
  → EN_ROUTE_DELIVERY
  → DELIVERED
  → RATED
  → CLOSED

Any active state → CANCELLED (customer cancel in prototype)
```

In production, add partner-side transitions, payment authorization holds, refunds, and dispute states.

---

## 11. Current technical architecture (prototype)

### 11.1 Stack

| Layer | Technology |
|---|---|
| UI | Static HTML |
| Styles | `css/styles.css` |
| Logic | Vanilla JavaScript (`js/app.js`) |
| Maps | Leaflet + OSM |
| Persistence | `localStorage` |
| Hosting | Static (Vercel-ready) |
| PWA | `manifest.json`, `sw.js`, `js/register-sw.js` |

### 11.2 Repository layout

```text
sudsnear-deploy/
├── index.html          # App shell / markup
├── css/styles.css      # Design system & layout
├── js/
│   ├── app.js          # Booking, map, trip, history
│   └── register-sw.js  # Service worker registration
├── sw.js               # Offline cache
├── manifest.json       # PWA metadata
├── icons/              # App icons
├── vercel.json         # Static hosting headers
├── README.md           # Deploy quickstart
└── DOCUMENTATION.md    # This document
```

### 11.3 Important client keys

| Key | Purpose |
|---|---|
| `skywash_orders` | Order history JSON |
| `skywash_payment` | Last payment method preference |
| Cache name | `skywash-v1` |

### 11.4 What is simulated today

- Partner inventory (static array in JS)  
- Matching (client-side distance)  
- Address geocoding (typed address ≈ map center noise)  
- Courier movement (straight-line animation)  
- Payments (UI only)  
- Chat replies (random canned messages)  
- Auth (none)  

---

## 12. Target production architecture (Uber-like)

To become a real multi-city product, skyWash needs a **frontend + backend**, like Uber’s client apps + services.

```text
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│ Customer app    │────▶│ API gateway      │────▶│ Services        │
│ (Web / PWA /    │     │ Auth, rate limit │     │ Users, Orders,  │
│  future native) │◀────│                  │◀────│ Partners, Pay,  │
└─────────────────┘     └──────────────────┘     │ Dispatch, Chat  │
                                                  └────────┬────────┘
                                                           │
                                                  ┌────────▼────────┐
                                                  │ Postgres + Redis │
                                                  │ + object storage │
                                                  └─────────────────┘
```

### 12.1 Recommended stack (pragmatic)

| Concern | Suggestion |
|---|---|
| API | Node (Express/Nest) or Supabase Edge + Postgres |
| Database | PostgreSQL |
| Auth | Phone OTP / email (Supabase Auth or custom) |
| Realtime | WebSockets / Supabase Realtime for trip updates |
| Maps | Mapbox or Google Maps + Places geocoding |
| Payments | Paystack / Flutterwave |
| Push | FCM / APNs |
| Admin | Internal dashboard |
| Partner app | Separate web or mobile app |

### 12.2 Core domain entities

**User** — id, phone, name, default payment, ratings given  

**City** — id, name, currency, active, fee config  

**Partner** — id, city_id, name, location (lat/lng), rating, hours, capacity, status  

**Courier** — id, city_id, location, status (online/busy)  

**Order** — id, customer_id, partner_id, courier_ids, pickup geo, service, weight, pricing breakdown, status, timestamps  

**Payment** — id, order_id, provider, amount, status  

**Rating** — id, order_id, stars, comment  

**ChatMessage** — id, order_id, sender, body, created_at  

---

## 13. Proposed API surface (production)

Illustrative REST shape (Uber-style resource model):

### Auth
- `POST /auth/otp/request`  
- `POST /auth/otp/verify` → session token  

### Places & supply
- `GET /partners/nearby?lat=&lng=&radius_km=`  
- `GET /partners/:id`  
- `GET /cities`  

### Orders
- `POST /orders` — create from estimate + selected partner  
- `GET /orders/:id`  
- `GET /orders` — history  
- `POST /orders/:id/cancel`  
- `POST /orders/:id/rate`  

### Pricing
- `POST /pricing/quote` — returns line items + total  

### Payments
- `POST /payments/initialize`  
- `POST /payments/webhook` (Paystack/Flutterwave)  

### Realtime
- `WS /orders/:id/stream` — status + courier location  

Partner / courier apps would add accept, status update, and location ping endpoints.

---

## 14. Trust, safety & quality

Uber-class expectations for skyWash:

| Area | Requirement |
|---|---|
| Identity | Verified customer & partner accounts |
| Payments | Tokenized cards; no raw PAN storage |
| Cancellations | Clear policies for both sides |
| Support | In-app help + dispute flow |
| Ratings | Two-way ratings long-term |
| Data privacy | Consent for location; NDPR/GDPR-minded storage |
| Partner quality | Onboarding standards, mystery shops, SLA |

---

## 15. Mobile UX principles (current)

- Desktop: sidebar booking + map  
- Mobile (&lt;820px): single column, bottom nav (Book / Map / Orders), map FAB  
- Safe-area padding for notched phones  
- Installable PWA (`standalone` display)  

---

## 16. Running the prototype locally

```bash
cd sudsnear-deploy
python3 -m http.server 3000
```

Open **http://127.0.0.1:3000/** and hard-refresh after code changes.

Deploy: static upload to Vercel (see `README.md`). No build step.

---

## 17. Roadmap: prototype → Uber-scale product

### Phase 0 — Current (demo)
- Multi-city seed data  
- Choose-partner flow  
- Simulated trip + local history  

### Phase 1 — Real backend MVP
- Auth  
- Partners in Postgres by city  
- Create/list orders  
- Wire UI to API  

### Phase 2 — Payments & ops
- Paystack checkout  
- Partner accept flow  
- Admin city/partner tools  

### Phase 3 — Live logistics
- Courier GPS  
- Real ETAs  
- Push notifications  
- Chat backend  

### Phase 4 — Scale
- More cities  
- Native apps  
- Dynamic pricing  
- Analytics & fraud  

---

## 18. Glossary

| Term | Meaning |
|---|---|
| Offer | A nearby partner shown for customer selection |
| Partner | Laundry business fulfilling wash/fold/etc. |
| Order | A single laundry job from request to rating |
| ETA | Estimated time for pickup or stage |
| PWA | Progressive Web App — installable website |
| Haversine | Great-circle distance used for nearby search |
| Marketplace | Platform connecting demand (customers) and supply (partners) |

---

## 19. Document ownership

| Item | Value |
|---|---|
| Product | skyWash Cleaning |
| Doc | Complete product & platform documentation |
| Audience | Founders, engineers, designers, investors, city ops |
| Source of truth for UI behavior | `index.html`, `js/app.js`, `css/styles.css` |

---

*skyWash Cleaning — laundry, on demand, in any city we serve.*
