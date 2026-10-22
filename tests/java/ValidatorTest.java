package cn.cbhcool.classnote;
import org.json.*;
import java.io.*;
import java.nio.file.*;
public final class ValidatorTest {
 static JSONObject base()throws Exception{return new JSONObject(new String(Files.readAllBytes(Paths.get("test-results/seed.json")),"UTF-8"));}
 static void reject(JSONObject s,File dir,String label)throws Exception{boolean bad=false;try{StateValidator.validate(s.toString(),dir);}catch(Exception e){bad=true;}if(!bad)throw new AssertionError(label+" must be rejected");}
 public static void main(String[]args)throws Exception{
  File dir=Files.createTempDirectory("state-test").toFile();
  try{
   StateValidator.validate(base().toString(),dir);
   for(String filename:new String[]{"fresh.json","imported.json"})StateValidator.validate(new String(Files.readAllBytes(Paths.get("test-results/"+filename)),"UTF-8"),dir);
   JSONObject timed=new JSONObject(new String(Files.readAllBytes(Paths.get("test-results/imported.json")),"UTF-8"));timed.getJSONObject("settings").getJSONArray("periodTimes").put(1,new JSONArray("[\"08:30\",\"09:30\"]"));reject(timed,dir,"overlapping period times");
   JSONObject adjusted=new JSONObject(new String(Files.readAllBytes(Paths.get("test-results/adjusted.json")),"UTF-8"));
   StateValidator.validate(adjusted.toString(),dir);
   JSONObject broken=new JSONObject(adjusted.toString());broken.getJSONArray("adjustments").getJSONObject(0).getJSONArray("changes").getJSONObject(0).getJSONObject("before").put("start",1);reject(broken,dir,"mismatched adjustment source");
   broken=new JSONObject(adjusted.toString());broken.getJSONArray("adjustments").getJSONObject(0).getJSONArray("changes").getJSONObject(0).getJSONObject("after").put("courseId","english");reject(broken,dir,"changed occurrence identity");
   broken=new JSONObject(adjusted.toString());broken.getJSONObject("settings").put("semesterStart","2026-09-14");reject(broken,dir,"changed semester anchor");
   JSONArray scenarios=new JSONArray(new String(Files.readAllBytes(Paths.get("test-results/schedule-scenarios.json")),"UTF-8"));
   for(int i=0;i<scenarios.length();i++)StateValidator.validate(scenarios.getJSONObject(i).toString(),dir);
   broken=new JSONObject(adjusted.toString());JSONObject target=broken.getJSONArray("adjustments").getJSONObject(0).getJSONArray("changes").getJSONObject(0).getJSONObject("after");target.put("date","2026-09-08").put("start",3).put("end",4);reject(broken,dir,"adjustment target conflict");
   JSONObject fractional=base();fractional.getJSONObject("settings").put("weeks",2.5);reject(fractional,dir,"fractional week count");
   JSONObject stringNumber=base();stringNumber.getJSONObject("settings").put("weeks","17");reject(stringNumber,dir,"string numeric field");
   JSONObject badShort=base();badShort.getJSONArray("courses").getJSONObject(0).put("shortName",42);reject(badShort,dir,"number shortName");
   JSONObject notes=base();JSONObject note=new JSONObject().put("id","untitled").put("courseId","health").put("revision",1).put("updatedAt",1).put("title","").put("number",1).put("date","2026-09-29").put("body","课堂重点").put("photos",new JSONArray());
   notes.getJSONArray("notes").put(note);StateValidator.validate(notes.toString(),dir);
   note.put("body","");reject(notes,dir,"empty note");
   note.put("body","　\u00a0\ufeff");reject(notes,dir,"unicode-whitespace-only note");note.put("body","");
   File media=new File(dir,"media");media.mkdirs();Files.write(new File(media,"photo.jpg").toPath(),new byte[]{1,2,3});
   note.getJSONArray("photos").put(new JSONObject().put("name","photo.jpg"));StateValidator.validate(notes.toString(),dir);
   Files.write(new File(dir,"state.json").toPath(),notes.toString().getBytes("UTF-8"));
   File zip=new File(dir.getParentFile(),"untitled-"+System.nanoTime()+".zip"),restored=new File(dir.getParentFile(),"restored-"+System.nanoTime());
   try{try(OutputStream out=new FileOutputStream(zip)){SafeArchive.write(dir,out);}try(InputStream in=new FileInputStream(zip)){SafeArchive.extract(in,restored,1024*1024);}StateValidator.validate(new String(Files.readAllBytes(new File(restored,"state.json").toPath()),"UTF-8"),restored);}finally{zip.delete();SafeArchive.removeTree(restored);}
   JSONObject big=base();String body=new String(new char[19000]).replace('\0','课');
   for(int i=0;i<160;i++){big.getJSONArray("notes").put(new JSONObject().put("id","note"+i).put("courseId","health").put("revision",1).put("updatedAt",1).put("title","笔记").put("number",i+1).put("date","2026-09-20").put("body",body).put("photos",new JSONArray()));}
   reject(big,dir,"UTF-8 byte limit protects next load");
   System.out.println("PASS native validation: old data, all four adjustments, replay, conflicts, numeric/text types, UTF-8 size");
  }finally{SafeArchive.removeTree(dir);}
 }
}
