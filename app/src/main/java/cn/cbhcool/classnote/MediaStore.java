package cn.cbhcool.classnote;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.OpenableColumns;
import org.json.JSONObject;
import java.io.*;
import java.util.UUID;

final class MediaStore {
 private final Context context;
 MediaStore(Context c){context=c;}
 JSONObject importUri(Uri uri)throws Exception{synchronized(AppStore.LOCK){AppStore store=new AppStore(context);store.recover();String mime=context.getContentResolver().getType(uri),extension="jpg";if("image/png".equals(mime))extension="png";else if("image/webp".equals(mime))extension="webp";else if("image/gif".equals(mime))extension="gif";else if("image/heic".equals(mime))extension="heic";else if("image/heif".equals(mime))extension="heif";
  String name=UUID.randomUUID()+"."+extension,label="课堂照片";
  try(Cursor c=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())label=c.getString(0);}catch(Exception ignored){}
  File target=new File(store.root(),"media/"+name);boolean ok=false;try{
   try(InputStream in=context.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(target)){if(in==null)throw new IOException("无法读取照片");byte[]b=new byte[32768];long total=0;int n;while((n=in.read(b))!=-1){total+=n;if(total>40L*1024*1024)throw new IOException("单张照片不能超过40MB");out.write(b,0,n);}}
   BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(target.getPath(),o);if(o.outWidth<1||o.outHeight<1)throw new IOException("不支持该图片格式，请选择JPG或PNG图片");ok=true;return new JSONObject().put("name",name).put("label",label==null?"课堂照片":label.substring(0,Math.min(300,label.length())));
  }finally{if(!ok)target.delete();}
 }}
 JSONObject dimensions(String name)throws Exception{
  File source=file(name,false);BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeFile(source.getPath(),bounds);
  int width=bounds.outWidth,height=bounds.outHeight;
  if(width<1||height<1)throw new IOException("不支持该图片格式");
  try{int orientation=new ExifInterface(source.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);if(orientation>=5&&orientation<=8){int swap=width;width=height;height=swap;}}catch(IOException ignored){}
  return new JSONObject().put("width",width).put("height",height);
 }
 File file(String name,boolean thumb)throws Exception{
  if(!SafeArchive.mediaName(name))throw new IOException("图片路径无效");File original=new File(new AppStore(context).root(),"media/"+name);if(!original.isFile())throw new FileNotFoundException("图片不存在");if(!thumb)return original;
  File dir=new File(context.getCacheDir(),"thumbs");dir.mkdirs();File cached=new File(dir,name+".jpg");if(cached.isFile())return cached;
  synchronized(this){if(cached.isFile())return cached;BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(original.getPath(),o);o.inSampleSize=1;while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>1200)o.inSampleSize*=2;o.inJustDecodeBounds=false;Bitmap bitmap=BitmapFactory.decodeFile(original.getPath(),o);if(bitmap==null)return original;
   try{int orientation=new ExifInterface(original.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);Matrix m=new Matrix();if(orientation==6)m.postRotate(90);else if(orientation==3)m.postRotate(180);else if(orientation==8)m.postRotate(270);else if(orientation==2)m.setScale(-1,1);else if(orientation==4)m.setScale(1,-1);else if(orientation==5){m.setScale(-1,1);m.postRotate(270);}else if(orientation==7){m.setScale(-1,1);m.postRotate(90);}if(!m.isIdentity()){Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),m,true);if(rotated!=bitmap){bitmap.recycle();bitmap=rotated;}}}catch(Exception ignored){}
   File tmp=new File(dir,name+".tmp");try(OutputStream out=new FileOutputStream(tmp)){bitmap.compress(Bitmap.CompressFormat.JPEG,85,out);}finally{bitmap.recycle();}if(!tmp.renameTo(cached))throw new IOException("生成缩略图失败");return cached;
  }
 }
}
