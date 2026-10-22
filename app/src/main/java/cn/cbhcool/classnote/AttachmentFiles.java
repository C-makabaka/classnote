package cn.cbhcool.classnote;
import java.io.*;
import org.json.JSONObject;

/** Shared storage rules for imported documents and backup validation. */
final class AttachmentFiles {
 static final long MAX_BYTES=64L*1024*1024;
 static boolean name(String name){return name!=null&&name.matches("[a-zA-Z0-9_-]{1,80}\\.bin");}
 static boolean type(String type){return type!=null&&type.length()<=120&&type.matches("[a-z0-9][a-z0-9!#$&^_.+-]*/[a-z0-9][a-z0-9!#$&^_.+-]*");}
 static boolean acknowledges(String pending,String reply){
  try{JSONObject stored=new JSONObject(pending),received=new JSONObject(reply);String batch=stored.getString("batchId");return !batch.isEmpty()&&batch.equals(received.getString("batchId"))&&stored.getString("draftId").equals(received.getString("draftId"));}catch(Exception ignored){return false;}
 }
 static boolean belongsToDraft(String pending,String draft){
  try{JSONObject saved=new JSONObject(draft);return "note".equals(saved.getString("kind"))&&new JSONObject(pending).getString("draftId").equals(saved.getJSONObject("data").getString("id"));}catch(Exception ignored){return false;}
 }
 static long copy(InputStream in,OutputStream out)throws IOException{
  if(in==null)throw new IOException("无法读取文件");byte[] buffer=new byte[32768];long total=0;int count;
  while((count=in.read(buffer))!=-1){total+=count;if(total>MAX_BYTES)throw new IOException("单个文件不能超过64MB");out.write(buffer,0,count);}return total;
 }
}
