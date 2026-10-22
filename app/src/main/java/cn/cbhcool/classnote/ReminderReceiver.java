package cn.cbhcool.classnote;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import org.json.*;

public final class ReminderReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context,Intent intent){PendingResult result=goAsync();new Thread(()->{try{
  if(!"cn.cbhcool.classnote.REMIND".equals(intent.getAction())){AlarmScheduler.rebuild(context);return;}
  String id=intent.getStringExtra("taskId");if(id==null)return;JSONObject t=new AppStore(context).task(id);
  if(t==null||!t.isNull("completedAt")||t.isNull("dueAt")||t.isNull("reminderMinutes"))return;
  long target=t.getLong("dueAt")-t.getLong("reminderMinutes")*60000;if(System.currentTimeMillis()+500<target){AlarmScheduler.rebuild(context);return;}
  SharedPreferences p=context.getSharedPreferences("reminders",0);String token=AlarmScheduler.token(t);if(token.equals(p.getString("fired:"+id,"")))return;
  NotificationManager nm=context.getSystemService(NotificationManager.class);if(!nm.areNotificationsEnabled()||(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED))return;
  AlarmScheduler.channel(context);Intent open=new Intent(context,MainActivity.class).setData(android.net.Uri.parse("classnote://task/"+android.net.Uri.encode(id))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("taskId",id);
  PendingIntent pi=PendingIntent.getActivity(context,id.hashCode(),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  String due=new java.text.SimpleDateFormat("MM月dd日 HH:mm",java.util.Locale.CHINA).format(new java.util.Date(t.getLong("dueAt")));
  String content="第"+t.getInt("number")+"次作业 · "+due+"前提交";
  Notification n=new Notification.Builder(context,AlarmScheduler.CHANNEL).setSmallIcon(R.drawable.ic_notify).setContentTitle(t.getString("title")).setContentText(content).setStyle(new Notification.BigTextStyle().bigText(content+"\n"+t.optString("description"))).setContentIntent(pi).setAutoCancel(true).build();
  nm.notify(id,0,n);p.edit().putString("fired:"+id,token).apply();
 }catch(Exception e){android.util.Log.e("ClassNote","Reminder failed",e);}finally{result.finish();}},"classnote-reminder").start();}
}
