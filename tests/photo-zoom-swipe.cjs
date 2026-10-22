const {chromium}=require('playwright'),assert=require('node:assert/strict'),http=require('node:http'),fs=require('node:fs'),path=require('node:path');
const base=path.resolve(__dirname,'../app/src/main/assets');
(async()=>{
 const server=http.createServer((req,res)=>fs.readFile(path.join(base,req.url==='/'?'index.html':req.url.split('?')[0]),(e,d)=>{if(e)res.writeHead(404).end();else{res.setHeader('Content-Type',req.url.endsWith('.js')?'application/javascript':req.url.endsWith('.css')?'text/css':'text/html');res.end(d)}})).listen(0,'127.0.0.1');
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROME_BIN,args:['--no-sandbox']});let failed=0,completed=0;
 process.on('beforeExit',()=>{if(completed!==5){console.error(`FAIL incomplete run ${completed}/5`);process.exitCode=1}});
 const current=p=>p.locator('#overlay .pswp__item[aria-hidden="false"] img');
 async function zoom(p){const before=await current(p).boundingBox();await p.touchscreen.tap(195,422);await p.touchscreen.tap(195,422);await p.waitForTimeout(250);assert.ok((await current(p).boundingBox()).width>before.width*2,'双击后图片保持放大')}
 async function swipe(p,t,from,to,hold=160){await t('touchStart',[[from,422]]);for(let i=1;i<=12;i++){await t('touchMove',[[from+(to-from)*i/12,422]]);await p.waitForTimeout(12)}await p.waitForTimeout(hold);await t('touchEnd',[]);await p.waitForTimeout(650)}
 async function edge(p,t,direction){for(let i=0;i<4;i++){const r=await current(p).boundingBox();if(direction==='next'?Math.abs(r.x+r.width-390)<3:Math.abs(r.x)<3)return;await swipe(p,t,direction==='next'?350:40,direction==='next'?40:350)}const r=await current(p).boundingBox();assert.ok(direction==='next'?Math.abs(r.x+r.width-390)<3:Math.abs(r.x)<3,'图片应先移到边缘')}
 async function run(name,fn,start=0){
  const p=await browser.newPage({viewport:{width:390,height:844},hasTouch:true,isMobile:true});const errors=[];p.on('pageerror',e=>errors.push(e.message));
  try{
   await p.goto(`http://127.0.0.1:${server.address().port}`);await p.evaluate(start=>{
    const c=document.createElement('canvas'),ctx=c.getContext('2d'),items=[];
    for(const [name,label,w,h,color]of [['a.png','A',800,450,'#478466'],['b.png','B',450,800,'#48668c'],['c.png','C',800,450,'#c7a568']]){c.width=w;c.height=h;ctx.fillStyle=color;ctx.fillRect(0,0,w,h);const src=c.toDataURL();localStorage.setItem('media-'+name,src);items.push({name,label,msrc:src,width:w,height:h})}
    window.hits=0;const b=document.createElement('button');b.id='underlying';b.onclick=()=>hits++;document.querySelector('#view').append(b);PhotoViewer.open(items,start,b);
   },start);
   await p.waitForFunction(()=>document.querySelector('#overlay .pswp__item[aria-hidden="false"] img')?.naturalWidth>0);
   const cdp=await p.context().newCDPSession(p),touch=(type,pts)=>cdp.send('Input.dispatchTouchEvent',{type,touchPoints:pts.map(([x,y,id=0])=>({x,y,id}))});
   await fn(p,touch);assert.deepEqual(errors,[]);assert.equal(await p.evaluate(()=>hits),0);console.log('PASS',name);
  }catch(e){failed++;console.error('FAIL',name,e.message)}finally{await p.close();completed++}
 }
 try{
  await run('放大横图内部拖动，右边缘继续左滑切到下一张',async(p,t)=>{
   await zoom(p);const before=await current(p).boundingBox();await swipe(p,t,240,180);const after=await current(p).boundingBox();assert.equal(await current(p).getAttribute('alt'),'A');assert.ok(after.x<before.x-40,'图片内部仍然平移');
   await edge(p,t,'next');assert.equal(await current(p).getAttribute('alt'),'A');await swipe(p,t,350,40);assert.equal(await current(p).getAttribute('alt'),'B','放大后无需缩小即可切到下一张');
   const next=await current(p).boundingBox();assert.ok(next.width<=390.1&&next.height<=844.1,'下一张自动适配屏幕');
  });
  await run('放大竖图，左边缘继续右滑切回上一张',async(p,t)=>{
   await zoom(p);await edge(p,t,'prev');assert.equal(await current(p).getAttribute('alt'),'B');await swipe(p,t,40,350);assert.equal(await current(p).getAttribute('alt'),'A','放大竖图也能切回上一张');
  },1);
  await run('放大边缘短拖回弹，取消不切换不退出',async(p,t)=>{
   await zoom(p);await edge(p,t,'next');await swipe(p,t,260,200,200);assert.equal(await current(p).getAttribute('alt'),'A');const r=await current(p).boundingBox();assert.ok(r.width>900&&Math.abs(r.x+r.width-390)<3,'短拖后保持放大与边缘位置');
   await t('touchStart',[[260,422]]);await t('touchMove',[[200,422]]);await t('touchCancel',[]);await p.waitForTimeout(650);assert.equal(await current(p).getAttribute('alt'),'A');assert.ok(await p.locator('#overlay').isVisible());
  });
  await run('双指缩放后的剩余手指越过边缘只平移',async(p,t)=>{
   await t('touchStart',[[145,422,0],[245,422,1]]);await t('touchMove',[[125,422,0],[265,422,1]]);await t('touchMove',[[85,422,0],[305,422,1]]);await p.waitForTimeout(50);
   await t('touchEnd',[[85,422,0]]);for(let i=1;i<=10;i++){await t('touchMove',[[85+290*i/10,422,0]]);await p.waitForTimeout(15)}await t('touchEnd',[]);await p.waitForTimeout(650);
   assert.equal(await current(p).getAttribute('alt'),'B','同一次双指手势不能意外切图');assert.ok(await p.locator('#overlay').isVisible());
  },1);
  await run('第一张和最后一张放大后仍不越界',async(p,t)=>{
   await zoom(p);await edge(p,t,'next');await swipe(p,t,350,40);assert.equal(await current(p).getAttribute('alt'),'C');await p.evaluate(()=>appBack());await p.waitForFunction(()=>document.querySelector('#overlay').hidden);
   await p.evaluate(()=>PhotoViewer.open([{name:'a.png',label:'A',width:800,height:450},{name:'b.png',label:'B',width:450,height:800}],0));await p.waitForFunction(()=>document.querySelector('#overlay img[alt="A"]')?.naturalWidth>0);
   await zoom(p);await edge(p,t,'prev');await swipe(p,t,40,350);assert.equal(await current(p).getAttribute('alt'),'A');assert.ok(await p.locator('#overlay').isVisible());
  },2);
 }finally{await browser.close();server.close()}console.log(`COMPLETE photo-zoom-swipe: ${completed}/5`);if(failed)process.exitCode=1;
})().catch(e=>{console.error(e);process.exitCode=1});
