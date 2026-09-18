# Dexter Browser Worker — £0 self-hosted browser

Runs Chromium on the Dexters PC and exposes a small authenticated browser API for Dexter AI.

## What it can do
- Open pages
- Keep login sessions in a persistent browser profile
- Snapshot clickable/form elements
- Click
- Fill fields
- Select dropdowns
- Press keys
- Read page text
- Take screenshots
- Close sessions

## Install on the Dexters Windows PC

1. Install Node.js LTS.
2. Open PowerShell in this folder.
3. Run:
   npm install
   npm run install-browser
4. Create a long random browser-worker token.
5. Set:
   $env:DEXTER_BROWSER_WORKER_TOKEN="YOUR_LONG_RANDOM_TOKEN"
6. Start:
   npm start

The worker only listens on 127.0.0.1 by default.

## Free remote connection

Install Cloudflare cloudflared and expose the local worker with a Cloudflare Tunnel. Point the tunnel at:
http://127.0.0.1:8765

Then configure the Dexter AI Edge Function secrets:
DEXTER_BROWSER_WORKER_URL=https://YOUR-BROWSER-HOSTNAME/tool
DEXTER_BROWSER_WORKER_TOKEN=the same token used by this worker

For a quick development-only tunnel:
cloudflared tunnel --url http://127.0.0.1:8765

A named Cloudflare Tunnel is recommended for a stable hostname.

## Security

- Never put the browser token in GitHub or frontend JavaScript.
- Keep the browser profile folder local; it contains logged-in sessions.
- Dexter command centre approval rules should remain enabled for login, submit, send, publish, delete, purchases and payment actions.
