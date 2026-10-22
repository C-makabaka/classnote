package cn.cbhcool.classnote;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import android.view.*;
import android.webkit.*;
import android.widget.FrameLayout;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
 private WebView web;private FrameLayout container;private AppStore store;private MediaStore media;private final ExecutorService io=Executors.newSingleThreadExecutor();
 private static final int PICK=10,CAMERA=11,EXPORT=12,IMPORT=13,PICK_FILES=14;private boolean ready=false,photoImmersive=false;private ProgressDialog activeProgress;
 @Override public void onCreate(Bundle state){super.onCreate(state);store=new AppStore(this);media=new MediaStore(this);getWindow().setStatusBarColor(Color.rgb(255,249,231));getWindow().setNavigationBarColor(Color.rgb(255,251,238));getWindow().getDecorView().setSystemUiVisibility(stableLayoutFlags()|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  web=new WebView(this);web.setBackgroundColor(Color.rgb(255,249,231));container=new FrameLayout(this);container.setBackgroundColor(Color.rgb(255,249,231));container.addView(web,new FrameLayout.LayoutParams(-1,-1));setContentView(container);
  if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
  container.setOnApplyWindowInsetsListener((v,insets)->{
   int left,top,right,bottom;
   if(Build.VERSION.SDK_INT>=30){
    // Reserve the same viewport when bars are visible, hidden or transient.
    android.graphics.Insets bars=insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
    android.graphics.Insets keyboard=insets.getInsets(WindowInsets.Type.ime());
    left=bars.left;top=bars.top;right=bars.right;bottom=Math.max(bars.bottom,photoImmersive?0:keyboard.bottom);
   }else{
    left=insets.getStableInsetLeft();top=insets.getStableInsetTop();right=insets.getStableInsetRight();bottom=insets.getStableInsetBottom();
    if(!photoImmersive)bottom=Math.max(bottom,insets.getSystemWindowInsetBottom());
    if(Build.VERSION.SDK_INT>=28&&insets.getDisplayCutout()!=null){DisplayCutout cutout=insets.getDisplayCutout();left=Math.max(left,cutout.getSafeInsetLeft());top=Math.max(top,cutout.getSafeInsetTop());right=Math.max(right,cutout.getSafeInsetRight());bottom=Math.max(bottom,cutout.getSafeInsetBottom());}
   }
   v.setPadding(left,top,right,bottom);return insets;
  });
  WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(false);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setBlockNetworkLoads(true);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);settings.setSupportZoom(false);if(Build.VERSION.SDK_INT>=29)settings.setForceDark(WebSettings.FORCE_DARK_OFF);
  web.setWebChromeClient(new WebChromeClient());web.addJavascriptInterface(new Bridge(),"Native");
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest request){return true;}
   @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest request){Uri uri=request.getUrl();try{if(!"https".equals(uri.getScheme())||!"app.local".equals(uri.getHost())||!"GET".equals(request.getMethod()))return denied();String path=uri.getPath();
    if(path.startsWith("/media/")){String name=path.substring(7);boolean thumb="1".equals(uri.getQueryParameter("thumb"));File f=media.file(name,thumb);String mime=thumb?"image/jpeg":name.endsWith("png")?"image/png":name.endsWith("webp")?"image/webp":name.endsWith("gif")?"image/gif":name.endsWith("heic")?"image/heic":name.endsWith("heif")?"image/heif":"image/jpeg";return response(mime,new FileInputStream(f),true);}
    String asset=path.equals("/")?"index.html":path.substring(1);if(!AssetPolicy.isAllowed(asset))return denied();String mime=asset.endsWith("js")?"application/javascript":asset.endsWith("css")?"text/css":"text/html";return response(mime,getAssets().open(asset));
   }catch(Exception e){return denied();}}
   @Override public void onPageFinished(WebView v,String url){ready=true;deliverMedia();openNotificationTask();}
  });
  web.loadUrl("https://app.local/");rebuild();
 }
 private int stableLayoutFlags(){return View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;}
 private void applyPhotoImmersive(){
  container.setBackgroundColor(photoImmersive?Color.BLACK:Color.rgb(255,249,231));
  getWindow().setStatusBarColor(photoImmersive?Color.BLACK:Color.rgb(255,249,231));getWindow().setNavigationBarColor(photoImmersive?Color.BLACK:Color.rgb(255,251,238));
  View decor=getWindow().getDecorView();
  if(Build.VERSION.SDK_INT>=30){
   WindowInsetsController controller=getWindow().getInsetsController();
   if(controller!=null){
    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    controller.setSystemBarsAppearance(photoImmersive?0:(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS),WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
    if(photoImmersive){controller.hide(WindowInsets.Type.systemBars());controller.hide(WindowInsets.Type.ime());}
    else controller.show(WindowInsets.Type.systemBars());
   }
  }else{
   decor.setSystemUiVisibility(stableLayoutFlags()|(photoImmersive?(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION):(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)));
  }
  decor.requestApplyInsets();
 }
 @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused&&photoImmersive)applyPhotoImmersive();}
 private WebResourceResponse response(String mime,InputStream in){return response(mime,in,false);}
 private WebResourceResponse response(String mime,InputStream in,boolean image){Map<String,String>headers=new HashMap<>();headers.put("Cache-Control",image?"private, max-age=3600":"no-store");headers.put("X-Content-Type-Options","nosniff");return new WebResourceResponse(mime,"UTF-8",200,"OK",headers,in);}
 private WebResourceResponse denied(){return new WebResourceResponse("text/plain","UTF-8",404,"Not Found",Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));}
 private void event(String type,Object value){runOnUiThread(()->{if(web==null||!ready)return;web.evaluateJavascript("window.onNativeEvent&&window.onNativeEvent("+JSONObject.quote(type)+","+(value instanceof JSONArray||value instanceof JSONObject?value.toString():JSONObject.quote(String.valueOf(value)))+")",null);});}
 private void rebuild(){io.execute(()->{try{AlarmScheduler.rebuild(this);}catch(Exception e){event("message","记录已保存，但提醒设置失败："+e.getMessage());}});}
 private SharedPreferences prefs(){return getSharedPreferences("drafts",0);}
 private void deliverMedia(){if(!ready||web==null)return;for(String key:new String[]{"pending-media","pending-attachments"}){String raw=prefs().getString(key,"");if(!raw.isEmpty())try{event(key.equals("pending-media")?"media":"attachments",new JSONObject(raw));}catch(Exception e){event("message","导入结果读取失败");}}}
 private void openNotificationTask(){String id=getIntent().getStringExtra("taskId");if(id!=null){event("open-task",id);getIntent().removeExtra("taskId");}}
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(ready)openNotificationTask();}
 @Override protected void onResume(){super.onResume();if(web!=null){event("resume","");rebuild();}}
 @Override public void onBackPressed(){web.evaluateJavascript("window.appBack&&window.appBack()",null);}
 @Override protected void onDestroy(){ready=false;if(activeProgress!=null&&activeProgress.isShowing())activeProgress.dismiss();activeProgress=null;super.onDestroy();io.shutdown();if(web!=null){web.removeJavascriptInterface("Native");web.destroy();web=null;}}
 private void launch(Intent intent,int request){runOnUiThread(()->{try{startActivityForResult(intent,request);}catch(ActivityNotFoundException e){event("message","手机上没有能完成此操作的应用");}});}
 private interface Work{Object run()throws Exception;}
 private String answer(Work work){try{return new JSONObject().put("ok",true).put("value",work.run()).toString();}catch(Exception e){try{return new JSONObject().put("ok",false).put("error",e.getMessage()==null?"操作失败":e.getMessage()).toString();}catch(Exception ignored){return "{\"ok\":false,\"error\":\"操作失败\"}";}}}
 public final class Bridge {
  private String loadedState=null;
  @JavascriptInterface public String load(){return answer(()->{synchronized(AppStore.LOCK){loadedState=store.load();return loadedState;}});}
  @JavascriptInterface public String save(String raw){return answer(()->{synchronized(AppStore.LOCK){if(!Objects.equals(loadedState,store.load()))throw new IOException("资料已在其他页面或恢复操作中更新，请退出并重新打开应用再编辑");store.save(raw);loadedState=raw;}rebuild();return "saved";});}
  @JavascriptInterface public String saveDraft(String raw){return answer(()->{synchronized(AppStore.LOCK){if(raw.length()>1024*1024)throw new IOException("草稿过大");SharedPreferences.Editor edit=prefs().edit().putString("draft",raw);String pending=prefs().getString("pending-attachments","");if(!pending.isEmpty()&&!AttachmentFiles.belongsToDraft(pending,raw))edit.remove("pending-attachments");if(raw.isEmpty())edit.remove("files-target");if(!edit.commit())throw new IOException("无法保存草稿");}return "saved";});}
  @JavascriptInterface public String loadDraft(){return answer(()->prefs().getString("draft",""));}
  @JavascriptInterface public String takeMedia(){return answer(()->prefs().getString("pending-media","null"));}
  @JavascriptInterface public String ackMedia(String payload){return answer(()->{synchronized(AppStore.LOCK){String pending=prefs().getString("pending-media","");if(pending.equals(payload)&&!prefs().edit().remove("pending-media").commit())throw new IOException("无法确认照片保存状态");}return "acknowledged";});}
  private void targetDraft()throws Exception{String raw=prefs().getString("draft","");JSONObject draft=new JSONObject(raw);if(!prefs().edit().putString("media-target",draft.getJSONObject("data").getString("id")).commit())throw new IOException("无法保存图片关联");}
  @JavascriptInterface public String pickImages(){return answer(()->{targetDraft();Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);launch(i,PICK);return "opened";});}
  @JavascriptInterface public String pickFiles(){return answer(()->{
   if(!prefs().getString("pending-attachments","").isEmpty())throw new IOException("请先恢复对应草稿，接收上次导入的文件");
   JSONObject draft=new JSONObject(prefs().getString("draft",""));if(!"note".equals(draft.getString("kind")))throw new IOException("请在课堂笔记中添加文件");
   if(!prefs().edit().putString("files-target",draft.getJSONObject("data").getString("id")).commit())throw new IOException("无法保存附件关联");
   launch(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true),PICK_FILES);return "opened";
  });}
  @JavascriptInterface public String takeFiles(){return answer(()->prefs().getString("pending-attachments","null"));}
  @JavascriptInterface public String ackFiles(String payload){return answer(()->{synchronized(AppStore.LOCK){String pending=prefs().getString("pending-attachments","");if(!pending.isEmpty()){if(!AttachmentFiles.acknowledges(pending,payload))throw new IOException("附件批次已更新，请恢复对应草稿");if(!prefs().edit().remove("pending-attachments").commit())throw new IOException("无法确认附件保存状态");}}return "acknowledged";});}
  @JavascriptInterface public String openAttachment(String name){return answer(()->{
   if(!AttachmentFiles.name(name))throw new IOException("附件路径无效");JSONObject descriptor=null;
   synchronized(AppStore.LOCK){String raw=store.load();if(!raw.isEmpty()){JSONArray notes=new JSONObject(raw).getJSONArray("notes");for(int i=0;i<notes.length();i++)if(notes.getJSONObject(i).isNull("deletedAt")){JSONArray files=notes.getJSONObject(i).optJSONArray("attachments");if(files!=null)for(int j=0;j<files.length();j++)if(name.equals(files.getJSONObject(j).getString("name")))descriptor=files.getJSONObject(j);}}
    String draftRaw=prefs().getString("draft","");if(!draftRaw.isEmpty()){JSONObject draft=new JSONObject(draftRaw);if("note".equals(draft.optString("kind"))){JSONArray files=draft.getJSONObject("data").optJSONArray("attachments");if(files!=null)for(int j=0;j<files.length();j++)if(name.equals(files.getJSONObject(j).getString("name")))descriptor=files.getJSONObject(j);}}}
   if(descriptor==null)throw new IOException("找不到附件对应的笔记或草稿");Intent intent=new AttachmentStore(MainActivity.this).viewIntent(descriptor);
   runOnUiThread(()->{try{startActivity(intent);}catch(ActivityNotFoundException e){event("message","手机上没有能打开此类文件的应用，请安装对应阅读器后重试");}catch(Exception e){event("message","打开文件失败："+e.getMessage());}});return "opened";
  });}
  @JavascriptInterface public String camera(){return answer(()->{targetDraft();Uri uri=Uri.parse("content://"+getPackageName()+".camera/capture.jpg");new File(getCacheDir(),"capture.jpg").delete();Intent i=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).putExtra(android.provider.MediaStore.EXTRA_OUTPUT,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);i.setClipData(ClipData.newRawUri("photo",uri));launch(i,CAMERA);return "opened";});}
  @JavascriptInterface public String calendar(String id){return answer(()->{JSONObject t=store.task(id);if(t==null||t.isNull("dueAt"))throw new IOException("请先设置作业截止时间");String courseName="";JSONArray cs=new JSONObject(store.load()).getJSONArray("courses");for(int i=0;i<cs.length();i++)if(cs.getJSONObject(i).getString("id").equals(t.getString("courseId")))courseName=cs.getJSONObject(i).getString("name");long end=t.getLong("dueAt");Intent intent=new Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI).putExtra(CalendarContract.Events.TITLE,courseName+" · "+t.getString("title")).putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,end).putExtra(CalendarContract.EXTRA_EVENT_END_TIME,end+15*60000).putExtra(CalendarContract.Events.DESCRIPTION,"第"+t.getInt("number")+"次作业\n截止时间见日程开始时间\n"+t.optString("description")+"\n提交方式："+t.optString("submission")+"\n"+t.optString("remarks"));launch(intent,20);return "opened";});}
  @JavascriptInterface public String copyText(String value){return answer(()->{if(value==null||value.length()>100000)throw new IOException("复制内容过长");getSystemService(android.content.ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("课表导入提示词",value));return "copied";});}
  @JavascriptInterface public String backup(){return answer(()->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/zip").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"课记备份-"+java.time.LocalDate.now()+".zip");launch(i,EXPORT);return "opened";});}
  @JavascriptInterface public String restore(){return answer(()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);launch(i,IMPORT);return "opened";});}
  @JavascriptInterface public String permissions(){return answer(()->new JSONObject().put("notifications",getSystemService(NotificationManager.class).areNotificationsEnabled()).put("exact",Build.VERSION.SDK_INT<31||getSystemService(AlarmManager.class).canScheduleExactAlarms()).toString());}
  @JavascriptInterface public String requestNotifications(){return answer(()->{runOnUiThread(()->{if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},40);else startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));});return "opened";});}
  @JavascriptInterface public String requestExact(){return answer(()->{if(Build.VERSION.SDK_INT>=31)runOnUiThread(()->{try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}catch(ActivityNotFoundException e){event("message","系统未提供精确提醒设置");}});return "opened";});}
  @JavascriptInterface public String immersive(boolean enabled){return answer(()->{runOnUiThread(()->{photoImmersive=enabled;applyPhotoImmersive();});return "updated";});}
  @JavascriptInterface public String exit(){return answer(()->{runOnUiThread(()->finish());return "closed";});}
 }
 @Override public void onRequestPermissionsResult(int request,String[]permissions,int[]results){super.onRequestPermissionsResult(request,permissions,results);rebuild();event("resume","");}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK){if(request==CAMERA)revokeUriPermission(Uri.parse("content://"+getPackageName()+".camera/capture.jpg"),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);return;}
  ArrayList<Uri>uris=new ArrayList<>();if(request==CAMERA)uris.add(Uri.parse("content://"+getPackageName()+".camera/capture.jpg"));else if(data!=null){if(data.getClipData()!=null)for(int i=0;i<data.getClipData().getItemCount();i++)uris.add(data.getClipData().getItemAt(i).getUri());else if(data.getData()!=null)uris.add(data.getData());}
  if(uris.isEmpty()||request==20)return;
  ProgressDialog progress=new ProgressDialog(this);activeProgress=progress;progress.setMessage(request==IMPORT?"正在校验并恢复备份…":request==EXPORT?"正在导出完整备份…":request==PICK_FILES?"正在保存文件…":"正在保存照片…");progress.setCancelable(false);progress.show();
  final String targetDraft=prefs().getString("media-target","");
  final String filesTarget=prefs().getString("files-target","");
  io.execute(()->{try{
   if(request==PICK||request==CAMERA){if(uris.size()>100)throw new IOException("每次最多导入100张照片");JSONArray photos=new JSONArray();String failure=null;for(Uri uri:uris){try{photos.put(media.importUri(uri));}catch(Exception e){failure=e.getMessage();break;}}if(photos.length()>0){JSONObject payload=new JSONObject().put("draftId",targetDraft).put("photos",photos);if(!prefs().edit().putString("pending-media",payload.toString()).commit())throw new IOException("无法保存照片关联结果，请暂勿退出");runOnUiThread(()->deliverMedia());}if(failure!=null)event("message","部分照片未导入："+failure);}
   else if(request==PICK_FILES){synchronized(AppStore.LOCK){
    JSONObject draft=new JSONObject(prefs().getString("draft",""));JSONObject dataDraft=draft.getJSONObject("data");JSONArray existing=dataDraft.optJSONArray("attachments");
    if(!filesTarget.equals(dataDraft.getString("id")))throw new IOException("草稿已更改，请恢复原草稿后重新选择文件");
    if(uris.size()+(existing==null?0:existing.length())>100)throw new IOException("每条笔记最多100个文件附件，请减少选择的文件");
    AttachmentStore attachments=new AttachmentStore(MainActivity.this);JSONArray files=new JSONArray();String failure=null;
    for(Uri uri:uris){try{files.put(attachments.importUri(uri));}catch(Exception e){failure=e.getMessage();break;}}
    if(files.length()>0){JSONObject payload=new JSONObject().put("batchId",UUID.randomUUID().toString()).put("draftId",filesTarget).put("attachments",files);if(!prefs().edit().putString("pending-attachments",payload.toString()).commit())throw new IOException("无法保存附件关联结果，请暂勿退出");runOnUiThread(()->deliverMedia());}
    if(failure!=null)event("message",(files.length()>0?"部分文件未导入：":"文件未导入：")+failure);
   }}
   else if(request==EXPORT){try(OutputStream out=getContentResolver().openOutputStream(uris.get(0),"wt")){if(out==null)throw new IOException("无法写入备份文件");store.backup(out);}event("message","完整备份已导出");}
   else if(request==IMPORT){try(InputStream in=getContentResolver().openInputStream(uris.get(0))){if(in==null)throw new IOException("无法读取备份");store.restore(in);}prefs().edit().clear().commit();event("restored","");try{AlarmScheduler.rebuild(this);}catch(Exception e){event("message","备份已恢复，但提醒未能重设，请检查权限后重新打开应用。");}}
  }catch(Exception e){event("message","操作未完成："+e.getMessage());}finally{if(request==CAMERA){revokeUriPermission(uris.get(0),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);new File(getCacheDir(),"capture.jpg").delete();}runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()&&progress.isShowing())progress.dismiss();if(activeProgress==progress)activeProgress=null;});}});
 }
}
