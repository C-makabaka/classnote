package cn.cbhcool.classnote;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** No Android dependencies: ZIP names and expanded byte budgets are checked before commit. */
public final class SafeArchive {
 private SafeArchive(){}
 public static boolean mediaName(String s){return s!=null&&s.matches("[a-zA-Z0-9_-]+\\.(jpg|jpeg|png|webp|gif|heic|heif)");}
 public static void extract(InputStream input,File folder,long limit)throws IOException{
  if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("无法创建恢复目录");
  long total=0;int count=0;Set<String>names=new HashSet<>();byte[]buffer=new byte[32768];
  try(ZipInputStream zip=new ZipInputStream(new BufferedInputStream(input))){ZipEntry e;
   while((e=zip.getNextEntry())!=null){String name=e.getName();
    if(e.isDirectory()){if(!name.equals("media/")&&!name.equals("attachments/"))throw new IOException("备份包含未知目录");continue;}
    if(!name.equals("state.json")&&!(name.startsWith("media/")&&mediaName(name.substring(6)))&&!(name.startsWith("attachments/")&&AttachmentFiles.name(name.substring(12))))throw new IOException("备份包含非法文件路径");
    if(!names.add(name)||++count>30000)throw new IOException("备份含重复文件或文件过多");
    File target=new File(folder,name);File parent=target.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("无法创建目录");
    long size=0;try(OutputStream out=new FileOutputStream(target)){int n;while((n=zip.read(buffer))!=-1){size+=n;total+=n;if(total>limit||size>64L*1024*1024)throw new IOException("备份超过恢复大小限制");out.write(buffer,0,n);}}
    zip.closeEntry();
   }
  }
  if(!names.contains("state.json"))throw new IOException("不是课记完整备份");
 }
 public static void write(File folder,OutputStream output)throws IOException{
  File state=new File(folder,"state.json");long total=state.length();int count=1;List<File>files=new ArrayList<>();
  for(String dir:new String[]{"media","attachments"}){File[] children=new File(folder,dir).listFiles();if(children!=null)for(File f:children)if(f.isFile()&&(dir.equals("media")?mediaName(f.getName()):AttachmentFiles.name(f.getName())))files.add(f);}
  for(File f:files){if(f.length()>64L*1024*1024)throw new IOException("单个备份文件过大");total+=f.length();count++;}
  if(total>2L*1024*1024*1024||count>30000)throw new IOException("资料超过首版备份容量（2GB或30000个文件），请保留应用并联系开发者迁移");
  try(ZipOutputStream zip=new ZipOutputStream(new BufferedOutputStream(output))){add(zip,state,"state.json");for(File f:files)add(zip,f,f.getParentFile().getName()+"/"+f.getName());}
 }
 private static void add(ZipOutputStream z,File file,String name)throws IOException{z.putNextEntry(new ZipEntry(name));try(InputStream in=new FileInputStream(file)){byte[]b=new byte[32768];int n;while((n=in.read(b))!=-1)z.write(b,0,n);}z.closeEntry();}
 public static void removeTree(File f){File[]children=f.listFiles();if(children!=null)for(File c:children)removeTree(c);if(f.exists()&&!f.delete())f.deleteOnExit();}
}
