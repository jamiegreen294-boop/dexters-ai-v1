# Dexter AI Test Workspace

Dexter AI is the isolated test command centre for Dexters.

## Current architecture

- Frontend: `index.html`
- Command centre: Supabase Edge Function `ai-command-centre`
- Database: separate `Dexters-AI-Test` Supabase project
- AI: Dexter Home PC / Ollama with cloud fallback when configured
- Connectors: GitHub, Vercel, Supabase, browser worker and Home PC
- Work system: tasks, approvals, orchestration, persistent artifacts and audit logs
- Knowledge: synced Dexters business knowledge plus live-menu reads
- Memory: owner-approved durable memory, separate from conversation history

## Safety boundary

This repository is TEST only.

- No direct live POS write access
- No direct live KDS write access
- No direct live Loyalty App write access
- No direct live Back Office write access
- Live business data is read only where explicitly enabled
- Consequential connector tools require owner approval
- Owner approval currently permits TEST execution/preview only
- Secrets are stored server-side and must never be placed in frontend code

## Workspace areas

- Chat
- Work
- Tasks
- Artifacts
- Approvals
- Memory
- Learning
- Connections
- Health
- Audit activity

## Authentication

The browser sends the Dexter owner code using `x-dexter-token` to the command-centre Edge Function. The function hashes it and validates it against active access keys. Supabase JWT verification stays disabled for this Edge Function because it uses this custom owner-access scheme instead.

## Database access

Public Dexter AI tables use RLS. Tables without client policies are intentionally deny-by-default because the browser does not access them directly; the authenticated command-centre function performs required server-side operations using the service role.

## Legacy API

`/api/chat` no longer has a separate model/personality. It forwards to the Supabase command centre so Dexter has one AI path.
