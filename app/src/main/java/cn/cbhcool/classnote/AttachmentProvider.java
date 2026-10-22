package cn.cbhcool.classnote;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Gives a selected external viewer read-only access to a single attachment. */
public final class AttachmentProvider extends ContentProvider {
 @Override public boolean onCreate(){return true;}
 private File file(Uri uri)throws FileNotFoundException{
  String path=uri.getPath();if(path==null||!path.startsWith("/")||!AttachmentFiles.name(path.substring(1)))throw new FileNotFoundException("附件路径无效");
  File file=new File(getContext().getFilesDir(),"data/attachments"+path);if(!file.isFile())throw new FileNotFoundException("附件不存在");return file;
 }
 @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
  if(!"r".equals(mode))throw new FileNotFoundException("附件仅允许读取");return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);
 }
 @Override public String getType(Uri uri){String type=uri.getQueryParameter("type");return AttachmentFiles.type(type)?type:"application/octet-stream";}
 @Override public Cursor query(Uri uri,String[]projection,String selection,String[]args,String sort){
  String[]columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor cursor=new MatrixCursor(columns);
  try{File file=file(uri);String label=uri.getQueryParameter("label");Object[]row=new Object[columns.length];for(int i=0;i<columns.length;i++){if(OpenableColumns.DISPLAY_NAME.equals(columns[i]))row[i]=label==null?"文件附件":label;else if(OpenableColumns.SIZE.equals(columns[i]))row[i]=file.length();}cursor.addRow(row);}catch(FileNotFoundException ignored){}return cursor;
 }
 @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
 @Override public int delete(Uri uri,String selection,String[]args){throw new UnsupportedOperationException();}
 @Override public int update(Uri uri,ContentValues values,String selection,String[]args){throw new UnsupportedOperationException();}
}
