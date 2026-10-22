const {chromium}=require('playwright'),assert=require('node:assert/strict'),http=require('node:http'),fs=require('node:fs'),path=require('node:path');
const base=path.resolve(__dirname,'../app/src/main/assets');
(async()=>{
 const server=http.createServer((req,res)=>fs.readFile(path.join(base,req.url==='/'?'index.html':req.url.split('?')[0]),(e,d)=>{if(e)res.writeHead(404).end();else{res.setHeader('Content-Type',req.url.endsWith('.js')?'application/javascript':req.url.endsWith('.css')?'text/css':'text/html');res.end(d)}})).listen(0,'127.0.0.1');
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROME_BIN,args:['--no-sandbox']});let failed=0,completed=0;
 process.on('beforeExit',()=>{if(completed!==9){console.error(`FAIL incomplete run: ${completed}/9`);process.exitCode=1}});
 async function run(name,fn){
  const p=await browser.newPage({viewport:{width:390,height:844},hasTouch:true,isMobile:true});p.setDefaultTimeout(5000);const errors=[];p.on('pageerror',e=>errors.push(e.message));
  try{
   await p.goto(`http://127.0.0.1:${server.address().port}`);
   await p.evaluate(()=>{
    window.thumbs={};const c=document.createElement('canvas'),ctx=c.getContext('2d');
    for(const [name,w,h,color]of [['a.png',800,450,'#486e9e'],['b.png',450,800,'#88ab7d']]){c.width=w;c.height=h;ctx.fillStyle=color;ctx.fillRect(0,0,w,h);thumbs[name]=c.toDataURL();localStorage.setItem('media-'+name,thumbs[name])}
    window.dimensionCalls=0;window.fullCalls=0;
    Platform.mediaDimensions=async items=>{dimensionCalls++;await new Promise(r=>setTimeout(r,500));return items.map(p=>p.name==='a.png'?{width:800,height:450}:{width:450,height:800})};
    Platform.media=(name,thumb=false)=>{if(!thumb)fullCalls++;return thumb?thumbs[name]:'/original/'+name};
    window.items=[{name:'a.png',label:'A',msrc:thumbs['a.png'],width:800,height:450},{name:'b.png',label:'B',msrc:thumbs['b.png'],width:450,height:800}];
    window.hits=0;const b=document.createElement('button');b.id='underlying';b.onclick=()=>hits++;document.querySelector('#view').append(b);
    window.openPhotos=()=>PhotoViewer.open(items,0,b);
   });
   await p.route('**/original/**',async route=>{await new Promise(r=>setTimeout(r,600));try{await route.fulfill({status:200,contentType:'image/png',body:Buffer.from((await p.evaluate(()=>thumbs['a.png'])).split(',')[1],'base64')})}catch{}});
   const cdp=await p.context().newCDPSession(p),touch=(type,pts)=>cdp.send('Input.dispatchTouchEvent',{type,touchPoints:pts.map(([x,y,id=0])=>({x,y,id}))});
   await fn(p,touch);assert.deepEqual(errors,[]);console.log('PASS',name);
  }catch(e){failed++;console.error('FAIL',name,e.message)}finally{await p.close();completed++}
 }
 try{
  await run('已加载缩略图立即打开，不等整组尺寸或原图',async p=>{
   const result=await p.evaluate(()=>{openPhotos();return {dimensions:dimensionCalls,full:fullCalls,image:!!document.querySelector('#overlay .pswp__img')}});
   assert.equal(result.dimensions,0,'不能在打开时同步读取整组尺寸');assert.equal(result.full,0,'浏览时不解码原图');assert.ok(result.image,'同一JS任务中建立首帧图层');
   await p.waitForFunction(()=>document.querySelector('#overlay img[alt="A"]')?.naturalWidth>0);assert.equal(await p.evaluate(()=>fullCalls),0);
  });
  await run('单次拖动就保留起步位移',async(p,t)=>{
   await p.evaluate(()=>openPhotos());await p.waitForFunction(()=>document.querySelector('#overlay img[alt="A"]')?.naturalWidth>0);await p.waitForTimeout(250);
   const before=await p.locator('#overlay img[alt="A"]').boundingBox();await t('touchStart',[[300,422]]);await t('touchMove',[[220,422]]);await p.waitForTimeout(50);
   const after=await p.locator('#overlay img[alt="A"]').boundingBox();assert.ok(after.x<before.x-70,`80px手指位移应保留，实际${before.x-after.x}px`);await t('touchEnd',[]);
  });
  await run('打开后立即返回，不等开场动画',async p=>{
   await p.evaluate(()=>{Platform.mediaDimensions=async()=>[{width:800,height:450},{width:450,height:800}];openPhotos()});await p.waitForTimeout(20);
   await p.evaluate(()=>appBack());await p.waitForTimeout(40);assert.ok(await p.evaluate(()=>document.querySelector('#overlay').hidden),'返回不能等180ms开场和退场动画');
  });
  await run('点空白立即退出，不等待双击且不穿透',async p=>{
   await p.evaluate(()=>openPhotos());await p.waitForFunction(()=>document.querySelector('#overlay img[alt="A"]')?.naturalWidth>0);await p.waitForTimeout(250);
   await p.touchscreen.tap(195,40);await p.waitForTimeout(50);assert.ok(await p.evaluate(()=>document.querySelector('#overlay').hidden),'空白区域不需要等待双击');
   await p.evaluate(()=>document.querySelector('#underlying').dispatchEvent(new MouseEvent('click',{bubbles:true,cancelable:true})));assert.equal(await p.evaluate(()=>hits),0);
  });
  await run('放大停下后加载原图，迟到结果不污染重开',async(p,t)=>{
   await p.evaluate(()=>openPhotos());await p.waitForFunction(()=>document.querySelector('#overlay img[alt="A"]')?.naturalWidth>0);await p.waitForTimeout(250);
   await t('touchStart',[[145,422,0],[245,422,1]]);await t('touchMove',[[125,422,0],[265,422,1]]);await t('touchMove',[[85,422,0],[305,422,1]]);await p.waitForTimeout(150);
   assert.equal(await p.evaluate(()=>fullCalls),0,'手指缩放中不能开始解码原图');await t('touchEnd',[]);await p.waitForFunction(()=>fullCalls>0);
   await p.evaluate(()=>appBack());await p.waitForTimeout(40);assert.ok(await p.evaluate(()=>document.querySelector('#overlay').hidden));await p.evaluate(()=>openPhotos());
   await p.waitForTimeout(800);assert.ok(await p.locator('#overlay').isVisible());assert.ok((await p.locator('#overlay img[alt="A"]').getAttribute('src')).startsWith('data:'),'旧会话原图回调不能改变新会话');
  });
  await run('未知邻图不拖住当前图，载入后保持比例',async p=>{
   await p.evaluate(()=>{items[1]={name:'b.png',label:'B'};openPhotos()});
   await p.waitForTimeout(40);assert.ok(await p.locator('#overlay img[alt="A"]').count(),'首图不等待邻图');
   await p.waitForFunction(()=>document.querySelector('#overlay img[alt="B"]')?.naturalWidth>0);
   const b=await p.locator('#overlay img[alt="B"]').boundingBox();assert.ok(Math.abs(b.width/b.height-450/800)<.01);
  });
  await run('空白处取消触摸不会关闭，损坏图片退出后可以重开',async(p,t)=>{
   await p.evaluate(()=>openPhotos());await p.waitForFunction(()=>document.querySelector('#overlay img[alt="A"]')?.naturalWidth>0);
   await t('touchStart',[[195,40]]);await t('touchCancel',[]);await p.waitForTimeout(250);assert.ok(await p.locator('#overlay').isVisible());
   await p.evaluate(()=>appBack());await p.waitForFunction(()=>document.querySelector('#overlay').hidden);
   await p.evaluate(()=>PhotoViewer.open([{name:'missing.png'}]));await p.waitForSelector('.photo-error');await p.evaluate(()=>appBack());await p.waitForFunction(()=>document.querySelector('#overlay').hidden);
   await p.evaluate(()=>openPhotos());await p.waitForFunction(()=>document.querySelector('#overlay img[alt="A"]')?.naturalWidth>0);
   await p.touchscreen.tap(195,422);await p.touchscreen.tap(195,422);await p.waitForTimeout(250);assert.ok(await p.locator('#overlay').isVisible(),'错误页不能遗留关闭监听器');
  });
  await run('原图加载失败保留缩略图和缩放',async p=>{
   await p.unroute('**/original/**');await p.route('**/original/**',route=>route.abort());await p.evaluate(()=>openPhotos());await p.waitForFunction(()=>document.querySelector('#overlay img[alt="A"]')?.naturalWidth>0);
   await p.touchscreen.tap(195,422);await p.touchscreen.tap(195,422);await p.waitForFunction(()=>fullCalls>0);await p.waitForTimeout(250);
   assert.ok((await p.locator('#overlay img[alt="A"]').getAttribute('src')).startsWith('data:'));assert.ok((await p.locator('#overlay img[alt="A"]').boundingBox()).width>800);
  });
  await run('迟到邻图不能取消当前放大图的原图升级',async p=>{
   let held,done;await p.route('**/late-thumb/**',route=>{held=route;return new Promise(r=>done=r)});
   await p.evaluate(()=>{
    const Original=PhotoSwipe;window.PhotoSwipe=class extends Original{constructor(options){super(options);window.testViewer=this}};
    const media=Platform.media;Platform.media=(name,thumb)=>name==='b.png'&&thumb?'/late-thumb/b.png':media(name,thumb);
    items[1]={name:'b.png',label:'B'};openPhotos();
   });
   await p.waitForFunction(()=>document.querySelector('#overlay img[alt="A"]')?.naturalWidth>0);assert.ok(held,'邻图尺寸请求应挂起');
   const binary=Buffer.from((await p.evaluate(()=>thumbs['b.png'])).split(',')[1],'base64');
   await p.evaluate(()=>testViewer.zoomTo(testViewer.currSlide.zoomLevels.secondary,{x:195,y:422},0));await p.waitForTimeout(30);
   await held.fulfill({status:200,contentType:'image/png',body:binary});done();await p.waitForTimeout(500);
   assert.ok(await p.evaluate(()=>fullCalls>0),'邻图的change通知不能取消当前图片的升级任务');
  });
 }finally{await browser.close();server.close()}console.log(`COMPLETE photo-latency: ${completed}/9`);if(failed)process.exitCode=1;
})().catch(e=>{console.error(e);process.exitCode=1});
