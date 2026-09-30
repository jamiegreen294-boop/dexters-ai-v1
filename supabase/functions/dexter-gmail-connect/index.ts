import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const CALLBACK = SUPABASE_URL + "/functions/v1/dexter-gmail-connect";
const GOOGLE_AUTH = "https://accounts.google.com/o/oauth2/v2/auth";
const GOOGLE_TOKEN = "https://oauth2.googleapis.com/token";
const GMAIL = "https://gmail.googleapis.com/gmail/v1/users/me";
const SCOPES = [
  "openid",
  "email",
  "https://www.googleapis.com/auth/gmail.readonly"
].join(" ");

function db() {
  return createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false } });
}
function html(body:string,status=200){
  return new Response("<!doctype html><html><head><meta name=viewport content='width=device-width,initial-scale=1'><title>Dexter Gmail Connect</title><style>body{font-family:system-ui;background:#0b0b0b;color:#fff;max-width:760px;margin:40px auto;padding:20px}input,button{width:100%;box-sizing:border-box;padding:14px;margin:7px 0;border-radius:10px;border:1px solid #333;background:#171717;color:#fff}button{background:#ef8b22;color:#111;font-weight:800;border:0}.ok{padding:18px;border:1px solid #2d5;border-radius:12px;background:#102016}.err{padding:18px;border:1px solid #d55;border-radius:12px;background:#241010}small{color:#aaa}code{word-break:break-all}</style></head><body>"+body+"</body></html>",{status,headers:{"content-type":"text/html; charset=utf-8","cache-control":"no-store"}});
}
function json(body:unknown,status=200){
  return new Response(JSON.stringify(body),{status,headers:{"content-type":"application/json","cache-control":"no-store","access-control-allow-origin":"*","access-control-allow-headers":"content-type,x-dexter-token","access-control-allow-methods":"POST,OPTIONS"}});
}
async function sha256(v:string){
  const b=await crypto.subtle.digest("SHA-256",new TextEncoder().encode(v));
  return Array.from(new Uint8Array(b)).map(x=>x.toString(16).padStart(2,"0")).join("");
}
function b64url(bytes:Uint8Array){
  return btoa(String.fromCharCode(...bytes)).replace(/\+/g,"-").replace(/\//g,"_").replace(/=+$/,"");
}
function fromB64url(v:string){
  const s=v.replace(/-/g,"+").replace(/_/g,"/");
  const padded=s+"=".repeat((4-s.length%4)%4);
  const bin=atob(padded);
  return new Uint8Array([...bin].map(c=>c.charCodeAt(0)));
}
function randomToken(n=32){const a=new Uint8Array(n);crypto.getRandomValues(a);return b64url(a);}
async function vaultStore(c:any,secret:string,name:string,desc:string){
  const {data,error}=await c.rpc("dexter_vault_store",{p_secret:secret,p_name:name,p_description:desc});
  if(error)throw error; return String(data);
}
async function vaultGet(c:any,id:string){
  const {data,error}=await c.rpc("dexter_vault_get",{p_id:id});
  if(error)throw error; return String(data||"");
}
async function binding(c:any,name:string){
  const {data,error}=await c.from("dexter_secret_bindings").select("*")
    .eq("provider","gmail").eq("secret_name",name).eq("active",true).maybeSingle();
  if(error)throw error; return data;
}
async function saveBinding(c:any,name:string,vaultId:string,purpose:string){
  const {error}=await c.from("dexter_secret_bindings").upsert({
    provider:"gmail",secret_name:name,vault_secret_id:vaultId,purpose,
    allowed_targets:["dexter-gmail-connect","ai-command-centre"],active:true,updated_at:new Date().toISOString()
  },{onConflict:"provider,secret_name"});
  if(error)throw error;
}
async function ownerAuth(req:Request,c:any){
  const token=req.headers.get("x-dexter-token")||"";
  if(token.length<12)return false;
  const hash=await sha256(token);
  const {data:key}=await c.from("dexter_access_keys").select("id,role,active")
    .eq("token_hash",hash).eq("active",true).maybeSingle();
  return Boolean(key && key.role==="owner");
}
async function clientCredentials(c:any){
  const cb=await binding(c,"oauth_client_id"), sb=await binding(c,"oauth_client_secret");
  if(!cb||!sb)throw new Error("Gmail OAuth app credentials are not configured.");
  return {
    clientId:await vaultGet(c,cb.vault_secret_id),
    clientSecret:await vaultGet(c,sb.vault_secret_id)
  };
}
async function currentTokens(c:any){
  const ab=await binding(c,"access_token"), rb=await binding(c,"refresh_token");
  if(!ab||!rb)throw new Error("Gmail is not connected yet.");
  const connector=await c.from("ai_connectors").select("config").eq("connector_key","gmail").maybeSingle();
  return {
    accessToken:await vaultGet(c,ab.vault_secret_id),
    refreshToken:await vaultGet(c,rb.vault_secret_id),
    expiresAt:String(connector.data?.config?.access_token_expires_at||"")
  };
}
async function refreshAccess(c:any,refreshToken:string){
  const {clientId,clientSecret}=await clientCredentials(c);
  const tr=await fetch(GOOGLE_TOKEN,{
    method:"POST",
    headers:{"content-type":"application/x-www-form-urlencoded"},
    body:new URLSearchParams({
      client_id:clientId,client_secret:clientSecret,refresh_token:refreshToken,grant_type:"refresh_token"
    })
  });
  const tok=await tr.json().catch(()=>({}));
  if(!tr.ok||!tok.access_token)throw new Error("Google token refresh failed.");
  const stamp=Date.now();
  const aid=await vaultStore(c,String(tok.access_token),"dexter_gmail_access_"+stamp,"Dexter Gmail OAuth access token");
  await saveBinding(c,"access_token",aid,"Gmail readonly API access token");
  const exp=new Date(Date.now()+Number(tok.expires_in||3600)*1000-60000).toISOString();
  const {data:row}=await c.from("ai_connectors").select("config").eq("connector_key","gmail").maybeSingle();
  await c.from("ai_connectors").update({
    config:{...(row?.config||{}),access_token_expires_at:exp},
    status:"ready",last_checked_at:new Date().toISOString(),last_error:null,updated_at:new Date().toISOString()
  }).eq("connector_key","gmail");
  return String(tok.access_token);
}
async function gmailToken(c:any){
  const t=await currentTokens(c);
  if(!t.expiresAt || Date.parse(t.expiresAt) < Date.now()+30000) return refreshAccess(c,t.refreshToken);
  return t.accessToken;
}
function flattenParts(p:any,acc:any[]=[]){
  if(!p)return acc;
  acc.push(p);
  for(const x of (p.parts||[]))flattenParts(x,acc);
  return acc;
}
function header(payload:any,name:string){
  return String((payload?.headers||[]).find((h:any)=>String(h.name||"").toLowerCase()===name.toLowerCase())?.value||"");
}
function decodeText(data:string){
  try{return new TextDecoder().decode(fromB64url(data));}catch{return "";}
}
function bodyText(payload:any){
  const parts=flattenParts(payload,[]);
  const plain=parts.find((p:any)=>p.mimeType==="text/plain"&&p.body?.data);
  if(plain)return decodeText(plain.body.data);
  const htmlPart=parts.find((p:any)=>p.mimeType==="text/html"&&p.body?.data);
  if(htmlPart)return decodeText(htmlPart.body.data).replace(/<[^>]+>/g," ").replace(/\s+/g," ").trim();
  return "";
}
function parseVoicemailText(text:string){
  const caller=text.match(/From:\s*([+0-9][0-9 ()-]{5,})/i)?.[1]?.trim()||null;
  const duration=text.match(/Duration:\s*([^\n\r<]+)/i)?.[1]?.trim()||null;
  const when=text.match(/Time:\s*([^\n\r<]+)/i)?.[1]?.trim()||null;
  return {caller,duration,when};
}
async function gmailFetch(c:any,path:string,init:RequestInit={}){
  let token=await gmailToken(c);
  let r=await fetch(GMAIL+path,{...init,signal:AbortSignal.timeout(12000),headers:{...(init.headers||{}),authorization:"Bearer "+token}});
  if(r.status===401){
    const t=await currentTokens(c);
    token=await refreshAccess(c,t.refreshToken);
    r=await fetch(GMAIL+path,{...init,signal:AbortSignal.timeout(12000),headers:{...(init.headers||{}),authorization:"Bearer "+token}});
  }
  if(!r.ok)throw new Error("Gmail API HTTP "+r.status);
  return r;
}
async function listVoicemails(c:any,maxResults=20){
  const q=encodeURIComponent('from:(automated@bonline.com) subject:"bOnline voicemail"');
  const lr=await gmailFetch(c,"/messages?q="+q+"&maxResults="+Math.min(50,Math.max(1,maxResults)));
  const list=await lr.json();
  const out=[];
  for(const m of (list.messages||[])){
    const rr=await gmailFetch(c,"/messages/"+encodeURIComponent(m.id)+"?format=full");
    const msg=await rr.json();
    const text=bodyText(msg.payload)||String(msg.snippet||"");
    const parsed=parseVoicemailText(text+" "+String(msg.snippet||""));
    const parts=flattenParts(msg.payload,[]);
    const att=parts.find((p:any)=>p.body?.attachmentId && /(audio\/|application\/wav|audio\/wav|audio\/x-wav)/i.test(String(p.mimeType||"")))
      || parts.find((p:any)=>p.body?.attachmentId && /\.wav$/i.test(String(p.filename||"")));
    out.push({
      id:msg.id,threadId:msg.threadId,subject:header(msg.payload,"Subject"),
      received:new Date(Number(msg.internalDate||0)).toISOString(),
      caller:parsed.caller,duration:parsed.duration,timeText:parsed.when,
      attachment:att?{filename:att.filename||"voicemail.wav",mimeType:att.mimeType,attachmentId:att.body.attachmentId}:null,
      snippet:String(msg.snippet||"")
    });
  }
  return out;
}
async function transcribeVoicemail(c:any,messageId:string){
  const rr=await gmailFetch(c,"/messages/"+encodeURIComponent(messageId)+"?format=full");
  const msg=await rr.json();
  const text=bodyText(msg.payload)||String(msg.snippet||"");
  const parsed=parseVoicemailText(text+" "+String(msg.snippet||""));
  const parts=flattenParts(msg.payload,[]);
  const att=parts.find((p:any)=>p.body?.attachmentId && /(audio\/|application\/wav|audio\/wav|audio\/x-wav)/i.test(String(p.mimeType||"")))
    || parts.find((p:any)=>p.body?.attachmentId && /\.wav$/i.test(String(p.filename||"")));
  if(!att)throw new Error("No voicemail audio attachment found.");
  const ar=await gmailFetch(c,"/messages/"+encodeURIComponent(messageId)+"/attachments/"+encodeURIComponent(att.body.attachmentId));
  const aj=await ar.json();
  const bytes=fromB64url(String(aj.data||""));
  if(!bytes.length)throw new Error("Voicemail attachment was empty.");
  const openai=Deno.env.get("OPENAI_API_KEY")||"";
  if(!openai)throw new Error("Cloud transcription provider is not configured.");
  const fd=new FormData();
  fd.append("model","gpt-4o-mini-transcribe");
  fd.append("file",new Blob([bytes],{type:String(att.mimeType||"audio/wav")}),String(att.filename||"voicemail.wav"));
  const tr=await fetch("https://api.openai.com/v1/audio/transcriptions",{method:"POST",headers:{authorization:"Bearer "+openai},body:fd});
  const tj=await tr.json().catch(()=>({}));
  if(!tr.ok)throw new Error("Voicemail transcription failed.");
  const transcript=String(tj.text||"").trim();
  const received=new Date(Number(msg.internalDate||0)).toISOString();
  const row={
    gmail_message_id:msg.id,gmail_thread_id:msg.threadId||null,caller_number:parsed.caller,
    voicemail_time_text:parsed.when,duration_text:parsed.duration,subject:header(msg.payload,"Subject"),
    attachment_filename:String(att.filename||"voicemail.wav"),transcript,transcript_model:"gpt-4o-mini-transcribe",
    email_received_at:received,transcribed_at:new Date().toISOString(),
    metadata:{mime_type:att.mimeType||null,attachment_size:bytes.length},
    updated_at:new Date().toISOString()
  };
  const {error}=await c.from("dexter_voicemails").upsert(row,{onConflict:"gmail_message_id"});
  if(error)throw error;
  return {...row};
}

Deno.serve(async(req)=>{
  const c=db(),u=new URL(req.url);
  if(req.method==="OPTIONS")return json({ok:true});
  try{
    if(req.method==="GET" && u.searchParams.get("code")){
      const code=u.searchParams.get("code")||"", state=u.searchParams.get("state")||"";
      const {data:st,error}=await c.from("dexter_oauth_states").select("*")
        .eq("provider","gmail").eq("state",state).is("used_at",null)
        .gt("expires_at",new Date().toISOString()).maybeSingle();
      if(error||!st)return html("<div class=err><h2>Connection expired</h2><p>Start the Gmail connection again.</p></div>",400);
      const {clientId,clientSecret}=await clientCredentials(c);
      const tr=await fetch(GOOGLE_TOKEN,{method:"POST",headers:{"content-type":"application/x-www-form-urlencoded"},body:new URLSearchParams({
        code,client_id:clientId,client_secret:clientSecret,redirect_uri:CALLBACK,grant_type:"authorization_code",code_verifier:String(st.code_verifier)
      })});
      const tok=await tr.json().catch(()=>({}));
      if(!tr.ok||!tok.access_token)return html("<div class=err><h2>Google token exchange failed</h2></div>",502);
      const stamp=Date.now();
      const aid=await vaultStore(c,String(tok.access_token),"dexter_gmail_access_"+stamp,"Dexter Gmail OAuth access token");
      await saveBinding(c,"access_token",aid,"Gmail readonly API access token");
      if(tok.refresh_token){
        const rid=await vaultStore(c,String(tok.refresh_token),"dexter_gmail_refresh_"+stamp,"Dexter Gmail OAuth refresh token");
        await saveBinding(c,"refresh_token",rid,"Gmail readonly OAuth refresh token");
      }
      const exp=new Date(Date.now()+Number(tok.expires_in||3600)*1000-60000).toISOString();
      await c.from("dexter_oauth_states").update({used_at:new Date().toISOString()}).eq("id",st.id);

      const pr=await fetch(GMAIL+"/profile",{headers:{authorization:"Bearer "+String(tok.access_token)}});
      const profile=await pr.json().catch(()=>({}));
      if(!pr.ok)return html("<div class=err><h2>Gmail connected, but verification failed</h2></div>",502);

      await c.from("ai_connectors").update({
        status:"ready",last_checked_at:new Date().toISOString(),last_error:null,updated_at:new Date().toISOString(),
        config:{mode:"oauth_gmail_api",account:String(profile.emailAddress||""),readonly:true,vault_storage:true,home_pc_required:false,access_token_expires_at:exp}
      }).eq("connector_key","gmail");
      await c.from("ai_audit_logs").insert({action:"gmail.connected",actor:"owner",environment:"test",details:{account:String(profile.emailAddress||""),readonly:true,home_pc_required:false}});
      return html("<div class=ok><h1>Dexter Gmail connected ✓</h1><p>Cloud Dexter AI now has read-only Gmail access independent of the home PC.</p><p>Account: <b>"+String(profile.emailAddress||"")+"</b></p><p>You can close this tab.</p></div>");
    }

    if(req.method==="POST"){
      const ct=req.headers.get("content-type")||"";
      if(ct.includes("application/json")){
        if(!(await ownerAuth(req,c)))return json({error:"Owner access required."},401);
        const body=await req.json().catch(()=>({}));
        const action=String(body.action||"");
        if(action==="status"){
          const {data:row}=await c.from("ai_connectors").select("status,config,last_checked_at,last_error").eq("connector_key","gmail").maybeSingle();
          return json({connector:row||null});
        }
        if(action==="search_messages"){
          const query=String(body.query||"newer_than:30d").trim().slice(0,500);
          const limit=Math.min(10,Math.max(1,Number(body.maxResults)||5));
          const response=await gmailFetch(c,"/messages?q="+encodeURIComponent(query)+"&maxResults="+limit);
          const list=await response.json();
          const messages=await Promise.all((list.messages||[]).map(async(m:any)=>{
            const r=await gmailFetch(c,"/messages/"+encodeURIComponent(m.id)+"?format=full");
            const msg=await r.json();
            return {id:msg.id,threadId:msg.threadId,subject:header(msg.payload,"Subject"),from:header(msg.payload,"From"),
              received:new Date(Number(msg.internalDate||0)).toISOString(),snippet:String(msg.snippet||"").slice(0,1000)};
          }));
          return json({messages,query,checkedAt:new Date().toISOString(),readOnly:true,homePcRequired:false});
        }
        if(action==="list_voicemails"){
          const rows=await listVoicemails(c,Number(body.maxResults||20));
          return json({voicemails:rows});
        }
        if(action==="transcribe_latest_voicemail"){
          const rows=await listVoicemails(c,5);
          if(!rows.length)return json({error:"No bOnline voicemail emails found."},404);
          const result=await transcribeVoicemail(c,rows[0].id);
          return json({voicemail:result});
        }
        if(action==="transcribe_voicemail"){
          const id=String(body.messageId||"");
          if(!id)return json({error:"messageId required."},400);
          const result=await transcribeVoicemail(c,id);
          return json({voicemail:result});
        }
        return json({error:"Unknown action."},400);
      }

      const form=await req.formData();
      const ownerCode=String(form.get("owner_code")||"");
      const clientId=String(form.get("client_id")||"").trim();
      const clientSecret=String(form.get("client_secret")||"").trim();
      if(ownerCode.length<12||!clientId||!clientSecret)return html("<div class=err><h2>Missing details</h2></div>",400);
      const hash=await sha256(ownerCode);
      const {data:key}=await c.from("dexter_access_keys").select("id,role,active").eq("token_hash",hash).eq("active",true).maybeSingle();
      if(!key||key.role!=="owner")return html("<div class=err><h2>Invalid Dexter owner access code</h2></div>",401);

      const stamp=Date.now();
      const cid=await vaultStore(c,clientId,"dexter_gmail_oauth_client_id_"+stamp,"Dexter Gmail OAuth client ID");
      const csid=await vaultStore(c,clientSecret,"dexter_gmail_oauth_client_secret_"+stamp,"Dexter Gmail OAuth client secret");
      await saveBinding(c,"oauth_client_id",cid,"Google OAuth app for Gmail readonly access");
      await saveBinding(c,"oauth_client_secret",csid,"Google OAuth app for Gmail readonly access");

      const state=randomToken(24),verifier=randomToken(48);
      const challenge=b64url(new Uint8Array(await crypto.subtle.digest("SHA-256",new TextEncoder().encode(verifier))));
      const {error}=await c.from("dexter_oauth_states").insert({
        provider:"gmail",state,code_verifier:verifier,redirect_uri:CALLBACK,
        expires_at:new Date(Date.now()+10*60*1000).toISOString()
      });
      if(error)throw error;

      const auth=new URL(GOOGLE_AUTH);
      auth.searchParams.set("response_type","code");
      auth.searchParams.set("client_id",clientId);
      auth.searchParams.set("redirect_uri",CALLBACK);
      auth.searchParams.set("scope",SCOPES);
      auth.searchParams.set("access_type","offline");
      auth.searchParams.set("prompt","consent");
      auth.searchParams.set("include_granted_scopes","true");
      auth.searchParams.set("state",state);
      auth.searchParams.set("code_challenge",challenge);
      auth.searchParams.set("code_challenge_method","S256");
      return Response.redirect(auth.toString(),302);
    }

    return html("<h1>Connect Dexter AI to Gmail</h1><p>This adds read-only Gmail access to the Dexter AI cloud server, so bOnline voicemail emails can be read and transcribed without the home PC.</p><p><b>Google OAuth redirect URI:</b><br><code>"+CALLBACK+"</code></p><form method=post><label>Dexter owner access code</label><input name=owner_code type=password autocomplete=off required><label>Google OAuth Client ID</label><input name=client_id autocomplete=off required><label>Google OAuth Client Secret</label><input name=client_secret type=password autocomplete=off required><button type=submit>Connect Gmail</button></form><small>Use a Google OAuth Web application with the redirect URI shown above. Access requested is Gmail read-only. Secrets are stored in Supabase Vault, not in the frontend.</small>");
  }catch(e){
    const message=String((e as Error)?.message||e).slice(0,500);
    await c.from("ai_connectors").update({status:/not connected|not configured/i.test(message)?"not_configured":"error",last_checked_at:new Date().toISOString(),last_error:message,updated_at:new Date().toISOString()}).eq("connector_key","gmail");
    return req.headers.get("content-type")?.includes("application/json")?json({error:message},500):html("<div class=err><h2>Dexter Gmail connection error</h2><p>"+message.replace(/[<>&]/g,"")+"</p></div>",500);
  }
});
