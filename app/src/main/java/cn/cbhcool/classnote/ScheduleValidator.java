package cn.cbhcool.classnote;
import org.json.*;
import java.time.*;
import java.util.*;
import static cn.cbhcool.classnote.StateValidator.*;

/** Validates replayable schedule patches before save or transactional backup restore. */
final class ScheduleValidator {
 static final String[] FIELDS={"key","courseId","lessonIndex","sourceDate","date","start","end","room","label"};
 static boolean same(JSONObject a,JSONObject b)throws Exception{if(a==null||b==null)return a==b;for(String f:FIELDS){Object av=a.get(f),bv=b.get(f);if(av instanceof Number&&bv instanceof Number){if(((Number)av).doubleValue()!=((Number)bv).doubleValue())return false;}else if(!av.equals(bv))return false;}return true;}
 static void inTerm(JSONObject o,String field,LocalDate start,long weeks)throws Exception{LocalDate d=date(o,field);require(!d.isBefore(start)&&d.isBefore(start.plusWeeks(weeks)),"调整日期必须在本学期内");}
 static void occurrence(JSONObject o,JSONObject original,LocalDate start,long weeks)throws Exception{
  for(String f:new String[]{"key","courseId","sourceDate"})require(text(o,f,150,false).equals(original.getString(f)),"调课来源无效");
  require(integer(o,"lessonIndex")==original.getInt("lessonIndex"),"调课时段标识无效");inTerm(o,"date",start,weeks);
  long a=integer(o,"start"),b=integer(o,"end");require(a>=1&&b>=a&&b<=14,"调课节次无效");text(o,"room",200,false);require(Arrays.asList("","调课","对调","补课","长期").contains(text(o,"label",10,false)),"调课标记无效");
 }
 static void validate(JSONObject s)throws Exception{
  if(!s.has("adjustments")||s.isNull("adjustments"))return;
  JSONArray events=s.getJSONArray("adjustments");require(events.length()<=200,"调整记录过多");
  JSONObject settings=s.getJSONObject("settings");LocalDate first=date(settings,"semesterStart");long weeks=integer(settings,"weeks");
  Map<String,JSONObject> base=new LinkedHashMap<>(),map=new LinkedHashMap<>();Set<String>live=new HashSet<>();JSONArray courses=s.getJSONArray("courses");
  for(int i=0;i<courses.length();i++){JSONObject c=courses.getJSONObject(i);String cid=c.getString("id");if(!c.optBoolean("archived",false)&&c.isNull("deletedAt"))live.add(cid);JSONArray ls=c.getJSONArray("lessons");for(int j=0;j<ls.length();j++){JSONObject l=ls.getJSONObject(j);for(int w=l.getInt("fromWeek");w<=Math.min(l.getInt("toWeek"),weeks);w++){String parity=l.getString("parity");if(!parity.equals("all")&&(parity.equals("odd")?w%2==0:w%2==1))continue;String d=first.plusDays((w-1)*7+l.getInt("day")-1).toString(),key=cid+"_"+j+"_"+d;JSONObject o=new JSONObject().put("key",key).put("courseId",cid).put("lessonIndex",j).put("sourceDate",d).put("date",d).put("start",l.getInt("start")).put("end",l.getInt("end")).put("room",l.getString("room")).put("label","");base.put(key,o);map.put(key,o);}}}
  Set<String>ids=new HashSet<>(),touched=new HashSet<>();int total=0;
  for(int i=0;i<events.length();i++){JSONObject e=events.getJSONObject(i);String id=text(e,"id",100,false);require(id.matches("[a-zA-Z0-9_-]{1,100}")&&ids.add(id),"调整标识无效");require(Arrays.asList("move","swap","holiday","permanent").contains(text(e,"kind",20,false)),"调整类型无效");text(e,"title",300,false);require(integer(e,"createdAt")>0,"调整时间无效");require(date(e,"semesterStart").equals(first)&&integer(e,"weeks")==weeks,"调整的学期已变化");JSONArray changes=e.getJSONArray("changes");total+=changes.length();require(changes.length()>=1&&changes.length()<=1000&&total<=20000,"调整范围过大");Set<String>unique=new HashSet<>();
   for(int j=0;j<changes.length();j++){JSONObject p=changes.getJSONObject(j);String key=text(p,"key",150,false);require(base.containsKey(key)&&unique.add(key),"调整来源重复或不存在");require(p.has("before")&&p.has("after"),"缺少调整内容");JSONObject before=p.isNull("before")?null:p.getJSONObject("before"),after=p.isNull("after")?null:p.getJSONObject("after");if(before!=null)occurrence(before,base.get(key),first,weeks);if(after!=null)occurrence(after,base.get(key),first,weeks);require(same(map.get(key),before)&&!same(before,after),"原安排已变化或没有变化");map.put(key,after);touched.add(key);}
  }
  Map<String,List<JSONObject>> days=new HashMap<>();for(JSONObject o:map.values())if(o!=null&&live.contains(o.getString("courseId"))){String d=o.getString("date");if(!days.containsKey(d))days.put(d,new ArrayList<JSONObject>());days.get(d).add(o);}
  for(List<JSONObject>ls:days.values()){ls.sort((a,b)->Integer.compare(a.optInt("start"),b.optInt("start")));for(int i=0;i<ls.size();i++)for(int j=i+1;j<ls.size()&&ls.get(j).getInt("start")<=ls.get(i).getInt("end");j++)require(!touched.contains(ls.get(i).getString("key"))&&!touched.contains(ls.get(j).getString("key")),"调课存在时间冲突");}
 }
}
