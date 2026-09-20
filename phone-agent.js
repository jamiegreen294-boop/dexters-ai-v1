(function(){
  const nativePhone=()=>typeof window.DexterDevice!=="undefined";
  const esc=v=>typeof escapeHtml==="function"?escapeHtml(String(v??"")):String(v??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
  function ensurePhonePage(){
    const nav=document.querySelector(".nav");
    if(nav&&!nav.querySelector('[data-page="phone"]')){
      const b=document.createElement("button"); b.dataset.page="phone"; b.innerHTML='Business Phone <small id="phoneNav">—</small>';
      b.onclick=()=>setPage("phone"); nav.insertBefore(b,nav.querySelector('[data-page="agents"]'));
    }
    if(!document.getElementById("page-phone")){
      const p=document.createElement("section");p.id="page-phone";p.className="page";
      p.innerHTML='<div class="hero"><h3>Dexter Business Phone</h3><p>Pair and manage the dedicated Meizu from Dexter AI TEST.</p></div>'+
      '<div class="workbox"><div class="row"><button id="pairThisPhone" class="primary">Pair this Meizu</button><button id="refreshPhones" class="secondary">Refresh phones</button><button id="unknownSources" class="secondary">Allow APK installs</button><span id="phoneState" class="status">Ready</span></div><p class="muted" id="localPhoneInfo"></p></div>'+
      '<div id="phoneList" class="tasklist"></div>';
      document.querySelector(".content").appendChild(p);
    }
    const prior=setPage;
    setPage=function(name){
      if(name==="phone"){
        currentPage=name;
        document.querySelectorAll(".page").forEach(x=>x.classList.remove("active"));
        document.querySelectorAll("[data-page]").forEach(x=>x.classList.toggle("active",x.dataset.page===name));
        document.getElementById("page-phone").classList.add("active");
        document.getElementById("pageTitle").textContent="Business Phone";
        document.getElementById("subtitle").textContent="Dexter-managed Meizu test agent.";
        renderPhone();return;
      }
      return prior(name);
    };
  }
  async function getPhones(){ try{return (await api({action:"device_list"})).devices||[]}catch(e){return []} }
  async function renderPhone(){
    const info=document.getElementById("localPhoneInfo"),state=document.getElementById("phoneState");
    if(info){
      if(nativePhone()){
        let d={};try{d=JSON.parse(window.DexterDevice.getDeviceInfo())}catch{}
        info.textContent="This device: "+(d.manufacturer||"Android")+" "+(d.model||"")+" · "+(window.DexterDevice.isPaired()?"paired":"not paired");
      }else info.textContent="Open this page inside the Dexter Business Phone Android app to pair the Meizu.";
    }
    const phones=await getPhones();
    const badge=document.getElementById("phoneNav");if(badge)badge.textContent=phones.filter(x=>x.status==="online").length||phones.length||"—";
    const list=document.getElementById("phoneList");if(!list)return;list.innerHTML="";
    phones.forEach(ph=>{
      const card=document.createElement("div");card.className="task";
      card.innerHTML='<div class="tasktop"><b>'+esc(ph.name||"Dexter Phone")+'</b><span class="status">'+esc(ph.status||"unknown")+'</span></div>'+
      '<p>'+esc((ph.manufacturer||"")+" "+(ph.model||""))+'</p><div class="meta">Android '+esc(ph.os_version||"")+' · Agent '+esc(ph.app_version||"")+' · last seen '+esc(ph.last_seen_at||"never")+'</div>'+
      '<div class="approval-actions"><button class="secondary phone-health">Health</button><button class="secondary phone-apps">Apps</button><button class="secondary phone-launch">Launch app</button><button class="secondary phone-install">Install APK</button><button class="secondary phone-uninstall">Uninstall app</button></div>';
      const q=(type,request={})=>api({action:"device_job_create",deviceId:ph.id,jobType:type,request});
      card.querySelector(".phone-health").onclick=async()=>{await q("device.health");state.textContent="Health check queued";};
      card.querySelector(".phone-apps").onclick=async()=>{await q("apps.inventory");state.textContent="App inventory queued";};
      card.querySelector(".phone-launch").onclick=async()=>{const pkg=prompt("Android package name to launch");if(pkg){await q("app.launch",{packageName:pkg});state.textContent="Launch queued";}};
      card.querySelector(".phone-install").onclick=async()=>{const url=prompt("HTTPS APK download URL");if(url){await q("app.install",{url});state.textContent="Install queued — Android may ask for confirmation";}};
      card.querySelector(".phone-uninstall").onclick=async()=>{const pkg=prompt("Android package name to uninstall");if(pkg){await q("app.uninstall",{packageName:pkg});state.textContent="Uninstall queued — Android will ask for confirmation";}};
      list.appendChild(card);
    });
    if(!phones.length)list.innerHTML='<div class="task"><p>No Dexter business phones paired yet.</p></div>';
  }
  function wire(){
    const pair=document.getElementById("pairThisPhone");
    if(pair)pair.onclick=async()=>{
      if(!nativePhone()){alert("Open Dexter AI inside the Dexter Business Phone app first.");return}
      const state=document.getElementById("phoneState");state.textContent="Pairing…";
      try{
        const deviceId=window.DexterDevice.getDeviceId(),info=JSON.parse(window.DexterDevice.getDeviceInfo());
        const d=await api({action:"device_enroll",deviceId,name:"Dexter Meizu Business Phone",info});
        if(!window.DexterDevice.saveDeviceToken(d.deviceToken))throw new Error("Phone rejected the device token.");
        state.textContent="Paired and agent started";await renderPhone();
      }catch(e){state.textContent=e.message}
    };
    const refresh=document.getElementById("refreshPhones");if(refresh)refresh.onclick=renderPhone;
    const unknown=document.getElementById("unknownSources");if(unknown)unknown.onclick=()=>{if(nativePhone())window.DexterDevice.openUnknownSourcesSettings();else alert("Open inside the Android app.");};
  }
  ensurePhonePage();wire();
})();