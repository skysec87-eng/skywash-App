# skyWash Cleaning — Extracted Backend API Spec

Derived from the current frontend (`js/app.js`, `index.html`).  
Each endpoint replaces logic that today runs entirely in the browser.

---

## 1. What the frontend does today (no backend)

| UI action | Current implementation | Needs API? |
|---|---|---|
| Load partners on map / browse | Hardcoded `LAUNDRIES` array | Yes |
| Nearby offers after “Request pickup” | Client Haversine, radius 40km, top 8 | Yes |
| Price estimate | Client formula + hardcoded rates | Yes (server is source of truth) |
| Promo `SKY10` | Hardcoded in JS | Yes |
| Confirm pickup / start trip | Local timers (`STATUSES`) | Yes — create + advance order |
| Cancel match / trip | Local reset | Yes |
| Order history | `localStorage` `skywash_orders` | Yes |
| Payment preference | `localStorage` `skywash_payment` | Optional (profile) |
| Geolocation | Browser GPS | No (client) |
| Typed address → lat/lng | Fake map-center jitter | Yes — geocode |
| In-trip chat | Fake auto-replies | Yes (later) |
| Rating | Saved only into local history | Yes |
| Map tiles | OSM / Leaflet | No (client CDN) |

---

## 2. Domain objects (from frontend shapes)

### Partner (from `LAUNDRIES`)
```json
{
  "id": "uuid",
  "name": "Laundry Care Lekki",
  "city": "Lagos",
  "area": "Lekki",
  "address": "1A Kayode Otitoju St, Eti-Osa, Lagos",
  "lat": 6.450511,
  "lng": 3.4704056,
  "rating": 5.0,
  "phone": "+2349072564972",
  "is_active": true
}
```

### Service catalog (from service cards)
| `type` | label | rate (₦) | unit |
|---|---|---|---|
| `wash` | Wash & Fold | 500 | kg |
| `dry` | Dry Cleaning | 1500 | item |
| `iron` | Iron Only | 300 | kg |
| `express` | Express (4h) | 900 | kg |

### Order (assembled across form → confirm → rate)
```json
{
  "id": "uuid",
  "status": "confirmed",
  "pickup": { "lat": 6.45, "lng": 3.42, "address": "Sangotedo" },
  "scheduled_at": null,
  "partner_id": "uuid",
  "services": [{ "type": "wash", "qty": 3 }, { "type": "iron", "qty": 3 }],
  "quantity": 3,
  "payment_method": "card",
  "promo_code": "SKY10",
  "pricing": {
    "base_fee": 500,
    "service_cost": 2400,
    "platform_fee": 240,
    "discount": 314,
    "total": 2826,
    "currency": "NGN"
  },
  "rating": null,
  "created_at": "...",
  "updated_at": "..."
}
```

### Order status machine (from `STATUSES`)
`requested` → `offered` → `confirmed` → `enroute` → `pickedup` → `washing` → `delivering` → `delivered` → `rated`  
Also: `cancelled` from active states.

---

## 3. Pricing rules to move server-side

From `updateEstimate()` / `servicesCost()`:

```text
base_fee      = 500
service_cost  = Σ (qty × rate) for each selected service
platform_fee  = service_cost × 0.10
subtotal      = base_fee + service_cost + platform_fee
discount      = subtotal × promo_rate   // SKY10 → 0.10
total         = subtotal − discount
```

Nearby matching (from `requestBtn`):

```text
dist = haversine(user, partner)
filter dist ≤ 40
sort by dist ASC
limit 8
```

ETA (asap): `max(8, round(dist_km * 4 + 6))` minutes  
ETA (scheduled): return `scheduled_at`

---

## 4. API surface (MVP for Vercel `/api`)

Base URL: same origin, e.g. `https://your-app.vercel.app/api`

### Priority A — required to un-hardcode the app

#### `GET /api/health`
Liveness check.

**Response**
```json
{ "ok": true, "service": "skywash-api" }
```

---

#### `GET /api/services`
Replace hardcoded service cards / rates.

**Response**
```json
{
  "services": [
    { "type": "wash", "label": "Wash & Fold", "rate": 500, "unit": "kg", "icon": "🧺" },
    { "type": "dry", "label": "Dry Cleaning", "rate": 1500, "unit": "item", "icon": "🧥" },
    { "type": "iron", "label": "Iron Only", "rate": 300, "unit": "kg", "icon": "👔" },
    { "type": "express", "label": "Express (4h)", "rate": 900, "unit": "kg", "icon": "⚡" }
  ],
  "base_fee": 500,
  "platform_fee_rate": 0.10,
  "currency": "NGN"
}
```

**Frontend today:** rates in `data-rate` on buttons + estimate math.

---

#### `GET /api/partners`
Browse / map pins (replaces `LAUNDRIES` + `renderBrowse`).

**Query**
| Param | Meaning |
|---|---|
| `q` | optional search (name, area, city) |
| `city` | optional city filter |
| `lat`, `lng` | optional — if set, include `distance_km` |

**Response**
```json
{
  "partners": [
    {
      "id": "...",
      "name": "...",
      "city": "Lagos",
      "area": "Lekki",
      "address": "...",
      "lat": 6.45,
      "lng": 3.47,
      "rating": 5.0,
      "phone": "+234...",
      "distance_km": 2.1
    }
  ]
}
```

---

#### `GET /api/partners/nearby`
Replace client matching after Request pickup.

**Query**
| Param | Default | Meaning |
|---|---|---|
| `lat`, `lng` | required | pickup point |
| `radius_km` | `40` | `NEARBY_RADIUS_KM` |
| `limit` | `8` | top N |

**Response**
```json
{
  "pickup": { "lat": 6.45, "lng": 3.42 },
  "radius_km": 40,
  "offers": [
    {
      "partner": { "id": "...", "name": "...", "city": "...", "area": "...", "lat": 0, "lng": 0, "rating": 4.9, "phone": "..." },
      "distance_km": 1.2,
      "eta_minutes": 11
    }
  ]
}
```

**Frontend today:** `requestBtn` → filter/sort `LAUNDRIES` → `showOffers()`.

---

#### `POST /api/pricing/quote`
Replace client `updateEstimate()` so totals can’t be spoofed.

**Request**
```json
{
  "services": [
    { "type": "wash", "qty": 3 },
    { "type": "iron", "qty": 3 }
  ],
  "promo_code": "SKY10"
}
```

**Response**
```json
{
  "currency": "NGN",
  "line_items": [
    { "type": "wash", "label": "Wash & Fold", "qty": 3, "unit": "kg", "rate": 500, "amount": 1500 },
    { "type": "iron", "label": "Iron Only", "qty": 3, "unit": "kg", "rate": 300, "amount": 900 }
  ],
  "base_fee": 500,
  "service_cost": 2400,
  "platform_fee": 240,
  "promo": { "code": "SKY10", "rate": 0.10, "valid": true },
  "discount": 314,
  "total": 2826,
  "label": "Wash & Fold · 3kg + Iron Only · 3kg"
}
```

---

#### `POST /api/promos/validate`
Optional split of promo check (today: `promoBtn` + `SKY10`).

**Request** `{ "code": "SKY10" }`  
**Response** `{ "valid": true, "code": "SKY10", "rate": 0.10, "message": "10% off" }`

---

#### `POST /api/geocode`
Replace fake address → coords.

**Request** `{ "address": "Sangotedo" }`  
**Response** `{ "lat": 6.43, "lng": 3.55, "formatted_address": "Sangotedo, Lagos, Nigeria" }`

---

#### `POST /api/orders`
Replace “Confirm pickup” (`confirmBtn` → `startTrip`).

**Request**
```json
{
  "partner_id": "uuid",
  "pickup": { "lat": 6.45, "lng": 3.42, "address": "Sangotedo" },
  "scheduled_at": null,
  "services": [{ "type": "wash", "qty": 3 }, { "type": "iron", "qty": 3 }],
  "payment_method": "card",
  "promo_code": "SKY10"
}
```

**Response** — full order with `status: "confirmed"` and server-computed `pricing`.

---

#### `GET /api/orders`
Replace `loadHistory()` / `renderHistory()`.

**Query:** `limit` (default 50)

**Response**
```json
{
  "orders": [
    {
      "id": "...",
      "provider_name": "Laundry Care Lekki",
      "service_label": "Wash & Fold · 3kg + Iron Only · 3kg",
      "total": 2826,
      "total_display": "₦2,826",
      "payment": "Debit / Credit Card",
      "rating": 5,
      "status": "rated",
      "created_at": "2026-08-06T..."
    }
  ]
}
```

---

#### `GET /api/orders/:id`
Live trip screen — poll instead of local `advanceStatus()` timers.

**Response**
```json
{
  "id": "...",
  "status": "enroute",
  "status_label": "Partner heading to you",
  "partner": { "id": "...", "name": "...", "rating": 5.0 },
  "pickup": { "lat": 6.45, "lng": 3.42 },
  "partner_location": { "lat": 6.44, "lng": 3.46 },
  "eta_label": "12 min",
  "timeline": [
    { "key": "confirmed", "label": "Request confirmed", "at": "..." },
    { "key": "enroute", "label": "Partner heading to you", "at": "..." }
  ],
  "pricing": { "total": 2826, "currency": "NGN" }
}
```

---

#### `POST /api/orders/:id/cancel`
Replace `cancelMatchBtn` / `cancelTripBtn` → `resetToForm` for real orders.

**Response** `{ "id": "...", "status": "cancelled" }`

---

#### `POST /api/orders/:id/rate`
Replace `doneBtn` rating write.

**Request** `{ "rating": 5, "comment": null }`  
**Response** `{ "id": "...", "status": "rated", "rating": 5 }`

---

### Priority B — auth & payments (needed for real product)

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/auth/otp/request` | Phone/email OTP start |
| `POST` | `/api/auth/otp/verify` | Return session JWT |
| `GET` | `/api/me` | Profile + payment preference |
| `PATCH` | `/api/me` | Save payment pref (replaces `skywash_payment`) |
| `POST` | `/api/payments/initialize` | Paystack/Flutterwave start |
| `POST` | `/api/payments/webhook` | Payment confirmation |

### Priority C — partner / courier / chat (later)

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/orders/:id/accept` | Partner accepts job |
| `POST` | `/api/orders/:id/status` | Partner/courier advances status |
| `POST` | `/api/couriers/:id/location` | Live GPS ping |
| `GET` | `/api/orders/:id/messages` | Chat history |
| `POST` | `/api/orders/:id/messages` | Send chat (replaces fake replies) |
| `GET` | `/api/cities` | Active cities for expansion |

---

## 5. Frontend → API call map

```text
App load
  GET /api/services
  GET /api/partners                 → map pins + browse

User sets location (GPS stays client)
  optional POST /api/geocode        → typed address

User changes services / qty / promo
  POST /api/pricing/quote           → estimate box

Request pickup
  GET /api/partners/nearby?lat&lng  → offer list

Confirm pickup
  POST /api/orders                  → create order
  poll GET /api/orders/:id          → trip stepper / map

Cancel
  POST /api/orders/:id/cancel

Rate + done
  POST /api/orders/:id/rate

My orders tab
  GET /api/orders
```

---

## 6. Suggested Vercel file layout

```text
api/
  health.js
  services.js
  partners/
    index.js          # GET list / search
    nearby.js         # GET nearby
  pricing/
    quote.js          # POST quote
  promos/
    validate.js       # POST validate
  geocode.js          # POST geocode
  orders/
    index.js          # GET list, POST create
    [id].js           # GET one
    [id]/
      cancel.js       # POST cancel
      rate.js         # POST rate
```

All Node serverless handlers — same repo, same Vercel deploy as the static frontend.

---

## 7. Build order (minimal risk)

1. **Catalog + partners** — `GET /services`, `GET /partners`, `GET /partners/nearby`  
2. **Pricing** — `POST /pricing/quote` (+ promo)  
3. **Orders CRUD** — create, get, list, cancel, rate  
4. **Geocode**  
5. **Auth + Paystack**  
6. **Realtime status / chat / courier GPS**

---

## 8. What stays on the client (no API)

- Leaflet map rendering & animations  
- Tab / mobile shell UI  
- Browser geolocation permission  
- PWA install / service worker shell  

---

*Source of truth for this extract: current `js/app.js` behavior as of skyWash multi-select + schedule picker.*
