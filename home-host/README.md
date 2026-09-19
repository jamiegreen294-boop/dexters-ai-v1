# Dexter AI Home PC Host

This makes the Dexters home PC the runtime host for Dexter AI.

## What runs on the PC
- Dexter AI web interface
- Persistent Chromium browser worker
- Saved browser/login sessions in a local browser profile

## What remains in the separate Supabase project
- AI command-centre backend
- Memory
- Knowledge
- Tasks
- Approvals
- Audit logs
- Connector registry

That keeps the expensive part at £0 while preserving the separate Dexters-AI-Test database.

## Install on Windows
1. Install Node.js LTS.
2. Open PowerShell in this folder.
3. Run:
   powershell -ExecutionPolicy Bypass -File .\install.ps1
4. Reopen Command Prompt.
5. Run:
   START-DEXTER.bat
6. Open:
   http://127.0.0.1:8787

## Remote access for £0
Use Cloudflare Tunnel to publish http://127.0.0.1:8787.

For Dexter's Supabase browser connector, point:
DEXTER_BROWSER_WORKER_URL=https://YOUR-HOST/browser/tool
DEXTER_BROWSER_WORKER_TOKEN=the PC token

Keep the browser profile folder private. It contains login sessions for sites you authorize Dexter to use.
