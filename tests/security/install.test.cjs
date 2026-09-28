const {readFileSync} = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const source = readFileSync('NannyApp.Web/assets/js/install.js', 'utf8');
function fixture(standalone = false) {
  const events = {}, button = {hidden:true, disabled:false, addEventListener:(n,f)=>events[n]=f};
  const status = {textContent:''};
  vm.runInNewContext(source, {
    document:{getElementById:id=>id === 'install-app' ? button : status},
    window:{matchMedia:()=>({matches:standalone}), addEventListener:(n,f)=>events[n]=f}, navigator:{}
  });
  return {events, button, status};
}
(async () => {
  for (const outcome of ['accepted', 'dismissed', 'error']) {
    const {events, button, status} = fixture();
    let calls = 0, prevented = false;
    events.beforeinstallprompt({preventDefault(){prevented=true;},
      async prompt(){calls++; if(outcome === 'error') throw Error('browser rejected');},
      userChoice:Promise.resolve({outcome})});
    assert(prevented); assert.equal(button.hidden,false);
    await events.click(); await events.click();
    assert.equal(calls,1); assert.equal(button.hidden,true); assert.equal(button.disabled,false);
    assert(status.textContent.length > 0);
    events.appinstalled(); assert.match(status.textContent,/ready/);
  }
  assert.match(fixture(true).status.textContent,/ready/);
  console.log('PASS install accepted/dismissed/error, single-use prompt and standalone state');
})();
