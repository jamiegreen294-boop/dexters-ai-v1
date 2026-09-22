import http from "node:http";
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { spawn } from "node:child_process";
import { chromium } from "playwright";

const __dirname=path.dirname(fileURLToPath(import.meta.url));
const ROOT=path.resolve(__dirname,"..");
const WORKSPACE=path.resolve(process.env.DEXTER_WORKSPACE||path.join(__dirname,"workspace"));
const JOB_DIR=path.join(__dirname,"jobs");
const PORT=Number(process.env.PORT||8787);
const TOKEN=process.env.DEXTER_BROWSER_WORKER_TOKEN||"";
const PROFILE=process.env.DEXTER_BROWSER_PROFILE||path.resolve(__dirname,"browser-profile");
const HEADLESS=String(process.env.DEXTER_BROWSER_HEADLESS||"false").toLowerCase()==="true";
const OLLAMA_URL=(process.env.DEXTER_OLLAMA_URL||"http://127.0.0.1:11434").replace(/\/$/,"");
const COMFY_URL=(process.env.DEXTER_COMFY_URL||"http://127.0.0.1:8188").replace(/\/$/,"");
const IMAGE_DIR=path.join(WORKSPACE,"generated-images");
const SDCPP_DIR=path.join(WORKSPACE,"image-engine","stable-diffusion-cpp");
const SDCPP_MODEL_DIR=path.join(WORKSPACE,"image-engine","models");
const SDCPP_MODEL=path.join(SDCPP_MODEL_DIR,"dreamshaper-7-lcm-q4_0.gguf");
const SDCPP_RELEASE_URL="https://github.com/leejet/stable-diffusion.cpp/releases/download/master-874-656a135/sd-master-656a135-bin-win-cpu-x64.zip";
const SDCPP_RELEASE_SHA256="396d3395be15e6c83d8e5f9021a44e42c874c37070320d36feb115b1f13f7312";
const SDCPP_MODEL_URL="https://huggingface.co/darkmaniac7/TokForge-DreamShaper-LCM-GGUF-q4/resolve/main/dreamshaper-7-lcm-q4_0.gguf?download=true";
const SDCPP_MODEL_SHA256="8b080d29432a3185936585971cca09236eea3a018161dd0af11b6f59b5dc4dfb";
const LOCAL_MODEL=process.env.DEXTER_CHAT_MODEL||"qwen3:1.7b";
const CODE_MODEL=process.env.DEXTER_CODE_MODEL||process.env.DEXTER_LOCAL_MODEL||"qwen3:4b";
const MAX_AGENT_STEPS=Math.max(1,Math.min(30,Number(process.env.DEXTER_MAX_AGENT_STEPS||15)));
const LIVE_ACTIONS=String(process.env.DEXTER_LIVE_ACTIONS||"false").toLowerCase()==="true";
const AGENT_ENDPOINT=(process.env.DEXTER_AGENT_ENDPOINT||"https://eikruaxxzzxmfjvsmwwo.supabase.co/functions/v1/dexter-home-agent").replace(/\/$/,"");
const AGENT_TOKEN=process.env.DEXTER_AGENT_TOKEN||"";
let agentBusy=false;

if(!TOKEN||TOKEN.length<24){
  console.error("Set DEXTER_BROWSER_WORKER_TOKEN to a long random value before starting Dexter.");
  process.exit(1);
}
for(const dir of [PROFILE,WORKSPACE,JOB_DIR,IMAGE_DIR,SDCPP_DIR,SDCPP_MODEL_DIR])fs.mkdirSync(dir,{recursive:true});

let context;
const pageSessions=new Map();
const pageDiagnostics=new WeakMap();

function json(res,status,body){
  const data=JSON.stringify(body);
  res.writeHead(status,{"Content-Type":"application/json","Cache-Control":"no-store","Content-Length":Buffer.byteLength(data)});
  res.end(data);
}
function authOk(req){
  const supplied=String(req.headers.authorization||"").replace(/^Bearer\s+/i,"");
  const a=Buffer.from(supplied),b=Buffer.from(TOKEN);
  return a.length===b.length&&a.length>0&&crypto.timingSafeEqual(a,b);
}
async function readBody(req){
  let raw="";
  for await(const chunk of req){raw+=chunk;if(raw.length>2_000_000)throw new Error("Request too large.");}
  return JSON.parse(raw||"{}");
}
function safeUrl(v){
  const u=new URL(String(v||""));
  if(!["http:","https:"].includes(u.protocol))throw new Error("Only http/https URLs are allowed.");
  return u.toString();
}
function safeWorkspacePath(rel=""){
  const full=path.resolve(WORKSPACE,String(rel||""));
  if(full!==WORKSPACE&&!full.startsWith(WORKSPACE+path.sep))throw new Error("Workspace path escapes Dexter workspace.");
  return full;
}
function requireApprovedLive(request,action){
  if(!LIVE_ACTIONS)throw new Error(action+" is disabled while Dexter is in TEST mode.");
  if(request?.approval_granted!==true)throw new Error(action+" requires owner approval.");
}
async function resetBrowserContext(){
  pageSessions.clear();
  if(context){
    try{await context.close();}catch{}
    context=null;
  }
}
async function getContext(){
  if(context){
    try{
      context.pages();
      return context;
    }catch{
      context=null;
      pageSessions.clear();
    }
  }
  context=await chromium.launchPersistentContext(PROFILE,{
    headless:HEADLESS,viewport:{width:1440,height:1000},acceptDownloads:true
  });
  context.on("close",()=>{
    context=null;
    pageSessions.clear();
  });
  return context;
}
function attachPageDiagnostics(page){
  if(pageDiagnostics.has(page))return;
  const state={console_errors:[],page_errors:[],failed_requests:[]};
  pageDiagnostics.set(page,state);
  page.on("console",msg=>{if(msg.type()==="error")state.console_errors.push(msg.text().slice(0,1000));});
  page.on("pageerror",err=>state.page_errors.push(String(err?.message||err).slice(0,1000)));
  page.on("requestfailed",req=>state.failed_requests.push({url:req.url().slice(0,1000),error:req.failure()?.errorText||"failed"}));
}
async function getPage(session="default"){
  let ctx=await getContext();
  if(pageSessions.has(session)){
    const p=pageSessions.get(session);
    if(!p.isClosed())return p;
    pageSessions.delete(session);
  }
  try{
    const p=await ctx.newPage();
    attachPageDiagnostics(p);
    pageSessions.set(session,p);
    return p;
  }catch(err){
    if(!/Target page, context or browser has been closed|Target closed/i.test(String(err?.message||err)))throw err;
    await resetBrowserContext();
    ctx=await getContext();
    const p=await ctx.newPage();
    attachPageDiagnostics(p);
    pageSessions.set(session,p);
    return p;
  }
}
async function snapshot(page){
  const elements=await page.evaluate(()=>{
    document.querySelectorAll("[data-dexter-ref]").forEach(el=>el.removeAttribute("data-dexter-ref"));
    const selectors=["a[href]","button","input","textarea","select","[role=button]","[contenteditable=true]"];
    return Array.from(document.querySelectorAll(selectors.join(","))).filter(el=>{
      const r=el.getBoundingClientRect();return r.width>0&&r.height>0;
    }).slice(0,250).map((el,i)=>{
      const ref="e"+(i+1);el.setAttribute("data-dexter-ref",ref);return ({
      ref,tag:el.tagName.toLowerCase(),
      text:(el.innerText||el.getAttribute("aria-label")||el.getAttribute("placeholder")||"").trim().slice(0,240),
      type:el.getAttribute("type"),name:el.getAttribute("name"),
      value:(el.tagName==="SELECT"?"":(el.value||"")).slice(0,120),
      href:el instanceof HTMLAnchorElement?el.href:null
    })});
  });
  const text=(await page.locator("body").innerText().catch(()=>"" )).slice(0,16000);
  const diagnostics=pageDiagnostics.get(page)||{console_errors:[],page_errors:[],failed_requests:[]};
  const accessibility=await page.evaluate(()=>{
    const missingAlt=Array.from(document.querySelectorAll("img")).filter(x=>!x.getAttribute("alt")).length;
    const unnamedButtons=Array.from(document.querySelectorAll("button,[role=button]")).filter(x=>!((x.textContent||"").trim()||x.getAttribute("aria-label")||x.getAttribute("title"))).length;
    const unlabeled=Array.from(document.querySelectorAll("input,textarea,select")).filter(x=>{
      const id=x.id;return !(x.getAttribute("aria-label")||x.getAttribute("aria-labelledby")||(id&&document.querySelector('label[for="'+CSS.escape(id)+'"]')));
    }).length;
    return {missing_alt:missingAlt,unnamed_buttons:unnamedButtons,unlabeled_controls:unlabeled,total_issues:missingAlt+unnamedButtons+unlabeled};
  }).catch(()=>({missing_alt:0,unnamed_buttons:0,unlabeled_controls:0,total_issues:0}));
  return {url:page.url(),title:await page.title(),text,elements,diagnostics,accessibility};
}
async function byRef(page,ref){
  const value=String(ref||"");
  if(!/^e\d+$/.test(value))throw new Error("Invalid element ref.");
  const loc=page.locator('[data-dexter-ref="'+value+'"]');
  if(await loc.count()<1)throw new Error("Element ref not found; refresh snapshot.");
  return loc.first();
}
async function ollama(messages,format,model=LOCAL_MODEL){
  const controller=new AbortController();
  const timer=setTimeout(()=>controller.abort(),120000);
  let r;
  try{
    r=await fetch(OLLAMA_URL+"/api/chat",{method:"POST",signal:controller.signal,headers:{"Content-Type":"application/json"},body:JSON.stringify({
      model,messages,stream:false,format:format||undefined,options:{temperature:0.1,num_ctx:4096,num_predict:384}
    })});
  }finally{clearTimeout(timer);}
  const data=await r.json().catch(()=>({}));
  if(!r.ok)throw new Error(data?.error||"Local Ollama request failed");
  return String(data?.message?.content||"").trim();
}
async function ollamaFast(messages,format,model=CODE_MODEL){
  const controller=new AbortController();
  const timer=setTimeout(()=>controller.abort(),150000);
  const fastMessages=(Array.isArray(messages)?messages:[]).map((m,i)=>i===0&&m?.role==="user"
    ?{...m,content:"/no_think\n"+String(m.content||"")}
    :m);
  let r;
  try{
    r=await fetch(OLLAMA_URL+"/api/chat",{method:"POST",signal:controller.signal,headers:{"Content-Type":"application/json"},body:JSON.stringify({
      model,messages:fastMessages,stream:false,think:false,format:format||undefined,options:{temperature:0,num_ctx:2048,num_predict:768}
    })});
  }finally{clearTimeout(timer);}
  const data=await r.json().catch(()=>({}));
  if(!r.ok)throw new Error(data?.error||"Local Ollama fast request failed");
  return String(data?.message?.content||"").trim();
}
function parseJson(s){
  try{return JSON.parse(s)}catch{}
  const m=String(s).match(/\{[\s\S]*\}/);if(m){try{return JSON.parse(m[0])}catch{}}
  throw new Error("Local model returned invalid JSON.");
}
function isConsequence(action){
  const text=JSON.stringify(action).toLowerCase();
  return /purchase|pay|refund|delete|publish|deploy|merge|send|submit|login|sign in|create account|change password|confirm order|place order/.test(text);
}
async function githubActionsSecretsTool(page,request={}){
  if(request?.approval_granted!==true)throw new Error("GitHub Actions secret changes require explicit owner approval.");
  const repo=String(request.repo||"").trim();
  if(repo!=="jamiegreen294-boop/dexters-ai-v1")throw new Error("Secure secret sync is restricted to Dexter AI's own repository.");
  const values=request.secure_values&&typeof request.secure_values==="object"?request.secure_values:{};
  const allowed=new Set([
    "DEXTER_ANDROID_KEYSTORE_B64",
    "DEXTER_ANDROID_KEYSTORE_PASSWORD",
    "DEXTER_ANDROID_KEY_ALIAS",
    "DEXTER_ANDROID_KEY_PASSWORD"
  ]);
  const names=Object.keys(values);
  if(!names.length||names.some(n=>!allowed.has(n)))throw new Error("Unexpected GitHub Actions secret name.");
  const completed=[];
  for(const name of names){
    const value=String(values[name]??"");
    if(!value)throw new Error("Missing secure value for "+name);
    const base="https://github.com/"+repo+"/settings/secrets/actions";
    await page.goto(base+"/new",{waitUntil:"domcontentloaded",timeout:45000});
    if(/\/login(?:\?|$)/.test(page.url())||/Sign in to GitHub/i.test((await page.locator("body").innerText().catch(()=>"")).slice(0,5000))){
      throw new Error("Dexter Home PC GitHub browser session is not signed in.");
    }
    const nameInput=page.locator('input[name="secret_name"],#secret_name').first();
    const valueInput=page.locator('textarea[name="secret_value"],#secret_value').first();
    if(await nameInput.count()<1||await valueInput.count()<1)throw new Error("GitHub Actions secret form was not found.");
    await nameInput.fill(name);
    await valueInput.fill(value);
    const submit=page.getByRole("button",{name:/Add secret|Save secret|Update secret/i}).first();
    if(await submit.count()<1)throw new Error("GitHub Actions secret save button was not found.");
    await submit.click({timeout:15000});
    await page.waitForTimeout(1200);
    const body=(await page.locator("body").innerText().catch(()=>"")).slice(0,8000);
    if(/already (?:exists|been taken)|name has already been taken/i.test(body)){
      await page.goto(base+"/"+encodeURIComponent(name),{waitUntil:"domcontentloaded",timeout:45000});
      const editValue=page.locator('textarea[name="secret_value"],#secret_value').first();
      if(await editValue.count()<1)throw new Error("Existing GitHub Actions secret could not be opened for update: "+name);
      await editValue.fill(value);
      const update=page.getByRole("button",{name:/Update secret|Save secret/i}).first();
      if(await update.count()<1)throw new Error("GitHub Actions secret update button was not found.");
      await update.click({timeout:15000});
      await page.waitForTimeout(1000);
    }
    // Do not snapshot or return form state: secret values must never enter logs/results.
    completed.push(name);
  }
  return {ok:true,repo,updated:completed};
}

async function browserTool(tool,request={}){
  const session=String(request.session||"default").slice(0,80),page=await getPage(session);
  if(tool==="browser.navigate"){
    await page.goto(safeUrl(request.url),{waitUntil:"domcontentloaded",timeout:45000});
    return await snapshot(page);
  }
  if(tool==="browser.snapshot")return await snapshot(page);
  if(tool==="browser.click"){const el=await byRef(page,request.ref);await el.click({timeout:15000});await page.waitForTimeout(700);return await snapshot(page);}
  if(tool==="browser.click_text"){
    const label=String(request.text||"").trim();
    if(!label)throw new Error("Button text is required.");
    const el=page.getByRole("button",{name:label,exact:false}).first();
    if(await el.count()<1)throw new Error("Button text not found.");
    await el.click({timeout:15000});await page.waitForTimeout(700);return await snapshot(page);
  }
  if(tool==="browser.fill"){const el=await byRef(page,request.ref);await el.fill(String(request.value??""));return {ok:true,url:page.url()};}
  if(tool==="browser.select"){const el=await byRef(page,request.ref);await el.selectOption(String(request.value??""));return {ok:true,url:page.url()};}
  if(tool==="browser.press"){await page.keyboard.press(String(request.key||"Enter"));await page.waitForTimeout(500);return await snapshot(page);}
  if(tool==="browser.text")return {url:page.url(),title:await page.title(),text:(await page.locator("body").innerText()).slice(0,60000)};
  if(tool==="browser.screenshot"){const b=await page.screenshot({fullPage:Boolean(request.fullPage)});return {url:page.url(),image_base64:b.toString("base64")};}
  if(tool==="browser.close"){await page.close();pageSessions.delete(session);return {ok:true};}
  if(tool==="browser.navigate_and_act")return await autonomousBrowser(page,request);
  if(tool==="browser.github_actions_secrets")return await githubActionsSecretsTool(page,request);
  throw new Error("Unsupported browser tool: "+tool);
}
async function autonomousBrowser(page,request={}){
  const goal=String(request.instruction||request.goal||"").trim();
  if(!goal)throw new Error("Browser instruction is required.");
  const allowConsequential=LIVE_ACTIONS&&request.approval_granted===true;
  if(request.url)await page.goto(safeUrl(request.url),{waitUntil:"domcontentloaded",timeout:45000});
  const history=[];
  const stepLimit=Math.max(1,Math.min(MAX_AGENT_STEPS,Number(request.max_steps||MAX_AGENT_STEPS)));
  const deadline=Date.now()+Math.max(15000,Math.min(300000,Number(request.timeout_ms||180000)));
  for(let step=1;step<=stepLimit;step++){
    if(Date.now()>deadline)return {status:"timeout",reason:"Browser task exceeded its time limit.",history,state:await snapshot(page)};
    const state=await snapshot(page);
    const prompt=[
      "You are Dexter Browser Agent running on the owner's local PC.",
      "Goal: "+goal,
      "Choose exactly one next action from: click, fill, select, press, navigate, done, blocked.",
      "Never invent element refs. Use only refs shown in PAGE STATE.",
      allowConsequential
        ?"Owner approval is verified for this request. You may perform only the consequential actions necessary for this exact goal. Do not expand scope."
        :"Do not perform purchases, payments, refunds, deletion, publishing, deployment, sending messages, login, submitting irreversible forms, or account/security changes. If one is necessary return blocked.",
      "Return JSON only: {action,ref,value,url,key,reason,result}.",
      "PAGE STATE:",JSON.stringify(state).slice(0,30000),
      "RECENT ACTIONS:",JSON.stringify(history.slice(-8))
    ].join("\n");
    if(Date.now()>deadline)return {status:"timeout",reason:"Browser task exceeded its time limit before the next AI step.",history,state};
    const decision=parseJson(await ollamaFast([{role:"user",content:prompt}],"json",LOCAL_MODEL));
    if(isConsequence(decision)&&!allowConsequential)return {status:"blocked",reason:"Consequence requires separate owner approval and live mode.",state,history};
    if(decision.action==="done")return {status:"completed",result:String(decision.result||decision.reason||"Completed"),state,history};
    if(decision.action==="blocked")return {status:"blocked",reason:String(decision.reason||"Blocked"),state,history};
    let result;
    if(decision.action==="click")result=await browserTool("browser.click",{session:request.session,ref:decision.ref});
    else if(decision.action==="fill")result=await browserTool("browser.fill",{session:request.session,ref:decision.ref,value:decision.value});
    else if(decision.action==="select")result=await browserTool("browser.select",{session:request.session,ref:decision.ref,value:decision.value});
    else if(decision.action==="press")result=await browserTool("browser.press",{session:request.session,key:decision.key||"Enter"});
    else if(decision.action==="navigate")result=await browserTool("browser.navigate",{session:request.session,url:decision.url});
    else throw new Error("Unsupported agent decision: "+decision.action);
    history.push({step,decision,result:{url:result?.url,title:result?.title,ok:result?.ok}});
  }
  return {status:"step_limit",history,state:await snapshot(page)};
}
function runProcess(command,args,cwd,timeout=120000){
  return new Promise((resolve,reject)=>{
    let executable=command;
    let runArgs=Array.isArray(args)?args.map(x=>String(x)):[];
    if(process.platform==="win32"&&["npm","npx"].includes(command)){
      executable=process.env.ComSpec||path.join(process.env.SystemRoot||"C:\\Windows","System32","cmd.exe");
      const quoteCmdArg=v=>{
        const s=String(v);
        if(!/[\s"&|<>^]/.test(s))return s;
        return '"'+s.replace(/(["^])/g,"^$1")+'"';
      };
      const commandLine=[command,...runArgs].map(quoteCmdArg).join(" ");
      runArgs=["/d","/s","/c",commandLine];
    }
    if(process.platform==="win32"&&command==="git"){
      const candidates=[
        String(process.env.DEXTER_GIT||"").trim(),
        path.join(process.env.ProgramFiles||"C:\\Program Files","Git","cmd","git.exe"),
        path.join(process.env.ProgramFiles||"C:\\Program Files","Git","bin","git.exe"),
        path.join(process.env["ProgramFiles(x86)"]||"C:\\Program Files (x86)","Git","cmd","git.exe"),
        path.join(process.env.LOCALAPPDATA||"","Programs","Git","cmd","git.exe"),
        path.join(process.env.USERPROFILE||"","AppData","Local","Programs","Git","cmd","git.exe")
      ].filter(Boolean);
      const found=candidates.find(p=>p&&fs.existsSync(p));
      if(found)executable=found;
    }
    if(process.platform==="win32"&&command==="ollama"){
      const candidates=[
        path.join(process.env.LOCALAPPDATA||"","Programs","Ollama","ollama.exe"),
        path.join(process.env.USERPROFILE||"","AppData","Local","Programs","Ollama","ollama.exe")
      ];
      const found=candidates.find(p=>p&&fs.existsSync(p));
      if(found)executable=found;
    }
    const child=spawn(executable,runArgs,{cwd,windowsHide:true,shell:false,env:{...process.env,CI:"1"}});
    let stdout="",stderr="",killed=false,settled=false;
    const timer=setTimeout(()=>{killed=true;try{child.kill();}catch{}},timeout);
    child.stdout?.on("data",d=>stdout+=d.toString());
    child.stderr?.on("data",d=>stderr+=d.toString());
    child.on("error",err=>{if(settled)return;settled=true;clearTimeout(timer);reject(err);});
    child.on("close",code=>{if(settled)return;settled=true;clearTimeout(timer);resolve({code,killed,stdout:stdout.slice(-100000),stderr:stderr.slice(-100000)});});
  });
}
async function listTree(rel="",depth=3,maxEntries=500){
  const root=safeWorkspacePath(rel),out=[];
  function walk(dir,level){
    if(level>depth||out.length>=maxEntries)return;
    for(const ent of fs.readdirSync(dir,{withFileTypes:true})){
      if(["node_modules",".git",".next","dist","build"].includes(ent.name))continue;
      const full=path.join(dir,ent.name),r=path.relative(WORKSPACE,full);
      out.push({path:r,type:ent.isDirectory()?"directory":"file"});
      if(ent.isDirectory())walk(full,level+1);
      if(out.length>=maxEntries)break;
    }
  }
  walk(root,0);return out;
}
async function internetResearch(request={}){
  const query=String(request.query||request.goal||"").trim();
  if(!query)throw new Error("Research query is required.");
  const session=String(request.session||("research-"+crypto.randomUUID())).slice(0,80);
  const page=await getPage(session);
  const attempts=[];
  const combined=[];
  const seen=new Set();

  async function ddgSearch(q,label){
    const searchUrl="https://html.duckduckgo.com/html/?q="+encodeURIComponent(q);
    try{
      await page.goto(searchUrl,{waitUntil:"domcontentloaded",timeout:45000});
      await page.waitForTimeout(700);
      const rows=await page.evaluate(()=>{
        const out=[];
        function cleanHref(raw){
          try{
            const u=new URL(raw,location.href);
            const uddg=u.searchParams.get("uddg");
            if(uddg)return decodeURIComponent(uddg);
            if(u.hostname.endsWith("duckduckgo.com"))return "";
            return u.href;
          }catch{return "";}
        }
        for(const node of Array.from(document.querySelectorAll(".result"))){
          const a=node.querySelector("a.result__a[href]");
          if(!a)continue;
          const href=cleanHref(a.getAttribute("href")||a.href||"");
          const title=(a.textContent||"").trim();
          const snippet=(node.querySelector(".result__snippet")?.textContent||"").trim();
          if(href&&title)out.push({title:title.slice(0,300),url:href,snippet:snippet.slice(0,700)});
          if(out.length>=10)break;
        }
        return out;
      });
      attempts.push({source:"duckduckgo",label,query:q,url:searchUrl,count:rows.length});
      for(const row of rows){
        if(seen.has(row.url))continue;
        seen.add(row.url);combined.push(row);
        if(combined.length>=20)break;
      }
      return rows.length;
    }catch(e){
      attempts.push({source:"duckduckgo",label,query:q,url:searchUrl,count:0,error:String(e?.message||e).slice(0,300)});
      return 0;
    }
  }

  await ddgSearch(query,"exact");

  const cleaned=query
    .replace(/\bOR\b/gi," ")
    .replace(/["']/g," ")
    .replace(/\s+/g," ")
    .trim();
  if(cleaned && cleaned!==query && combined.length<8) await ddgSearch(cleaned,"broader");

  const phoneToken=(query.match(/\+?44[\s()\-]*(?:\d[\s()\-]*){9,10}/)||query.match(/0(?:[\s()\-]*\d){9,10}/)||[])[0]||"";
  const phoneLike=Boolean(phoneToken);
  let businessMatch=null;
  if(phoneLike){
    const digits=phoneToken.replace(/\D/g,"");
    const local=digits.startsWith("44")?"0"+digits.slice(2):digits;
    const intl=local.startsWith("0")?"+44"+local.slice(1):"+"+digits;
    if(combined.length<8) await ddgSearch(local,"phone-local");
    if(combined.length<8) await ddgSearch(intl,"phone-international");

    try{
      const mapsUrl="https://www.google.com/maps/search/?api=1&query="+encodeURIComponent(local);
      await page.goto(mapsUrl,{waitUntil:"domcontentloaded",timeout:45000});
      await page.waitForTimeout(900);
      const reject=page.getByRole("button",{name:/Reject all/i});
      if(await reject.count()){
        await reject.first().click().catch(()=>{});
        await page.waitForTimeout(1400);
      }
      const mapText=(await page.locator("body").innerText().catch(()=> "")).slice(0,16000);
      attempts.push({source:"google-maps",label:"business-phone",query:local,url:page.url(),count:mapText?1:0});
      if(mapText && !/unusual traffic/i.test(mapText) && !/Before you continue to Google/i.test(mapText)){
        const phoneMatch=mapText.match(/(?:\+44\s?|0)\d[\d\s]{8,}/);
        const addressMatch=mapText.match(/\d+[A-Za-z]?[^\n]{2,90},\s*Glasgow[^\n]{0,50}/i);
        const title=(await page.title().catch(()=> "")).replace(/\s*-\s*Google Maps\s*$/i,"").trim();
        businessMatch={
          title:title||null,
          url:page.url(),
          phone:phoneMatch?.[0]?.trim()||null,
          address:addressMatch?.[0]?.trim()||null,
          text:mapText
        };
      }
    }catch(e){
      attempts.push({source:"google-maps",label:"business-phone",query:local,count:0,error:String(e?.message||e).slice(0,300)});
    }
  }

  if(request.deep===true){
    const result=await autonomousBrowser(page,{
      session,
      instruction:"Research this question using the public web and continue across reasonable alternative sources if the first source is blocked or empty. Return when you have enough evidence or the bounded research budget is exhausted: "+query+". Read pages only. Do not log in, submit forms, send messages, purchase anything, publish, deploy, delete, or change accounts.",
      url:page.url(),
      max_steps:Math.max(1,Math.min(5,Number(request.max_steps||5))),
      timeout_ms:Math.max(15000,Math.min(90000,Number(request.timeout_ms||60000)))
    });
    return {status:"completed",mode:"deep",query,results:combined,business_match:businessMatch,attempts,result};
  }

  return {
    status:"completed",
    mode:businessMatch?"multi-source-business":"multi-source",
    query,
    results:combined,
    business_match:businessMatch,
    attempts,
    searched_all_planned_sources:true
  };
}

async function directCodingPlan(request={}){
  const repoPath=String(request.path||request.project_path||"").trim();
  if(!repoPath)throw new Error("Direct coding requires a project path.");
  const root=safeWorkspacePath(repoPath);
  fs.mkdirSync(root,{recursive:true});
  if(!fs.existsSync(path.join(root,".git"))){
    await runProcess("git",["init"],root,30000).catch(()=>null);
  }
  const files=Array.isArray(request.files)?request.files:[];
  if(!files.length)throw new Error("Direct coding requires at least one file.");
  if(files.length>25)throw new Error("Too many files in one direct coding job.");
  const written=[];
  for(const item of files){
    const rel=String(item?.path||"").trim();
    if(!rel)throw new Error("A direct coding file path was empty.");
    const full=path.resolve(root,rel);
    if(full!==root&&!full.startsWith(root+path.sep))throw new Error("Direct coding path escapes project workspace.");
    fs.mkdirSync(path.dirname(full),{recursive:true});
    fs.writeFileSync(full,String(item?.content??""),"utf8");
    written.push(rel);
  }
  const verification=written.map(rel=>{
    const full=path.resolve(root,rel);
    const content=fs.readFileSync(full,"utf8");
    return {path:rel,bytes:Buffer.byteLength(content),preview:content.slice(0,500)};
  });
  const checks=[];
  for(const chk of (Array.isArray(request.checks)?request.checks:[]).slice(0,8)){
    const kind=String(chk?.kind||"");
    if(!["node-check","npm-test","npm-build"].includes(kind))continue;
    const result=await workspaceTool("code.check",{path:repoPath,kind,file:String(chk?.file||"")});
    checks.push({kind,file:String(chk?.file||""),result});
  }
  const git_status=await workspaceTool("git.status",{path:repoPath}).catch(()=>null);
  const git_diff=await workspaceTool("git.diff",{path:repoPath,file:"."}).catch(()=>null);
  return {status:"completed",direct:true,repo_path:repoPath,files_written:written,verification,checks,git_status,git_diff};
}

const TRUSTED_CODING_SOURCES=[
  "MDN Web Docs — https://developer.mozilla.org/",
  "Node.js Docs — https://nodejs.org/docs/latest/api/",
  "TypeScript Docs — https://www.typescriptlang.org/docs/",
  "React Docs — https://react.dev/",
  "GitHub Docs — https://docs.github.com/",
  "Supabase Docs — https://supabase.com/docs",
  "PostgreSQL Docs — https://www.postgresql.org/docs/current/",
  "Vercel Docs — https://vercel.com/docs",
  "Playwright Docs — https://playwright.dev/docs/intro",
  "Square Developer Docs — https://developer.squareup.com/docs",
  "OWASP — https://owasp.org/www-project-top-ten/",
  "Web.dev — https://web.dev/learn/"
];

function codingNeedsResearch(goal=""){
  return /\b(api|sdk|oauth|webhook|supabase|square|vercel|github|playwright|react|typescript|node|postgres|pwa|security|auth|payment|terminal|browser|deploy|edge function|rls|realtime)\b/i.test(String(goal));
}

async function codingLearningContext(goal){
  if(!codingNeedsResearch(goal))return null;
  try{
    const result=await internetResearch({
      query:"Official current developer documentation for: "+String(goal).slice(0,500),
      deep:false,
      session:"coding-learning-"+crypto.randomUUID()
    });
    return {trusted_sources:TRUSTED_CODING_SOURCES,research:result};
  }catch(e){
    return {trusted_sources:TRUSTED_CODING_SOURCES,research_error:String(e?.message||e)};
  }
}


async function createSafeCodingBranch(root){
  try{
    const inside=await runProcess("git",["rev-parse","--is-inside-work-tree"],root,30000);
    if(inside.code!==0)return null;
    const current=await runProcess("git",["branch","--show-current"],root,30000);
    const currentName=String(current.stdout||"").trim();
    if(currentName.startsWith("dexter-test-"))return currentName;
    const stamp=new Date().toISOString().replace(/[-:TZ.]/g,"").slice(0,14);
    const name="dexter-test-"+stamp;
    const made=await runProcess("git",["checkout","-b",name],root,30000);
    return made.code===0?name:currentName||null;
  }catch{return null;}
}
async function automaticCodeChecks(repoPath){
  const root=safeWorkspacePath(repoPath),pkg=path.join(root,"package.json"),results=[];
  if(!fs.existsSync(pkg))return results;
  try{
    const data=JSON.parse(fs.readFileSync(pkg,"utf8")),scripts=data?.scripts||{};
    for(const name of ["lint","test","build"]){
      if(!scripts[name])continue;
      const result=await runProcess("npm",["run",name],root,180000).catch(e=>({code:1,stdout:"",stderr:String(e?.message||e)}));
      results.push({name,result});
    }
  }catch(e){results.push({name:"package-check",result:{code:1,stderr:String(e?.message||e)}});}
  return results;
}
async function verifyCodingPreview(url,baselinePath=""){
  if(!url)return null;
  try{
    const ctx=await getContext(),page=await ctx.newPage();attachPageDiagnostics(page);
    await page.goto(safeUrl(url),{waitUntil:"networkidle",timeout:45000}).catch(async()=>page.goto(safeUrl(url),{waitUntil:"domcontentloaded",timeout:45000}));
    await page.waitForTimeout(800);
    const shot=await page.screenshot({fullPage:true});
    const file=path.join(IMAGE_DIR,"code-preview-"+Date.now()+".png");fs.writeFileSync(file,shot);
    const currentHash=crypto.createHash("sha256").update(shot).digest("hex");
    let baseline=null;
    if(baselinePath){
      try{
        const base=fs.readFileSync(safeWorkspacePath(baselinePath));
        baseline={path:baselinePath,sha256:crypto.createHash("sha256").update(base).digest("hex"),changed:!base.equals(shot)};
      }catch(e){baseline={path:baselinePath,error:String(e?.message||e)};}
    }
    const snap=await snapshot(page);
    const failed=snap.diagnostics.console_errors.length>0||snap.diagnostics.page_errors.length>0||snap.diagnostics.failed_requests.length>0;
    const out={url:page.url(),title:await page.title(),screenshot_path:file,screenshot_sha256:currentHash,visual_comparison:baseline,diagnostics:snap.diagnostics,accessibility:snap.accessibility,verification_passed:!failed};
    await page.close();return out;
  }catch(e){return {error:String(e?.message||e),url:String(url),verification_passed:false};}
}

async function codingAgent(request={}){
  const goal=String(request.goal||request.instruction||"").trim();
  if(!goal)throw new Error("Coding goal is required.");
  let repoPath=String(request.path||"").trim();
  if(!repoPath&&!request.repo_url){
    const slug=String(request.project_name||goal).toLowerCase().replace(/[^a-z0-9]+/g,"-").replace(/^-|-$/g,"").slice(0,48)||("dexter-project-"+Date.now());
    repoPath=path.join("projects",slug);
    const fresh=safeWorkspacePath(repoPath);
    fs.mkdirSync(fresh,{recursive:true});
    await runProcess("git",["init"],fresh,30000).catch(()=>null);
  }
  if(request.repo_url){
    if(!repoPath)repoPath=path.basename(new URL(String(request.repo_url)).pathname).replace(/\.git$/,"");
    const dest=safeWorkspacePath(repoPath);
    if(!fs.existsSync(dest)||!fs.readdirSync(dest).length){
      await workspaceTool("git.clone",{url:String(request.repo_url),path:repoPath});
    }
  }
  const root=safeWorkspacePath(repoPath);
  if(!fs.existsSync(root))throw new Error("Workspace/repository path does not exist.");
  const testBranch=await createSafeCodingBranch(root);
  function repoRelative(rel=""){
    const full=path.resolve(root,String(rel||""));
    if(full!==root&&!full.startsWith(root+path.sep))throw new Error("Coding-agent path escapes its project workspace.");
    return path.relative(WORKSPACE,full);
  }

  const learningContext=await codingLearningContext(goal);
  let fastPathError=null;
  try{
    const initialTree=await listTree(repoPath,2,120);
    const fastPrompt=[
      "You are Dexter Fast Coding Agent running locally for Dexters.",
      "Goal: "+goal,
      "Work only inside this project. Never push, deploy, publish, merge, access credentials, or touch live systems.",
      "Return ONE JSON object only.",
      "Schema: {files:[{path,content}],checks:[{kind,file}],result}.",
      "files must contain complete replacement contents, never patches, placeholders or ellipses.",
      "Use no more than 8 files in this fast pass.",
      "Allowed checks: node-check only. If no lightweight check fits, return an empty checks array.",
      "For difficult, unfamiliar, security-sensitive or version-sensitive work, use current official documentation before deciding implementation details.",
      "Prefer primary sources from this trusted list: "+TRUSTED_CODING_SOURCES.join(" | "),
      "Never blindly copy snippets. Adapt them to the actual repository and installed versions, then verify the result.",
      "Never store or expose passwords, API keys, tokens, secrets, private customer data or credentials as learned knowledge.",
      "CURRENT LEARNING CONTEXT: "+JSON.stringify(learningContext||{}).slice(0,12000),
      "Existing tree: "+JSON.stringify(initialTree).slice(0,5000)
    ].join("\n");
    const plan=parseJson(await ollamaFast([{role:"user",content:fastPrompt}],"json",CODE_MODEL));
    if(Array.isArray(plan?.files)&&plan.files.length>0&&plan.files.length<=8){
      const written=[];
      for(const item of plan.files){
        const rel=String(item?.path||"").trim();
        if(!rel)throw new Error("Fast coding plan returned an empty file path.");
        const workspaceRel=repoRelative(rel);
        await workspaceTool("workspace.write",{path:workspaceRel,content:String(item?.content??"")});
        written.push(rel);
      }
      const checks=[];
      for(const chk of (Array.isArray(plan.checks)?plan.checks:[]).slice(0,4)){
        if(String(chk?.kind||"")!=="node-check")continue;
        const result=await workspaceTool("code.check",{path:repoPath,kind:"node-check",file:String(chk?.file||"")});
        checks.push({kind:"node-check",file:String(chk?.file||""),result});
      }
      const verification=[];
      for(const rel of written.slice(0,8)){
        const content=fs.readFileSync(path.resolve(root,rel),"utf8");
        verification.push({path:rel,bytes:Buffer.byteLength(content),preview:content.slice(0,500)});
      }
      const finalStatus=await workspaceTool("git.status",{path:repoPath}).catch(()=>null);
      const diff=await workspaceTool("git.diff",{path:repoPath,file:"."}).catch(()=>null);
      const automatic_checks=await automaticCodeChecks(repoPath);
      const preview=await verifyCodingPreview(request.preview_url||request.previewUrl||"",request.baseline_screenshot_path||request.baselineScreenshotPath||"");
      return {
        status:"completed",
        result:String(plan.result||"Fast coding pass completed"),
        repo_path:repoPath,
        test_branch:testBranch,
        fast_path:true,
        files_written:written,
        verification,
        checks,
        automatic_checks,
        preview,
        git_status:finalStatus,
        git_diff:diff
      };
    }
    fastPathError="Fast coding plan did not return a usable files array.";
  }catch(e){
    fastPathError=String(e?.message||e);
    if(/aborted|timeout|timed out/i.test(fastPathError)){
      return {status:"failed",reason:"Fast local coding planner timed out.",repo_path:repoPath,fast_path:true,error:fastPathError};
    }
  }

  const history=[];
  history.push({step:0,fast_path_fallback:true,error:fastPathError});
  for(let step=1;step<=Math.max(5,MAX_AGENT_STEPS);step++){
    const tree=await listTree(repoPath,3,350);
    const status=await runProcess("git",["status","--short","--branch"],root,30000).catch(()=>({code:1,stdout:"",stderr:"not a git repo"}));
    const prompt=[
      "You are Dexter Coding Agent running locally for Dexters.",
      "Goal: "+goal,
      "You may inspect and edit files ONLY inside the Dexter workspace/repository.",
      "Never push, deploy, publish, merge, delete remote resources, access credentials, or change live systems.",
      "Choose one action only from: list, read, write, mkdir, git_status, git_diff, check, research, done, blocked.",
      "For write, provide the COMPLETE replacement content for one file. Never use placeholders or ellipses.",
      "For check, kind must be one of npm-build, npm-test, node-check.",
      "Use research whenever current official documentation would materially improve correctness, especially for APIs, SDKs, authentication, payments, security or version-sensitive frameworks.",
      "Prefer these sources: "+TRUSTED_CODING_SOURCES.join(" | "),
      "Do not blindly paste community snippets. Understand, adapt and verify them against the repository.",
      "CURRENT LEARNING CONTEXT: "+JSON.stringify(learningContext||{}).slice(0,12000),
      "When the requested work is complete, run relevant checks and inspect git_diff before returning done.",
      "Return JSON only with fields: action,path,content,file,kind,query,reason,result.",
      "REPO PATH: "+repoPath,
      "TREE: "+JSON.stringify(tree).slice(0,8000),
      "GIT STATUS: "+JSON.stringify(status).slice(0,2500),
      "RECENT STEPS: "+JSON.stringify(history.slice(-6)).slice(0,6000)
    ].join("\n");
    const decision=parseJson(await ollama([{role:"user",content:prompt}],"json",CODE_MODEL));
    let result;
    if(decision.action==="list")result={entries:await listTree(repoRelative(decision.path||""),2,300)};
    else if(decision.action==="read")result=await workspaceTool("workspace.read",{path:repoRelative(decision.path||"")});
    else if(decision.action==="write")result=await workspaceTool("workspace.write",{path:repoRelative(decision.path||""),content:String(decision.content??"")});
    else if(decision.action==="mkdir")result=await workspaceTool("workspace.mkdir",{path:repoRelative(decision.path||"")});
    else if(decision.action==="git_status")result=await workspaceTool("git.status",{path:repoPath});
    else if(decision.action==="git_diff")result=await workspaceTool("git.diff",{path:repoPath,file:String(decision.file||".")});
    else if(decision.action==="check")result=await workspaceTool("code.check",{path:repoPath,kind:String(decision.kind||""),file:String(decision.file||"")});
    else if(decision.action==="research")result=await internetResearch({query:String(decision.query||goal),session:"code-research-"+crypto.randomUUID()});
    else if(decision.action==="done"){
      const diff=await workspaceTool("git.diff",{path:repoPath,file:"."}).catch(()=>null);
      const finalStatus=await workspaceTool("git.status",{path:repoPath}).catch(()=>null);
      const automatic_checks=await automaticCodeChecks(repoPath);
      const preview=await verifyCodingPreview(request.preview_url||request.previewUrl||"",request.baseline_screenshot_path||request.baselineScreenshotPath||"");
      return {status:"completed",result:String(decision.result||decision.reason||"Completed"),repo_path:repoPath,test_branch:testBranch,history,automatic_checks,preview,git_status:finalStatus,git_diff:diff};
    }else if(decision.action==="blocked")return {status:"blocked",reason:String(decision.reason||"Blocked"),repo_path:repoPath,history};
    else throw new Error("Unsupported coding-agent action: "+decision.action);
    history.push({step,decision:{...decision,content:decision.content?"[file content written]":undefined},result:JSON.stringify(result).slice(0,12000)});
  }
  return {status:"step_limit",repo_path:repoPath,history,git_status:await workspaceTool("git.status",{path:repoPath}).catch(()=>null),git_diff:await workspaceTool("git.diff",{path:repoPath,file:"."}).catch(()=>null)};
}
async function systemInfo(){
  const out={platform:process.platform,arch:process.arch,node:process.version,cpu:process.env.PROCESSOR_IDENTIFIER||"",ram:null,gpu:[],python:null};
  if(process.platform==="win32"){
    const ps=[
      "$os=Get-CimInstance Win32_OperatingSystem;",
      "$g=Get-CimInstance Win32_VideoController | Select-Object Name,AdapterRAM,DriverVersion;",
      "$p=(Get-Command python -ErrorAction SilentlyContinue).Source;",
      "[pscustomobject]@{ram=[math]::Round($os.TotalVisibleMemorySize/1MB,1);gpu=$g;python=$p}|ConvertTo-Json -Depth 4 -Compress"
    ].join("");
    const r=await runProcess("powershell",["-NoProfile","-Command",ps],WORKSPACE,30000);
    if(r.code===0){
      try{
        const d=JSON.parse(r.stdout.trim());
        out.ram=d.ram??null;
        out.gpu=Array.isArray(d.gpu)?d.gpu:(d.gpu?[d.gpu]:[]);
        out.python=d.python||null;
      }catch{}
    }
  }
  return out;
}
async function sha256File(file){
  return await new Promise((resolve,reject)=>{
    const h=crypto.createHash("sha256");
    const s=fs.createReadStream(file);
    s.on("data",d=>h.update(d));s.on("error",reject);s.on("end",()=>resolve(h.digest("hex")));
  });
}
async function downloadFile(url,dest){
  const temp=dest+".part";
  if(fs.existsSync(temp))fs.rmSync(temp,{force:true});
  const r=await fetch(url,{redirect:"follow"});
  if(!r.ok||!r.body)throw new Error("Download failed: "+r.status+" "+url);
  const out=fs.createWriteStream(temp);
  let bytes=0;
  for await(const chunk of r.body){bytes+=chunk.length;if(!out.write(chunk))await new Promise(ok=>out.once("drain",ok));}
  await new Promise((ok,fail)=>out.end(err=>err?fail(err):ok()));
  fs.renameSync(temp,dest);
  return bytes;
}
function findSdCli(){
  if(!fs.existsSync(SDCPP_DIR))return null;
  const stack=[SDCPP_DIR];
  while(stack.length){
    const dir=stack.pop();
    for(const ent of fs.readdirSync(dir,{withFileTypes:true})){
      const full=path.join(dir,ent.name);
      if(ent.isDirectory())stack.push(full);
      else if(/^sd(?:-cli)?\.exe$/i.test(ent.name))return full;
    }
  }
  return null;
}
async function sdCppStatus(){
  const exe=findSdCli();
  const modelExists=fs.existsSync(SDCPP_MODEL);
  return {
    ready:Boolean(exe&&modelExists),
    provider:"stable-diffusion.cpp-cpu",
    free:true,
    exe:exe?path.relative(WORKSPACE,exe):null,
    model:modelExists?path.relative(WORKSPACE,SDCPP_MODEL):null,
    model_bytes:modelExists?fs.statSync(SDCPP_MODEL).size:0
  };
}
async function installSdCpp(){
  if(process.platform!=="win32")throw new Error("The automatic image installer currently supports the Dexter Windows Home PC.");
  const zip=path.join(SDCPP_DIR,"stable-diffusion-cpp.zip");
  const exe=findSdCli();
  if(!exe){
    await downloadFile(SDCPP_RELEASE_URL,zip);
    const hash=await sha256File(zip);
    if(hash!==SDCPP_RELEASE_SHA256){fs.rmSync(zip,{force:true});throw new Error("stable-diffusion.cpp checksum verification failed.");}
    const ps="Expand-Archive -LiteralPath '"+zip.replace(/'/g,"''")+"' -DestinationPath '"+SDCPP_DIR.replace(/'/g,"''")+"' -Force";
    const un=await runProcess("powershell",["-NoProfile","-Command",ps],WORKSPACE,120000);
    if(un.code!==0)throw new Error("Could not unpack image engine: "+un.stderr);
    fs.rmSync(zip,{force:true});
  }
  if(!fs.existsSync(SDCPP_MODEL)){
    await downloadFile(SDCPP_MODEL_URL,SDCPP_MODEL);
    const hash=await sha256File(SDCPP_MODEL);
    if(hash!==SDCPP_MODEL_SHA256){fs.rmSync(SDCPP_MODEL,{force:true});throw new Error("Image model checksum verification failed.");}
  }
  const status=await sdCppStatus();
  if(!status.ready)throw new Error("Image engine files installed but sd-cli.exe was not found.");
  return {...status,installed:true};
}
async function sdCppGenerate(request={}){
  const status=await sdCppStatus();
  if(!status.ready)throw new Error("Free CPU image engine is not installed.");
  const prompt=String(request.prompt||"").trim();
  if(!prompt)throw new Error("Image prompt is required.");
  const negative=String(request.negative_prompt||"blurry, low quality, distorted, watermark, unreadable text").slice(0,1500);
  const width=Math.max(256,Math.min(768,Number(request.width||512)));
  const height=Math.max(256,Math.min(768,Number(request.height||512)));
  const steps=Math.max(4,Math.min(8,Number(request.steps||6)));
  const seed=Number.isFinite(Number(request.seed))?Math.trunc(Number(request.seed)):Math.floor(Math.random()*2147483647);
  const filename="dexter-"+new Date().toISOString().replace(/[:.]/g,"-")+"-"+crypto.randomUUID().slice(0,8)+".png";
  const out=path.join(IMAGE_DIR,filename);
  const exe=path.resolve(WORKSPACE,status.exe);
  const args=["-M","img_gen","-m",SDCPP_MODEL,"-p",prompt.slice(0,3000),"-n",negative,
    "--sampling-method","lcm","--scheduler","lcm","--steps",String(steps),"--cfg-scale","1.5",
    "-W",String(width),"-H",String(height),"-s",String(seed),"-o",out];
  const r=await runProcess(exe,args,SDCPP_DIR,600000);
  if(r.code!==0||!fs.existsSync(out))throw new Error("Local image generation failed: "+String(r.stderr||r.stdout).slice(-3000));
  const result={status:"completed",provider:"stable-diffusion.cpp-cpu",free:true,prompt,width,height,steps,seed,filename,path:path.relative(WORKSPACE,out),bytes:fs.statSync(out).size,mime_type:"image/png"};
  if(request.return_base64===true)result.image_base64=fs.readFileSync(out).toString("base64");
  return result;
}

async function comfyStatus(){
  try{
    const [statsRes,objRes]=await Promise.all([
      fetch(COMFY_URL+"/system_stats",{signal:AbortSignal.timeout(5000)}),
      fetch(COMFY_URL+"/object_info/CheckpointLoaderSimple",{signal:AbortSignal.timeout(5000)})
    ]);
    if(!statsRes.ok||!objRes.ok)throw new Error("ComfyUI not ready");
    const stats=await statsRes.json();
    const obj=await objRes.json();
    const choices=obj?.CheckpointLoaderSimple?.input?.required?.ckpt_name?.[0]||[];
    return {ready:true,url:COMFY_URL,checkpoints:Array.isArray(choices)?choices.slice(0,50):[],system:stats};
  }catch(e){
    return {ready:false,url:COMFY_URL,error:String(e?.message||e)};
  }
}
async function comfyGenerate(request={}){
  const prompt=String(request.prompt||"").trim();
  if(!prompt)throw new Error("Image prompt is required.");
  const status=await comfyStatus();
  if(!status.ready)throw new Error("Local image engine is not running. Install/start ComfyUI on the Dexter Home PC first.");
  const checkpoint=String(request.checkpoint||status.checkpoints?.[0]||"");
  if(!checkpoint)throw new Error("No local image checkpoint is installed.");
  const width=Math.max(256,Math.min(1536,Number(request.width||1024)));
  const height=Math.max(256,Math.min(1536,Number(request.height||1024)));
  const steps=Math.max(8,Math.min(40,Number(request.steps||24)));
  const cfg=Math.max(1,Math.min(12,Number(request.cfg||7)));
  const seed=Number.isFinite(Number(request.seed))?Number(request.seed):Math.floor(Math.random()*2147483647);
  const negative=String(request.negative_prompt||"blurry, low quality, distorted text, watermark").slice(0,2000);
  const clientId="dexter-"+crypto.randomUUID();
  const workflow={
    "1":{class_type:"CheckpointLoaderSimple",inputs:{ckpt_name:checkpoint}},
    "2":{class_type:"CLIPTextEncode",inputs:{text:prompt.slice(0,4000),clip:["1",1]}},
    "3":{class_type:"CLIPTextEncode",inputs:{text:negative,clip:["1",1]}},
    "4":{class_type:"EmptyLatentImage",inputs:{width,height,batch_size:1}},
    "5":{class_type:"KSampler",inputs:{seed,steps,cfg,sampler_name:"euler",scheduler:"normal",denoise:1,model:["1",0],positive:["2",0],negative:["3",0],latent_image:["4",0]}},
    "6":{class_type:"VAEDecode",inputs:{samples:["5",0],vae:["1",2]}},
    "7":{class_type:"SaveImage",inputs:{filename_prefix:"DexterAI",images:["6",0]}}
  };
  const qr=await fetch(COMFY_URL+"/prompt",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({prompt:workflow,client_id:clientId}),signal:AbortSignal.timeout(15000)});
  const q=await qr.json().catch(()=>({}));
  if(!qr.ok||!q.prompt_id)throw new Error(q?.error?.message||q?.error||"ComfyUI rejected the image job.");
  const deadline=Date.now()+Math.max(60000,Math.min(600000,Number(request.timeout_ms||300000)));
  let imageMeta=null;
  while(Date.now()<deadline){
    await new Promise(r=>setTimeout(r,1500));
    const hr=await fetch(COMFY_URL+"/history/"+encodeURIComponent(q.prompt_id),{signal:AbortSignal.timeout(10000)});
    if(!hr.ok)continue;
    const h=await hr.json();
    const outputs=h?.[q.prompt_id]?.outputs||{};
    for(const value of Object.values(outputs)){
      const imgs=value?.images;
      if(Array.isArray(imgs)&&imgs.length){imageMeta=imgs[0];break;}
    }
    if(imageMeta)break;
  }
  if(!imageMeta)throw new Error("Local image generation timed out.");
  const params=new URLSearchParams({filename:imageMeta.filename,subfolder:imageMeta.subfolder||"",type:imageMeta.type||"output"});
  const ir=await fetch(COMFY_URL+"/view?"+params.toString(),{signal:AbortSignal.timeout(30000)});
  if(!ir.ok)throw new Error("Generated image could not be downloaded from ComfyUI.");
  const buf=Buffer.from(await ir.arrayBuffer());
  const ext=path.extname(String(imageMeta.filename||""))||".png";
  const filename="dexter-"+new Date().toISOString().replace(/[:.]/g,"-")+"-"+crypto.randomUUID().slice(0,8)+ext;
  const file=path.join(IMAGE_DIR,filename);
  fs.writeFileSync(file,buf);
  return {status:"completed",provider:"comfyui-local",free:true,prompt,checkpoint,width,height,steps,seed,filename,path:path.relative(WORKSPACE,file),bytes:buf.length};
}

async function installChatModel(request={}){
  const model=String(request.model||"qwen3:1.7b").trim();
  if(!/^qwen3:(?:0\.6b|1\.7b|4b)$/i.test(model))throw new Error("Unsupported local chat model.");
  const controller=new AbortController();
  const timer=setTimeout(()=>controller.abort(),600000);
  try{
    const r=await fetch(OLLAMA_URL+"/api/pull",{
      method:"POST",
      headers:{"Content-Type":"application/json"},
      body:JSON.stringify({name:model,stream:false}),
      signal:controller.signal
    });
    const d=await r.json().catch(()=>({}));
    if(!r.ok)throw new Error("Ollama model install failed: "+String(d?.error||r.status));
    return {installed:true,model,free:true,provider:"ollama-local",status:d?.status||"success"};
  }finally{clearTimeout(timer);}
}


async function runHardwarePowerShell(script,timeout=45000){
  if(process.platform!=="win32")throw new Error("Windows hardware tools require the Dexter Windows PC.");
  const encoded=Buffer.from(String(script),"utf16le").toString("base64");
  const r=await runProcess("powershell.exe",["-NoProfile","-NonInteractive","-ExecutionPolicy","Bypass","-EncodedCommand",encoded],__dirname,timeout);
  if(r.code!==0)throw new Error((r.stderr||r.stdout||("PowerShell exited "+r.code)).trim());
  return String(r.stdout||"").trim();
}
function parseHardwareJson(v){
  const s=String(v||"").trim();
  if(!s)return null;
  try{return JSON.parse(s)}catch{}
  const a=s.indexOf("{"),b=s.lastIndexOf("}");
  if(a>=0&&b>a)return JSON.parse(s.slice(a,b+1));
  const x=s.indexOf("["),y=s.lastIndexOf("]");
  if(x>=0&&y>x)return JSON.parse(s.slice(x,y+1));
  throw new Error("Hardware diagnostic returned invalid JSON.");
}
async function hardwareInspect(){
  const ps=[
    "$ErrorActionPreference='SilentlyContinue'",
    "$printers=@(Get-CimInstance Win32_Printer|Select-Object Name,DriverName,PortName,Default,Network,WorkOffline,PrinterStatus,DetectedErrorState)",
    "$ports=@(Get-PrinterPort|Select-Object Name,Description,PrinterHostAddress,PortNumber)",
    "$spooler=Get-Service Spooler|Select-Object Name,Status,StartType",
    "$p='Registry::HKEY_CLASSES_ROOT\\dexterscitaq\\shell\\open\\command'",
    "$cmd=(Get-ItemProperty $p -ErrorAction SilentlyContinue).'(default)'",
    "$procs=@(Get-CimInstance Win32_Process|Where-Object {$_.Name -match 'dexter|citaq|bridge|pos|print' -or $_.CommandLine -match 'dexterscitaq|print-pos|hardware bridge'}|Select-Object Name,ProcessId,ExecutablePath,CommandLine)",
    "[ordered]@{computer=$env:COMPUTERNAME;printers=$printers;pos80=@($printers|Where-Object {$_.Name -match 'POS-80|POS80|80'});ports=$ports;spooler=$spooler;protocol=[ordered]@{registered=[bool]$cmd;command=$cmd};bridge_processes=$procs}|ConvertTo-Json -Depth 7 -Compress"
  ].join(";");
  return parseHardwareJson(await runHardwarePowerShell(ps,45000));
}
async function hardwarePrinters(){
  const ps="@(Get-CimInstance Win32_Printer|Select-Object Name,DriverName,PortName,Default,Network,WorkOffline,PrinterStatus,DetectedErrorState)|ConvertTo-Json -Depth 5 -Compress";
  return parseHardwareJson(await runHardwarePowerShell(ps,30000))||[];
}
async function hardwarePorts(){
  const ps="@(Get-PrinterPort|Select-Object Name,Description,PrinterHostAddress,PortNumber)|ConvertTo-Json -Depth 5 -Compress";
  return parseHardwareJson(await runHardwarePowerShell(ps,30000))||[];
}
async function hardwareSpooler(){
  return parseHardwareJson(await runHardwarePowerShell("Get-Service Spooler|Select-Object Name,Status,StartType|ConvertTo-Json -Compress",15000));
}
async function hardwareBridgeRead(){
  const ps=[
    "$ErrorActionPreference='SilentlyContinue'",
    "$p='Registry::HKEY_CLASSES_ROOT\\dexterscitaq\\shell\\open\\command'",
    "$cmd=(Get-ItemProperty $p -ErrorAction SilentlyContinue).'(default)'",
    "$procs=@(Get-CimInstance Win32_Process|Where-Object {$_.Name -match 'dexter|citaq|bridge|pos|print' -or $_.CommandLine -match 'dexterscitaq|print-pos|hardware bridge'}|Select-Object Name,ProcessId,ExecutablePath,CommandLine)",
    "[ordered]@{registered=[bool]$cmd;command=$cmd;processes=$procs}|ConvertTo-Json -Depth 6 -Compress"
  ].join(";");
  return parseHardwareJson(await runHardwarePowerShell(ps,30000));
}


async function documentExtract(request={}){
  const url=String(request.url||"").trim();
  const name=String(request.name||"document").replace(/[^A-Za-z0-9._-]/g,"_");
  if(!url)throw new Error("Document URL is required.");
  const ext=path.extname(name).toLowerCase();
  const tempDir=path.join(WORKSPACE,"document-extract");fs.mkdirSync(tempDir,{recursive:true});
  const file=path.join(tempDir,Date.now()+"-"+name);
  const rr=await fetch(url);if(!rr.ok)throw new Error("Document download failed HTTP "+rr.status);
  fs.writeFileSync(file,Buffer.from(await rr.arrayBuffer()));
  if([".txt",".md",".csv",".json",".html",".xml",".js",".ts",".css",".sql",".yml",".yaml"].includes(ext)){
    return {text:fs.readFileSync(file,"utf8").slice(0,1000000),method:"utf8"};
  }
  const py=[
    "import sys,zipfile,re,html,os",
    "p=sys.argv[1]; ext=os.path.splitext(p)[1].lower()",
    "def clean(x): return re.sub(r'\\\\s+',' ',html.unescape(re.sub(r'<[^>]+>',' ',x))).strip()",
    "out=''",
    "if ext=='.docx':",
    " z=zipfile.ZipFile(p); out=clean(z.read('word/document.xml').decode('utf-8','ignore'))",
    "elif ext=='.xlsx':",
    " z=zipfile.ZipFile(p); parts=[]; shared=[]",
    " if 'xl/sharedStrings.xml' in z.namelist(): shared=re.findall(r'<t[^>]*>(.*?)</t>',z.read('xl/sharedStrings.xml').decode('utf-8','ignore'),re.S)",
    " for n in sorted(x for x in z.namelist() if x.startswith('xl/worksheets/sheet') and x.endswith('.xml')):",
    "  x=z.read(n).decode('utf-8','ignore'); vals=[]",
    "  for t,b in re.findall(r'<c[^>]*?(?:t=\\\"(.*?)\\\")?[^>]*>(.*?)</c>',x,re.S):",
    "   m=re.search(r'<v>(.*?)</v>',b,re.S); v=m.group(1) if m else clean(b)",
    "   if t=='s' and v.isdigit() and int(v)<len(shared): v=shared[int(v)]",
    "   vals.append(clean(v))",
    "  parts.append(n+'\\\\n'+' | '.join(vals))",
    " out='\\\\n\\\\n'.join(parts)",
    "elif ext=='.pdf':",
    " from pypdf import PdfReader",
    " out='\\\\n'.join((pg.extract_text() or '') for pg in PdfReader(p).pages)",
    "else: sys.exit(4)",
    "print(out[:1000000])"
  ].join("\n");
  const pr=await runProcess("python",["-c",py,file],WORKSPACE,120000).catch(e=>({code:1,stdout:"",stderr:String(e?.message||e)}));
  if(pr.code===0)return {text:String(pr.stdout||"").slice(0,1000000),method:"python"};
  if(ext===".pdf"){
    const outFile=file+".txt";
    const pd=await runProcess("pdftotext",[file,outFile],WORKSPACE,120000).catch(()=>null);
    if(pd&&pd.code===0&&fs.existsSync(outFile))return {text:fs.readFileSync(outFile,"utf8").slice(0,1000000),method:"pdftotext"};
  }
  throw new Error("Document extraction failed: "+String(pr.stderr||"unsupported format").slice(0,1000));
}
function minimalPdf(text){
  const safe=String(text||"").replace(/[()\\]/g,m=>"\\"+m).replace(/[^\x20-\x7E\n]/g,"?");
  const lines=safe.split(/\r?\n/).slice(0,120).map(x=>x.slice(0,100));
  const stream="BT /F1 10 Tf 50 790 Td 12 TL "+lines.map((l,i)=>(i?"T* ":"")+"("+l+") Tj").join(" ")+" ET";
  const objs=["<< /Type /Catalog /Pages 2 0 R >>","<< /Type /Pages /Kids [3 0 R] /Count 1 >>","<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>","<< /Length "+Buffer.byteLength(stream)+" >>\nstream\n"+stream+"\nendstream","<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"];
  let out="%PDF-1.4\n",offs=[0];objs.forEach((o,i)=>{offs.push(Buffer.byteLength(out));out+=(i+1)+" 0 obj\n"+o+"\nendobj\n";});
  const x=Buffer.byteLength(out);out+="xref\n0 "+(objs.length+1)+"\n0000000000 65535 f \n"+offs.slice(1).map(n=>String(n).padStart(10,"0")+" 00000 n ").join("\n")+"\ntrailer << /Size "+(objs.length+1)+" /Root 1 0 R >>\nstartxref\n"+x+"\n%%EOF";
  return Buffer.from(out,"binary");
}
async function richArtifactExport(request={}){
  const format=String(request.format||"pdf").toLowerCase();
  const name=String(request.name||"dexter-artifact").replace(/[^A-Za-z0-9._-]/g,"-");
  const content=String(request.content||"");
  const dir=path.join(WORKSPACE,"exports");fs.mkdirSync(dir,{recursive:true});
  const xmlEsc=v=>String(v||"").replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;");
  if(format==="pdf"){const file=path.join(dir,name+".pdf");fs.writeFileSync(file,minimalPdf(content));return {path:path.relative(WORKSPACE,file),format,size:fs.statSync(file).size};}
  if(format==="zip"){
    const src=path.join(dir,name+".txt"),zip=path.join(dir,name+".zip");fs.writeFileSync(src,content,"utf8");
    await runHardwarePowerShell("Compress-Archive -Path "+JSON.stringify(src)+" -DestinationPath "+JSON.stringify(zip)+" -Force",60000);
    return {path:path.relative(WORKSPACE,zip),format,size:fs.statSync(zip).size};
  }
  if(format==="docx"){
    const work=path.join(dir,name+"-docx-"+Date.now());fs.mkdirSync(path.join(work,"_rels"),{recursive:true});fs.mkdirSync(path.join(work,"word"),{recursive:true});
    fs.writeFileSync(path.join(work,"[Content_Types].xml"),'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>');
    fs.writeFileSync(path.join(work,"_rels",".rels"),'<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>');
    const body=content.split(/\r?\n/).map(x=>'<w:p><w:r><w:t xml:space="preserve">'+xmlEsc(x)+'</w:t></w:r></w:p>').join("");
    fs.writeFileSync(path.join(work,"word","document.xml"),'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>'+body+'</w:body></w:document>');
    const tmp=path.join(dir,name+".docx.zip"),out=path.join(dir,name+".docx");
    await runHardwarePowerShell("Compress-Archive -Path "+JSON.stringify(path.join(work,"*"))+" -DestinationPath "+JSON.stringify(tmp)+" -Force",60000);
    fs.renameSync(tmp,out);return {path:path.relative(WORKSPACE,out),format,size:fs.statSync(out).size};
  }
  if(format==="xlsx"){
    const work=path.join(dir,name+"-xlsx-"+Date.now());fs.mkdirSync(path.join(work,"_rels"),{recursive:true});fs.mkdirSync(path.join(work,"xl","worksheets"),{recursive:true});fs.mkdirSync(path.join(work,"xl","_rels"),{recursive:true});
    fs.writeFileSync(path.join(work,"[Content_Types].xml"),'<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>');
    fs.writeFileSync(path.join(work,"_rels",".rels"),'<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>');
    fs.writeFileSync(path.join(work,"xl","workbook.xml"),'<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Dexter" sheetId="1" r:id="rId1"/></sheets></workbook>');
    fs.writeFileSync(path.join(work,"xl","_rels","workbook.xml.rels"),'<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>');
    const rows=content.split(/\r?\n/).slice(0,5000).map((x,i)=>'<row r="'+(i+1)+'"><c r="A'+(i+1)+'" t="inlineStr"><is><t>'+xmlEsc(x)+'</t></is></c></row>').join("");
    fs.writeFileSync(path.join(work,"xl","worksheets","sheet1.xml"),'<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>'+rows+'</sheetData></worksheet>');
    const tmp=path.join(dir,name+".xlsx.zip"),out=path.join(dir,name+".xlsx");
    await runHardwarePowerShell("Compress-Archive -Path "+JSON.stringify(path.join(work,"*"))+" -DestinationPath "+JSON.stringify(tmp)+" -Force",60000);
    fs.renameSync(tmp,out);return {path:path.relative(WORKSPACE,out),format,size:fs.statSync(out).size};
  }
  throw new Error("Rich export supports PDF, DOCX, XLSX and ZIP on the Home PC.");
}


function findAdb(){
  const explicit=String(process.env.DEXTER_ADB||"").trim();
  const candidates=[
    explicit,
    path.join(process.env.LOCALAPPDATA||"","Android","Sdk","platform-tools","adb.exe"),
    path.join(process.env.USERPROFILE||"","AppData","Local","Android","Sdk","platform-tools","adb.exe"),
    path.join(WORKSPACE,"android-tools","platform-tools","adb.exe"),
    path.join(WORKSPACE,"android-tools","adb.exe")
  ].filter(Boolean);
  return candidates.find(p=>fs.existsSync(p))||null;
}
async function ensureAdb(){
  const existing=findAdb(); if(existing)return existing;
  if(process.platform!=="win32")throw new Error("ADB is not installed and automatic platform-tools setup is only enabled on the Dexter Windows PC.");
  const root=path.join(WORKSPACE,"android-tools");
  const zip=path.join(root,"platform-tools-latest-windows.zip");
  fs.mkdirSync(root,{recursive:true});
  const url="https://dl.google.com/android/repository/platform-tools-latest-windows.zip";
  const r=await fetch(url,{cache:"no-store"}); if(!r.ok)throw new Error("Android platform-tools download failed HTTP "+r.status);
  fs.writeFileSync(zip,Buffer.from(await r.arrayBuffer()));
  await runHardwarePowerShell("Expand-Archive -Path "+JSON.stringify(zip)+" -DestinationPath "+JSON.stringify(root)+" -Force",120000);
  try{fs.unlinkSync(zip)}catch{}
  const adb=findAdb(); if(!adb)throw new Error("Android platform-tools installed but adb.exe was not found.");
  return adb;
}
async function adbRun(args,timeout=60000){
  const adb=await ensureAdb();
  const result=await runProcess(adb,args,WORKSPACE,timeout);
  if(result.code!==0)throw new Error(String(result.stderr||result.stdout||"ADB command failed").slice(0,3000));
  return result;
}
function parseAdbDevices(stdout){
  return String(stdout||"").split(/\r?\n/).slice(1).map(x=>x.trim()).filter(Boolean).map(line=>{
    const parts=line.split(/\s+/),serial=parts.shift()||"",state=parts.shift()||"unknown",meta={};
    for(const p of parts){const i=p.indexOf(":");if(i>0)meta[p.slice(0,i)]=p.slice(i+1);}
    return {serial,state,model:meta.model||null,product:meta.product||null,device:meta.device||null,transport_id:meta.transport_id||null};
  });
}
function parseAdbMdnsServices(stdout){
  const out=[];
  for(const line of String(stdout||"").split(/\r?\n/)){
    const m=line.match(/_adb-tls-connect\._tcp\.?\s+([^\s:]+|\d{1,3}(?:\.\d{1,3}){3}):(\d{2,5})/i);
    if(m)out.push(m[1]+":"+m[2]);
  }
  return [...new Set(out)];
}
async function androidAutoReconnect(){
  const before=await adbRun(["devices","-l"],30000).catch(()=>({stdout:""}));
  const already=parseAdbDevices(before.stdout).filter(d=>d.state==="device");
  const mdns=await adbRun(["mdns","services"],30000).catch(()=>({stdout:""}));
  const endpoints=parseAdbMdnsServices(mdns.stdout);
  const attempts=[];
  for(const address of endpoints){
    try{
      const r=await adbRun(["connect",address],12000);
      attempts.push({address,ok:/connected|already connected/i.test(String(r.stdout||"")+String(r.stderr||"")),output:String(r.stdout||r.stderr||"").trim().slice(0,500)});
    }catch(e){
      attempts.push({address,ok:false,error:String(e?.message||e).slice(0,500)});
    }
  }
  const after=await adbRun(["devices","-l"],30000).catch(()=>({stdout:""}));
  return {already_connected:already,endpoints,attempts,devices:parseAdbDevices(after.stdout)};
}

async function androidDeviceInfo(serial){
  const target=serial?["-s",String(serial)]:[];
  const [model,brand,version,battery,storage]=await Promise.all([
    adbRun([...target,"shell","getprop","ro.product.model"]).then(x=>x.stdout.trim()).catch(()=>null),
    adbRun([...target,"shell","getprop","ro.product.brand"]).then(x=>x.stdout.trim()).catch(()=>null),
    adbRun([...target,"shell","getprop","ro.build.version.release"]).then(x=>x.stdout.trim()).catch(()=>null),
    adbRun([...target,"shell","dumpsys","battery"]).then(x=>x.stdout.slice(0,12000)).catch(()=>null),
    adbRun([...target,"shell","df","-h","/data"]).then(x=>x.stdout.slice(0,4000)).catch(()=>null)
  ]);
  return {serial:serial||null,model,brand,android_version:version,battery,storage};
}
async function androidInstallApproved(request={}){
  if(request?.approval_granted!==true)throw new Error("Android app installation requires owner approval.");
  const installUrl=String(request.url||"").trim();
  const approvedDexterTest=/^https:\/\/jamiegreen294-boop\.github\.io\/dexters-ai-v1\/device\/Dexter-Business-Phone-v\d+\.apk$/i.test(installUrl);
  const approvedDexterRaw=/^https:\/\/raw\.githubusercontent\.com\/jamiegreen294-boop\/dexters-ai-v1\/gh-pages\/device\/Dexter-Business-Phone-v\d+\.apk$/i.test(installUrl);
  const approvedDexterOsPreview=/^https:\/\/jamiegreen294-boop\.github\.io\/dexters-ai-v1\/device\/Dexter-OS-Preview-v\d+\.apk$/i.test(installUrl);
  if(!LIVE_ACTIONS&&!approvedDexterTest&&!approvedDexterRaw&&!approvedDexterOsPreview)throw new Error("Only owner-approved Dexter Business Phone or Dexter OS Preview test APKs may be installed while Dexter is in TEST mode.");
  const serial=String(request.serial||"").trim();
  const url=String(request.url||"").trim();
  if(!/^https:\/\//i.test(url))throw new Error("Approved APK URL must use HTTPS.");
  const expectedSha=String(request.sha256||"").trim().toLowerCase();
  const safeName=(String(request.name||"dexter-app").replace(/[^A-Za-z0-9._-]+/g,"-").slice(0,80)||"dexter-app")+".apk";
  const dir=path.join(WORKSPACE,"android-apks");fs.mkdirSync(dir,{recursive:true});
  const dest=path.join(dir,Date.now()+"-"+safeName);
  await downloadFile(url,dest);
  const actual=String(await sha256File(dest)).toLowerCase();
  if(expectedSha&&actual!==expectedSha)throw new Error("APK checksum mismatch; install blocked.");
  const args=[];if(serial)args.push("-s",serial);args.push("install","-r",dest);
  const result=await adbRun(args,180000);
  return {ok:true,serial:serial||null,name:safeName,sha256:actual,output:result.stdout.slice(-4000)};
}
async function androidTool(tool,request={}){
  if(tool==="android.devices"){
    const r=await adbRun(["devices","-l"],30000);
    return {devices:parseAdbDevices(r.stdout),adb:findAdb()};
  }
  if(tool==="android.connect"){
    const address=String(request.address||"").trim();
    if(!/^[A-Za-z0-9_.:-]+:\d{2,5}$/.test(address))throw new Error("Android wireless-debug address must be host:port.");
    const r=await adbRun(["connect",address],30000);
    return {address,output:r.stdout.trim()};
  }
  if(tool==="android.autoconnect"){
    return await androidAutoReconnect();
  }
  if(tool==="android.pair"){
    const address=String(request.address||"").trim(),code=String(request.code||"").trim();
    if(!/^[A-Za-z0-9_.:-]+:\d{2,5}$/.test(address))throw new Error("Pairing address must be host:port.");
    if(!/^\d{6}$/.test(code))throw new Error("Wireless debugging pairing code must be 6 digits.");
    const r=await adbRun(["pair",address,code],30000);
    return {address,paired:/success/i.test(r.stdout+r.stderr),output:(r.stdout+r.stderr).trim().slice(0,2000)};
  }
  if(tool==="android.info")return await androidDeviceInfo(String(request.serial||"").trim());
  if(tool==="android.dexter_os_activate"){
    if(request?.approval_granted!==true)throw new Error("Dexter OS launcher activation requires owner approval.");
    const serial=String(request.serial||"").trim();
    const target=serial?["-s",serial]:[];
    const component="uk.co.dextersspot.dexteros.preview/uk.co.dextersspot.dexterai.DexterHomeActivity";
    const steps=[];
    for(const cmd of [
      [...target,"shell","cmd","package","set-home-activity",component],
      [...target,"shell","settings","put","global","policy_control","immersive.navigation=*"],
      [...target,"shell","am","start","-a","android.intent.action.MAIN","-c","android.intent.category.HOME"]
    ]){
      try{
        const r=await adbRun(cmd,30000);
        steps.push({ok:true,command:cmd.slice(target.length+1).join(" "),output:String(r.stdout||"").trim().slice(0,3000)});
      }catch(e){
        steps.push({ok:false,command:cmd.slice(target.length+1).join(" "),error:String(e?.message||e)});
      }
    }
    const verify=await adbRun([...target,"shell","cmd","package","resolve-activity","--brief","-a","android.intent.action.MAIN","-c","android.intent.category.HOME"],30000).catch(()=>({stdout:""}));
    return {serial:serial||null,component,navigation:"immersive",resolved_home:String(verify.stdout||"").trim(),steps};
  }
  if(tool==="android.shell"){
    const command=String(request.command||"").trim();
    if(!command)throw new Error("Android shell command is required.");
    const serial=String(request.serial||"").trim();
    const args=[];if(serial)args.push("-s",serial);args.push("shell","sh","-c",command);
    const r=await adbRun(args,60000);
    return {serial:serial||null,output:String(r.stdout||"").slice(0,12000)};
  }
  if(tool==="android.package_info"){
    const pkg=String(request.package||"uk.co.dextersspot.dexterai").trim();
    if(!/^[A-Za-z0-9_.]+$/.test(pkg))throw new Error("Invalid Android package name.");
    const args=[];if(request.serial)args.push("-s",String(request.serial));args.push("shell","dumpsys","package",pkg);
    const r=await adbRun(args,30000);
    const out=String(r.stdout||"");
    const vc=(out.match(/versionCode=(\d+)/)||[])[1]||null;
    const vn=(out.match(/versionName=([^\r\n]+)/)||[])[1]?.trim()||null;
    return {package:pkg,versionCode:vc?Number(vc):null,versionName:vn,raw:out.slice(0,12000)};
  }
  if(tool==="android.packages"){
    const args=[];if(request.serial)args.push("-s",String(request.serial));args.push("shell","pm","list","packages","-3");
    const r=await adbRun(args,30000);
    return {packages:r.stdout.split(/\r?\n/).map(x=>x.replace(/^package:/,"").trim()).filter(Boolean).sort()};
  }
  if(tool==="android.launch"){
    const pkg=String(request.package||"").trim();
    if(!/^[A-Za-z0-9_.]+$/.test(pkg))throw new Error("Invalid Android package name.");
    const args=[];if(request.serial)args.push("-s",String(request.serial));args.push("shell","monkey","-p",pkg,"-c","android.intent.category.LAUNCHER","1");
    const r=await adbRun(args,30000);return {package:pkg,output:r.stdout.slice(-3000)};
  }
  if(tool==="android.install")return await androidInstallApproved(request);
  if(tool==="android.apply_business_profile"){
    const serial=String(request.serial||"").trim(),target=serial?["-s",serial]:[];
    const results=[];
    const safeCommands=[
      ["shell","settings","put","system","screen_off_timeout","300000"],
      ["shell","settings","put","global","stay_on_while_plugged_in","3"],
      ["shell","settings","put","system","accelerometer_rotation","1"]
    ];
    for(const cmd of safeCommands){try{const r=await adbRun([...target,...cmd],20000);results.push({command:cmd.join(" "),ok:true,output:r.stdout.trim()});}catch(e){results.push({command:cmd.join(" "),ok:false,error:String(e?.message||e)})}}
    return {serial:serial||null,profile:"Dexter Business Phone",calls_untouched:true,results};
  }
  if(tool==="android.uninstall"){
    requireApprovedLive({...request,approved_live:true},"Android app uninstall");
    const pkg=String(request.package||"").trim();if(!/^[A-Za-z0-9_.]+$/.test(pkg))throw new Error("Invalid Android package name.");
    const args=[];if(request.serial)args.push("-s",String(request.serial));args.push("uninstall",pkg);
    const r=await adbRun(args,60000);return {package:pkg,output:r.stdout.trim()};
  }
  throw new Error("Unsupported Android device tool: "+tool);
}

async function workspaceTool(tool,request={}){
  if(tool==="system.info")return await systemInfo();
  if(String(tool).startsWith("android."))return await androidTool(tool,request);
  if(tool==="hardware.inspect")return await hardwareInspect();
  if(tool==="hardware.printers.read"||tool==="system.printers")return await hardwarePrinters();
  if(tool==="hardware.ports.read")return await hardwarePorts();
  if(tool==="hardware.spooler.read")return await hardwareSpooler();
  if(tool==="hardware.bridge.read")return await hardwareBridgeRead();
  if(tool==="local_ai.install_chat_model")return await installChatModel(request);
  if(tool==="image.install")return await installSdCpp();
  if(tool==="image.status"){
    const comfy=await comfyStatus();
    const cpu=await sdCppStatus();
    return {ready:comfy.ready||cpu.ready,preferred:comfy.ready?"comfyui":cpu.ready?"stable-diffusion.cpp-cpu":null,comfyui:comfy,cpu};
  }
  if(tool==="image.generate"){
    const comfy=await comfyStatus();
    if(comfy.ready)return await comfyGenerate(request);
    return await sdCppGenerate(request);
  }
  if(tool==="workspace.list"){
    const dir=safeWorkspacePath(request.path||"");
    return {path:path.relative(WORKSPACE,dir),entries:fs.readdirSync(dir,{withFileTypes:true}).map(x=>({name:x.name,type:x.isDirectory()?"directory":"file"})).slice(0,500)};
  }
  if(tool==="workspace.tree")return {path:String(request.path||""),entries:await listTree(String(request.path||""),Number(request.depth||3),Number(request.max_entries||500))};
  if(tool==="web.research")return await internetResearch(request);
  if(tool==="document.extract")return await documentExtract(request);
  if(tool==="artifact.export.rich")return await richArtifactExport(request);
  if(tool==="code.agent")return await codingAgent(request);
  if(tool==="code.direct")return await directCodingPlan(request);
  if(tool==="workspace.read"){
    const file=safeWorkspacePath(request.path);return {path:request.path,content:fs.readFileSync(file,"utf8").slice(0,200000)};
  }
  if(tool==="workspace.write"){
    const file=safeWorkspacePath(request.path);fs.mkdirSync(path.dirname(file),{recursive:true});fs.writeFileSync(file,String(request.content??""),"utf8");return {ok:true,path:request.path};
  }
  if(tool==="workspace.mkdir"){const dir=safeWorkspacePath(request.path);fs.mkdirSync(dir,{recursive:true});return {ok:true,path:request.path};}
  if(tool==="git.clone"){
    const url=String(request.url||"");if(!/^https:\/\/github\.com\/[\w.-]+\/[\w.-]+(?:\.git)?$/i.test(url))throw new Error("Only GitHub HTTPS repositories are allowed.");
    const dest=safeWorkspacePath(request.path||path.basename(url).replace(/\.git$/,""));
    if(fs.existsSync(dest)&&fs.readdirSync(dest).length)throw new Error("Destination already exists and is not empty.");
    fs.mkdirSync(path.dirname(dest),{recursive:true});
    return await runProcess("git",["clone","--depth","1",url,dest],WORKSPACE,180000);
  }
  if(tool==="git.init")return await runProcess("git",["init"],safeWorkspacePath(request.path||""),30000);
  if(tool==="git.status")return await runProcess("git",["status","--short","--branch"],safeWorkspacePath(request.path||""),30000);
  if(tool==="git.diff")return await runProcess("git",["diff","--",request.file||"."],safeWorkspacePath(request.path||""),30000);
  if(tool==="git.commit"){
    requireApprovedLive(request,"Git commit");
    const cwd=safeWorkspacePath(request.path||"");
    const add=await runProcess("git",["add","-A"],cwd,30000);
    if(add.code!==0)return add;
    return await runProcess("git",["commit","-m",String(request.message||"Dexter approved change").slice(0,180)],cwd,60000);
  }
  if(tool==="git.push"){
    requireApprovedLive(request,"Git push");
    const cwd=safeWorkspacePath(request.path||"");
    return await runProcess("git",["push",String(request.remote||"origin"),String(request.ref||"HEAD")],cwd,180000);
  }
  if(tool==="github.pr.create"){
    requireApprovedLive(request,"GitHub PR creation");
    const cwd=safeWorkspacePath(request.path||"");
    const args=["pr","create","--title",String(request.title||"Dexter change").slice(0,180),"--body",String(request.body||"Created by Dexter AI after owner approval").slice(0,4000)];
    if(request.base)args.push("--base",String(request.base));
    if(request.head)args.push("--head",String(request.head));
    return await runProcess("gh",args,cwd,120000);
  }
  if(tool==="vercel.deploy"){
    requireApprovedLive(request,"Vercel deploy");
    const cwd=safeWorkspacePath(request.path||"");
    const args=["vercel","deploy","--yes"];
    if(request.production===true)args.push("--prod");
    return await runProcess("npx",args,cwd,240000);
  }
  if(tool==="code.check"){
    const cwd=safeWorkspacePath(request.path||"");
    const rawKind=String(request.kind||"").toLowerCase().trim();
    const aliases={build:"npm-build","npm build":"npm-build",test:"npm-test","npm test":"npm-test",lint:"npm-lint","npm lint":"npm-lint",node:"node-check","node check":"node-check"};
    const kind=aliases[rawKind]||rawKind;
    if(kind==="npm-test")return await runProcess("npm",["test","--","--runInBand"],cwd,180000);
    if(kind==="npm-build")return await runProcess("npm",["run","build"],cwd,180000);
    if(kind==="npm-lint")return await runProcess("npm",["run","lint"],cwd,180000);
    if(kind==="node-check"){
      const file=String(request.file||"").trim();
      if(!file)throw new Error("node-check requires a file.");
      return await runProcess(process.execPath,["--check",file],cwd,30000);
    }
    throw new Error("Unsupported code check: "+rawKind+". Supported: node-check, npm-test, npm-build, npm-lint.");
  }
  throw new Error("Unsupported workspace tool: "+tool);
}
function saveJob(job){fs.writeFileSync(path.join(JOB_DIR,job.id+".json"),JSON.stringify(job,null,2));}
function loadJobs(){
  return fs.readdirSync(JOB_DIR).filter(x=>x.endsWith(".json")).map(x=>JSON.parse(fs.readFileSync(path.join(JOB_DIR,x),"utf8"))).sort((a,b)=>String(b.updated_at).localeCompare(String(a.updated_at))).slice(0,100);
}
async function createJob(body){
  const job={id:crypto.randomUUID(),type:String(body.type||"work"),status:"queued",request:body.request||{},created_at:new Date().toISOString(),updated_at:new Date().toISOString(),result:null,error:null};
  saveJob(job);
  queueMicrotask(async()=>{try{job.status="running";job.updated_at=new Date().toISOString();saveJob(job);
    if(job.type==="browser")job.result=await browserTool("browser.navigate_and_act",job.request);
    else if(job.type==="workspace")job.result=await workspaceTool(String(job.request.tool||""),job.request.input||{});
    else if(job.type==="research")job.result=await internetResearch(job.request);
    else if(job.type==="coding")job.result=await codingAgent(job.request);
    else throw new Error("Unsupported job type.");
    job.status="completed";
  }catch(e){job.status="failed";job.error=String(e?.message||e);}finally{job.updated_at=new Date().toISOString();saveJob(job);}});
  return job;
}
function scheduleSelfRestart(delayMs=1500){
  setTimeout(()=>process.exit(0),Math.max(500,Number(delayMs)||1500));
  return {restart_scheduled:true,restart_in_ms:Math.max(500,Number(delayMs)||1500),method:"launcher-watchdog"};
}

async function selfUpdateWorker(){
  const rawUrl="https://raw.githubusercontent.com/jamiegreen294-boop/dexters-ai-v1/build/real-dexter-ai/home-host/server.mjs?dexter_update="+Date.now();
  const controller=new AbortController();
  const timer=setTimeout(()=>controller.abort(),30000);
  try{
    const r=await fetch(rawUrl,{signal:controller.signal,cache:"no-store",headers:{"Cache-Control":"no-cache, no-store, must-revalidate","Pragma":"no-cache"}});
    if(!r.ok)throw new Error("Worker update download failed: HTTP "+r.status);
    const next=await r.text();
    if(!next.includes("Dexter AI Home Host running")||!next.includes("executeCloudJob"))throw new Error("Downloaded worker failed validation.");
    const current=path.join(__dirname,"server.mjs");
    const backup=path.join(__dirname,"server.mjs.backup");
    fs.copyFileSync(current,backup);
    fs.writeFileSync(current,next,"utf8");
    try{
      const launcherUrl="https://raw.githubusercontent.com/jamiegreen294-boop/dexters-ai-v1/build/real-dexter-ai/home-host/START-DEXTER.bat?dexter_update="+Date.now();
      const lr=await fetch(launcherUrl,{cache:"no-store",headers:{"Cache-Control":"no-cache, no-store, must-revalidate","Pragma":"no-cache"}});
      if(lr.ok){
        const launcher=await lr.text();
        if(launcher.includes("Dexter Home Host updates")&&launcher.includes("npm start")){
          fs.writeFileSync(path.join(__dirname,"START-DEXTER.bat"),launcher,"utf8");
        }
      }
    }catch{}
    const restart=scheduleSelfRestart(4500);
    return {updated:true,bytes:Buffer.byteLength(next),restart_required:false,...restart};
  }finally{clearTimeout(timer);}
}

async function executeCloudJob(job){
  const request=job?.request||{};
  if(job.job_type==="browser")return await browserTool(String(job.tool_name||"browser.navigate_and_act"),request);
  if(job.job_type==="workspace")return await workspaceTool(String(job.tool_name||""),request);
  if(job.job_type==="research")return await internetResearch(request);
  if(job.job_type==="coding"){
    if(String(job.tool_name||"")==="code.direct"||Array.isArray(request.files))return await directCodingPlan(request);
    return await codingAgent(request);
  }
  if(job.job_type==="system")return await workspaceTool(String(job.tool_name||"system.info"),request);
  if(job.job_type==="image")return await workspaceTool(String(job.tool_name||"image.generate"),request);
  if(job.job_type==="self_update")return await selfUpdateWorker();
  if(job.job_type==="self_restart")return scheduleSelfRestart(3000);
  if(job.job_type==="local_ai"){
    const messages=Array.isArray(request.messages)?request.messages:[{role:"user",content:String(request.prompt||"")}];
    const fast=request.fast!==false;
    const reply=fast
      ? await ollamaFast(messages,request.format,request.model||LOCAL_MODEL)
      : await ollama(messages,request.format,request.model||LOCAL_MODEL);
    return {reply,model:request.model||LOCAL_MODEL,provider:fast?"ollama-local-fast":"ollama-local"};
  }
  throw new Error("Unsupported cloud job type: "+job.job_type);
}
async function agentRequest(body){
  if(!AGENT_TOKEN)return null;
  const r=await fetch(AGENT_ENDPOINT,{method:"POST",headers:{"Content-Type":"application/json","x-dexter-agent-token":AGENT_TOKEN},body:JSON.stringify(body)});
  const d=await r.json().catch(()=>({}));
  if(!r.ok)throw new Error(d?.error||("Home-agent API failed "+r.status));
  return d;
}
async function heartbeat(){
  if(!AGENT_TOKEN)return;
  try{await agentRequest({action:"heartbeat"});}catch(e){console.error("Dexter heartbeat:",String(e?.message||e));}
}
async function pollHomeJobs(){
  if(!AGENT_TOKEN||agentBusy)return;
  agentBusy=true;
  try{
    const d=await agentRequest({action:"poll"});
    const job=d?.job;
    if(!job)return;
    try{
      const result=await executeCloudJob(job);
      await agentRequest({action:"result",job_id:job.id,status:"completed",result});
    }catch(e){
      await agentRequest({action:"result",job_id:job.id,status:"failed",error:String(e?.message||e).slice(0,5000)});
    }
  }catch(e){
    console.error("Dexter home-agent poll:",String(e?.message||e));
  }finally{
    agentBusy=false;
  }
}

async function health(){
  let ollamaReady=false,models=[];
  try{const r=await fetch(OLLAMA_URL+"/api/tags");const d=await r.json();ollamaReady=r.ok;models=(d.models||[]).map(x=>x.name).slice(0,20);}catch{}
  const comfy=await comfyStatus();
  const cpuImage=await sdCppStatus();
  return {status:"ready",host:"home-pc",browser:"chromium",internet:true,coding_agent:true,hardware_doctor:process.platform==="win32",hardware_tools:["hardware.inspect","hardware.printers.read","hardware.ports.read","hardware.spooler.read","hardware.bridge.read"],image_generation:comfy.ready||cpuImage.ready,live_actions:LIVE_ACTIONS,cloud_agent_paired:Boolean(AGENT_TOKEN),agent_endpoint:AGENT_ENDPOINT,headless:HEADLESS,workspace:WORKSPACE,ollama:{ready:ollamaReady,url:OLLAMA_URL,model:LOCAL_MODEL,code_model:CODE_MODEL,models},image:{preferred:comfy.ready?"comfyui":cpuImage.ready?"stable-diffusion.cpp-cpu":null,comfyui:comfy,cpu:cpuImage},jobs:loadJobs().length};
}
function serveFile(res,file,contentType){const data=fs.readFileSync(file);res.writeHead(200,{"Content-Type":contentType,"Cache-Control":"no-store"});res.end(data);}

let androidReconnectTimer=null;
async function startAndroidReconnectWatchdog(){
  if(androidReconnectTimer)return;
  const tick=()=>androidAutoReconnect().catch(()=>null);
  setTimeout(tick,5000);
  androidReconnectTimer=setInterval(tick,60000);
  androidReconnectTimer.unref?.();
}

const server=http.createServer(async(req,res)=>{
  try{
    if(req.method==="GET"&&(req.url==="/"||req.url==="/index.html"))return serveFile(res,path.join(ROOT,"index.html"),"text/html; charset=utf-8");
    if(req.method==="GET"&&req.url==="/health")return json(res,200,await health());
    if(req.method==="GET"&&req.url?.startsWith("/generated-images/")){
      const name=decodeURIComponent(req.url.slice("/generated-images/".length));
      if(!/^[A-Za-z0-9._-]+$/.test(name))return json(res,400,{error:"Invalid image name"});
      const file=path.join(IMAGE_DIR,name);
      if(!fs.existsSync(file))return json(res,404,{error:"Image not found"});
      return serveFile(res,file,"image/png");
    }
    if(req.method==="GET"&&req.url==="/jobs"){if(!authOk(req))return json(res,401,{error:"Unauthorized"});return json(res,200,{jobs:loadJobs()});}
    if(req.method==="POST"&&req.url==="/browser/tool"){if(!authOk(req))return json(res,401,{error:"Unauthorized"});const body=await readBody(req);return json(res,200,{result:await browserTool(String(body.tool||""),body.request||{})});}
    if(req.method==="POST"&&req.url==="/workspace/tool"){if(!authOk(req))return json(res,401,{error:"Unauthorized"});const body=await readBody(req);return json(res,200,{result:await workspaceTool(String(body.tool||""),body.request||{})});}
    if(req.method==="POST"&&req.url==="/jobs"){if(!authOk(req))return json(res,401,{error:"Unauthorized"});return json(res,202,{job:await createJob(await readBody(req))});}
    if(req.method==="POST"&&req.url==="/ai/chat"){if(!authOk(req))return json(res,401,{error:"Unauthorized"});const body=await readBody(req);const reply=await ollama(Array.isArray(body.messages)?body.messages:[{role:"user",content:String(body.prompt||"")}],body.format);return json(res,200,{reply,model:LOCAL_MODEL,provider:"ollama-local"});}
    return json(res,404,{error:"Not found"});
  }catch(e){return json(res,500,{error:String(e?.message||e)});}
});

server.listen(PORT,"127.0.0.1",()=>{
  console.log("Dexter AI Home Host running at http://127.0.0.1:"+PORT);
  console.log("Browser: /browser/tool | Workspace: /workspace/tool | Local AI: /ai/chat | Jobs: /jobs");
  console.log(AGENT_TOKEN?"Dexter cloud agent: paired":"Dexter cloud agent: not paired");
  if(AGENT_TOKEN){
    setTimeout(pollHomeJobs,1000);
    setInterval(pollHomeJobs,5000);
    setInterval(heartbeat,30000);
  }
});