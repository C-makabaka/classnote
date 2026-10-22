const {chromium}=require('playwright'),assert=require('node:assert/strict');
const http=require('node:http'),fs=require('node:fs'),path=require('node:path');
const base=path.resolve(__dirname,'../app/src/main/assets');
(async()=>{
 const server=http.createServer((req,res)=>{const file=path.join(base,req.url==='/'?'index.html':req.url.split('?')[0]);fs.readFile(file,(error,data)=>{if(error)return res.writeHead(404).end();res.setHeader('Content-Type',file.endsWith('.js')?'application/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(data)})}).listen(0,'127.0.0.1');
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROME_BIN,args:['--no-sandbox']});
 try{
  const page=await browser.newPage({viewport:{width:390,height:844}}),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(s=>{if(!localStorage.getItem('classnote-state'))localStorage.setItem('classnote-state',JSON.stringify(s))},require('./fixtures/legacy-seed.cjs').create());
  await page.goto(`http://127.0.0.1:${server.address().port}`);await page.locator('[data-action="all-weeks"]').click();await page.locator('[data-course="health"]').first().click();await page.locator('[data-action="add-note"]').click();
  const chooser=page.waitForEvent('filechooser');await page.locator('[data-action="pick-files"]').click({timeout:1500});
  await page.evaluate(()=>{const original=Platform.draft;window.attachmentDraftWrites=[];Platform.draft=function(...args){if(args.length)window.attachmentDraftWrites.push(args[0]?'saved':'cleared');return original.apply(this,args)}});
  const original=Buffer.from('%PDF-1.4\n课堂资料\n');
  await(await chooser).setFiles([{name:'课堂资料.pdf',mimeType:'application/pdf',buffer:original},{name:'复习<重点>.txt',mimeType:'text/plain',buffer:Buffer.from('复习重点')}]);
  await page.waitForFunction(()=>document.querySelectorAll('.attachment-row').length===2);
  assert.ok(!(await page.evaluate(()=>window.attachmentDraftWrites)).includes('cleared'),'receiving first files never clears the draft before persisting associations');
  assert.match(await page.locator('.attachment-list').textContent(),/课堂资料.pdf/);
  assert.equal(await page.locator('.attachment-list img').count(),0,'filename is escaped');
  let result=await page.evaluate(()=>{const d=JSON.parse(localStorage.getItem('classnote-draft'));return window.onNativeEvent('attachments',{draftId:d.data.id,attachments:d.data.attachments})});assert.equal(result,true);
  assert.equal(await page.locator('.attachment-row').count(),2,'duplicate result delivery is deduplicated');
  result=await page.evaluate(()=>window.onNativeEvent('attachments',{draftId:'other-note',attachments:[{name:'other.bin',label:'其他.pdf',type:'application/pdf',size:1}]}));assert.equal(result,false,'wrong draft rejected');
  await page.reload();await page.getByRole('button',{name:'草稿',exact:true}).click();assert.equal(await page.locator('.attachment-row').count(),2,'draft survives reload');
  await page.getByRole('button',{name:'保存笔记',exact:true}).click();assert.equal(await page.locator('.note .attachment-row').count(),2,'file-only note saved');
  const downloadEvent=page.waitForEvent('download');await page.locator('[data-action="open-file"]').first().click();const download=await downloadEvent;assert.equal(download.suggestedFilename(),'课堂资料.pdf');
  const downloaded=await download.path();assert.deepEqual(fs.readFileSync(downloaded),original,'stored bytes downloaded without change');
  await page.locator('[data-action="edit-note"]').click();await page.locator('[data-action="file-remove"]').first().click();await page.locator('#confirm-dialog [data-cancel]').click();await page.locator('#confirm-dialog').waitFor({state:'detached'});assert.equal(await page.locator('.attachment-row').count(),2);
  await page.locator('[data-action="file-remove"]').first().click();await page.locator('#confirm-dialog [data-confirm]').click();await page.locator('#confirm-dialog').waitFor({state:'detached'});assert.equal(await page.locator('.attachment-row').count(),1);
  await page.getByRole('button',{name:'保存笔记',exact:true}).click();await page.reload();await page.locator('[data-action="all-weeks"]').click();await page.locator('[data-course="health"]').first().click();assert.equal(await page.locator('.attachment-row').count(),1,'removal persists');
  await page.setViewportSize({width:320,height:800});assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
  await page.screenshot({path:path.resolve(__dirname,'../test-results/attachments-320.png'),fullPage:true});
  assert.deepEqual(errors,[]);console.log('PASS attachments UI: picker, multiple files, metadata, deduplication, wrong draft, reload, file-only save, open bytes, remove confirmation, persistence, 320px');
 }finally{await browser.close();server.close()}
})().catch(e=>{console.error(e);process.exit(1)});
