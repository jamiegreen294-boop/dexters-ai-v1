# Dexter AI

Private, server-backed Dexters assistant. This branch replaces the placeholder with a working chat endpoint, live read-only business context, permission-aware responses, and Supabase conversation memory.

## Required Vercel environment variables

- OPENAI_API_KEY
- OPENAI_MODEL (optional; defaults to gpt-5-mini)
- DEXTER_ACCESS_TOKEN
- DEXTER_AI_SUPABASE_URL
- DEXTER_AI_SUPABASE_SERVICE_ROLE_KEY
- DEXTERS_DATA_SUPABASE_URL
- DEXTERS_DATA_SUPABASE_SERVICE_ROLE_KEY

No secret is stored in browser code or GitHub. Live writes and deployments remain approval-gated.
