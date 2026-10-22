const {test}=require('node:test'),assert=require('node:assert/strict');
const Core=require('../app/src/main/assets/core.js');
const fresh=()=>require('./fixtures/legacy-seed.cjs').create();
const file={name:'document-1.bin',label:'课堂资料.pdf',size:12,type:'application/pdf'};
const note={id:'files-only',courseId:'health',title:'',number:1,date:'2026-10-22',body:'',photos:[],attachments:[file]};
test('仅附件笔记可保存、重载、编辑和删除，原输入不变',()=>{
 const input=fresh(),s=Core.saveEntity(input,'notes',note);
 assert.equal(input.notes.length,0);
 const restored=Core.validateState(JSON.parse(JSON.stringify(s)));
 assert.deepEqual(restored.notes[0].attachments,[file]);
 const edited=Core.saveEntity(restored,'notes',{...restored.notes[0],body:'补充说明'});
 assert.deepEqual(edited.notes[0].attachments,[file]);
 assert.ok(Core.tombstone(edited,'notes',note.id).notes[0].deletedAt);
});
test('旧笔记没有附件字段仍可使用，移除唯一内容后拒绝保存',()=>{
 assert.doesNotThrow(()=>Core.saveEntity(fresh(),'notes',{...note,title:'旧笔记',attachments:undefined}));
 assert.throws(()=>Core.saveEntity(fresh(),'notes',{...note,attachments:[]}));
});
test('拒绝不安全路径、重复文件、非法元数据和超出附件容量',()=>{
 for(const patch of [{name:'../document.bin'},{name:'document.pdf'},{size:-1},{size:'12'},{size:1.5},{size:64*1024*1024+1},{type:'text/html\nother'},{label:''},{label:42}]){
  assert.throws(()=>Core.saveEntity(fresh(),'notes',{...note,title:'有主题也必须校验文件',attachments:[{...file,...patch}]}),JSON.stringify(patch));
 }
 assert.throws(()=>Core.saveEntity(fresh(),'notes',{...note,attachments:[file,file]}));
 assert.throws(()=>Core.saveEntity(fresh(),'notes',{...note,attachments:null}));
 assert.throws(()=>Core.saveEntity(fresh(),'notes',{...note,attachments:Array.from({length:101},(_,i)=>({...file,name:'file-'+i+'.bin'}))}));
 assert.doesNotThrow(()=>Core.saveEntity(fresh(),'notes',{...note,attachments:[{...file,size:64*1024*1024},{...file,name:'zero.bin',size:0,type:'application/octet-stream'}]}));
});
