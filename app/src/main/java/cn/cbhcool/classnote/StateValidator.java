package cn.cbhcool.classnote;
import org.json.*;
import java.io.*;
import java.time.*;
import java.util.*;

final class StateValidator {
 static void require(boolean ok,String message)throws IOException{if(!ok)throw new IOException(message);}
 // Match ECMAScript trim whitespace so restored notes obey the same content rule as the UI.
 static boolean hasContent(String value){return !value.matches("[\\x09-\\x0D\\x20\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]*");}
 static long integer(JSONObject o,String key)throws Exception{Object value=o.get(key);require(value instanceof Number,"整数字段类型无效："+key);double number=((Number)value).doubleValue();require(!Double.isNaN(number)&&!Double.isInfinite(number)&&Math.abs(number)<=9007199254740991L&&number==Math.rint(number),"整数字段无效："+key);return ((Number)value).longValue();}
 static String text(JSONObject o,String key,int max,boolean optional)throws Exception{if(optional&&(!o.has(key)||o.isNull(key)))return "";Object value=o.get(key);require(value instanceof String&&((String)value).length()<=max,"文字字段无效："+key);return (String)value;}
 static LocalDate date(JSONObject o,String key)throws Exception{String value=text(o,key,10,false);require(value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"),"日期格式无效");return LocalDate.parse(value);}
 static JSONObject validate(String raw,File dataDir)throws Exception{
  require(raw!=null&&raw.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=8*1024*1024,"文字记录超过8MB上限，请减少超长文字后重试（照片不计入）");JSONObject s=new JSONObject(raw);
  require(integer(s,"schemaVersion")==1,"不支持的数据版本");require(!text(s,"ownerId",100,false).isEmpty(),"用户标识无效");JSONObject settings=s.getJSONObject("settings");
  require(date(settings,"semesterStart").getDayOfWeek()==DayOfWeek.MONDAY,"第一周必须从周一开始");long weeks=integer(settings,"weeks");require(weeks>=1&&weeks<=52,"学期周数无效");
  if(settings.has("periodTimes")){JSONArray times=settings.getJSONArray("periodTimes");require(times.length()==14,"请填写全部14节课的时间");int previous=-1;for(int i=0;i<14;i++){JSONArray pair=times.getJSONArray(i);require(pair.length()==2,"节次时间格式无效");String a=pair.getString(0),b=pair.getString(1);require(a.matches("([01][0-9]|2[0-3]):[0-5][0-9]")&&b.matches("([01][0-9]|2[0-3]):[0-5][0-9]"),"节次时间无效");int startMinute=LocalTime.parse(a).toSecondOfDay()/60,endMinute=LocalTime.parse(b).toSecondOfDay()/60;require(startMinute>=previous&&endMinute>startMinute,"节次时间重叠或顺序错误");previous=endMinute;}}
  Set<String>ids=new HashSet<>(),courses=new HashSet<>();
  for(String key:new String[]{"courses","tasks","notes"}){
   JSONArray list=s.getJSONArray(key);require(list.length()<=20000,"记录数过多");
   for(int i=0;i<list.length();i++){JSONObject e=list.getJSONObject(i);String id=text(e,"id",100,false);require(id.matches("[a-zA-Z0-9_-]{1,100}")&&ids.add(id),"记录标识重复或无效");require(integer(e,"revision")>=1,"记录版本无效");integer(e,"updatedAt");
    if(!e.isNull("deletedAt"))require(integer(e,"deletedAt")>0,"删除时间无效");
    if(key.equals("courses")){courses.add(id);require(!text(e,"name",200,false).trim().isEmpty(),"课程名无效");text(e,"shortName",100,true);JSONArray ls=e.getJSONArray("lessons");require(ls.length()<=100,"课表时段过多");for(int j=0;j<ls.length();j++){JSONObject l=ls.getJSONObject(j);long day=integer(l,"day"),start=integer(l,"start"),end=integer(l,"end"),from=integer(l,"fromWeek"),to=integer(l,"toWeek");require(day>=1&&day<=7&&start>=1&&end>=start&&end<=14&&from>=1&&to>=from&&to<=52&&Arrays.asList("all","odd","even").contains(l.getString("parity"))&&text(l,"room",200,false).length()<=200,"课表时段无效");}}
    else{
     require(courses.contains(text(e,"courseId",100,false)),"记录找不到对应课程");String title=text(e,"title",300,false);if(key.equals("tasks"))require(hasContent(title),"作业标题不能为空");long number=integer(e,"number");require(number>=1&&number<=9999,"课次无效");
     JSONArray photos=e.getJSONArray("photos");require(photos.length()<=100,"照片数量过多");for(int j=0;j<photos.length();j++){JSONObject p=photos.getJSONObject(j);String name=text(p,"name",100,false);require(SafeArchive.mediaName(name),"照片文件名无效");text(p,"label",300,true);require(new File(dataDir,"media/"+name).isFile(),"备份缺少照片："+name);}
     if(key.equals("notes")){
      JSONArray files=e.has("attachments")?e.getJSONArray("attachments"):new JSONArray();require(files.length()<=100,"每条笔记最多100个文件附件");Set<String>names=new HashSet<>();
      for(int j=0;j<files.length();j++){JSONObject f=files.getJSONObject(j);String name=text(f,"name",100,false);require(AttachmentFiles.name(name)&&names.add(name),"附件文件名重复或无效");require(hasContent(text(f,"label",300,false)),"附件名称无效");require(AttachmentFiles.type(text(f,"type",120,false)),"附件类型无效");long size=integer(f,"size");require(size>=0&&size<=AttachmentFiles.MAX_BYTES,"单个文件不能超过64MB");File stored=new File(dataDir,"attachments/"+name);require(stored.isFile()&&stored.length()==size,"备份缺少附件或文件大小不符："+name);}
      require(hasContent(title)||hasContent(text(e,"body",20000,true))||photos.length()>0||files.length()>0,"请添加照片、文件或填写笔记内容");date(e,"date");if(e.has("sourceDate"))date(e,"sourceDate");text(e,"occurrenceKey",150,true);}
     else {if(!e.isNull("dueAt"))require(integer(e,"dueAt")>0,"截止日期无效");if(!e.isNull("completedAt"))require(integer(e,"completedAt")>0,"完成时间无效");if(!e.isNull("reminderMinutes")){long offset=integer(e,"reminderMinutes");require(!e.isNull("dueAt")&&offset>=0&&offset<=525600,"提醒时间无效");}}
    }
    for(String f:new String[]{"body","requirements","grading","remarks","teacher","submission","description"})if(e.has(f)&&!e.isNull(f))require(e.get(f) instanceof String&&e.getString(f).length()<=20000,"备注格式无效");
   }
  }
  ScheduleValidator.validate(s);
  return s;
 }
}
