import http from "node:http";
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "playwright";

const __dirname=path.dirname(fileURLToPath(import.meta.url));
const ROOT=path.resolve(__dirname,"..");
const PORT=Number(process.env.PORT||8787);
const TOKEN=process.env.DEXTER_BROWSER_WORKER_TOKEN||"";
const PROFILE=process.env.DEXTER_BROWSER_PROFILE||path.resolve(__dirname,"browser-profile");
const HEADLESS=String(process.env.DEXTER_BROWSER_HEADLESS||"false").toLowerCase()==="true";

if(!TOKEN||TOKEN.length<24){
  console.error("Set DEXTER_BROWSER_WORKER_TOKEN to a long random value before starting Dexter.");
  process.exit(1);
}
fs.mkdirSync(PROFILE,{recursive:true});

let context;
async function getContext(){
  if(!context){
    context=await chromium.launchPersistentContext(PROFILE,{
      headless:HEADLESS,
      viewport:{width:1440,height:1000},
      acceptDownloads:true
    });
  }
  return context;
}
async function getPage(session="default"){
  const ctx=await getContext();
  let page=ctx.pages().find(p=>p.__dexterSession===session);
  if(!page){page=await ctx.newPage();page.__dexterSession=session;}
  return page;
}
function json(res,status,body){
  const data=JSON.stringify(body);
  res.writeHead(status,{"Content-Type":"application/json","Cache-Control":"no-store","Content-Length":Buffer.byteLength(data)});
  res.end(data);
}
function safeUrl(v){
  const u=new URL(String(v||""));
  if(!["http:","https:"].includes(u.protocol))throw new Error("Only http/https URLs are allowed.");
  return u.toString();
}
async function snapshot(page){
  return await page.evaluate(()=>{
    const selectors=["a[href]","button","input","textarea","select","[role=button]","[contenteditable=true]"];
    return Array.from(document.querySelectorAll(selectors.join(","))).slice(0,250).map((el,i)=>({
      ref:"e"+(i+1),
      tag:el.tagName.toLowerCase(),
      text:(el.innerText||el.getAttribute("aria-label")||el.getAttribute("placeholder")||"").trim().slice(0,300),
      type:el.getAttribute("type"),
      name:el.getAttribute("name"),
      href:el instanceof HTMLAnchorElement?el.href:null
    }));
  });
}
async function byRef(page,ref){
  const index=Number(String(ref||"").replace(/^e/,""))-1;
  if(!Number.isInteger(index)||index<0)throw new Error("Invalid element ref.");
  const sel=["a[href]","button","input","textarea","select","[role=button]","[contenteditable=true]"].join(",");
  const loc=page.locator(sel).nth(index);
  if(await loc.count()<1)throw new Error("Element ref not found; refresh snapshot.");
  return loc;
}
async function runBrowser(tool,request={}){
  const page=await getPage(String(request.session||"default").slice(0,80));
  if(tool==="browser.navigate"){
    await page.goto(safeUrl(request.url),{waitUntil:"domcontentloaded",timeout:45000});
    return {url:page.url(),title:await page.title(),elements:await snapshot(page)};
  }
  if(tool==="browser.snapshot")return {url:page.url(),title:await page.title(),elements:await snapshot(page)};
  if(tool==="browser.click"){const el=await byRef(page,request.ref);await el.click({timeout:15000});await page.waitForTimeout(500);return {url:page.url(),title:await page.title(),elements:await snapshot(page)};}
  if(tool==="browser.fill"){const el=await byRef(page,request.ref);await el.fill(String(request.value??""));return {ok:true,url:page.url()};}
  if(tool==="browser.select"){const el=await byRef(page,request.ref);await el.selectOption(String(request.value??""));return {ok:true,url:page.url()};}
  if(tool==="browser.press"){await page.keyboard.press(String(request.key||"Enter"));await page.waitForTimeout(300);return {url:page.url(),title:await page.title(),elements:await snapshot(page)};}
  if(tool==="browser.text")return {url:page.url(),title:await page.title(),text:(await page.locator("body").innerText()).slice(0,60000)};
  if(tool==="browser.screenshot"){const b=await page.screenshot({fullPage:Boolean(request.fullPage)});return {url:page.url(),image_base64:b.toString("base64")};}
  if(tool==="browser.close"){await page.close();return {ok:true};}
  throw new Error("Unsupported browser tool: "+tool);
}
function authOk(req){
  const supplied=String(req.headers.authorization||"").replace(/^Bearer\s+/i,"");
  const a=Buffer.from(supplied),b=Buffer.from(TOKEN);
  return a.length===b.length&&a.length>0&&crypto.timingSafeEqual(a,b);
}
function serveFile(res,file,contentType){
  const data=fs.readFileSync(file);
  res.writeHead(200,{"Content-Type":contentType,"Cache-Control":"no-store"});
  res.end(data);
}

const server=http.createServer(async(req,res)=>{
  try{
    if(req.method==="GET"&&(req.url==="/"||req.url==="/index.html")){
      return serveFile(res,path.join(ROOT,"index.html"),"text/html; charset=utf-8");
    }
    if(req.method==="GET"&&req.url==="/health"){
      return json(res,200,{status:"ready",host:"home-pc",browser:"chromium",headless:HEADLESS});
    }
    if(req.method==="POST"&&req.url==="/browser/tool"){
      if(!authOk(req))return json(res,401,{error:"Unauthorized"});
      let raw="";
      for await(const chunk of req){raw+=chunk;if(raw.length>1_000_000)throw new Error("Request too large.");}
      const body=JSON.parse(raw||"{}");
      const result=await runBrowser(String(body.tool||""),body.request||{});
      return json(res,200,{result});
    }
    return json(res,404,{error:"Not found"});
  }catch(e){
    return json(res,500,{error:String(e?.message||e)});
  }
});

server.listen(PORT,"127.0.0.1",()=>{
  console.log("Dexter AI home host running at http://127.0.0.1:"+PORT);
  console.log("Browser worker endpoint: http://127.0.0.1:"+PORT+"/browser/tool");
});
