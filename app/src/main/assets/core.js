(function(root,factory){const value=factory();if(typeof module==='object'&&module.exports)module.exports=value;else root.Core=value})(typeof globalThis==='object'?globalThis:this,function(){
 'use strict';
 const DAY=86400000;
 const clone=x=>JSON.parse(JSON.stringify(x));
 const assert=(ok,message)=>{if(!ok)throw Error(message)};
 function dateValue(value){assert(typeof value==='string'&&/^\d{4}-\d{2}-\d{2}$/.test(value),'日期格式应为年-月-日');const n=Date.parse(value+'T00:00:00Z');assert(Number.isFinite(n)&&new Date(n).toISOString().slice(0,10)===value,'日期无效');return n}
 function today(){const d=new Date();return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`}
 function currentWeek(start,date=today()){return Math.floor((dateValue(date)-dateValue(start))/(7*DAY))+1}
 function weekRange(start,week){assert(Number.isInteger(week)&&week>=1,'周次无效');const n=dateValue(start)+(week-1)*7*DAY;return [n,n+6*DAY].map(v=>new Date(v).toISOString().slice(0,10))}
 function matches(l,week){return week>=l.fromWeek&&week<=l.toWeek&&(l.parity==='all'||(l.parity==='odd'?week%2===1:week%2===0))}
 function activeLessons(s,week){const list=[];s.courses.filter(c=>!c.deletedAt&&!c.archived).forEach(c=>(c.lessons||[]).forEach(l=>{if(week==='all'||matches(l,+week))list.push({...l,courseId:c.id,course:c})}));return list}
 function scheduleGroups(s,week){const groups=[];for(const l of activeLessons(s,week).sort((a,b)=>a.day-b.day||a.start-b.start)){const last=groups[groups.length-1];if(last&&last.day===l.day&&l.start<=last.end){last.end=Math.max(last.end,l.end);last.lessons.push(l)}else groups.push({day:l.day,start:l.start,end:l.end,lessons:[l]})}return groups}
 function pendingCount(s,id){return s.tasks.filter(t=>!t.deletedAt&&(!id||t.courseId===id)&&!t.completedAt).length}
 function orderedTasks(s,id){return s.tasks.filter(t=>!t.deletedAt&&(!id||t.courseId===id)).slice().sort((a,b)=>!!a.completedAt-!!b.completedAt||(a.completedAt?b.completedAt-a.completedAt:(a.dueAt??Infinity)-(b.dueAt??Infinity))||b.updatedAt-a.updatedAt)}
 function uuid(){if(typeof crypto==='object'&&crypto.randomUUID)return crypto.randomUUID();return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g,c=>{const v=Math.floor(Math.random()*16);return (c==='x'?v:(v&3|8)).toString(16)})}
 function text(v,max=20000){return typeof v==='string'&&v.length<=max}
 function validateState(s){
  assert(s&&s.schemaVersion===1,'不支持的数据版本');assert(s.settings&&text(s.ownerId,100)&&s.ownerId.length>0,'数据结构无效');
  const start=dateValue(s.settings.semesterStart);assert(new Date(start).getUTCDay()===1,'第一周起始日必须是周一');
  assert(Number.isInteger(s.settings.weeks)&&s.settings.weeks>=1&&s.settings.weeks<=52,'学期周数应为1–52');
  if(s.settings.periodTimes!==undefined)(typeof module==='object'?require('./time-axis.js'):TimeAxis).validate(s.settings.periodTimes);
  const globalIds=new Set();
  for(const k of ['courses','tasks','notes']){assert(Array.isArray(s[k])&&s[k].length<=20000,'记录数量或结构无效');for(const e of s[k]){
   assert(text(e.id,100)&&/^[a-zA-Z0-9_-]+$/.test(e.id)&&!globalIds.has(e.id),'记录标识重复或无效');globalIds.add(e.id);
   assert(Number.isSafeInteger(e.revision)&&e.revision>=1&&Number.isSafeInteger(e.updatedAt),'记录版本无效');
   assert(e.deletedAt==null||(Number.isSafeInteger(e.deletedAt)&&e.deletedAt>0),'删除时间无效');
   if(k==='courses'){
    assert(text(e.name,200)&&e.name.trim(),'课程名称不能为空');assert(text(e.shortName||'',100),'简称过长');
    assert(Array.isArray(e.lessons)&&e.lessons.length<=100,'课表时段无效');
    for(const l of e.lessons){assert([l.day,l.start,l.end,l.fromWeek,l.toWeek].every(Number.isInteger),'课表时段必须为整数');assert(l.day>=1&&l.day<=7&&l.start>=1&&l.end>=l.start&&l.end<=14&&l.fromWeek>=1&&l.toWeek>=l.fromWeek&&l.toWeek<=52&&['all','odd','even'].includes(l.parity)&&text(l.room,200),'课表时段范围无效')}
   }else{
    assert(s.courses.some(c=>c.id===e.courseId),'记录没有对应课程');assert(text(e.title,300),'标题格式无效');if(k==='tasks')assert(e.title.trim(),'作业标题不能为空');
    assert(Number.isInteger(e.number)&&e.number>=1&&e.number<=9999,'课次或作业序号应为1–9999');
    assert(Array.isArray(e.photos)&&e.photos.length<=100,'照片数据无效');
    for(const p of e.photos)assert(p&&text(p.name,100)&&/^[a-zA-Z0-9_-]+\.(jpg|jpeg|png|webp|gif|heic|heif)$/i.test(p.name)&&text(p.label||'',300),'照片文件名无效');
    if(k==='notes'){
     const files=e.attachments===undefined?[]:e.attachments;
     assert(Array.isArray(files)&&files.length<=100,'每条笔记最多100个文件附件');const names=new Set();
     for(const f of files){assert(f&&text(f.name,100)&&/^[a-zA-Z0-9_-]{1,80}\.bin$/.test(f.name)&&!names.has(f.name),'附件文件名重复或无效');names.add(f.name);assert(text(f.label,300)&&f.label.trim()&&Number.isSafeInteger(f.size)&&f.size>=0&&f.size<=64*1024*1024&&text(f.type,120)&&/^[a-z0-9][a-z0-9!#$&^_.+-]*\/[a-z0-9][a-z0-9!#$&^_.+-]*$/.test(f.type),'附件信息无效（单个文件最多64MB）')}
     assert(e.title.trim()||(typeof e.body==='string'&&e.body.trim())||e.photos.length||files.length,'请添加照片、文件或填写笔记内容');dateValue(e.date);if(e.sourceDate!==undefined)dateValue(e.sourceDate);assert(e.occurrenceKey==null||text(e.occurrenceKey,150),'原课次标识无效')}
    else{assert(e.dueAt==null||(Number.isSafeInteger(e.dueAt)&&e.dueAt>0),'截止时间无效');assert(e.completedAt==null||(Number.isSafeInteger(e.completedAt)&&e.completedAt>0),'完成时间无效');assert(e.reminderMinutes==null||(Number.isInteger(e.reminderMinutes)&&e.reminderMinutes>=0&&e.reminderMinutes<=525600&&e.dueAt!=null),'设置提醒需要有效截止时间')}
   }
   for(const f of ['body','requirements','grading','remarks','teacher','submission','description'])assert(e[f]==null||text(e[f]),'文字内容过长或格式错误');
  }}
  if(s.adjustments!=null){const scheduler=typeof module==='object'&&module.exports?require('./schedule.js'):globalThis.Schedule;assert(scheduler,'调课模块未加载');scheduler.validate(s)}
  return s;
 }
 function saveEntity(state,collection,input){
  assert(['courses','tasks','notes'].includes(collection),'未知记录类型');const s=clone(state),i=s[collection].findIndex(e=>e.id===input.id),old=i<0?{}:s[collection][i];
  const e={...old,...clone(input),id:input.id||uuid(),ownerId:state.ownerId,spaceId:old.spaceId||'personal',revision:(old.revision||0)+1,updatedAt:Date.now(),deletedAt:null};
  if(collection!=='courses'){e.photos=e.photos||[];e.title=(e.title||'').trim()}
  if(collection==='tasks'){e.dueAt=e.dueAt??null;e.reminderMinutes=e.reminderMinutes??null;e.completedAt=e.completedAt??null}
  if(i<0)s[collection].push(e);else s[collection][i]=e;validateState(s);if(collection==='courses'){const scheduler=typeof module==='object'&&module.exports?require('./schedule.js'):globalThis.Schedule;scheduler.checkCourseChange(state,s,e.id)}return s;
 }
 function toggleTask(s,id){const t=s.tasks.find(t=>t.id===id&&!t.deletedAt);assert(t,'作业不存在');return saveEntity(s,'tasks',{...t,completedAt:t.completedAt?null:Date.now()})}
 function tombstone(state,k,id){const s=clone(state),e=s[k].find(e=>e.id===id);assert(e,'记录不存在');e.deletedAt=Date.now();e.updatedAt=e.deletedAt;e.revision++;validateState(s);return s}
 return {clone,today,currentWeek,weekRange,activeLessons,scheduleGroups,pendingCount,orderedTasks,uuid,validateState,saveEntity,toggleTask,tombstone};
});
