(function(){
  function el(id){return document.getElementById(id)}
  function esc(v){return typeof escapeHtml==="function"?escapeHtml(String(v??"")):String(v??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#39;"}[c]))}
  function downloadTextFile(d){
    const blob=new Blob([d.content||""],{type:d.mimeType||"text/plain"}),url=URL.createObjectURL(blob),a=document.createElement("a");
    a.href=url;a.download=d.filename||"dexter-export.txt";document.body.appendChild(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),1000);
  }
  function addAutomationPage(){
    const nav=document.querySelector(".nav");
    if(nav&&!nav.querySelector('[data-page="automation"]')){
      const b=document.createElement("button");b.dataset.page="automation";b.innerHTML='Automation <small id="alertNav">—</small>';
      b.onclick=()=>setPage("automation");nav.insertBefore(b,nav.querySelector('[data-page="agents"]'));
    }
    if(!el("page-automation")){
      const p=document.createElement("section");p.id="page-automation";p.className="page";
      p.innerHTML='<div class="hero"><h3>Automation & Alerts</h3><p>Scheduled checks, proactive alerts, credential health and Dexter quality evaluations.</p></div>'+
      '<div class="row" style="max-width:920px;margin:0 auto 12px"><button id="runSecretHealth" class="secondary">Check credential health</button><button id="runEvals" class="secondary">Run Dexter evals</button><button id="exportAuditJson" class="secondary">Export audit JSON</button><button id="exportAuditCsv" class="secondary">Export audit CSV</button><span id="automationState" class="status">Ready</span></div>'+
      '<div class="hero"><h3>Alerts</h3></div><div id="notificationList" class="tasklist"></div>'+
      '<div class="hero"><h3>Scheduled jobs</h3></div><div id="scheduleList" class="tasklist"></div>'+
      '<div class="hero"><h3>Credential health</h3></div><div id="secretHealthList" class="tasklist"></div>'+
      '<div class="hero"><h3>Evaluation history</h3></div><div id="evalList" class="tasklist"></div>';
      document.querySelector(".content").appendChild(p);
    }
    const priorSetPage=setPage;
    setPage=function(name){
      if(name==="automation"){
        currentPage=name;
        document.querySelectorAll(".page").forEach(x=>x.classList.remove("active"));
        document.querySelectorAll("[data-page]").forEach(x=>x.classList.toggle("active",x.dataset.page===name));
        el("page-automation").classList.add("active");
        el("pageTitle").textContent="Automation";el("subtitle").textContent="Scheduled checks, alerts and quality controls.";
        loadDashboard();return;
      }
      return priorSetPage(name);
    };
  }
  function projectOptions(select){
    if(!select)return;const keep=select.value;select.innerHTML="";
    (dashboard.projects||[]).forEach(p=>{const o=document.createElement("option");o.value=p.id;o.textContent=p.name;select.appendChild(o)});
    if(keep&&Array.from(select.options).some(o=>o.value===keep))select.value=keep;
  }
  function ensureProjectAccess(){
    if(el("projectMemberBox")){projectOptions(el("memberProject"));return}
    const list=el("projectList");if(!list)return;
    const box=document.createElement("div");box.id="projectMemberBox";box.className="detailbox";
    box.innerHTML='<b>Project access</b><p class="muted">Optional workspace-level roles for future manager/staff use.</p><select id="memberProject" class="miniinput"></select><input id="memberSubject" class="miniinput" style="margin-top:8px" placeholder="Subject / staff identifier"><select id="memberRole" class="miniinput" style="margin-top:8px"><option value="viewer">viewer</option><option value="editor">editor</option><option value="manager">manager</option><option value="owner">owner</option></select><button id="saveProjectMember" class="secondary" style="margin-top:8px">Save project role</button>';
    list.before(box);projectOptions(el("memberProject"));
    el("saveProjectMember").onclick=async()=>{const projectId=el("memberProject").value,subject=el("memberSubject").value.trim(),memberRole=el("memberRole").value;if(!projectId||!subject)return;await api({action:"project_member_set",projectId,subject,memberRole});el("memberSubject").value="";await loadDashboard()};
  }
  function enhanceFiles(){
    const list=el("projectFileList");if(!list)return;
    Array.from(list.querySelectorAll(".task")).forEach((card,idx)=>{
      const f=(dashboard.projectFiles||[])[idx];if(!f)return;
      const actions=card.querySelector(".approval-actions");if(!actions||actions.querySelector(".advanced-extract"))return;
      const status=String(f.extraction_status||"");
      if(status==="needs_extraction"||status==="failed"){
        const b=document.createElement("button");b.className="secondary advanced-extract";b.textContent="Extract & index";
        b.onclick=async()=>{b.disabled=true;try{
          const d=await api({action:"project_file_extract",fileId:f.id});let tries=0;
          const timer=setInterval(async()=>{tries++;try{
            const x=await api({action:"project_file_finalize_extraction",jobId:d.job.id});
            if(x.status==="indexed"){clearInterval(timer);await loadDashboard();alert("Document extracted and indexed.");}
            else if(x.status==="failed"){clearInterval(timer);alert(x.error||"Extraction failed.");}
          }catch(e){if(tries>30){clearInterval(timer);alert(e.message)}}if(tries>60)clearInterval(timer)},2000);
        }catch(e){alert(e.message)}finally{b.disabled=false}};
        actions.appendChild(b);
      }
      const badge=card.querySelector(".status");if(badge&&status)badge.textContent=status;
    });
  }
  function enhanceArtifacts(){
    document.querySelectorAll(".artifact-page-open").forEach(open=>{
      const actions=open.parentElement;if(!actions||actions.querySelector(".rich-export"))return;
      ["pdf","zip"].forEach(fmt=>{
        const b=document.createElement("button");b.className="secondary rich-export";b.textContent="Build "+fmt.toUpperCase()+" on Home PC";
        b.onclick=async()=>{b.disabled=true;try{
          const d=await api({action:"artifact_rich_export",artifactId:open.dataset.artifact,format:fmt});let tries=0;
          const timer=setInterval(async()=>{tries++;const st=await api({action:"home_job_status",jobId:d.job.id});
            if(st.job.status==="completed"){clearInterval(timer);alert("Created on Home PC: "+((st.job.result&&st.job.result.path)||fmt));}
            else if(st.job.status==="failed"){clearInterval(timer);alert(st.job.error||"Export failed.");}
            if(tries>60)clearInterval(timer);
          },2000);
        }catch(e){alert(e.message)}finally{b.disabled=false}};
        actions.appendChild(b);
      });
    });
  }
  function renderAdvanced(){
    const notes=dashboard.notifications||[];if(el("alertNav"))el("alertNav").textContent=notes.filter(x=>x.status==="unread").length||"OK";
    const nl=el("notificationList");if(nl){nl.innerHTML="";notes.filter(x=>x.status!=="dismissed").forEach(x=>{const d=document.createElement("div");d.className="task";d.innerHTML='<div class="tasktop"><b>'+esc(x.title)+'</b><span class="status">'+esc(x.severity)+'</span></div><p>'+esc(x.message)+'</p><div class="approval-actions"><button class="secondary note-read" data-id="'+x.id+'">Mark read</button><button class="secondary note-dismiss" data-id="'+x.id+'">Dismiss</button></div>';nl.appendChild(d)});if(!nl.children.length)nl.innerHTML='<div class="task"><p>No active alerts.</p></div>';nl.querySelectorAll(".note-read,.note-dismiss").forEach(b=>b.onclick=async()=>{await api({action:"notification_review",notificationId:b.dataset.id,status:b.classList.contains("note-dismiss")?"dismissed":"read"});await loadDashboard()})}
    const sl=el("scheduleList");if(sl){sl.innerHTML="";(dashboard.scheduledJobs||[]).forEach(x=>{const d=document.createElement("div");d.className="task";d.innerHTML='<div class="tasktop"><b>'+esc(x.name)+'</b><span class="status">'+(x.enabled?"enabled":"paused")+'</span></div><p>'+esc(x.cron_expression)+' · '+esc(x.action)+'</p><div class="meta">Next '+esc(x.next_run_at||"")+'</div><div class="approval-actions"><button class="secondary schedule-toggle" data-id="'+x.id+'" data-enabled="'+(!x.enabled)+'">'+(x.enabled?"Pause":"Enable")+'</button></div>';sl.appendChild(d)});sl.querySelectorAll(".schedule-toggle").forEach(b=>b.onclick=async()=>{await api({action:"schedule_toggle",jobId:b.dataset.id,enabled:b.dataset.enabled==="true"});await loadDashboard()})}
    const sh=el("secretHealthList");if(sh){sh.innerHTML="";(dashboard.secretHealth||[]).forEach(x=>{const d=document.createElement("div");d.className="task";d.innerHTML='<div class="tasktop"><b>'+esc(x.provider+" / "+x.secret_name)+'</b><span class="status">'+esc(x.status)+'</span></div><p>'+esc(x.note||"")+'</p><div class="meta">'+esc(String(x.age_days||0))+' days</div>';sh.appendChild(d)});if(!sh.children.length)sh.innerHTML='<div class="task"><p>No secret-health records yet.</p></div>'}
    const ev=el("evalList");if(ev){ev.innerHTML="";(dashboard.evalRuns||[]).forEach(x=>{const d=document.createElement("div");d.className="task";d.innerHTML='<div class="tasktop"><b>Evaluation run</b><span class="status">'+esc(x.status)+'</span></div><p>'+esc(String(x.passed||0))+' passed · '+esc(String(x.failed||0))+' failed</p><div class="meta">'+esc(x.created_at||"")+'</div>';ev.appendChild(d)});if(!ev.children.length)ev.innerHTML='<div class="task"><p>No evaluation runs yet.</p></div>'}
  }
  function wireAutomation(){
    if(el("runSecretHealth"))el("runSecretHealth").onclick=async()=>{el("automationState").textContent="Checking credentials…";try{await api({action:"secret_health_check"});el("automationState").textContent="Complete";await loadDashboard()}catch(e){el("automationState").textContent=e.message}};
    if(el("runEvals"))el("runEvals").onclick=async()=>{el("automationState").textContent="Running evals…";try{const d=await api({action:"eval_run"});el("automationState").textContent=d.passed+" passed / "+d.failed+" failed";await loadDashboard()}catch(e){el("automationState").textContent=e.message}};
    if(el("exportAuditJson"))el("exportAuditJson").onclick=async()=>downloadTextFile(await api({action:"audit_export",format:"json"}));
    if(el("exportAuditCsv"))el("exportAuditCsv").onclick=async()=>downloadTextFile(await api({action:"audit_export",format:"csv"}));
  }
  function wireOfflineDrafts(){
    try{
      if(el("message")){if(!el("message").value)el("message").value=localStorage.dexterChatDraft||"";el("message").addEventListener("input",()=>localStorage.dexterChatDraft=el("message").value);}
      if(el("workRequest")){if(!el("workRequest").value)el("workRequest").value=localStorage.dexterWorkDraft||"";el("workRequest").addEventListener("input",()=>localStorage.dexterWorkDraft=el("workRequest").value);}
      window.addEventListener("offline",()=>{if(el("workState"))el("workState").textContent="Offline — drafts saved";});
      window.addEventListener("online",()=>loadDashboard().catch(()=>{}));
    }catch{}
  }
  addAutomationPage();wireAutomation();wireOfflineDrafts();
  const baseLoad=loadDashboard;
  loadDashboard=async function(){
    await baseLoad();
    try{localStorage.dexterDashboardCache=JSON.stringify(dashboard)}catch{}
    ensureProjectAccess();enhanceFiles();enhanceArtifacts();renderAdvanced();
  };
  try{if(!navigator.onLine&&localStorage.dexterDashboardCache){dashboard=JSON.parse(localStorage.dexterDashboardCache);ensureProjectAccess();enhanceFiles();enhanceArtifacts();renderAdvanced();}}catch{}
  if(token)loadDashboard().catch(()=>{});
})();