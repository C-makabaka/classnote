(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.TimeAxis=api})(globalThis,function(){
 'use strict';
 const defaults=[['08:00','08:45'],['08:55','09:40'],['10:00','10:45'],['10:55','11:40'],['12:30','13:15'],['13:25','14:10'],['14:30','15:15'],['15:25','16:10'],['16:20','17:05'],['17:15','18:00'],['19:00','19:45'],['19:55','20:40'],['20:50','21:35'],['21:45','22:30']];
 const minutes=s=>{if(typeof s!=='string'||!/^([01]\d|2[0-3]):[0-5]\d$/.test(s))throw Error('请填写有效的上下课时间');return +s.slice(0,2)*60+ +s.slice(3)};
 function validate(times){if(!Array.isArray(times)||times.length!==14)throw Error('请填写全部14节课的时间');let previous=-1;times.forEach((p,i)=>{if(!Array.isArray(p)||p.length!==2)throw Error('节次时间格式不正确');const a=minutes(p[0]),b=minutes(p[1]);if(a>=b||a<previous)throw Error(`第${i+1}节时间重叠或下课早于上课，请检查`);previous=b});return times}
 function position(times,now){validate(times);const n=now.getHours()*60+now.getMinutes()+now.getSeconds()/60+now.getMilliseconds()/60000;if(n<minutes(times[0][0])||n>minutes(times[13][1]))return null;for(let i=0;i<14;i++){const a=minutes(times[i][0]),b=minutes(times[i][1]);if(n<=b)return {index:i,fraction:(n-a)/(b-a),gap:false};if(i<13&&n<minutes(times[i+1][0]))return {index:i,fraction:(n-b)/(minutes(times[i+1][0])-b),gap:true}}return null}
 return {defaults,validate,position};
});
