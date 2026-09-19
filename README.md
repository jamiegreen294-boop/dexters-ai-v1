# Dexter AI — Test Workspace

Dexter AI is the isolated test assistant for Dexters.

## Current test architecture

- Frontend: `index.html` on branch `build/real-dexter-ai`
- Backend: Supabase Edge Function `ai-command-centre` in project `Dexters-AI-Test`
- Memory: `dexter_sessions` + `dexter_messages`
- Test knowledge: `dexter_ai_knowledge`
- Work tasks: `ai_tasks`, `ai_agent_tasks`, `ai_task_events`
- Agents: coding, platform doctor, business and customer assistant
- Security: hashed Dexter access keys plus server-side Supabase service role
- AI provider: OpenAI Responses API through the Supabase Edge Function

## Safety boundary

This build is intentionally test-only. It does not write to the live Loyalty App, live Supabase project, POS, KDS, Back Office, WhatsApp, payments, staff records or customer records.

Work mode creates and completes tasks inside `Dexters-AI-Test` only. Any future external/live tool connection must remain approval-gated.

## Branch

Do not merge this branch to `main` or connect live write tools until the owner explicitly approves the completed test build.
