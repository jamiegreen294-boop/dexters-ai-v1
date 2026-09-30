import test from 'node:test';
import assert from 'node:assert/strict';
import { operatorIntent, connectorEvidence, operationalStatus, requiresExecutionEvidence, gmailQuery, cloudReport, toolEvidenceMissing, nextUkMorning } from '../supabase/functions/ai-command-centre/operator.ts';

test('ordinary requests reach the right executor while questions stay conversational', () => {
  for (const message of ['Run the shop check', 'Dexter, can you run a daily business report', 'What needs attention?']) assert.equal(operatorIntent(message), 'operator_report');
  assert.equal(operatorIntent('Show my tasks'), 'task_summary');
  assert.equal(operatorIntent('Check supplier emails'), 'gmail_search');
  assert.equal(operatorIntent('Check if Dunns replied'), 'gmail_search');
  assert.equal(operatorIntent('Check bOnline voicemails'), 'voicemail_list');
  assert.equal(operatorIntent('Print all Sunday roast labels'), 'work');
  assert.equal(operatorIntent('Fix the test app'), 'work');
  assert.equal(operatorIntent('How do I print labels?'), 'chat');
  assert.equal(operatorIntent('What is a roast?'), 'chat');
});

test('historic ready markers and missing dates never count as verified availability', () => {
  const at = Date.parse('2026-09-30T22:00:00Z');
  assert.equal(connectorEvidence({connector_key:'home-host',status:'ready',last_checked_at:'2026-09-19T03:28:03Z'},at).status,'stale');
  assert.equal(connectorEvidence({connector_key:'github',status:'ready',last_checked_at:'2026-09-30T08:00:00Z'},at).status,'verified');
  assert.equal(connectorEvidence({connector_key:'home-host',status:'ready',last_checked_at:'2026-09-30T21:59:00Z'},at).status,'verified');
  assert.equal(connectorEvidence({status:'ready'},at).status,'stale');
  assert.equal(connectorEvidence({status:'not_configured'},at).status,'not_configured');
  assert.equal(operationalStatus([],at),'unknown');
  assert.equal(operationalStatus([{status:'healthy',checked_at:'2026-09-19T00:00:00Z'}],at),'unverified');
  assert.equal(operationalStatus([{status:'offline',checked_at:'2026-09-30T22:00:00Z'}],at),'attention');
});

test('action completion requires evidence but a requested draft can be delivered as a draft', () => {
  assert.equal(requiresExecutionEvidence('Print the collection labels'),true);
  assert.equal(requiresExecutionEvidence('Install the EPOS'),true);
  assert.equal(requiresExecutionEvidence('Fix the app'),true);
  assert.equal(requiresExecutionEvidence('Draft an email asking how to fix the printer'),false);
  assert.equal(requiresExecutionEvidence('How do I install the EPOS?'),false);
});

test('supplier names are carried into a bounded Gmail query', () => {
  assert.equal(gmailQuery('Check if Dunns has replied'), 'newer_than:30d "Dunns"');
  assert.equal(gmailQuery('Check emails from Fáilte Foods'), 'newer_than:30d "Fáilte Foods"');
  assert.equal(gmailQuery('Check suppliers'), 'newer_than:30d');
  assert.equal(gmailQuery('Search: from:dunns newer_than:7d'), 'from:dunns newer_than:7d');
});

test('queued tool results and empty payloads cannot count as completed execution',()=>{
  for(const payload of [null,{}, {coding:{}}, {result:{status:'queued'}}, {success:false}, {status:'running'}, {coding:{error:'printer offline'}}])assert.equal(toolEvidenceMissing(payload),true);
  assert.equal(toolEvidenceMissing({coding:{status:'completed',files:['index.html']}}),false);
});

test('daily cloud reports stay at 06:00 UK time through clock changes',()=>{
  assert.equal(nextUkMorning(Date.parse('2026-09-30T22:00:00Z')),'2026-10-01T05:00:00.000Z');
  assert.equal(nextUkMorning(Date.parse('2026-10-24T12:00:00Z')),'2026-10-25T06:00:00.000Z');
  assert.equal(nextUkMorning(Date.parse('2027-03-27T12:00:00Z')),'2027-03-28T05:00:00.000Z');
});

function fakeDb(records, failureTable) {
  return {from(table) {
    const response = table === failureTable ? {error:{message:'database unavailable'}} : {data:records[table]||[],count:(records[table]||[]).length,error:null};
    const query={select(){return query},eq(){return query},in(){return query},order(){return query},limit(){return query},then(resolve,reject){return Promise.resolve(response).then(resolve,reject)}};
    return query;
  }};
}
test('cloud shop report remains useful with no home PC and no model provider', async () => {
  const db=fakeDb({ai_connectors:[{name:'Gmail',connector_key:'gmail',status:'not_configured'}],dexter_business_knowledge:[{},{},{}],dexter_approved_memory:[{}]});
  const {report,reply}=await cloudReport(db,{cloud:false,directLocal:true});
  assert.equal(report.cloudDatabase,'verified');
  assert.equal(report.homePcOnline,false);
  assert.equal(report.homePcRequired,false);
  assert.equal(report.modelCalls,0);
  assert.equal(report.ai.inferenceVerified,false);
  assert.equal(report.knowledgeItems,3);
  assert.match(reply,/no confirmed available model/);
  assert.ok(report.attention.some(x=>x.includes('Gmail: not_configured')));
});
test('cloud report fails closed when required data cannot be read', async () => {
  await assert.rejects(cloudReport(fakeDb({},'ai_connectors'),{}),/could not read required test data/);
});
