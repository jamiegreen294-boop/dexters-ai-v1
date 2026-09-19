(function(){
  var deferredPrompt=null;

  if("serviceWorker" in navigator){
    window.addEventListener("load",function(){
      navigator.serviceWorker.register("./sw.js").catch(function(){});
    });
  }

  window.addEventListener("beforeinstallprompt",function(event){
    event.preventDefault();
    deferredPrompt=event;
    var button=document.getElementById("installApp");
    if(button)button.style.display="";
  });

  window.addEventListener("appinstalled",function(){
    deferredPrompt=null;
    var button=document.getElementById("installApp");
    if(button){button.textContent="Installed";button.disabled=true;}
  });

  window.addEventListener("DOMContentLoaded",function(){
    var install=document.getElementById("installApp");
    if(install){
      install.addEventListener("click",async function(){
        if(window.matchMedia && window.matchMedia("(display-mode: standalone)").matches){
          install.textContent="Installed";install.disabled=true;return;
        }
        if(deferredPrompt){
          deferredPrompt.prompt();
          try{await deferredPrompt.userChoice;}catch(e){}
          deferredPrompt=null;
        }else{
          window.alert("Open the browser menu and choose Install app or Add to Home screen.");
        }
      });
    }

    if(!document.querySelector(".mobile-nav")){
      var nav=document.createElement("nav");
      nav.className="mobile-nav";
      [["chat","Chat"],["work","Work"],["learning","Learn"],["approvals","Approve"],["system","System"]].forEach(function(item,i){
        var b=document.createElement("button");
        b.type="button";b.dataset.page=item[0];b.textContent=item[1];
        if(i===0)b.className="active";
        nav.appendChild(b);
      });
      document.body.appendChild(nav);
      nav.querySelectorAll("[data-page]").forEach(function(b){
        b.addEventListener("click",function(){
          if(typeof setPage==="function")setPage(b.dataset.page);
        });
      });
    }
  });
})();