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
const LOCAL_MODEL=process.env.DEXTER_LOCAL_MODEL||"qwen3:4b";
const CODE_MODEL=process.env.DEXTER_CODE_MODEL||LOCAL_MODEL;
const MAX_AGENT_STEPS=Math.max(1,Math.min(30,Number(process.env.DEXTER_MAX_AGENT_STEPS||15)));
const LIVE_ACTIONS=String(process.env.DEXTER_LIVE_ACTIONS||"false").toLowerCase()==="true";
const AGENT_ENDPOINT=(process.env.DEXTER_AGENT_ENDPOINT||"https://eikruaxxzzxmfjvsmwwo.supabase.co/functions/v1/dexter-home-agent").replace(/\/$/,"");
const AGENT_TOKEN=process.env.DEXTER_AGENT_TOKEN||"";
let agentBusy=false;

if(!TOKEN||TOKEN.length<24){
  console.error("Set DEXTER_BROWSER_WORKER_TOKEN to a long random value before starting Dexter.");
  process.exit(1);
}
for(const dir of [PROFILE,WORKSPACE,JOB_DIR])fs.mkdirSync(dir,{recursive:true});

let context;
const pageSessions=new Map();

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
async function getContext(){
  if(!context){
    context=await chromium.launchPersistentContext(PROFILE,{
      headless:HEADLESS,viewport:{width:1440,height:1000},acceptDownloads:true
    });
  }
  return context;
}
async function getPage(session="default"){
  const ctx=await getContext();
  if(pageSessions.has(session)){
    const p=pageSessions.get(session);
    if(!p.isClosed())return p;
  }
  const p=await ctx.newPage();pageSessions.set(session,p);return p;
}
async function snapshot(page){
  const elements=await page.evaluate(()=>{
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
  return {url:page.url(),title:await page.title(),text,elements};
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
  const timer=setTimeout(()=>controller.abort(),90000);
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
async function browserTool(tool,request={}){
  const session=String(request.session||"default").slice(0,80),page=await getPage(session);
  if(tool==="browser.navigate"){
    await page.goto(safeUrl(request.url),{waitUntil:"domcontentloaded",timeout:45000});
    return await snapshot(page);
  }
  if(tool==="browser.snapshot")return await snapshot(page);
  if(tool==="browser.click"){const el=await byRef(page,request.ref);await el.click({timeout:15000});await page.waitForTimeout(700);return await snapshot(page);}
  if(tool==="browser.fill"){const el=await byRef(page,request.ref);await el.fill(String(request.value??""));return {ok:true,url:page.url()};}
  if(tool==="browser.select"){const el=await byRef(page,request.ref);await el.selectOption(String(request.value??""));return {ok:true,url:page.url()};}
  if(tool==="browser.press"){await page.keyboard.press(String(request.key||"Enter"));await page.waitForTimeout(500);return await snapshot(page);}
  if(tool==="browser.text")return {url:page.url(),title:await page.title(),text:(await page.locator("body").innerText()).slice(0,60000)};
  if(tool==="browser.screenshot"){const b=await page.screenshot({fullPage:Boolean(request.fullPage)});return {url:page.url(),image_base64:b.toString("base64")};}
  if(tool==="browser.close"){await page.close();pageSessions.delete(session);return {ok:true};}
  if(tool==="browser.navigate_and_act")return await autonomousBrowser(page,request);
  throw new Error("Unsupported browser tool: "+tool);
}
async function autonomousBrowser(page,request={}){
  const goal=String(request.instruction||request.goal||"").trim();
  if(!goal)throw new Error("Browser instruction is required.");
  const allowConsequential=LIVE_ACTIONS&&request.approval_granted===true;
  if(request.url)await page.goto(safeUrl(request.url),{waitUntil:"domcontentloaded",timeout:45000});
  const history=[];
  const stepLimit=Math.max(1,Math.min(MAX_AGENT_STEPS,Number(request.max_steps||MAX_AGENT_STEPS)));
  const deadline=Date.now()+Math.max(15000,Math.min(180000,Number(request.timeout_ms||120000)));
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
    const decision=parseJson(await ollama([{role:"user",content:prompt}],"json",LOCAL_MODEL));
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
    const executable=process.platform==="win32"&&["npm","npx"].includes(command)?command+".cmd":command;
    const child=spawn(executable,args,{cwd,windowsHide:true,shell:false,env:{...process.env,CI:"1"}});
    let stdout="",stderr="",killed=false;
    const timer=setTimeout(()=>{killed=true;child.kill();},timeout);
    child.stdout.on("data",d=>stdout+=d.toString());
    child.stderr.on("data",d=>stderr+=d.toString());
    child.on("error",reject);
    child.on("close",code=>{clearTimeout(timer);resolve({code,killed,stdout:stdout.slice(-100000),stderr:stderr.slice(-100000)});});
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
      return {
        status:"completed",
        result:String(plan.result||"Fast coding pass completed"),
        repo_path:repoPath,
        fast_path:true,
        files_written:written,
        verification,
        checks,
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
      return {status:"completed",result:String(decision.result||decision.reason||"Completed"),repo_path:repoPath,history,git_status:finalStatus,git_diff:diff};
    }else if(decision.action==="blocked")return {status:"blocked",reason:String(decision.reason||"Blocked"),repo_path:repoPath,history};
    else throw new Error("Unsupported coding-agent action: "+decision.action);
    history.push({step,decision:{...decision,content:decision.content?"[file content written]":undefined},result:JSON.stringify(result).slice(0,12000)});
  }
  return {status:"step_limit",repo_path:repoPath,history,git_status:await workspaceTool("git.status",{path:repoPath}).catch(()=>null),git_diff:await workspaceTool("git.diff",{path:repoPath,file:"."}).catch(()=>null)};
}
async function workspaceTool(tool,request={}){
  if(tool==="workspace.list"){
    const dir=safeWorkspacePath(request.path||"");
    return {path:path.relative(WORKSPACE,dir),entries:fs.readdirSync(dir,{withFileTypes:true}).map(x=>({name:x.name,type:x.isDirectory()?"directory":"file"})).slice(0,500)};
  }
  if(tool==="workspace.tree")return {path:String(request.path||""),entries:await listTree(String(request.path||""),Number(request.depth||3),Number(request.max_entries||500))};
  if(tool==="web.research")return await internetResearch(request);
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
    const kind=String(request.kind||"").toLowerCase();
    if(kind==="npm-test")return await runProcess("npm",["test","--","--runInBand"],cwd,180000);
    if(kind==="npm-build")return await runProcess("npm",["run","build"],cwd,180000);
    if(kind==="node-check")return await runProcess("node",["--check",String(request.file||"")],cwd,30000);
    throw new Error("Unsupported code check.");
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
  if(job.job_type==="self_update")return await selfUpdateWorker();
  if(job.job_type==="self_restart")return scheduleSelfRestart(3000);
  if(job.job_type==="local_ai"){
    const messages=Array.isArray(request.messages)?request.messages:[{role:"user",content:String(request.prompt||"")}];
    const reply=await ollama(messages,request.format,request.model||LOCAL_MODEL);
    return {reply,model:request.model||LOCAL_MODEL,provider:"ollama-local"};
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
  return {status:"ready",host:"home-pc",browser:"chromium",internet:true,coding_agent:true,live_actions:LIVE_ACTIONS,cloud_agent_paired:Boolean(AGENT_TOKEN),agent_endpoint:AGENT_ENDPOINT,headless:HEADLESS,workspace:WORKSPACE,ollama:{ready:ollamaReady,url:OLLAMA_URL,model:LOCAL_MODEL,code_model:CODE_MODEL,models},jobs:loadJobs().length};
}
function serveFile(res,file,contentType){const data=fs.readFileSync(file);res.writeHead(200,{"Content-Type":contentType,"Cache-Control":"no-store"});res.end(data);}

const server=http.createServer(async(req,res)=>{
  try{
    if(req.method==="GET"&&(req.url==="/"||req.url==="/index.html"))return serveFile(res,path.join(ROOT,"index.html"),"text/html; charset=utf-8");
    if(req.method==="GET"&&req.url==="/health")return json(res,200,await health());
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