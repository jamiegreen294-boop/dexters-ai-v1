import test from 'node:test';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import {readFileSync} from 'node:fs';
const html=readFileSync(new URL('../index.html',import.meta.url),'utf8');
const script=html.match(/<script>([\s\S]*?)<\/script>/)[1];

function harness(fetcher){
 const nodes=new Map();
 function element(){return {value:'',textContent:'',innerHTML:'',disabled:false,children:[],classList:{add(){},remove(){},toggle(){}},addEventListener(){},focus(){},append(e){this.children.push(e)},get lastElementChild(){return this.children.at(-1)},setAttribute(){}}}
 const document={hidden:false,getElementById(id){if(!nodes.has(id))nodes.set(id,element());return nodes.get(id)},querySelectorAll(){return []},querySelector(){return null},addEventListener(){},createElement:element};
 const context=vm.createContext({document,window:{addEventListener(){},innerWidth:390},navigator:{},localStorage:{getItem(){return null}},sessionStorage:{getItem(){return null}},crypto:{randomUUID(){return '43000000-0000-4000-8000-000000000001'}},AbortController,setTimeout,clearTimeout,setInterval(){},fetch:fetcher,console,URL,Date});
 vm.runInContext(script,context);
 return {context,nodes,document,run:s=>vm.runInContext(s,context)};
}

test('Enter and Send cannot create duplicate tasks while a request is in flight',async()=>{
 let chatCalls=0;
 const app=harness(async(url,init)=>{
  const action=JSON.parse(init.body).action;
  if(action==='chat'){chatCalls++;await new Promise(resolve=>setTimeout(resolve,10));}
  const data=action==='chat'?{reply:'Task is awaiting verification.',task:{id:'43000000-0000-4000-8000-000000000001',status:'needs_verification'}}:action==='health'?{homeAgentOnline:false}:{};
  return {ok:true,status:200,json:async()=>data};
 });
 app.run("tok='test';sid=uid();$('msg').value='Print labels'");
 await Promise.all([app.run('send()'),app.run('send()')]);
 assert.equal(chatCalls,1);
 assert.equal(app.nodes.get('send').disabled,false);
 assert.equal(app.nodes.get('send').textContent,'Send');
 const reply=app.nodes.get('chat').children.at(-1);
 assert.ok(reply.children.some(x=>x.textContent==='Open tracked task'));
});

test('a failed mutation is not retried and its text is recoverable',async()=>{
 let calls=0;
 const app=harness(async()=>{calls++;throw Error('Connection lost; task outcome unverified')});
 app.run("tok='test';sid=uid();$('msg').value='Fix the test app'");
 await app.run('send()');
 assert.equal(calls,1);
 assert.equal(app.nodes.get('msg').value,'Fix the test app');
 assert.equal(app.nodes.get('send').disabled,false);
});

test('empty operations data displays unverified and Gmail setup remains reachable',()=>{
 const app=harness(async()=>{});
 app.run("renderDashboard({connectors:[{connector_key:'gmail',name:'Gmail',runtime_ready:false,evidence:{status:'not_configured'}}]})");
 assert.equal(app.nodes.get('operatorStatePill').textContent,'UNVERIFIED');
 assert.match(app.nodes.get('connectors').innerHTML,/Connect cloud Gmail/);
 app.run('showHealth({homeAgentOnline:false})');
 assert.equal(app.nodes.get('env').textContent,'TEST · HOME PC OFFLINE');
});
