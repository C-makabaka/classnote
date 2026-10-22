// Synthetic course names and rooms; stable IDs and lesson patterns support compatibility tests.
(function(root,factory){const v=factory();if(typeof module==='object'&&module.exports)module.exports=v;else root.Seed=v})(typeof globalThis==='object'?globalThis:this,function(){
 function create(){
  const now=Date.now(),ownerId='local-'+now.toString(36)+'-'+Math.random().toString(36).slice(2);
  const lesson=(day,start,room,fromWeek=1,toWeek=17,parity='all')=>({day,start,end:start+1,room,fromWeek,toWeek,parity});
  const rows=[
   ['career','职业生涯规划','生涯规划',false,[lesson(1,9,'示例教室C1-2',2,16,'even')]],
   ['ops','管理学基础','管理学基础',false,[lesson(2,3,'教学楼B-303',1,17,'odd'),lesson(4,7,'教学楼B-402')]],
   ['policy','通识课程','通识课程',false,[lesson(2,5,'教学楼B-204',5,8)]],
   ['health','健康与急救','健康与急救',true,[lesson(2,7,'教学楼C-606'),lesson(4,3,'教学楼B-104',1,17,'odd')]],
   ['cabin','实践操作','实践操作',true,[lesson(2,9,'教学楼C-607'),lesson(3,3,'教学楼C-206',1,17,'odd')]],
   ['english','大学英语','大学英语',false,[lesson(3,7,'教学楼B-402')]],
   ['writing','大学写作','大学写作',false,[lesson(4,9,'示例教室A1-2')]]
  ];
  return {schemaVersion:1,ownerId,settings:{semesterStart:'2026-09-07',weeks:17,semesterName:'2026–2027学年 第一学期'},courses:rows.map(([id,name,shortName,uniform,lessons])=>({id,name,shortName,uniform,lessons,teacher:'',requirements:'',grading:'',remarks:'',archived:false,ownerId,spaceId:'personal',revision:1,updatedAt:now,deletedAt:null})),tasks:[],notes:[]};
 }
 return {create};
});
