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
const MAX_AGENT_STEPS=Math.max(1,Math.min(30,Number(process.env.DEXTER_MAX_AGENT_STEPS||15)));

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
async function ollama(messages,format){
  const r=await fetch(OLLAMA_URL+"/api/chat",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({
    model:LOCAL_MODEL,messages,stream:false,format:format||undefined,options:{temperature:0.1}
  })});
  const data=await r.json().catch(()=>({}));
  if(!r.ok)throw new Error(data?.error||"Local Ollama request failed");
  return String(data?.message?.content||"").trim();
}
function parseJson(s){
  try{return JSON.parse(s)}catch{}
  const m=String(s).match(/\{[\s\S]*\}/);if(m){try{return JSON.parse(m[0])}catch{}}
  throw new Error("Local model returned invalid JSON.");
}
function isConsequence(action){
  const text=JSON.stringify(action).toLowerCase();
  return /purchase|pay|refund|delete|publish|deploy|merge|send|submit|create account|change password|confirm order|place order/.test(text);
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
  if(request.url)await page.goto(safeUrl(request.url),{waitUntil:"domcontentloaded",timeout:45000});
  const history=[];
  for(let step=1;step<=MAX_AGENT_STEPS;step++){
    const state=await snapshot(page);
    const prompt=[
      "You are Dexter Browser Agent running on the owner's local PC.",
      "Goal: "+goal,
      "Choose exactly one next action from: click, fill, select, press, navigate, done, blocked.",
      "Never invent element refs. Use only refs shown in PAGE STATE.",
      "Do not perform purchases, payments, refunds, deletion, publishing, deployment, sending messages, submitting irreversible forms, or account/security changes. If one is necessary return blocked.",
      "Return JSON only: {action,ref,value,url,key,reason,result}.",
      "PAGE STATE:",JSON.stringify(state).slice(0,30000),
      "RECENT ACTIONS:",JSON.stringify(history.slice(-8))
    ].join("\n");
    const decision=parseJson(await ollama([{role:"user",content:prompt}],"json"));
    if(isConsequence(decision))return {status:"blocked",reason:"Consequence requires separate owner approval.",state,history};
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
    const child=spawn(command,args,{cwd,windowsHide:true,shell:false,env:{...process.env,CI:"1"}});
    let stdout="",stderr="",killed=false;
    const timer=setTimeout(()=>{killed=true;child.kill();},timeout);
    child.stdout.on("data",d=>stdout+=d.toString());
    child.stderr.on("data",d=>stderr+=d.toString());
    child.on("error",reject);
    child.on("close",code=>{clearTimeout(timer);resolve({code,killed,stdout:stdout.slice(-100000),stderr:stderr.slice(-100000)});});
  });
}
async function workspaceTool(tool,request={}){
  if(tool==="workspace.list"){
    const dir=safeWorkspacePath(request.path||"");
    return {path:path.relative(WORKSPACE,dir),entries:fs.readdirSync(dir,{withFileTypes:true}).map(x=>({name:x.name,type:x.isDirectory()?"directory":"file"})).slice(0,500)};
  }
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
  if(tool==="git.status")return await runProcess("git",["status","--short","--branch"],safeWorkspacePath(request.path||""),30000);
  if(tool==="git.diff")return await runProcess("git",["diff","--",request.file||"."],safeWorkspacePath(request.path||""),30000);
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
    else throw new Error("Unsupported job type.");
    job.status="completed";
  }catch(e){job.status="failed";job.error=String(e?.message||e);}finally{job.updated_at=new Date().toISOString();saveJob(job);}});
  return job;
}
async function health(){
  let ollamaReady=false,models=[];
  try{const r=await fetch(OLLAMA_URL+"/api/tags");const d=await r.json();ollamaReady=r.ok;models=(d.models||[]).map(x=>x.name).slice(0,20);}catch{}
  return {status:"ready",host:"home-pc",browser:"chromium",headless:HEADLESS,workspace:WORKSPACE,ollama:{ready:ollamaReady,url:OLLAMA_URL,model:LOCAL_MODEL,models},jobs:loadJobs().length};
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
  console.log("Dexter AI Home Host v2 running at http://127.0.0.1:"+PORT);
  console.log("Browser: /browser/tool | Workspace: /workspace/tool | Local AI: /ai/chat | Jobs: /jobs");
});