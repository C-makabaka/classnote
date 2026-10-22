package cn.cbhcool.classnote;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.*;

/** Exposes exactly one temporary capture file through a one-time URI grant. */
public final class CameraProvider extends ContentProvider {
 @Override public boolean onCreate(){return true;}
 private File file(Uri uri)throws FileNotFoundException{if(!"/capture.jpg".equals(uri.getPath()))throw new FileNotFoundException();return new File(getContext().getCacheDir(),"capture.jpg");}
 @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{return ParcelFileDescriptor.open(file(uri),mode.contains("w")?ParcelFileDescriptor.MODE_READ_WRITE|ParcelFileDescriptor.MODE_CREATE|ParcelFileDescriptor.MODE_TRUNCATE:ParcelFileDescriptor.MODE_READ_ONLY);}
 @Override public String getType(Uri uri){return "image/jpeg";}
 @Override public Cursor query(Uri uri,String[]p,String selection,String[]args,String sort){MatrixCursor c=new MatrixCursor(new String[]{"_display_name","_size"});try{c.addRow(new Object[]{"capture.jpg",file(uri).length()});}catch(Exception ignored){}return c;}
 @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
 @Override public int delete(Uri u,String s,String[]a){throw new UnsupportedOperationException();}
 @Override public int update(Uri u,ContentValues v,String s,String[]a){throw new UnsupportedOperationException();}
}
