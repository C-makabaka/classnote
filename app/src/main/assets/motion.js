(()=>{
 const reduced=()=>matchMedia('(prefers-reduced-motion: reduce)').matches;
 function close(dialog,done){if(!dialog||dialog.dataset.closing)return;dialog.dataset.closing='true';const finish=()=>{if(dialog.open)dialog.close();dialog.remove();if(done)done()};if(reduced()){finish();return}dialog.classList.add('closing');let finished=false;const once=()=>{if(!finished){finished=true;finish()}};dialog.addEventListener('animationend',e=>{if(e.target===dialog)once()},{once:true});setTimeout(once,180)}
 function page(view){if(!reduced()&&view.animate)view.animate([{opacity:.35,transform:'translateY(5px)'},{opacity:1,transform:'translateY(0)'}],{duration:160,easing:'ease-out'})}
 window.Motion={close,page};
})();
