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
      '<div class="workbox"><div class="row"><button id="pairThisPhone" class="primary">Pair this Meizu</button><button id="refreshPhones" class="secondary">Refresh phones</button><button id="managementStatus" class="secondary">Check managed status</button><button id="applyBusinessMode" class="secondary">Apply Dexters business mode</button><button id="unknownSources" class="secondary">Allow APK installs</button><button id="openWirelessDebugging" class="secondary">Wireless debugging</button><span id="phoneState" class="status">Ready</span></div><p class="muted" id="localPhoneInfo"></p><div id="managedInfo" class="detailbox" style="margin-top:10px"><b>Managed-device setup</b><p>For full Device Owner control, install the Device Owner-capable Dexter APK, factory reset the dedicated Meizu, then enrol Dexter during Android setup. Once Android confirms Device Owner, this screen will show it as fully managed.</p></div><div class="detailbox" style="margin-top:10px"><b>Dexter Local Admin Bridge</b><p class="muted">Pairs Dexter directly to this phone\'s Wireless Debugging service. The six-digit code stays on this phone and is not sent to Dexter\'s cloud backend.</p><input id="adbPairAddress" class="miniinput" placeholder="Pairing address, e.g. 192.168.0.139:46381"><input id="adbPairCode" class="miniinput" style="margin-top:8px" inputmode="numeric" placeholder="6-digit pairing code"><input id="adbConnectAddress" class="miniinput" style="margin-top:8px" placeholder="ADB address, e.g. 192.168.0.139:45027"><div class="approval-actions"><button id="pairLocalAdb" class="secondary">Pair Dexter locally</button><button id="runLocalDiagnostics" class="secondary">Run Device Owner diagnostics</button></div><pre id="localAdbResult" style="white-space:pre-wrap;word-break:break-word"></pre></div></div>'+
      '<div id="phoneList" class="tasklist"></div>';
      document.querySelector(".content").appendChild(p);
    }
    const existingPage=document.getElementById("page-phone");
    if(existingPage&&!document.getElementById("pairLocalAdb")){
      const host=existingPage.querySelector(".workbox")||existingPage;
      const bridge=document.createElement("div");
      bridge.className="detailbox";
      bridge.style.marginTop="10px";
      bridge.innerHTML='<b>Dexter Local Admin Bridge</b><p class="muted">Pairs Dexter directly to this phone\'s Wireless Debugging service. The six-digit code stays on this phone and is not sent to Dexter\'s cloud backend.</p><button id="openWirelessDebugging" class="secondary" type="button">Wireless debugging</button><input id="adbPairAddress" class="miniinput" style="margin-top:8px" placeholder="Pairing address, e.g. 192.168.0.139:46381"><input id="adbPairCode" class="miniinput" style="margin-top:8px" inputmode="numeric" placeholder="6-digit pairing code"><input id="adbConnectAddress" class="miniinput" style="margin-top:8px" placeholder="ADB address, e.g. 192.168.0.139:45027"><div class="approval-actions"><button id="pairLocalAdb" class="secondary">Pair Dexter locally</button><button id="runLocalDiagnostics" class="secondary">Run Device Owner diagnostics</button></div><pre id="localAdbResult" style="white-space:pre-wrap;word-break:break-word"></pre>';
      host.appendChild(bridge);
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
    const phones=(await getPhones()).filter(x=>x.active!==false);
    const badge=document.getElementById("phoneNav");if(badge)badge.textContent=phones.filter(x=>x.status==="online").length||phones.length||"—";
    const list=document.getElementById("phoneList");if(!list)return;list.innerHTML="";
    phones.forEach(ph=>{
      const card=document.createElement("div");card.className="task";
      card.innerHTML='<div class="tasktop"><b>'+esc(ph.name||"Dexter Phone")+'</b><span class="status">'+esc(ph.status||"unknown")+'</span></div>'+
      '<p>'+esc((ph.manufacturer||"")+" "+(ph.model||""))+'</p><div class="meta">Android '+esc(ph.os_version||"")+' · Agent '+esc(ph.app_version||"")+' · '+(ph.capabilities&&ph.capabilities.deviceOwner?'<b>Device Owner</b> · ':'')+'Compliance: <b>'+esc(ph.compliance_status||"unknown")+'</b> · Patch '+esc(ph.security_patch||"pending")+' · last seen '+esc(ph.last_seen_at||"never")+'</div>'+
      '<div class="approval-actions"><button class="secondary phone-health">Health</button><button class="secondary phone-apps">Apps</button><button class="secondary phone-policy">Policy</button><button class="secondary phone-business">Business Mode</button><button class="secondary phone-internet">Internet Protect</button><button class="secondary phone-config">Config Backup</button><button class="secondary phone-restore">Restore Standard Setup</button><button class="secondary phone-compliance">Compliance</button><button class="secondary phone-lock">Remote Lock</button><button class="secondary phone-launch">Launch app</button><button class="secondary phone-install">Install APK</button><button class="secondary phone-uninstall">Uninstall app</button><button class="secondary phone-protectapps">Protect Business Apps</button><button class="secondary phone-release">Recovery Launcher</button><button class="secondary phone-wipe">REMOTE WIPE</button></div>';
      const q=(type,request={})=>api({action:"device_job_create",deviceId:ph.id,jobType:type,request});
      card.querySelector(".phone-health").onclick=async()=>{await q("device.health");state.textContent="Health check queued";};
      card.querySelector(".phone-apps").onclick=async()=>{await q("apps.inventory");state.textContent="App inventory queued";};
      card.querySelector(".phone-policy").onclick=async()=>{await q("device.policy.status");state.textContent="Policy status queued";};
      card.querySelector(".phone-business").onclick=async()=>{if(confirm("Apply Dexter business security mode to this managed phone?")){await q("device.policy.apply_business");state.textContent="Business mode queued";}};
      card.querySelector(".phone-internet").onclick=async()=>{if(confirm("Enable Dexter protected DNS on this phone?")){await q("device.internet.protect",{hostname:"security.cloudflare-dns.com"});state.textContent="Internet protection queued";}};
      card.querySelector(".phone-config").onclick=async()=>{await q("device.config.snapshot");state.textContent="Configuration snapshot queued";};
      card.querySelector(".phone-restore").onclick=async()=>{if(confirm("Restore the standard Dexter business-phone configuration?")){await q("device.config.restore");state.textContent="Standard configuration restore queued";}};
      card.querySelector(".phone-compliance").onclick=async()=>{const d=await api({action:"device_compliance",deviceId:ph.id});const a=d.alerts||[];state.textContent=a.length?(a.length+" compliance alert"+(a.length===1?"":"s")):"Phone compliant";alert(a.length?a.map(x=>x.severity.toUpperCase()+": "+x.message).join("\n"):"No active compliance alerts.");};
      card.querySelector(".phone-lock").onclick=async()=>{if(confirm("Lock this phone now?")){await q("device.lock");state.textContent="Remote lock queued";}};
      card.querySelector(".phone-launch").onclick=async()=>{const pkg=prompt("Android package name to launch");if(pkg){await q("app.launch",{packageName:pkg});state.textContent="Launch queued";}};
      card.querySelector(".phone-install").onclick=async()=>{const url=prompt("HTTPS APK download URL");if(url){await q("app.install",{url});state.textContent="Install queued — Android may ask for confirmation";}};
      card.querySelector(".phone-uninstall").onclick=async()=>{const pkg=prompt("Android package name to uninstall");if(pkg){await q("app.uninstall",{packageName:pkg});state.textContent="Uninstall queued — Android will ask for confirmation";}};
      card.querySelector(".phone-protectapps").onclick=async()=>{await q("device.apps.protect",{packages:["uk.co.dextersspot.dexterai","com.google.android.gm","com.whatsapp.w4b","com.android.chrome","com.google.android.apps.maps"]});state.textContent="Business-app protection queued";};
      card.querySelector(".phone-release").onclick=async()=>{if(confirm("Release Dexter Home as the forced launcher for recovery? You can re-apply Business Mode later.")){await q("device.launcher.release");state.textContent="Launcher recovery queued";}};
      card.querySelector(".phone-wipe").onclick=async()=>{
        const a=prompt("DANGER: this permanently erases the managed phone. Type WIPE DEXTER PHONE to continue.");
        if(a!=="WIPE DEXTER PHONE"){state.textContent="Wipe cancelled";return}
        const reason=prompt("Reason for remote wipe (required)");
        if(!reason){state.textContent="Wipe cancelled — reason required";return}
        if(confirm("FINAL CONFIRMATION: erase this phone now?")){await q("device.wipe",{reason});state.textContent="Remote wipe queued";}
      };
      list.appendChild(card);
    });
    if(!phones.length)list.innerHTML='<div class="task"><p>No Dexter business phones paired yet.</p></div>';
  }
  async function ensureNativeAgentEnrollment(){
    if(!nativePhone())return;
    try{
      if(window.DexterDevice.isPaired())return;
      const deviceId=window.DexterDevice.getDeviceId();
      const info=JSON.parse(window.DexterDevice.getDeviceInfo());
      const d=await api({action:"device_enroll",deviceId,name:"Dexter Meizu Business Phone",info});
      if(d&&d.deviceToken&&window.DexterDevice.saveDeviceToken(d.deviceToken)){
        const state=document.getElementById("phoneState");
        if(state)state.textContent="Paired automatically — agent started";
      }
    }catch(e){
      const state=document.getElementById("phoneState");
      if(state)state.textContent="Agent pairing needs owner sign-in";
    }
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
    const ms=document.getElementById("managementStatus");if(ms)ms.onclick=()=>{
      const st=document.getElementById("phoneState");
      if(!nativePhone()){st.textContent="Open inside Dexter Business Phone";return}
      try{const d=JSON.parse(window.DexterDevice.getManagementStatus());st.textContent=d.deviceOwner?"Fully managed Device Owner":"Not Device Owner yet";document.getElementById("managedInfo").innerHTML='<b>Managed-device status</b><pre>'+esc(JSON.stringify(d,null,2))+'</pre>';}catch(e){st.textContent=e.message}
    };
    const bm=document.getElementById("applyBusinessMode");if(bm)bm.onclick=()=>{
      const st=document.getElementById("phoneState");
      if(!nativePhone()){st.textContent="Open inside Dexter Business Phone";return}
      try{const d=JSON.parse(window.DexterDevice.applyBusinessMode());st.textContent=d.error||"Dexters business mode applied";document.getElementById("managedInfo").innerHTML='<b>Dexters business mode</b><pre>'+esc(JSON.stringify(d,null,2))+'</pre>';}catch(e){st.textContent=e.message}
    };
    const unknown=document.getElementById("unknownSources");if(unknown)unknown.onclick=()=>{if(nativePhone())window.DexterDevice.openUnknownSourcesSettings();else alert("Open inside the Android app.");};
    const box=document.querySelector("#page-phone .workbox .row");
    if(box&&!document.getElementById("batteryProtection")){
      const b=document.createElement("button");b.id="batteryProtection";b.className="secondary";b.textContent="Battery protection";
      b.onclick=()=>{if(nativePhone())window.DexterDevice.openBatteryOptimizationSettings();else alert("Open inside the Android app.");};box.insertBefore(b,document.getElementById("phoneState"));
      const a=document.createElement("button");a.id="businessApps";a.className="secondary";a.textContent="Business Apps";a.onclick=()=>{if(nativePhone())window.DexterDevice.openBusinessApps();};box.insertBefore(a,document.getElementById("phoneState"));
      const q=document.createElement("button");q.id="quickStart";q.className="secondary";q.textContent="Staff Quick Start";q.onclick=()=>{if(nativePhone())window.DexterDevice.openQuickStart();};box.insertBefore(q,document.getElementById("phoneState"));
    }
    const wd=document.getElementById("openWirelessDebugging");if(wd)wd.onclick=()=>{if(nativePhone())window.DexterDevice.openWirelessDebuggingSettings();else alert("Open inside the Android app.");};
    const splitAddress=(value)=>{const v=(value||"").trim();const i=v.lastIndexOf(":");if(i<1)throw new Error("Enter address as IP:port");const host=v.slice(0,i);const port=parseInt(v.slice(i+1),10);if(!host||!port)throw new Error("Invalid IP:port");return {host,port};};
    const pairAdb=document.getElementById("pairLocalAdb");if(pairAdb)pairAdb.onclick=()=>{
      const out=document.getElementById("localAdbResult");if(!nativePhone()){out.textContent="Open inside Dexter Business Phone.";return}
      try{const a=splitAddress(document.getElementById("adbPairAddress").value),code=document.getElementById("adbPairCode").value.trim();if(!/^\d{6}$/.test(code))throw new Error("Enter the current 6-digit pairing code");out.textContent="Pairing Dexter locally…";const d=JSON.parse(window.DexterDevice.pairLocalAdb(a.host,a.port,code));out.textContent=JSON.stringify(d,null,2);}catch(e){out.textContent=e.message}
    };
    const diag=document.getElementById("runLocalDiagnostics");if(diag)diag.onclick=()=>{
      const out=document.getElementById("localAdbResult");if(!nativePhone()){out.textContent="Open inside Dexter Business Phone.";return}
      try{
        const raw=(document.getElementById("adbConnectAddress").value||"").trim();
        let host="",port=0;
        if(raw){const a=splitAddress(raw);host=a.host;port=a.port;}
        out.textContent="Running local Android policy diagnostics…";
        const d=JSON.parse(window.DexterDevice.runLocalOwnerDiagnostics(host,port));
        out.textContent=JSON.stringify(d,null,2);
      }catch(e){out.textContent=e.message}
    };
  }
  ensurePhonePage();wire();setTimeout(ensureNativeAgentEnrollment,800);
})();