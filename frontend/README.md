# HotelReviewAI — Frontend

Next.js (App Router) + Tailwind + shadcn/ui + TanStack Query frontend for the Spring Boot HotelReviewAI backend.

## Data source modes

| `NEXT_PUBLIC_DATA_SOURCE` | Behaviour |
|---|---|
| `mock` (default) | 40 seeded reviews stored in `localStorage`. Submitting/retrying simulates async analysis (Pending → Processing → Completed) using a TS port of the backend fallback heuristic. Demo login: `demo` / `demo123`. |
| `api` | Calls the Spring Boot REST API at `/api/v1/*`, proxied to `BACKEND_URL` via `next.config.ts` rewrites so session cookies stay first-party. |

See `.env.example`.

## Run locally

```bash
npm install
npm run dev
```

Open http://localhost:3000.

## Deploy on Vercel

1. Import the Git repository in Vercel.
2. Set **Root Directory** to `frontend` (framework is auto-detected).
3. Optional env var: `NEXT_PUBLIC_DATA_SOURCE=mock` (default).
4. Deploy.

When the backend is live, set `NEXT_PUBLIC_DATA_SOURCE=api` and `BACKEND_URL=https://your-backend` and redeploy.
