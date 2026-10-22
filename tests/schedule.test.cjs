const test=require('node:test'),assert=require('node:assert/strict');
const Core=require('../app/src/main/assets/core.js'),Seed=require('./fixtures/legacy-seed.cjs');
let S;try{S=require('../app/src/main/assets/schedule.js')}catch{}
const base=()=>Seed.create();
const find=(s,date,cid)=>S.list(s).find(o=>o.date===date&&o.courseId===cid);
function move(s){return S.apply(s,{kind:'move',key:find(s,'2026-09-08','health').key,date:'2026-09-12',start:3,end:4,room:'新教室'});}
test('schedule feature is available',()=>assert.ok(S));
test('moving one occurrence preserves next week, assignments and reversible original slot',()=>{
 let s=base();s=Core.saveEntity(s,'tasks',{courseId:'health',title:'原截止',number:1,photos:[],dueAt:1800000000000,reminderMinutes:120});const original=Core.clone(s);s=move(s);
 assert.equal(find(s,'2026-09-08','health'),undefined);assert.equal(find(s,'2026-09-12','health').room,'新教室');assert.ok(find(s,'2026-09-15','health'));assert.deepEqual(s.tasks,original.tasks);
 assert.equal(S.ghosts(s,1).find(o=>o.courseId==='health').date,'2026-09-08');assert.deepEqual(S.list(S.undo(s)),S.list(original));assert.equal(original.adjustments,undefined);
});
test('swap is atomic and exchanges slots including room without losing either course',()=>{
 let s=base(),a=find(s,'2026-09-08','ops'),b=find(s,'2026-09-08','health');s=S.apply(s,{kind:'swap',key:a.key,otherKey:b.key});
 assert.deepEqual([find(s,'2026-09-08','ops').start,find(s,'2026-09-08','health').start],[7,3]);assert.equal(find(s,'2026-09-08','ops').room,b.room);
});
test('holiday makeup uses exact source date and odd weeks; supports canceled source later',()=>{
 let s=S.apply(base(),{kind:'holiday',from:'2026-09-07',to:'2026-09-11',mappings:[{source:'2026-09-09',target:'2026-09-12'}]});
 assert.equal(S.list(s).filter(o=>o.date==='2026-09-12').length,2);assert.equal(S.list(s).filter(o=>o.date==='2026-09-09').length,0);
 s=S.apply(s,{kind:'holiday',mappings:[{source:'2026-09-08',target:'2026-09-13'}]});assert.equal(S.list(s).filter(o=>o.date==='2026-09-13').length,3);
 assert.throws(()=>S.apply(s,{kind:'holiday',mappings:[{source:'2026-09-09',target:'2026-09-13'}]}),/已调整|重复|冲突/);
 const even=S.apply(base(),{kind:'holiday',mappings:[{source:'2026-09-16',target:'2026-09-19'}]});assert.equal(S.list(even).filter(o=>o.date==='2026-09-19').length,1);
});
test('long-term change starts at requested week and preserves individual exceptions',()=>{
 let s=move(base()),key=find(s,'2026-09-15','health').key;
 s=S.apply(s,{kind:'permanent',key,fromWeek:2,day:5,start:5,end:6,room:'长期教室'});
 assert.ok(find(s,'2026-09-12','health'));assert.equal(find(s,'2026-09-15','health'),undefined);assert.ok(find(s,'2026-09-18','health'));assert.ok(find(s,'2026-09-25','health'));
 const undone=S.undo(s);assert.ok(find(undone,'2026-09-15','health'));assert.ok(find(undone,'2026-09-12','health'));
});
test('conflict and invalid input reject without altering state',()=>{
 const s=base(),snapshot=JSON.stringify(s),a=find(s,'2026-09-08','ops');
 assert.throws(()=>S.apply(s,{kind:'move',key:a.key,date:'2026-09-08',start:7,end:8,room:''}),/冲突/);
 assert.throws(()=>S.apply(s,{kind:'move',key:a.key,date:'2026-09-08',start:3,end:4,room:a.room}),/没有变化/);
 assert.throws(()=>S.apply(s,{kind:'move',key:a.key,date:'2026-09-06',start:1,end:2,room:''}),/学期/);
 assert.throws(()=>S.apply(s,{kind:'holiday',mappings:[{source:'2026-09-08',target:'2026-09-12'},{source:'2026-09-08',target:'2026-09-13'}]}),/重复/);
 assert.equal(JSON.stringify(s),snapshot);
});
test('backup replays validated patches and rejects broken references',()=>{
 const s=move(base());Core.validateState(s);const bad=Core.clone(s);bad.adjustments[0].changes[0].before.start=1;assert.throws(()=>Core.validateState(bad));
 const bad2=Core.clone(s);bad2.adjustments[0].changes[0].after.courseId='english';assert.throws(()=>Core.validateState(bad2));
 const bad3=Core.clone(s);bad3.settings.semesterStart='2026-09-14';assert.throws(()=>Core.validateState(bad3));
});
test('long-term change does not overwrite an already moved future occurrence',()=>{
 let s=base();s=S.apply(s,{kind:'move',key:find(s,'2026-09-22','health').key,date:'2026-09-26',start:3,end:4,room:'临时'});
 s=S.apply(s,{kind:'permanent',key:find(s,'2026-09-15','health').key,fromWeek:2,day:5,start:5,end:6,room:'固定'});
 assert.ok(find(s,'2026-09-26','health'));assert.equal(find(s,'2026-09-25','health'),undefined);
});
test('base timetable edits cannot silently corrupt active adjustments',()=>{
 const s=move(base()),ops=s.courses.find(c=>c.id==='ops'),changed=Core.clone(ops);changed.lessons[0].room='另一个教室';
 assert.throws(()=>Core.saveEntity(s,'courses',changed),/基础|撤销|时段/);const metadata=Core.saveEntity(s,'courses',{...ops,teacher:'新老师'});assert.equal(metadata.courses.find(c=>c.id==='ops').teacher,'新老师');
});
test('reject invalid extension and note provenance instead of accepting corrupt backup',()=>{
 const s=base();s.adjustments='';assert.throws(()=>Core.validateState(s));
 const n=base();assert.throws(()=>Core.saveEntity(n,'notes',{courseId:'health',title:'笔记',number:1,date:'2026-09-12',sourceDate:'无效',photos:[]}));
});
test('cross-year makeup uses local calendar dates and correct source week',()=>{
 let s=base();s.settings.semesterStart='2026-12-28';s=S.apply(s,{kind:'holiday',from:'2026-12-28',to:'2027-01-01',mappings:[{source:'2026-12-30',target:'2027-01-02'}]});assert.equal(S.list(s).filter(o=>o.date==='2027-01-02').length,2);assert.equal(S.week(s,1).filter(o=>o.day===6).length,2);
});
test('multiple makeup dates keep independent origins and rollback as one action',()=>{
 const s=S.apply(base(),{kind:'holiday',from:'2026-09-07',to:'2026-09-11',mappings:[{source:'2026-09-08',target:'2026-09-12'},{source:'2026-09-10',target:'2026-09-13'}]});
 assert.equal(S.list(s).filter(o=>o.date==='2026-09-12').length,3);assert.equal(S.list(s).filter(o=>o.date==='2026-09-13').length,3);assert.equal(S.undo(s).adjustments.length,0);
});
test('new course cannot overlap an adjusted occurrence and archive restoration checks conflicts',()=>{
 let s=move(base());const template=s.courses.find(c=>c.id==='health');
 assert.throws(()=>Core.saveEntity(s,'courses',{...template,id:'newcourse',lessons:[{day:6,start:3,end:4,room:'另',fromWeek:1,toWeek:1,parity:'all'}]}),/冲突/);
 const archived=Core.saveEntity(s,'courses',{...template,archived:true});assert.equal(S.list(archived).some(o=>o.courseId==='health'),false);const restored=Core.saveEntity(archived,'courses',{...template,archived:false});assert.ok(find(restored,'2026-09-12','health'));
});
test('undo rejects a restored occurrence colliding with a newly filled slot',()=>{
 let s=move(base());const template=s.courses.find(c=>c.id==='health');s=Core.saveEntity(s,'courses',{...template,id:'newcourse',name:'新增课程',lessons:[{day:2,start:7,end:8,room:'新',fromWeek:1,toWeek:1,parity:'all'}]});const before=JSON.stringify(s);assert.throws(()=>S.undo(s),/冲突/);assert.equal(JSON.stringify(s),before);
});
test('holiday cancellation does not alter archived course history',()=>{
 let s=base(),health=s.courses.find(c=>c.id==='health');s=Core.saveEntity(s,'courses',{...health,archived:true});s=S.apply(s,{kind:'holiday',from:'2026-09-08',to:'2026-09-08',mappings:[]});s=Core.saveEntity(s,'courses',{...health,archived:false});assert.ok(find(s,'2026-09-08','health'));
});
