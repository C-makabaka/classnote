const {test}=require('node:test'),assert=require('node:assert/strict');
const Core=require('../app/src/main/assets/core.js');
const fresh=()=>require('./fixtures/legacy-seed.cjs').create();
const note={id:'untitled',courseId:'health',title:'',number:1,date:'2026-09-29',body:'',photos:[]};
test('保存无主题的纯文字笔记，重载后仍为空主题',()=>{const s=Core.saveEntity(fresh(),'notes',{...note,body:'复习重点'});assert.equal(Core.validateState(JSON.parse(JSON.stringify(s))).notes[0].title,'')});
test('保存无主题的纯照片笔记',()=>{const s=Core.saveEntity(fresh(),'notes',{...note,photos:[{name:'photo.jpg',label:''}]});assert.equal(s.notes[0].photos.length,1)});
test('空白笔记不能保存，作业标题仍必填，旧版有主题笔记可用',()=>{assert.throws(()=>Core.saveEntity(fresh(),'notes',{...note,body:'　\u00a0\ufeff'}));assert.throws(()=>Core.saveEntity(fresh(),'tasks',{...note,description:'作业要求'}));assert.doesNotThrow(()=>Core.saveEntity(fresh(),'notes',{...note,title:'旧主题'}))});
