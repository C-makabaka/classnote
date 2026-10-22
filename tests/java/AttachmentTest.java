package cn.cbhcool.classnote;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;
public final class AttachmentTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 static void reject(JSONObject state,File root,String message)throws Exception{try{StateValidator.validate(state.toString(),root);}catch(Exception expected){return;}throw new AssertionError(message);}
 public static void main(String[]args)throws Exception{
  String pending="{\"batchId\":\"batch-1\",\"draftId\":\"note-1\",\"attachments\":[{\"type\":\"application\\/pdf\"}]}";
  String normalized=new JSONObject(pending).toString().replace("\\/","/");
  check(AttachmentFiles.acknowledges(pending,normalized),"Android escaped MIME and JavaScript JSON acknowledge same batch");
  check(!AttachmentFiles.acknowledges(pending,normalized.replace("batch-1","old-batch")),"stale batch cannot acknowledge current files");
  check(!AttachmentFiles.acknowledges(pending,"{}"),"missing batch cannot acknowledge files");
  check(AttachmentFiles.belongsToDraft(pending,"{\"kind\":\"note\",\"data\":{\"id\":\"note-1\"}}"),"keep pending for matching draft");
  check(!AttachmentFiles.belongsToDraft(pending,""),"discarding draft must release pending files");
  check(!AttachmentFiles.belongsToDraft(pending,"{\"kind\":\"note\",\"data\":{\"id\":\"note-2\"}}"),"new draft cannot remain blocked by abandoned batch");
  ByteArrayOutputStream copied=new ByteArrayOutputStream();check(AttachmentFiles.copy(new ByteArrayInputStream(new byte[]{0,-1,42}),copied)==3&&Arrays.equals(copied.toByteArray(),new byte[]{0,-1,42}),"stream preserves bytes");
  OutputStream sink=new OutputStream(){public void write(int b){}public void write(byte[]b,int start,int length){}};
  check(AttachmentFiles.copy(sized(AttachmentFiles.MAX_BYTES),sink)==AttachmentFiles.MAX_BYTES,"64MiB streaming boundary accepted");
  try{AttachmentFiles.copy(sized(AttachmentFiles.MAX_BYTES+1),sink);throw new AssertionError("oversized stream must fail without trusting provider size");}catch(IOException expected){}
  File root=Files.createTempDirectory("note-files").toFile(),restored=Files.createTempDirectory("note-files-restored").toFile();
  try{
   JSONObject state=new JSONObject(new String(Files.readAllBytes(Paths.get("test-results/seed.json")),"UTF-8"));
   File dir=new File(root,"attachments");dir.mkdirs();byte[] bytes=new byte[]{0,1,2,-1,42};Files.write(new File(dir,"document-1.bin").toPath(),bytes);
   JSONObject file=new JSONObject().put("name","document-1.bin").put("label","课堂资料.pdf").put("size",bytes.length).put("type","application/pdf");
   JSONObject note=new JSONObject().put("id","attachment-note").put("courseId","health").put("revision",1).put("updatedAt",1).put("title","").put("body","").put("number",1).put("date","2026-10-22").put("photos",new JSONArray()).put("attachments",new JSONArray().put(file));
   state.getJSONArray("notes").put(note);StateValidator.validate(state.toString(),root);
   Files.write(new File(root,"state.json").toPath(),state.toString().getBytes("UTF-8"));
   ByteArrayOutputStream zip=new ByteArrayOutputStream();SafeArchive.write(root,zip);SafeArchive.extract(new ByteArrayInputStream(zip.toByteArray()),restored,1024*1024);
   check(Arrays.equals(bytes,Files.readAllBytes(new File(restored,"attachments/document-1.bin").toPath())),"attachment bytes survive ZIP");
   StateValidator.validate(new String(Files.readAllBytes(new File(restored,"state.json").toPath()),"UTF-8"),restored);
   file.put("size",99);reject(state,root,"wrong stored size");file.put("size",bytes.length);
   for(Object value:new Object[]{-1,"5",1.5,64L*1024*1024+1}){file.put("size",value);reject(state,root,"bad size");}file.put("size",bytes.length);
   for(String name:new String[]{"../document-1.bin","document.pdf","missing.bin"}){file.put("name",name);reject(state,root,"unsafe or missing file");}file.put("name","document-1.bin");
   file.put("type","text/html\nother");reject(state,root,"invalid MIME");file.put("type","application/pdf");
   note.getJSONArray("attachments").put(file);reject(state,root,"duplicate file");note.put("attachments",JSONObject.NULL);reject(state,root,"invalid attachments array");
   for(String path:new String[]{"attachments/../state.json","attachments/sub/file.bin","attachments/x.pdf","attachments/.bin"}){
    try{SafeArchive.extract(new ByteArrayInputStream(ArchiveTest.zip(path,"bad")),new File(restored,"bad"),1024);throw new AssertionError("reject "+path);}catch(IOException expected){}
   }
   System.out.println("PASS native attachments: file-only note, bytes/metadata ZIP roundtrip, paths, MIME, sizes, duplicates");
  }finally{SafeArchive.removeTree(root);SafeArchive.removeTree(restored);}
 }
 static InputStream sized(long size){return new InputStream(){long remaining=size;public int read(){if(remaining--<=0)return -1;return 0;}public int read(byte[]b,int offset,int length){if(remaining<=0)return -1;int n=(int)Math.min(remaining,length);remaining-=n;Arrays.fill(b,offset,offset+n,(byte)0);return n;}};}
}
