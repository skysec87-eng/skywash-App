# skyWash API (Spring Boot)

Java 21 + Spring Boot 3 REST API for skyWash Cleaning.

> **Deploy note:** This runs as a long-lived server. Host it on **Railway / Render / Fly.io / any JVM host**.  
> The static frontend stays on **Vercel**. Spring Boot does **not** deploy as Vercel serverless.

## Endpoints

| Method | Path | Status |
|---|---|---|
| GET | `/api/health` | Live |
| GET | `/api/services` | Live |
| GET | `/api/partners` | Live |
| GET | `/api/partners/nearby` | Live |
| GET | `/api/cities` | Live |
| POST | `/api/pricing/quote` | Live |
| POST | `/api/promos/validate` | Live |
| POST | `/api/geocode` | Live (demo) |
| POST | `/api/orders` | Live |
| GET | `/api/orders` | Live |
| GET | `/api/orders/{id}` | Live |
| POST | `/api/orders/{id}/cancel` | Live |
| POST | `/api/orders/{id}/rate` | Live |
| POST | `/api/orders/{id}/accept` | Stub |
| POST | `/api/orders/{id}/status` | Stub/manual |
| GET/POST | `/api/orders/{id}/messages` | Demo chat |
| POST | `/api/auth/otp/request` | Demo OTP `123456` |
| POST | `/api/auth/otp/verify` | Demo token |
| GET/PATCH | `/api/me` | Demo profile |
| POST | `/api/payments/initialize` | Stub |
| POST | `/api/payments/webhook` | Stub |
| POST | `/api/couriers/{id}/location` | Stub |

Full contract: [`../API.md`](../API.md)

## Run locally

```bash
# requires JDK 21+
cd backend
./mvnw spring-boot:run
# or: mvn spring-boot:run
```

API base: `http://localhost:8080`

### Smoke tests

```bash
curl -s http://localhost:8080/api/health
curl -s http://localhost:8080/api/services | head
curl -s "http://localhost:8080/api/partners/nearby?lat=6.45&lng=3.47"
curl -s -X POST http://localhost:8080/api/pricing/quote \
  -H 'Content-Type: application/json' \
  -d '{"services":[{"type":"wash","qty":3},{"type":"iron","qty":3}],"promo_code":"SKY10"}'
```

## CORS

Configured in `application.properties`:

```
skywash.cors.allowed-origins=http://127.0.0.1:3000,http://localhost:3000,https://sudsnear-deploy.vercel.app
```

Add your production frontend origin when ready.

## Database (PostgreSQL)

Local defaults (see `application.properties`):

| Setting | Value |
|---|---|
| Host | `localhost:5432` |
| Database | `skywash` |
| User | `skywash` |
| Password | `skywash` |

Start Postgres:

```bash
brew services start postgresql@16
```

Tables are created automatically (`spring.jpa.hibernate.ddl-auto=update`) and partners/services are seeded on first boot.