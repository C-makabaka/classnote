package cn.cbhcool.classnote;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import org.json.*;
import java.util.*;

final class AlarmScheduler {
 static final String CHANNEL="homework";
 static void channel(Context c){NotificationManager n=c.getSystemService(NotificationManager.class);NotificationChannel ch=new NotificationChannel(CHANNEL,"作业截止提醒",NotificationManager.IMPORTANCE_HIGH);ch.setDescription("你设置的作业提交提醒");n.createNotificationChannel(ch);}
 static PendingIntent pending(Context c,String id){Intent i=new Intent(c,ReminderReceiver.class).setAction("cn.cbhcool.classnote.REMIND").setData(Uri.parse("classnote://task/"+Uri.encode(id))).putExtra("taskId",id);return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static String token(JSONObject task){return task.optLong("dueAt")+":"+task.optLong("reminderMinutes");}
 static void rebuild(Context c)throws Exception{synchronized(AppStore.LOCK){
  channel(c);AlarmManager am=c.getSystemService(AlarmManager.class);NotificationManager nm=c.getSystemService(NotificationManager.class);SharedPreferences p=c.getSharedPreferences("reminders",0);Set<String>old=p.getStringSet("scheduled",Collections.emptySet());
  for(String id:old)am.cancel(pending(c,id));
  String raw=new AppStore(c).load();Set<String>next=new HashSet<>();if(!raw.isEmpty()){
   JSONArray tasks=new JSONObject(raw).getJSONArray("tasks");long now=System.currentTimeMillis();
   for(int i=0;i<tasks.length();i++){JSONObject t=tasks.getJSONObject(i);String id=t.getString("id");
    if(!t.isNull("deletedAt")||!t.isNull("completedAt")){nm.cancel(id,0);continue;}
    if(t.isNull("dueAt")||t.isNull("reminderMinutes")||t.getLong("dueAt")<=now||token(t).equals(p.getString("fired:"+id,"")))continue;
    long when=Math.max(now+1000,t.getLong("dueAt")-t.getLong("reminderMinutes")*60000);PendingIntent pi=pending(c,id);
    if(Build.VERSION.SDK_INT<31||am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);next.add(id);
   }
  }
  p.edit().putStringSet("scheduled",next).apply();
 }}
}
