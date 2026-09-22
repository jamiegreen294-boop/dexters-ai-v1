# Dexter Wholesale eSIM Provider Integration

Dexter's wholesale eSIM platform is provider-agnostic. Supplier credentials remain server-side.

## Required supplier details
- API base URL
- authentication method and production credential
- webhook signing/authentication method
- plan/catalog endpoint
- order/profile-create endpoint
- profile-status endpoint
- usage/balance endpoint if available
- suspend/resume/delete lifecycle endpoints
- SM-DP+ / activation-code fields returned for each profile
- ICCID/MSISDN fields if supplied
- supported countries/networks
- webhook event catalogue
- sandbox/test credentials
- production go-live/certification process

## Normalized Dexter events
The provider adapter converts supplier payloads to:
- `profile.ready`
- `profile.status`
- `order.status`

## Normalized profile fields
- providerProfileRef
- orderRef
- planId
- ICCID
- MSISDN
- activationRef
- activationPayload
- status
- expiry
- provider metadata

## Security
- API credentials and webhook secret are server-side only.
- The phone never receives the supplier API key.
- Browser clients never read the raw provider tables directly.
- eSIM database tables use RLS and revoke anon/authenticated access.
- Provider webhooks are rejected until a server-side webhook secret is configured.

## Current state
The database, Dexter command-centre API, owner console, provider gateway and Dexter OS device client are implemented.
Provider-specific API mapping remains disabled until the wholesale supplier provides the fields above.
