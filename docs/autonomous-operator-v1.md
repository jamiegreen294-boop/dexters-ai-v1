# Dexter Autonomous Operator — Test Build

Status: implemented in the isolated Dexters-AI-Test Supabase project.

## What was already present
- multi-agent work orchestration
- approval gating and tool permissions
- audit logs, projects, artifacts, postmortems and retries
- durable-memory proposals/approval flow
- Home PC and Dexter OS/device job infrastructure
- GitHub, Vercel and Supabase connectors
- strict visual verifier and completion blocking
- operations checks, scheduled jobs and evaluation cases

## Added in this build
### System dependency map
Tables: `dexter_system_nodes`, `dexter_system_edges`.
The map distinguishes TEST, read-only live and production-protected systems. Production POS, KDS, Loyalty and Back Office remain blocked from direct Dexter AI writes.

### Regression engine
Tables: `dexter_regression_suites`, `dexter_regression_checks`, `dexter_regression_runs`.
Command-centre actions: `regression_list`, `regression_run`.
The first suite is `dexter-core` and covers task/audit storage, Home PC heartbeat, phone heartbeat, connector registry and operations status.

### Last-known-good / rollback points
Table: `dexter_rollback_points`.
Command-centre actions: `rollback_points`, `rollback_point_create`, `rollback_mark_good`.
Rollback points are TEST-only and cannot be created for production-protected systems.

### Command-centre integration
New action: `system_map`.
The dashboard payload now also includes system nodes/edges, regression suites/runs and rollback points.

## Safety
This build does not enable direct production writes to the live POS, KDS, Loyalty App or Back Office. Existing approval gates remain in place.

## Runtime
Supabase Edge Function `ai-command-centre` version 75 is the first runtime containing these additions.
The initial last-known-good marker is `supabase-edge:ai-command-centre:v75`.
