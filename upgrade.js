(function(){
  function el(id){return document.getElementById(id)}
  function html(s){return escapeHtml(s)}
  function addNav(page,label,badgeId){
    var nav=document.querySelector(".nav"); if(!nav||nav.querySelector('[data-page="'+page+'"]'))return;
    var b=document.createElement("button"); b.dataset.page=page; b.innerHTML=label+(badgeId?' <small id="'+badgeId+'">—</small>':'');
    b.onclick=function(){setPage(page)}; nav.insertBefore(b,nav.querySelector('[data-page="agents"]'));
  }
  function addPage(id,markup){
    if(el("page-"+id))return;
    var section=document.createElement("section"); section.id="page-"+id; section.className="page"; section.innerHTML=markup;
    document.querySelector(".content").appendChild(section);
  }
  addNav("projects","Projects","projectNav");
  addNav("files","Files","fileNav");
  addNav("operations","Operations","opsNav");
  addPage("projects",'<div class="hero"><h3>Projects & Workspaces</h3><p>Permanent Dexters workspaces keep tasks, files, artifacts and decisions together.</p></div><div class="workbox"><div class="row"><input id="newProjectName" class="miniinput" style="flex:1" placeholder="New project name"><button id="createProject" class="primary">Create project</button></div><textarea id="newProjectDescription" style="min-height:80px;margin-top:8px" placeholder="What this workspace is for"></textarea></div><div id="projectList" class="tasklist"></div>');
  addPage("files",'<div class="hero"><h3>Project Files</h3><p>Upload menus, documents, screenshots, code or other project material. Files stay private in the Dexter AI test project.</p></div><div class="workbox"><select id="fileProject" class="miniinput"></select><input id="projectFile" type="file" class="miniinput" style="margin-top:8px"><div class="row" style="margin-top:10px"><button id="uploadProjectFile" class="primary">Upload file</button><span id="fileState" class="status">10 MB max</span></div></div><div id="projectFileList" class="tasklist"></div>');
  addPage("operations",'<div class="hero"><h3>Dexters Operations</h3><p>One health view for Dexter Home PC, connectors, jobs, knowledge freshness and work queues.</p></div><div class="row" style="max-width:920px;margin:0 auto 12px"><button id="runOpsCheck" class="primary">Run system check</button><span id="opsState" class="status">Ready</span></div><div id="opsHealth" class="tasklist"></div><div class="hero"><h3>Post-mortems</h3><p>Dexter records what broke, why, how it was fixed and how to prevent it happening again.</p></div><div id="postmortemList" class="tasklist"></div>');

  var originalSetPage=setPage;
  setPage=function(name){
    if(["projects","files","operations"].includes(name)){
      currentPage=name;
      document.querySelectorAll(".page").forEach(function(x){x.classList.remove("active")});
      document.querySelectorAll("[data-page]").forEach(function(x){x.classList.toggle("active",x.dataset.page===name)});
      el("page-"+name).classList.add("active");
      var t={projects:["Projects","Permanent Dexters workspaces."],files:["Files","Private files attached to Dexter projects."],operations:["Operations","Dexters system health and recovery."]}[name];
      el("pageTitle").textContent=t[0]; el("subtitle").textContent=t[1]; loadDashboard(); return;
    }
    return originalSetPage(name);
  };

  function projectOptions(select,allowBlank){
    if(!select)return;
    var keep=select.value; select.innerHTML=allowBlank?'<option value="">No project / general</option>':'';
    (dashboard.projects||[]).forEach(function(p){var o=document.createElement("option");o.value=p.id;o.textContent=p.name;select.appendChild(o)});
    if(keep&&Array.from(select.options).some(function(o){return o.value===keep}))select.value=keep;
  }
  function ensureWorkProject(){
    if(el("workProject"))return;
    var wrap=el("workRequest")&&el("workRequest").parentElement; if(!wrap)return;
    var s=document.createElement("select");s.id="workProject";s.className="miniinput";s.style.marginBottom="8px";wrap.insertBefore(s,el("workRequest"));
  }
  function renderProjects(){
    ensureWorkProject(); projectOptions(el("workProject"),true); projectOptions(el("fileProject"),false);
    var rows=dashboard.projects||[]; if(el("projectNav"))el("projectNav").textContent=rows.length;
    var list=el("projectList"); if(!list)return; list.innerHTML="";
    rows.forEach(function(p){
      var tasks=(dashboard.tasks||[]).filter(function(t){return t.project_id===p.id}).length;
      var files=(dashboard.projectFiles||[]).filter(function(f){return f.project_id===p.id}).length;
      var arts=(dashboard.artifacts||[]).filter(function(a){return a.project_id===p.id}).length;
      var d=document.createElement("div");d.className="task";
      d.innerHTML='<div class="tasktop"><b>'+html(p.name)+'</b><span class="status">'+html(p.status)+'</span></div><p class="muted">'+html(p.description||"")+'</p><div class="meta">'+tasks+' tasks · '+files+' files · '+arts+' artifacts</div>';
      list.appendChild(d);
    });
  }
  function renderFiles(){
    var rows=dashboard.projectFiles||[]; if(el("fileNav"))el("fileNav").textContent=rows.length;
    var list=el("projectFileList"); if(!list)return; list.innerHTML="";
    if(!rows.length){list.innerHTML='<div class="task"><p>No project files yet.</p></div>';return}
    rows.forEach(function(f){
      var p=(dashboard.projects||[]).find(function(x){return x.id===f.project_id});
      var d=document.createElement("div");d.className="task";
      d.innerHTML='<div class="tasktop"><b>'+html(f.name)+'</b><span class="status">'+html(f.mime_type||f.file_type||"file")+'</span></div><div class="meta">'+html(p?p.name:"Project")+' · '+Math.ceil(Number(f.size_bytes||0)/1024)+' KB</div><div class="approval-actions"><button class="secondary open-project-file" data-id="'+html(f.id)+'">Open / download</button></div>';
      list.appendChild(d);
    });
    document.querySelectorAll(".open-project-file").forEach(function(b){b.onclick=async function(){try{var x=await api({action:"project_file_get",fileId:b.dataset.id});if(x.signedUrl)window.open(x.signedUrl,"_blank");else alert(x.file&&x.file.content_text||"File loaded.");}catch(e){alert(e.message)}}});
  }
  function renderMemoryProposals(){
    var old=el("memoryProposalBox"); if(old)old.remove();
    var rows=(dashboard.memoryProposals||[]).filter(function(x){return x.status==="pending"});
    var box=document.createElement("div");box.id="memoryProposalBox";box.className="detailbox";
    box.innerHTML='<b>Suggested memories ('+rows.length+')</b><p class="muted">Dexter can propose durable facts; nothing becomes memory until you approve it.</p>';
    rows.forEach(function(m){
      var d=document.createElement("div");d.className="memoryrow";d.style.marginTop="8px";
      d.innerHTML='<span class="kcat">'+html(m.category)+'</span><p>'+html(m.content)+'</p><div class="meta">'+html(m.reason||"")+'</div><div class="approval-actions"><button class="good mem-review" data-id="'+m.id+'" data-decision="approved">Approve</button><button class="danger mem-review" data-id="'+m.id+'" data-decision="rejected">Reject</button></div>';
      box.appendChild(d);
    });
    el("memoryList").before(box);
    box.querySelectorAll(".mem-review").forEach(function(b){b.onclick=async function(){await api({action:"memory_proposal_review",proposalId:b.dataset.id,decision:b.dataset.decision});await loadDashboard()}});
  }
  function renderOps(){
    var health=dashboard.operationsHealth||[],latest={};
    health.forEach(function(x){if(!latest[x.system_key])latest[x.system_key]=x});
    var rows=Object.values(latest); if(el("opsNav"))el("opsNav").textContent=rows.filter(function(x){return x.status!=="healthy"}).length||"OK";
    var list=el("opsHealth"); if(list){list.innerHTML="";rows.forEach(function(x){var d=document.createElement("div");d.className="task";d.innerHTML='<div class="tasktop"><b>'+html(x.system_name)+'</b><span class="status">'+html(x.status)+'</span></div><p>'+html(x.summary||"")+'</p><div class="meta">'+html(x.checked_at||"")+'</div>';list.appendChild(d)});if(!rows.length)list.innerHTML='<div class="task"><p>Run the first operations check.</p></div>'}
    var pm=el("postmortemList");if(pm){pm.innerHTML="";(dashboard.postmortems||[]).forEach(function(x){var d=document.createElement("div");d.className="task";d.innerHTML='<div class="tasktop"><b>'+html(x.title)+'</b><span class="status">lesson</span></div><p>'+html(x.incident)+'</p><div class="result"><b>Root cause:</b> '+html(x.root_cause||"Not recorded")+'\n<b>Resolution:</b> '+html(x.resolution||"Not recorded")+'\n<b>Prevention:</b> '+html(x.prevention||"Not recorded")+'</div>';pm.appendChild(d)});if(!(dashboard.postmortems||[]).length)pm.innerHTML='<div class="task"><p>No post-mortems recorded yet.</p></div>'}
  }
  function enhanceArtifacts(){
    document.querySelectorAll(".artifact-page-open").forEach(function(b){
      if(b.parentElement.querySelector(".artifact-edit"))return;
      var edit=document.createElement("button");edit.className="secondary artifact-edit";edit.dataset.artifact=b.dataset.artifact;edit.textContent="Edit";
      edit.onclick=async function(){
        try{var x=await api({action:"artifact_get",artifactId:edit.dataset.artifact});var next=prompt("Edit artifact content",x.artifact.content||"");if(next===null)return;await api({action:"artifact_update",artifactId:edit.dataset.artifact,content:next,name:x.artifact.name});await loadDashboard();alert("Artifact saved as a new version.");}catch(e){alert(e.message)}
      };
      b.parentElement.appendChild(edit);
      var versions=(dashboard.artifactVersions||[]).filter(function(v){return v.artifact_id===b.dataset.artifact});
      if(versions.length){var badge=document.createElement("span");badge.className="status";badge.textContent=versions.length+" versions";b.parentElement.appendChild(badge)}
    });
  }
  async function renderUpgrade(){renderProjects();renderFiles();renderMemoryProposals();renderOps();enhanceArtifacts()}
  var baseLoad=loadDashboard;
  loadDashboard=async function(){await baseLoad();await renderUpgrade()};

  el("createProject").onclick=async function(){var name=el("newProjectName").value.trim();if(!name)return;await api({action:"project_create",name:name,description:el("newProjectDescription").value.trim()});el("newProjectName").value="";el("newProjectDescription").value="";await loadDashboard()};
  el("uploadProjectFile").onclick=async function(){
    var file=el("projectFile").files[0],projectId=el("fileProject").value;if(!file||!projectId)return;
    if(file.size>10485760){alert("File is over the 10 MB test limit.");return}
    el("fileState").textContent="Uploading…";
    var data=await file.arrayBuffer();var bytes=new Uint8Array(data),chunk=0x8000,binary="";
    for(var i=0;i<bytes.length;i+=chunk)binary+=String.fromCharCode.apply(null,bytes.subarray(i,Math.min(i+chunk,bytes.length)));
    try{await api({action:"project_file_upload",projectId:projectId,name:file.name,mimeType:file.type||"application/octet-stream",base64:btoa(binary)});el("projectFile").value="";el("fileState").textContent="Uploaded";await loadDashboard()}catch(e){el("fileState").textContent=e.message}
  };
  el("runOpsCheck").onclick=async function(){el("opsState").textContent="Checking…";try{await api({action:"operations_run_check"});el("opsState").textContent="Complete";await loadDashboard()}catch(e){el("opsState").textContent=e.message}};

  if(el("runWork"))el("runWork").onclick=async function(){
    var req=el("workRequest").value.trim();if(!req)return;el("runWork").disabled=true;el("workState").textContent="Working…";
    try{var d=await api({action:"work",message:req,agent:el("agentSelect").value,projectId:el("workProject")?el("workProject").value:""});el("workState").textContent="Completed";el("workRequest").value="";await loadDashboard();if(d.reply)setPage("work")}catch(err){el("workState").textContent=err.message}finally{el("runWork").disabled=false}
  };

  if(token)loadDashboard().catch(function(){});
})();