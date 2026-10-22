(function(root,factory){const v=factory();if(typeof module==='object'&&module.exports)module.exports=v;else root.Seed=v})(typeof globalThis==='object'?globalThis:this,function(){
 function create(){const now=new Date(),monday=new Date(now);monday.setDate(now.getDate()-((now.getDay()+6)%7));const start=`${monday.getFullYear()}-${String(monday.getMonth()+1).padStart(2,'0')}-${String(monday.getDate()).padStart(2,'0')}`;return {schemaVersion:1,ownerId:'local-'+Date.now().toString(36)+'-'+Math.random().toString(36).slice(2),settings:{semesterStart:start,weeks:20,semesterName:'我的学期'},courses:[],tasks:[],notes:[]}}
 return {create};
});
