# Dexter AI cloud operator improvements

This release changes the isolated Dexter AI test workspace and its two test Edge Functions.

## User commands

- “Run the shop check” / “What needs attention?” reads cloud operational records, reports stale connections, outstanding work and overdue schedules, and saves a task and report artifact. It makes no model calls and works while the home PC is offline.
- “Show my tasks” reads the recent task queue without a model.
- “Check supplier emails” / “Check if Dunns replied” uses read-only cloud Gmail when the owner has connected it.
- “Check bOnline voicemails” lists voicemail email details; it does not claim to have transcribed audio.
- Clear imperative requests route from ordinary chat into tracked Work mode. Questions remain conversational. Unsupported physical commands are saved as needs_verification, never completed.

Chat has quick buttons for reports, tasks and emails, links to tracked work, prevents simultaneous submissions, restores the prompt after errors, and shows explicit unverified timeout outcomes.

## Verification improvements

- Old connector “ready” markers no longer count as recent verification. Device probes expire after two minutes; cloud connector probes after 36 hours.
- AI endpoint configuration does not count as verified inference. A recent local worker heartbeat indicates availability, not proof of a successful answer.
- Empty, queued, running and failed tool results cannot complete execution tasks.
- Work completion waits for the reviewer to finish. Visual verification artifacts now use the deployed database's supported diagnostic artifact type.
- Latest conversation messages are selected before reversing into chronological display order; long chats no longer continually reuse the oldest messages.
- Browser/research evidence is included in the chat context with an instruction to treat page content as untrusted evidence.
- Direct AI requests have response deadlines, and timed-out local model jobs are marked failed.
- Health/dashboard/connection refreshes do not all fail when one endpoint fails. The visible page refreshes every minute and on reconnection.
- The service worker only caches the static shell, refreshes cached shell responses and removes old Dexter shell caches. API results are not cached.

## Daily report

The existing authenticated Dexter scheduler runs `operator_report` daily at 06:00 Europe/London, accounting for clock changes. Reports appear in Tasks, Artifacts and Dexter's existing notifications. The report takes a cloud snapshot; existing scheduled endpoint checks remain separate. It does not send email, WhatsApp or push notifications outside Dexter.

## Limits

Cloud Gmail still requires owner OAuth setup. No new model provider, subscription or paid infrastructure is introduced. Existing model and hosting usage policies still apply to ordinary AI work. Hardware printing, physical cash drawer operation, real payment testing and order/KDS journeys are not verified by a cloud-record report.

The previous runtime and Gmail implementation are preserved under the respective `releases` directories. To roll back, deploy those files as the corresponding function entrypoint, retaining the existing custom authentication and `verify_jwt=false` configuration.

## Checks

Node tests cover routing, stale health, cloud-only operation, failed-data reads, duplicate task submissions, prompt recovery, Gmail queries, queued tool evidence and UK clock changes. Authenticated deployed checks cover the shop report, task lookup, unsupported printing and unavailable Gmail. Missing authentication and insufficient roles are rejected.

Browser rendering could not be inspected in the execution container because a browser binary is unavailable. Frontend JavaScript and DOM behaviour were checked with a mocked DOM; this is not visual verification.
