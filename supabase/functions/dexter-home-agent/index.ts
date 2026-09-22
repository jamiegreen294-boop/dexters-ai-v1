import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const cors={
  "Content-Type":"application/json",
  "Access-Control-Allow-Origin":"*",
  "Access-Control-Allow-Headers":"content-type,x-dexter-agent-token",
  "Access-Control-Allow-Methods":"POST,OPTIONS",
  "Cache-Control":"no-store"
};
const json=(body:unknown,status=200)=>new Response(JSON.stringify(body),{status,headers:cors});
const now=()=>new Date().toISOString();
function db(){return createClient(Deno.env.get("SUPABASE_URL")!,Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,{auth:{persistSession:false}});}
async function vaultGet(client:any,id:string){
  const {data,error}=await client.rpc("dexter_vault_get",{p_id:id});
  if(error)throw error;
  return String(data||"");
}
async function sha256(v:string){
  const bytes=await crypto.subtle.digest("SHA-256",new TextEncoder().encode(v));
  return Array.from(new Uint8Array(bytes)).map(b=>b.toString(16).padStart(2,"0")).join("");
}
function randomToken(bytes=32){
  const a=new Uint8Array(bytes);crypto.getRandomValues(a);
  return btoa(String.fromCharCode(...a)).replace(/\+/g,"-").replace(/\//g,"_").replace(/=+$/,"");
}
async function agentAuth(req:Request){
  const token=req.headers.get("x-dexter-agent-token")||"";
  if(token.length<24)throw new Error("INVALID_AGENT_TOKEN");
  const client=db(),hash=await sha256(token);
  const {data,error}=await client.from("dexter_home_agents").select("*").eq("token_hash",hash).eq("active",true).maybeSingle();
  if(error||!data)throw new Error("INVALID_AGENT_TOKEN");
  await client.from("dexter_home_agents").update({last_seen_at:now(),updated_at:now()}).eq("id",data.id);
  return {client,agent:data};
}

Deno.serve(async(req)=>{
  if(req.method==="OPTIONS")return new Response(null,{status:204,headers:cors});
  if(req.method!=="POST")return json({error:"POST required"},405);
  try{
    const body=await req.json().catch(()=>({}));
    const action=String(body.action||"poll").toLowerCase();

    if(action==="pair_claim"){
      const code=String(body.code||"").trim();
      const name=String(body.name||"Dexter Home PC").trim().slice(0,120);
      if(code.length<12)return json({error:"Invalid pairing code."},400);
      const client=db(),hash=await sha256(code);
      const {data:pair}=await client.from("dexter_home_pairings").select("*").eq("code_hash",hash).is("claimed_at",null).gt("expires_at",now()).maybeSingle();
      if(!pair)return json({error:"Pairing code is invalid or expired."},401);
      const token=randomToken(36),tokenHash=await sha256(token);
      const caps=["browser","research","workspace","coding","local_ai","hardware_doctor"];
      const {data:agent,error}=await client.from("dexter_home_agents").insert({name,token_hash:tokenHash,capabilities:caps,last_seen_at:now()}).select("id,name,capabilities").single();
      if(error)throw error;
      await client.from("dexter_home_pairings").update({claimed_at:now()}).eq("id",pair.id);
      return json({paired:true,agent,agent_token:token});
    }

    const {client,agent}=await agentAuth(req);

    if(action==="poll"){
      // Recover abandoned jobs after a Home Host crash/restart. Active jobs renew
      // their lease via heartbeat, so only stale running jobs are re-queued.
      const staleBefore=new Date(Date.now()-90_000).toISOString();
      const {data:staleJobs}=await client.from("dexter_home_jobs")
        .select("id,claimed_by,updated_at,status")
        .eq("status","running")
        .eq("claimed_by",agent.id)
        .lt("updated_at",staleBefore)
        .limit(25);
      for(const stale of (staleJobs||[])){
        await client.from("dexter_home_jobs").update({
          status:"queued",claimed_by:null,claimed_at:null,updated_at:now(),
          error:"Recovered automatically after Home Host interruption."
        }).eq("id",stale.id).eq("status","running").eq("claimed_by",agent.id);
      }

      const {data:jobs,error}=await client.from("dexter_home_jobs").select("*").eq("status","queued").order("created_at",{ascending:true}).limit(25);
      if(error)throw error;
      const job=(jobs||[]).find((j:any)=>{
        const target=String(j?.request?.target_agent_id||"").trim();
        return !target||target===String(agent.id);
      });
      if(!job)return json({job:null,agent:{id:agent.id,name:agent.name}});
      const {data:claimed,error:claimError}=await client.from("dexter_home_jobs")
        .update({status:"running",claimed_by:agent.id,claimed_at:now(),updated_at:now()})
        .eq("id",job.id).eq("status","queued").select("*").maybeSingle();
      if(claimError)throw claimError;
      if(!claimed)return json({job:null,agent:{id:agent.id,name:agent.name}});
      let outbound:any={...claimed,request:{...(claimed.request||{})}};
      if(String(claimed.tool_name||"")==="browser.github_actions_secrets"){
        const handoffs=(claimed.request?.secret_handoffs&&typeof claimed.request.secret_handoffs==="object")?claimed.request.secret_handoffs:{};
        const secureValues:any={};
        for(const [secretName,handoffId] of Object.entries(handoffs)){
          const {data:h}=await client.from("dexter_secret_handoffs").select("*")
            .eq("id",String(handoffId)).eq("target","github-actions").eq("status","pending").maybeSingle();
          if(!h)throw new Error("Secure secret handoff is missing, expired or already used.");
          if(Date.now()-new Date(h.created_at).getTime()>10*60*1000)throw new Error("Secure secret handoff expired.");
          const {data:b}=await client.from("dexter_secret_bindings").select("*")
            .eq("id",h.binding_id).eq("active",true).maybeSingle();
          if(!b)throw new Error("Secure secret binding is inactive.");
          const allowed=Array.isArray(b.allowed_targets)?b.allowed_targets:[];
          if(!allowed.includes("dexter-home-agent"))throw new Error("Secure secret binding is not allowed for the home agent.");
          secureValues[String(secretName)]=await vaultGet(client,String(b.vault_secret_id));
          await client.from("dexter_secret_handoffs").update({status:"issued"}).eq("id",h.id);
          await client.from("dexter_secret_bindings").update({last_used_at:now(),updated_at:now()}).eq("id",b.id);
        }
        outbound.request.secure_values=secureValues;
      }
      return json({job:outbound,agent:{id:agent.id,name:agent.name}});
    }

    if(action==="result"){
      const id=String(body.job_id||"");
      const status=String(body.status||"completed");
      if(!["completed","failed"].includes(status))return json({error:"Invalid result status."},400);
      const {data:job}=await client.from("dexter_home_jobs").select("*").eq("id",id).eq("claimed_by",agent.id).maybeSingle();
      if(!job)return json({error:"Job not found for this agent."},404);
      const patch:any={status,updated_at:now(),completed_at:now()};
      if(status==="completed"){
        let result:any=body.result??{};
        if(String(job.tool_name||"")==="image.generate" && result?.image_base64){
          const base64=String(result.image_base64||"");
          if(base64.length>14_000_000)throw new Error("Generated image payload is too large.");
          const bytes=Uint8Array.from(atob(base64),c=>c.charCodeAt(0));
          const filename=String(result.filename||("dexter-"+crypto.randomUUID()+".png")).replace(/[^a-zA-Z0-9._-]/g,"_");
          const storagePath=new Date().toISOString().slice(0,10)+"/"+crypto.randomUUID()+"-"+filename;
          const mime=String(result.mime_type||"image/png");
          const {error:uploadError}=await client.storage.from("dexter-ai-images").upload(storagePath,bytes,{contentType:mime,upsert:false});
          if(uploadError)throw uploadError;
          const {data:signed,error:signedError}=await client.storage.from("dexter-ai-images").createSignedUrl(storagePath,60*60*24*7);
          if(signedError)throw signedError;
          const clean={...result};
          delete clean.image_base64;
          result={...clean,image_storage_path:storagePath,image_url:signed?.signedUrl||null,image_url_expires_in:604800};
        }
        patch.result=result;
      } else patch.error=String(body.error||"Home agent job failed").slice(0,5000);
      const {data:updated,error}=await client.from("dexter_home_jobs").update(patch).eq("id",id).select("*").single();
      if(error)throw error;
      if(String(job.tool_name||"")==="browser.github_actions_secrets"){
        const handoffs=(job.request?.secret_handoffs&&typeof job.request.secret_handoffs==="object")?Object.values(job.request.secret_handoffs):[];
        const handoffStatus=status==="completed"?"completed":"failed";
        if(handoffs.length)await client.from("dexter_secret_handoffs").update({status:handoffStatus}).in("id",handoffs.map(String));
      }
      if(status==="completed" && String(job.tool_name||"")==="learn.research"){
        const r:any=body.result||{};
        const rows=Array.isArray(r?.results)?r.results.slice(0,8):[];
        const sources=rows.map((x:any)=>({title:String(x?.title||"").slice(0,300),url:String(x?.url||"").slice(0,1500)})).filter((x:any)=>x.url);
        const distinctHosts=new Set(sources.map((x:any)=>{try{return new URL(x.url).hostname.replace(/^www\./,"")}catch{return ""}}).filter(Boolean));
        const lesson=rows.map((x:any,i:number)=>{
          const title=String(x?.title||"").trim();
          const snippet=String(x?.snippet||"").trim();
          return (i+1)+". "+title+(snippet?": "+snippet:"");
        }).join("\n").slice(0,20000);
        const topic=String(job.request?.query||r?.query||"Internet research").slice(0,500);
        const category=String(job.request?.category||"general").slice(0,80);
        const verified=distinctHosts.size>=2 && rows.length>=2;
        const confidence=verified?0.78:(rows.length?0.58:0.3);
        if(lesson){
          await client.from("dexter_learning_notes").insert({
            topic,category,lesson,sources,confidence,verified,
            learned_by:agent.name,active:true,updated_at:now()
          });
          await client.from("ai_audit_logs").insert({
            action:"learning.saved",actor:agent.name,environment:"test",
            details:{home_job_id:id,topic,category,verified,confidence,source_count:sources.length}
          });
        }
      }
      if(job.tool_request_id){
        await client.from("ai_tool_requests").update({
          status:status==="completed"?"completed":"failed",
          result:status==="completed"?(body.result??{}):null,
          error:status==="failed"?patch.error:null,
          updated_at:now()
        }).eq("id",job.tool_request_id);
      }
      if(job.task_id){
        const taskPatch:any={
          status:status==="completed"?"completed":"failed",
          progress:100,
          updated_at:now()
        };
        if(status==="completed")taskPatch.result=JSON.stringify(body.result??{});
        else taskPatch.error=patch.error;
        await client.from("ai_tasks").update(taskPatch).eq("id",job.task_id);
        await client.from("ai_orchestration_tasks").update({
          stage:status==="completed"?"completed":"failed",
          progress:100,
          result:status==="completed"?JSON.stringify(body.result??{}):JSON.stringify({error:patch.error}),
          updated_at:now()
        }).eq("task_id",job.task_id);
        await client.from("ai_task_events").insert({
          task_id:job.task_id,
          event_type:status==="completed"?"home.completed":"home.failed",
          message:status==="completed"?"Dexter Home PC completed the queued job.":("Dexter Home PC job failed: "+patch.error).slice(0,500)
        });
      }
      await client.from("ai_audit_logs").insert({
        action:status==="completed"?"home_job.completed":"home_job.failed",
        actor:agent.name,environment:"test",
        details:{home_job_id:id,tool_request_id:job.tool_request_id,tool_name:job.tool_name}
      });
      return json({ok:true,job:updated});
    }

    if(action==="heartbeat"){
      const currentJobId=String(body.current_job_id||"").trim();
      if(currentJobId){
        await client.from("dexter_home_jobs").update({updated_at:now()})
          .eq("id",currentJobId).eq("status","running").eq("claimed_by",agent.id);
      }
      return json({ok:true,agent:{id:agent.id,name:agent.name,last_seen_at:now()},current_job_id:currentJobId||null});
    }

    return json({error:"Unknown action"},400);
  }catch(e){
    const m=String((e as Error)?.message||e);
    if(m==="INVALID_AGENT_TOKEN")return json({error:"Invalid home-agent token."},401);
    return json({error:m},500);
  }
});