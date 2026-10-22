/* PhotoSwipe 5.4.4: immediate thumbnail display, deferred original decoding. */
(()=>{
 'use strict';
 const overlay=document.querySelector('#overlay'),dimensions=new Map();
 let viewer=null,session=0,closing=false,origin,bodyOverflow,background=[],blockClickUntil=0;
 const active=()=>!overlay.hidden;
 function size(item){
  if(item.width>0&&item.height>0)return Promise.resolve({width:item.width,height:item.height});
  const src=item.msrc||Platform.media(item.name,true);
  if(!dimensions.has(src)){
   if(dimensions.size>=300)dimensions.clear();
   dimensions.set(src,new Promise(resolve=>{
    const image=new Image();image.decoding='async';
    image.onload=()=>resolve({width:image.naturalWidth,height:image.naturalHeight});
    image.onerror=()=>{dimensions.delete(src);resolve(null)};image.src=src;
   }));
  }
  return dimensions.get(src);
 }
 function release(token){
  if(token!==session)return;
  viewer=null;closing=false;overlay.hidden=true;overlay.onpointerup=null;overlay.replaceChildren();overlay.style.background='';
  document.body.style.overflow=bodyOverflow;
  for(const [el,wasInert]of background)el.inert=wasInert;background=[];
  Platform.immersive?.(false);
  if(origin?.isConnected)origin.focus({preventScroll:true});
 }
 function close(){
  if(!active())return false;if(closing)return true;
  closing=true;blockClickUntil=performance.now()+900;
  if(!viewer){session++;release(session);return true}
  viewer.close();return true;
 }
 async function open(items,start=0,button){
  if(!items.length||active())return;
  const token=++session,index=Math.max(0,Math.min(items.length-1,start));origin=button||document.activeElement;
  bodyOverflow=document.body.style.overflow;document.body.style.overflow='hidden';
  background=[...document.body.children].filter(el=>el!==overlay&&el.tagName!=='SCRIPT').map(el=>[el,el.inert]);for(const [el]of background)el.inert=true;
  closing=false;overlay.hidden=false;overlay.className='photo-viewer';overlay.tabIndex=-1;overlay.setAttribute('role','dialog');overlay.setAttribute('aria-modal','true');overlay.setAttribute('aria-label','查看照片');overlay.style.background='#000';overlay.focus({preventScroll:true});Platform.immersive?.(true);
  try{
   // A loaded list thumbnail already has the correct EXIF-adjusted aspect ratio.
   // Only the selected image may need measuring; neighbours never delay opening.
   const first=items[index].width>0&&items[index].height>0?items[index]:await size(items[index]);
   if(token!==session||!active()||closing)return;
   if(!first)throw Error('照片无法读取');
   const data=items.map((item,i)=>{
    const measured=i===index?first:item,msrc=item.msrc||Platform.media(item.name,true);
    return {src:msrc,msrc,alt:item.label||'课堂照片',width:measured.width||1,height:measured.height||1,measured:measured.width>0&&measured.height>0};
   });
   const pswp=new PhotoSwipe({
    dataSource:data,index,appendToEl:overlay,
    loop:false,spacing:.08,bgOpacity:1,preload:[1,1],allowPanToNext:true,
    close:false,zoom:false,arrowPrev:false,arrowNext:false,counter:false,preloader:false,
    tapAction:'close',doubleTapAction:'zoom',imageClickAction:'close',bgClickAction:'close',
    pinchToClose:false,closeOnVerticalDrag:true,wheelToZoom:true,
    initialZoomLevel:'fit',secondaryZoomLevel:z=>z.fit*2.5,maxZoomLevel:z=>z.fit*5,
    showHideAnimationType:'none',showAnimationDuration:0,hideAnimationDuration:0,zoomAnimationDuration:180,
    returnFocus:false,trapFocus:true,errorMsg:'照片暂时无法显示，轻点返回'
   });
   viewer=pswp;
   // The thumbnail is already the display source; a second placeholder adds work.
   pswp.addFilter('useContentPlaceholder',()=>false);
   const valid=()=>token===session&&viewer===pswp&&!closing;
   const measuring=new Set(),originals=new Map();let originalTimer=0;
   pswp.on('contentLoad',event=>{
    const {content}=event,i=content.index;
    if(data[i].measured)return;
    event.preventDefault();if(measuring.has(i))return;measuring.add(i);
    size(items[i]).then(result=>{
     if(!valid())return;
     Object.assign(data[i],result||{width:1,height:1},{measured:true});
     pswp.refreshSlideContent(i);
    });
   });
   pswp.on('contentLoadImage',({content})=>{if(content.element)content.element.decoding='async'});
   function upgrade(){
    clearTimeout(originalTimer);
    const slide=pswp.currSlide;
    if(!valid()||!slide||slide.currZoomLevel<=slide.zoomLevels.fit*1.05)return;
    // Never decode/upload a large original while fingers are moving.
    if(pswp.gestures.isDragging||pswp.gestures.isZooming||pswp.mainScroll.isShifted())return;
    originalTimer=setTimeout(()=>{
     if(!valid()||pswp.currSlide!==slide||pswp.gestures.isDragging||pswp.gestures.isZooming)return;
     const i=slide.index,cached=originals.get(i);
     if(cached){if(cached.ready&&slide.content.element)slide.content.element.src=cached.image.src;return}
     const image=new Image(),entry={image,ready:false};image.decoding='async';originals.set(i,entry);
     image.onload=async()=>{
      try{if(image.decode)await image.decode()}catch{return}
      if(!valid()||originals.get(i)!==entry)return;entry.ready=true;upgrade();
     };
     image.onerror=()=>{entry.failed=true};image.src=Platform.media(items[i].name);
    },100);
   }
   pswp.on('zoomPanUpdate',upgrade);
   pswp.on('pointerDown',()=>clearTimeout(originalTimer));
   pswp.on('pointerUp',event=>{
    const g=pswp.gestures;
    if(!event.originalEvent.type.includes('cancel')&&g._numActivePoints===0&&!g.isDragging&&!g.isZooming&&!g.isMultitouch&&!pswp.mainScroll.isShifted()&&!event.originalEvent.target.closest('.pswp__img')){
     event.preventDefault();close();return;
    }
    // PhotoSwipe finishes its pointer-up state changes after this event.
    clearTimeout(originalTimer);originalTimer=setTimeout(upgrade,0);
   });
   pswp.on('change',()=>{
    clearTimeout(originalTimer);
    // Keep decoded originals only for the current image, even in a 100-photo record.
    for(const [i,entry]of originals){if(i===pswp.currIndex)continue;entry.image.onload=null;entry.image.onerror=null;entry.image.src='';const content=pswp.contentLoader.getContentByIndex(i);if(content?.element)content.element.src=data[i].src;originals.delete(i)}
    overlay.setAttribute('aria-label',`查看照片，第 ${pswp.currIndex+1} 张，共 ${items.length} 张。单击退出，双击缩放，左右滑动切换。`);
    // refreshSlideContent also emits change for a late neighbour. Re-arm the
    // current image's upgrade instead of cancelling it until another gesture.
    upgrade();
   });
   pswp.on('verticalDrag',event=>{
    const slide=pswp.currSlide;
    if(slide&&event.panY<slide.bounds.center.y){event.preventDefault();slide.panTo(slide.pan.x,slide.bounds.center.y);pswp.applyBgOpacity(1)}
   });
   pswp.on('close',()=>{closing=true;blockClickUntil=performance.now()+900;clearTimeout(originalTimer)});
   pswp.on('destroy',()=>{
    clearTimeout(originalTimer);for(const entry of originals.values()){entry.image.onload=null;entry.image.onerror=null}originals.clear();
    queueMicrotask(()=>release(token));
   });
   pswp.init();overlay.style.background='transparent';
  }catch(error){
   if(token!==session)return;
   const message=document.createElement('p');message.className='photo-error';message.textContent='照片暂时无法显示，轻点返回';overlay.replaceChildren(message);
   overlay.onpointerup=()=>close();
  }
 }
 document.addEventListener('pointerdown',()=>{if(!active())blockClickUntil=0},true);
 for(const type of ['click','dblclick','contextmenu'])document.addEventListener(type,e=>{
  if(active()||performance.now()<blockClickUntil){e.preventDefault();e.stopImmediatePropagation()}
 },true);
 document.addEventListener('keydown',e=>{
  if(!active())return;
  if(e.key==='Escape'){e.preventDefault();e.stopImmediatePropagation();close()}
  else if(e.key==='Tab'){e.preventDefault();(viewer?.element||overlay).focus({preventScroll:true})}
 },true);
 window.PhotoViewer={open,close};
})();
