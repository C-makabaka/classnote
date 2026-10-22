package cn.cbhcool.classnote;
import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

final class AppStore {
 static final Object LOCK=new Object();
 private final Context context;
 AppStore(Context c){context=c.getApplicationContext();}
 File root(){return new File(context.getFilesDir(),"data");}
 void recover()throws IOException{
  File old=new File(context.getFilesDir(),"data-old");if(!root().exists()&&old.exists()&&!old.renameTo(root()))throw new IOException("恢复数据目录失败");
  if(!root().isDirectory()&&!root().mkdirs())throw new IOException("无法创建数据目录");
  File media=new File(root(),"media");if(!media.isDirectory()&&!media.mkdirs())throw new IOException("无法创建图片目录");
  File attachments=new File(root(),"attachments");if(!attachments.isDirectory()&&!attachments.mkdirs())throw new IOException("无法创建附件目录");
 }
 String load()throws Exception{synchronized(LOCK){recover();File base=new File(root(),"state.json");AtomicFile f=new AtomicFile(base);if(!base.exists()&&!new File(root(),"state.json.bak").exists())return "";try(InputStream in=f.openRead()){return read(in,8*1024*1024);}}}
 static String read(InputStream in,int max)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[]b=new byte[16384];int n;while((n=in.read(b))!=-1){if(out.size()+n>max)throw new IOException("文件超过大小限制");out.write(b,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8);}
 void save(String raw)throws Exception{synchronized(LOCK){recover();StateValidator.validate(raw,root());AtomicFile f=new AtomicFile(new File(root(),"state.json"));FileOutputStream out=null;try{out=f.startWrite();out.write(raw.getBytes(StandardCharsets.UTF_8));f.finishWrite(out);}catch(Exception e){if(out!=null)f.failWrite(out);throw e;}}}
 void backup(OutputStream out)throws Exception{synchronized(LOCK){String state=load();if(state.isEmpty())throw new IOException("暂无可备份的数据");StateValidator.validate(state,root());SafeArchive.write(root(),out);}}
 void restore(InputStream in)throws Exception{synchronized(LOCK){recover();File staging=new File(context.getFilesDir(),"restore-stage"),old=new File(context.getFilesDir(),"data-old");SafeArchive.removeTree(staging);try{SafeArchive.extract(in,staging,2L*1024*1024*1024);String raw;try(InputStream s=new FileInputStream(new File(staging,"state.json"))){raw=read(s,8*1024*1024);}StateValidator.validate(raw,staging);SafeArchive.removeTree(old);if(!root().renameTo(old))throw new IOException("无法备份现有数据目录");if(!staging.renameTo(root())){if(!old.renameTo(root()))throw new IOException("恢复中断：旧资料已保留，请重新打开应用");throw new IOException("无法替换数据，已保留原资料");}SafeArchive.removeTree(old);SafeArchive.removeTree(new File(context.getCacheDir(),"thumbs"));}finally{SafeArchive.removeTree(staging);}}}
 JSONObject task(String id)throws Exception{String raw=load();if(raw.isEmpty())return null;JSONArray tasks=new JSONObject(raw).getJSONArray("tasks");for(int i=0;i<tasks.length();i++){JSONObject t=tasks.getJSONObject(i);if(t.getString("id").equals(id)&&t.isNull("deletedAt"))return t;}return null;}
}
