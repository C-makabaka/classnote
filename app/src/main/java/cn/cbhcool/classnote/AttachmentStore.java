package cn.cbhcool.classnote;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import org.json.JSONObject;
import java.io.*;
import java.util.*;

final class AttachmentStore {
 private final Context context;
 AttachmentStore(Context context){this.context=context;}
 JSONObject importUri(Uri uri)throws Exception{synchronized(AppStore.LOCK){
  AppStore store=new AppStore(context);store.recover();String label="文件附件",mime=context.getContentResolver().getType(uri);
  try(Cursor cursor=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE},null,null,null)){
   if(cursor!=null&&cursor.moveToFirst()){
    int nameIndex=cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME),sizeIndex=cursor.getColumnIndex(OpenableColumns.SIZE);
    if(nameIndex>=0&&!cursor.isNull(nameIndex))label=cursor.getString(nameIndex);
    if(sizeIndex>=0&&!cursor.isNull(sizeIndex)&&cursor.getLong(sizeIndex)>AttachmentFiles.MAX_BYTES)throw new IOException("单个文件不能超过64MB");
   }
  }
  label=label.replaceAll("[\\p{Cntrl}/\\\\]","_").trim();if(label.isEmpty())label="文件附件";if(label.length()>300)label=label.substring(0,300);
  if(mime==null||"application/octet-stream".equals(mime)){int dot=label.lastIndexOf('.');if(dot>=0)mime=MimeTypeMap.getSingleton().getMimeTypeFromExtension(label.substring(dot+1).toLowerCase(Locale.ROOT));}
  if(mime!=null)mime=mime.toLowerCase(Locale.ROOT);if(!AttachmentFiles.type(mime))mime="application/octet-stream";
  String name=UUID.randomUUID()+".bin";File target=new File(store.root(),"attachments/"+name);boolean saved=false;
  try{long size;try(InputStream in=context.getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(target)){size=AttachmentFiles.copy(in,out);out.getFD().sync();}
   JSONObject result=new JSONObject().put("name",name).put("label",label).put("size",size).put("type",mime);saved=true;return result;
  }finally{if(!saved)target.delete();}
 }}
 File file(String name)throws IOException{
  if(!AttachmentFiles.name(name))throw new IOException("附件路径无效");File file=new File(new AppStore(context).root(),"attachments/"+name);if(!file.isFile())throw new FileNotFoundException("附件不存在，请检查备份是否完整");return file;
 }
 Intent viewIntent(JSONObject descriptor)throws Exception{
  String name=descriptor.getString("name");file(name);String mime=descriptor.getString("type");if(!AttachmentFiles.type(mime))mime="application/octet-stream";
  Uri uri=new Uri.Builder().scheme("content").authority(context.getPackageName()+".attachments").appendPath(name).appendQueryParameter("label",descriptor.getString("label")).appendQueryParameter("type",mime).build();
  Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);intent.setClipData(ClipData.newRawUri("文件附件",uri));return intent;
 }
}
