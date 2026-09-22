const http=require("http");
const fs=require("fs");
const path=require("path");
const {spawn,spawnSync}=require("child_process");
const root=__dirname;
const bat=path.join(root,"START-DEXTER.bat");
const log=path.join(root,"guardian-node.log");

function write(m){try{fs.appendFileSync(log,new Date().toISOString()+" "+m+"\r\n");}catch{}}
function healthy(){
  return new Promise(resolve=>{
    const req=http.get("http://127.0.0.1:8787/health",res=>{
      let b=""; res.on("data",c=>b+=c); res.on("end",()=>{
        try{resolve(res.statusCode===200&&JSON.parse(b).status==="ready");}catch{resolve(false);}
      });
    });
    req.setTimeout(4000,()=>{req.destroy();resolve(false);});
    req.on("error",()=>resolve(false));
  });
}
function start(){
  try{
    spawn(process.env.ComSpec||"cmd.exe",["/d","/c",bat],{
      cwd:root,detached:true,windowsHide:true,stdio:"ignore"
    }).unref();
    write("start requested");
  }catch(e){write("start failed: "+e.message);}
}
(async()=>{
  if(await healthy())process.exit(0);
  write("health check failed");
  // Kill only the Dexter Home Host process if one is stuck.
  try{
    spawnSync("wmic",["process","where","name='node.exe'","get","ProcessId,CommandLine","/FORMAT:CSV"],{encoding:"utf8",windowsHide:true});
  }catch{}
  start();
  setTimeout(async()=>process.exit((await healthy())?0:1),10000);
})();