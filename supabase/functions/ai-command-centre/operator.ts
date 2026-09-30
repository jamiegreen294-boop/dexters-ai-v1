// Cloud-only operator helpers. No model calls, device commands or live-system writes.
export function operatorIntent(message: string): string {
  const m = message.trim().replace(/^(?:dexter(?: ai)?[, :]*)/i, '').replace(/^(?:please|can you|could you|will you)\s+/i, '').toLowerCase();
  if (/^(?:run|do|give me|show me|check)\s+(?:(?:the|a|my|daily|full)\s+)*(?:shop|business|system|operations?)\s+(?:check|report|status)/.test(m)
    || /^(?:what(?:'s| is) working|what needs attention|what(?:'s| is) (?:offline|connected)|are (?:the )?systems (?:working|online))\??$/.test(m)) return 'operator_report';
  if (/^(?:show|list|check)\s+(?:(?:my|the|recent|outstanding|active)\s+)*(?:tasks|work queue)\b/.test(m)) return 'task_summary';
  if (/^(?:check|show|list|read|find)\b/.test(m) && /\b(voicemails?|voice mails?)\b/.test(m)) return 'voicemail_list';
  if (/^(?:check|show|list|read|find)\b/.test(m) && /\b(gmail|emails?|supplier replies|suppliers)\b/.test(m)) return 'gmail_search';
  if (/^check\b.{0,80}\breplied\b/.test(m)) return 'gmail_search';
  if (/^(?:build|fix|repair|create|prepare|draft|write|design|print|install|update|deploy|publish|send|delete|remove|amend|cancel|research|find online)\b/.test(m)) return 'work';
  return 'chat';
}

export function connectorEvidence(row: any, at = Date.now()) {
  const checked = Date.parse(row.last_checked_at || '');
  const age = at - checked;
  const fresh = Number.isFinite(age) && age >= -60000 && age <= 36 * 3600000;
  const device = ['home-host', 'browser', 'local-ai'].includes(row.connector_key);
  const maxAge = device ? 120000 : 36 * 3600000;
  const verified = row.status === 'ready' && fresh && age <= maxAge;
  const status = row.status === 'ready' ? (verified ? 'verified' : 'stale') : (row.status || 'unknown');
  return { connector_key: row.connector_key, name: row.name, status, checkedAt: row.last_checked_at || null,
    verification: verified ? 'Last successful probe is recent; this does not prove every feature works.' : 'No recent successful probe.',
    homePcRequired: device };
}

export function operationalStatus(rows: any[], at = Date.now()) {
  if (!rows.length) return 'unknown';
  if (rows.some(r => ['offline', 'failed', 'critical', 'error'].includes(String(r.status).toLowerCase()))) return 'attention';
  if (rows.some(r => !Number.isFinite(Date.parse(r.checked_at)) || at - Date.parse(r.checked_at) > 2 * 3600000
    || !['healthy', 'ready', 'passed', 'ok', 'online'].includes(String(r.status).toLowerCase()))) return 'unverified';
  return 'healthy';
}

export function requiresExecutionEvidence(request: string) {
  return /\b(print|install|restart|reboot|deploy|publish|send|pay|refund|charge|delete|update|fix|repair|connect|disconnect|pair|sync|amend|cancel)\b/i.test(request)
    && !/^(?:explain|how\b|why\b|draft\b|write\b|describe|suggest|plan\b)/i.test(request.trim());
}

export function toolEvidenceMissing(value: any): boolean {
  if (!value || typeof value !== 'object' || !Object.keys(value).length) return true;
  if (value.error || value.ok === false || value.success === false) return true;
  if (['failed', 'error', 'queued', 'pending', 'running', 'needs_verification', 'waiting_approval', 'queued_home', 'cancelled'].includes(String(value.status || '').toLowerCase())) return true;
  for (const key of ['result', 'coding', 'research']) if (key in value && toolEvidenceMissing(value[key])) return true;
  return false;
}

export function nextUkMorning(at = Date.now()) {
  const parts = new Intl.DateTimeFormat('en-GB', {timeZone:'Europe/London', year:'numeric',month:'numeric',day:'numeric'}).formatToParts(at);
  const number = (type:string) => Number(parts.find(p=>p.type===type)?.value);
  const candidate = Date.UTC(number('year'),number('month')-1,number('day')+1,6);
  const offset = new Intl.DateTimeFormat('en-GB',{timeZone:'Europe/London',timeZoneName:'shortOffset'}).formatToParts(candidate).find(p=>p.type==='timeZoneName')?.value || 'GMT';
  const hours = Number(offset.match(/GMT([+-]\d+)/)?.[1] || 0);
  return new Date(candidate-hours*3600000).toISOString();
}

export function gmailQuery(message: string) {
  const explicit = message.match(/(?:query|search)\s*:\s*(.{1,500})$/i)?.[1];
  if (explicit) return explicit.trim();
  const supplier = message.match(/\b(?:if|whether)\s+(.{1,60}?)\s+(?:has\s+)?replied\b/i)?.[1]
    || message.match(/^\s*(?:please\s+)?check\s+(.{1,60}?)\s+(?:has\s+)?replied\b/i)?.[1];
  const from = message.match(/\b(?:from|for)\s+([\p{L}\p{N}@.' -]{1,80})/iu)?.[1];
  const term = (supplier || from || '').trim().replace(/[^\p{L}\p{N}@. -]/gu, '');
  return 'newer_than:30d' + (term ? ' "' + term + '"' : '');
}

export async function cloudReport(db: any, modelConfig: any) {
  const at = Date.now(), checkedAt = new Date(at).toISOString();
  const results = await Promise.all([
    db.from('dexter_home_agents').select('name,last_seen_at,active,capabilities').eq('active', true).order('last_seen_at', { ascending: false }).limit(10),
    db.from('ai_connectors').select('connector_key,name,status,last_checked_at').order('name'),
    db.from('ai_tasks').select('id,title,status,progress,updated_at').in('status', ['running', 'queued_home', 'failed', 'waiting_approval', 'needs_verification']).order('updated_at', { ascending: false }).limit(30),
    db.from('dexter_operations_health').select('system_key,system_name,status,summary,checked_at').order('checked_at', { ascending: false }).limit(100),
    db.from('dexter_scheduled_jobs').select('name,action,last_run_at,next_run_at,last_status').eq('enabled', true).order('name'),
    db.from('dexter_business_knowledge').select('*', { count: 'exact', head: true }),
    db.from('dexter_approved_memory').select('*', { count: 'exact', head: true })
  ]);
  for (const r of results) if (r.error) throw new Error('Operator report could not read required test data: ' + r.error.message);
  const [agents, connectors, tasks, ops, jobs, knowledge, memory] = results;
  const onlineAgents = (agents.data || []).filter((a: any) => at - Date.parse(a.last_seen_at) < 90000);
  const localAi = onlineAgents.some((a: any) => Array.isArray(a.capabilities) && a.capabilities.includes('local_ai'));
  const latest = new Map<string, any>();
  for (const row of ops.data || []) if (!latest.has(row.system_key)) latest.set(row.system_key, row);
  const checks = [...latest.values()];
  const connections = (connectors.data || []).map((r: any) => connectorEvidence(r, at));
  const overdue = (jobs.data || []).filter((j: any) => Date.parse(j.next_run_at || '') < at - 30 * 60000 || j.last_status === 'failed');
  const attention = [
    ...(!onlineAgents.length ? ['Home PC has no recent heartbeat. Local AI and PC hardware jobs may be unavailable.'] : []),
    ...connections.filter((c: any) => c.status !== 'verified').map((c: any) => c.name + ': ' + c.status + '.'),
    ...(tasks.data || []).filter((t: any) => ['failed', 'needs_verification', 'waiting_approval'].includes(t.status)).map((t: any) => t.title + ': ' + t.status + '.'),
    ...checks.filter((r: any) => operationalStatus([r], at) !== 'healthy').map((r: any) => r.system_name + ': ' + r.status + ' (last check ' + r.checked_at + ').'),
    ...overdue.map((j: any) => j.name + ': scheduled check needs attention.')
  ];
  const report = { checkedAt, timezone: 'Europe/London', environment: 'test', modelCalls: 0, homePcRequired: false,
    cloudDatabase: 'verified', homePcOnline: onlineAgents.length > 0, homeLastSeenAt: agents.data?.[0]?.last_seen_at || null,
    ai: { localHeartbeat: localAi, cloudConfigured: Boolean(modelConfig.cloud), directLocalConfigured: Boolean(modelConfig.directLocal), inferenceVerified: false },
    connections, tasks: tasks.data || [], checks, overall: attention.length ? 'attention' : operationalStatus(checks, at),
    knowledgeItems: knowledge.count || 0, approvedMemoryItems: memory.count || 0, scheduledJobs: jobs.data || [], attention,
    limitations: ['This report reads cloud records. It does not test a payment, print paper or prove the KDS order journey.', 'Cloud AI configuration does not prove a model response will succeed.'] };
  const stamp = new Date(checkedAt).toLocaleString('en-GB', { timeZone: 'Europe/London' });
  const reply = ['Dexter shop check · ' + stamp + ' (UK time)',
    'Cloud database: checked successfully.', 'Home PC: ' + (report.homePcOnline ? 'recent heartbeat received.' : 'offline or heartbeat stale.'),
    'AI: ' + (localAi ? 'local worker checked in; inference untested.' : modelConfig.cloud ? 'cloud provider configured; inference untested.' : 'no confirmed available model.'),
    'Business knowledge: ' + report.knowledgeItems + ' items. Approved memory: ' + report.approvedMemoryItems + ' items.',
    'Attention: ' + (attention.length ? '\n- ' + attention.slice(0,20).join('\n- ') : 'none identified in these records.'),
    'These checks use no AI model. Printer output, payments and order flows still need their own verification.'
  ].join('\n');
  return { report, reply };
}
