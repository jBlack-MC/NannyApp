const {readFileSync} = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const events = {}, stored = new Map(), deleted = [];
const scope = 'https://synthetic.example.invalid/nannyapp/';
let online = true;
const response = {ok:true, redirected:false, type:'basic', headers:{get:()=>''}, clone(){return this;}};
const cache = {addAll:async()=>{}, put:async(req,res)=>stored.set(req.url,res), match:async req=>stored.get(req.url)};
const context = {URL, Set, Promise, self:{registration:{scope},addEventListener:(n,f)=>events[n]=f,skipWaiting(){},clients:{claim:async()=>{}}},
  caches:{open:async()=>cache, keys:async()=>['nannyapp-v9','nannyapp-public-v10','unrelated-cache'], delete:async k=>deleted.push(k)},
  fetch:async()=>{if(!online) throw Error('offline'); return response;}};
vm.runInNewContext(readFileSync('NannyApp.Web/service-worker.js','utf8'),context);
(async()=>{
  let pending;
  events.activate({waitUntil:p=>pending=p}); await pending;
  assert.deepEqual(deleted,['nannyapp-v9']);
  for(const path of ['parent/children.php','messages.php','account.php','media.php?f=portfolio/A.jpg','index.php','auth/logout.php']) {
    for(const state of [true,false]) {
      online=state; let intercepted=false;
      events.fetch({request:{method:'GET',url:scope+path},respondWith:()=>intercepted=true});
      assert.equal(intercepted,false,`private ${path} must use network, including offline/account B`);
    }
  }
  online=true;
  const req={method:'GET',url:scope+'assets/css/style.css'};
  events.fetch({request:req,respondWith:p=>pending=p}); await pending;
  assert.equal(stored.size,1);
  online=false;
  events.fetch({request:req,respondWith:p=>pending=p}); assert.equal(await pending,response);
  for(const url of [scope+'assets/css/style.css?private=1','https://other.invalid/assets/css/style.css']) {
    let intercepted=false; events.fetch({request:{method:'GET',url},respondWith:()=>intercepted=true}); assert.equal(intercepted,false);
  }
  events.message({data:{type:'LOGOUT'},waitUntil:p=>pending=p}); await pending;
  assert(deleted.includes('nannyapp-public-v10')); assert(!deleted.includes('unrelated-cache'));
  console.log('PASS PWA private routes, offline isolation, allowlist, migration, logout purge');
})().catch(e=>{console.error(e);process.exitCode=1});
