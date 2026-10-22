const {test}=require('node:test');
const assert=require('node:assert/strict');
let C,S;
try{C=require('../app/src/main/assets/core.js');S=require('./fixtures/legacy-seed.cjs')}catch{}
test('domain layer exists',()=>assert.ok(C&&S,'domain layer not implemented'));
test('local calendar dates and semester boundaries',()=>{
 assert.equal(C.currentWeek('2026-09-07','2026-09-20'),2);
 assert.equal(C.currentWeek('2026-09-07','2026-09-06'),0);
 assert.deepEqual(C.weekRange('2026-09-07',4),['2026-09-28','2026-10-04']);
 assert.deepEqual(C.weekRange('2026-09-07',17),['2026-12-28','2027-01-03']);
 assert.throws(()=>C.currentWeek('2026-02-30','2026-03-01'));
});
test('seed is real timetable only and week parity respected',()=>{
 const s=S.create();C.validateState(s);
 assert.equal(s.courses.length,7);assert.equal(s.tasks.length,0);assert.equal(s.notes.length,0);
 assert.equal(C.activeLessons(s,'all').length,10);
 assert.equal(C.activeLessons(s,2).length,6);
 assert.equal(C.activeLessons(s,5).length,9);
 assert.equal(C.activeLessons(s,6).length,7);
 assert.equal(C.activeLessons(s,18).length,0);
});
test('tasks sorted by due date, completed retained below pending, restore updates counts',()=>{
 let s=S.create();const c=s.courses[0].id;
 for(const [id,dueAt] of [['b',200],['a',100],['z',null]])s=C.saveEntity(s,'tasks',{id,courseId:c,title:id,number:1,dueAt,reminderMinutes:null,photos:[]});
 assert.deepEqual(C.orderedTasks(s,c).map(t=>t.id),['a','b','z']);
 s=C.toggleTask(s,'a');assert.equal(C.pendingCount(s,c),2);
 assert.deepEqual(C.orderedTasks(s,c).map(t=>t.id),['b','z','a']);
 assert.equal(s.tasks.length,3);s=C.toggleTask(s,'a');assert.equal(C.pendingCount(s,c),3);
});
test('blank title, bad course reference, invalid reminders are rejected without mutation',()=>{
 const s=S.create();const before=JSON.stringify(s),base={id:'t',courseId:s.courses[0].id,number:1,photos:[]};
 assert.throws(()=>C.saveEntity(s,'tasks',{...base,title:'  '}));
 assert.throws(()=>C.saveEntity(s,'tasks',{...base,title:'x',courseId:'missing'}));
 assert.throws(()=>C.saveEntity(s,'tasks',{...base,title:'x',reminderMinutes:60,dueAt:null}));
 assert.equal(JSON.stringify(s),before);
});
test('note images require safe filenames and invalid imports rejected',()=>{
 let s=S.create(),c=s.courses[0].id;
 assert.throws(()=>C.saveEntity(s,'notes',{id:'n',courseId:c,title:'笔记',number:1,date:'2026-09-20',photos:[{name:'../../a.jpg'}]}));
 s=C.saveEntity(s,'notes',{id:'n',courseId:c,title:'笔记',number:1,date:'2026-09-20',photos:[{name:'abc.jpg',label:'照片'}]});
 assert.equal(s.notes[0].photos[0].name,'abc.jpg');
 const bad=JSON.parse(JSON.stringify(s));bad.schemaVersion=999;assert.throws(()=>C.validateState(bad));
 const dup=JSON.parse(JSON.stringify(s));dup.notes.push({...dup.notes[0]});assert.throws(()=>C.validateState(dup));
});
test('deleted records remain as tombstones and no longer count',()=>{
 let s=S.create();const c=s.courses[0].id;
 s=C.saveEntity(s,'tasks',{id:'t',courseId:c,title:'练习',number:1,photos:[],dueAt:null,reminderMinutes:null});
 s=C.tombstone(s,'tasks','t');assert.equal(C.pendingCount(s,c),0);assert.equal(s.tasks.length,1);assert.ok(s.tasks[0].deletedAt);
});
test('semester start must be Monday and assignments stay available outside semester',()=>{
 const s=S.create();s.settings.semesterStart='2026-09-08';assert.throws(()=>C.validateState(s));
 s.settings.semesterStart='2026-09-07';s.settings.weeks=0;assert.throws(()=>C.validateState(s));
});
test('all-weeks groups overlapping cells without dropping disjoint-week courses',()=>{
 let s=S.create();s=C.saveEntity(s,'courses',{id:'alternate',name:'隔周课程',lessons:[{day:2,start:3,end:4,fromWeek:2,toWeek:16,parity:'even',room:'B-101'}]});
 const groups=C.scheduleGroups(s,'all'),g=groups.find(x=>x.day===2&&x.start===3);
 assert.deepEqual(g.lessons.map(l=>l.courseId).sort(),['alternate','ops']);
 assert.equal(C.scheduleGroups(s,2).find(x=>x.day===2&&x.start===3).lessons.length,1);
});
