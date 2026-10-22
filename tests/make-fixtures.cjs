const fs=require('node:fs');
const Seed=require('./fixtures/legacy-seed.cjs'),Schedule=require('../app/src/main/assets/schedule');
fs.mkdirSync('test-results',{recursive:true});
const seed=Seed.create();
fs.writeFileSync('test-results/seed.json',JSON.stringify(seed));
fs.writeFileSync('test-results/adjusted.json',JSON.stringify(Schedule.apply(seed,{kind:'move',key:'health_0_2026-09-08',date:'2026-09-12',start:3,end:4,room:'新教室'})));

const swap=Schedule.apply(seed,{kind:'swap',key:'ops_0_2026-09-08',otherKey:'health_0_2026-09-08'});
let holiday=Schedule.apply(seed,{kind:'holiday',from:'2026-09-07',to:'2026-09-11',mappings:[{source:'2026-09-09',target:'2026-09-12'}]});
holiday=Schedule.apply(holiday,{kind:'holiday',mappings:[{source:'2026-09-08',target:'2026-09-13'}]});
const permanent=Schedule.apply(holiday,{kind:'permanent',key:'health_0_2026-09-15',fromWeek:2,day:5,start:5,end:6,room:'长期教室'});
fs.writeFileSync('test-results/schedule-scenarios.json',JSON.stringify([swap,holiday,permanent,Schedule.undo(permanent)]));

const Fresh=require('../app/src/main/assets/seed'),Import=require('../app/src/main/assets/timetable-import');
fs.writeFileSync('test-results/fresh.json',JSON.stringify(Fresh.create()));
const timed=Import.prepare(Fresh.create(),JSON.stringify(Import.example));timed.settings.periodTimes=require('../app/src/main/assets/time-axis.js').defaults;fs.writeFileSync('test-results/imported.json',JSON.stringify(timed));
