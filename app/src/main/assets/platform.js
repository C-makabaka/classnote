(function(){
 'use strict';const native=typeof Native!=='undefined',mediaSession=Date.now().toString(36);let mediaGeneration=0;
 function call(name,...args){if(!native)throw Error('此功能请在安卓应用中使用');const r=JSON.parse(Native[name](...args));if(!r.ok)throw Error(r.error||'操作失败');return r.value}
 function readLocal(key){return localStorage.getItem(key)}
 window.Platform={
  native,
  immersive(enabled){if(native&&typeof Native.immersive==='function')call('immersive',enabled)},
  async copyText(value){if(native){call('copyText',value);return}if(navigator.clipboard){try{await navigator.clipboard.writeText(value);return}catch{}}const el=document.createElement('textarea');el.value=value;el.style.position='fixed';el.style.opacity='0';document.body.appendChild(el);el.select();const ok=document.execCommand('copy');el.remove();if(!ok)throw Error('复制失败，请手动复制')},
  load(){const raw=native?call('load'):readLocal('classnote-state');return raw?JSON.parse(raw):null},
  save(s){const raw=JSON.stringify(s);if(native)call('save',raw);else localStorage.setItem('classnote-state',raw)},
  draft(value){if(arguments.length){const raw=value?JSON.stringify(value):'';if(native)call('saveDraft',raw);else localStorage.setItem('classnote-draft',raw);return}const raw=native?call('loadDraft'):readLocal('classnote-draft');return raw?JSON.parse(raw):null},
  invalidateMedia(){mediaGeneration++},
  media(name,thumb=false){return native?'https://app.local/media/'+encodeURIComponent(name)+'?v='+mediaSession+'-'+mediaGeneration+(thumb?'&thumb=1':''):readLocal('media-'+name)||''},
  pickImages(camera=false){if(native){call(camera?'camera':'pickImages');return}if(camera)throw Error('请在安卓应用中拍照');const input=document.createElement('input');input.type='file';input.accept='image/*';input.multiple=true;input.onchange=async()=>{const photos=[];for(const f of input.files){const name=Core.uuid()+'.'+(f.type==='image/png'?'png':'jpg');const data=await new Promise((resolve,reject)=>{const r=new FileReader();r.onload=()=>resolve(r.result);r.onerror=reject;r.readAsDataURL(f)});localStorage.setItem('media-'+name,data);photos.push({name,label:f.name})}window.onNativeEvent('media',photos)};input.click()},
  pendingMedia(){return native?JSON.parse(call('takeMedia')||'null'):null},
  pickFiles(){
   if(native){call('pickFiles');return}
   const draft=Platform.draft();if(!draft||draft.kind!=='note')throw Error('请在课堂笔记中添加文件');
   const input=document.createElement('input');input.type='file';input.multiple=true;
   input.onchange=async()=>{const attachments=[];let failure;
    try{if((draft.data.attachments||[]).length+input.files.length>100)throw Error('每条笔记最多100个文件附件');
     for(const file of input.files){if(file.size>64*1024*1024)throw Error('单个文件不能超过64MB');const name=Core.uuid()+'.bin';
      const data=await new Promise((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(reader.result);reader.onerror=()=>reject(Error('无法读取文件'));reader.readAsDataURL(file)});
      localStorage.setItem('attachment-'+name,data);const type=/^[a-z0-9][a-z0-9!#$&^_.+-]*\/[a-z0-9][a-z0-9!#$&^_.+-]*$/.test(file.type)&&file.type.length<=120?file.type:'application/octet-stream';
      attachments.push({name,label:(file.name.trim()||'文件附件').slice(0,300),size:file.size,type});
     }
    }catch(error){failure=error.message||'文件保存失败'}
    if(attachments.length)window.onNativeEvent('attachments',{draftId:draft.data.id,attachments});if(failure)window.onNativeEvent('message',failure);
   };input.click();
  },
  pendingFiles(){return native?JSON.parse(call('takeFiles')||'null'):null},
  ackFiles(payload){if(native)call('ackFiles',JSON.stringify(payload))},
  openFile(file){if(native){call('openAttachment',file.name);return}const data=readLocal('attachment-'+file.name);if(!data)throw Error('附件不存在');const link=document.createElement('a');link.href=data;link.download=file.label;document.body.append(link);link.click();link.remove()},
  ackMedia(payload){if(native)call('ackMedia',JSON.stringify(payload))},
  calendar(id){return call('calendar',id)},
  backup(){return call('backup')},restore(){return call('restore')},
  permissions(){return native?JSON.parse(call('permissions')):{notifications:false,exact:false,browser:true}},
  requestNotifications(){return call('requestNotifications')},requestExact(){return call('requestExact')},
  exit(){if(native)call('exit')}
 };
})();
