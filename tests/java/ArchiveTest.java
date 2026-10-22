package cn.cbhcool.classnote;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
public final class ArchiveTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static byte[] zip(String name,String value)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(out)){z.putNextEntry(new ZipEntry(name));z.write(value.getBytes("UTF-8"));z.closeEntry();}return out.toByteArray();}
 public static void main(String[]args)throws Exception{
  File base=Files.createTempDirectory("classnote-test").toFile();
  try{
   File normal=new File(base,"normal");SafeArchive.extract(new ByteArrayInputStream(zip("state.json","{\"schemaVersion\":1}")),normal,1024);
   check(new File(normal,"state.json").isFile(),"valid archive extracted");
   for(String path:new String[]{"../escape","/absolute","media/../../escape","media/a.exe","media/sub/a.jpg"}){
    boolean rejected=false;try{SafeArchive.extract(new ByteArrayInputStream(zip(path,"bad")),new File(base,"bad"+Math.abs(path.hashCode())),1024);}catch(IOException e){rejected=true;}check(rejected,"reject "+path);
   }
   boolean rejected=false;try{SafeArchive.extract(new ByteArrayInputStream(zip("state.json","123456789")),new File(base,"limit"),4);}catch(IOException e){rejected=true;}check(rejected,"expanded-size limit");
   File source=new File(base,"source");source.mkdirs();Files.write(new File(source,"state.json").toPath(),"{}".getBytes("UTF-8"));new File(source,"media").mkdirs();Files.write(new File(source,"media/a.jpg").toPath(),new byte[]{1,2,3});
   ByteArrayOutputStream out=new ByteArrayOutputStream();SafeArchive.write(source,out);
   File restored=new File(base,"restored");SafeArchive.extract(new ByteArrayInputStream(out.toByteArray()),restored,1024);
   check(Files.readAllBytes(new File(restored,"media/a.jpg").toPath()).length==3,"photos roundtrip");
   try(RandomAccessFile large=new RandomAccessFile(new File(source,"media/oversized.jpg"),"rw")){large.setLength(64L*1024*1024+1);}
   boolean refused=false;try{SafeArchive.write(source,new ByteArrayOutputStream());}catch(IOException e){refused=true;}check(refused,"export must not produce unrestorable oversized files");
   File imported=new File(base,"imported");imported.mkdirs();byte[] input=Files.readAllBytes(Paths.get("test-results/imported.json"));Files.write(new File(imported,"state.json").toPath(),input);
   ByteArrayOutputStream roundtrip=new ByteArrayOutputStream();SafeArchive.write(imported,roundtrip);File target=new File(base,"imported-restored");SafeArchive.extract(new ByteArrayInputStream(roundtrip.toByteArray()),target,1024*1024);
   byte[] output=Files.readAllBytes(new File(target,"state.json").toPath());check(java.util.Arrays.equals(input,output),"generic imported state survives ZIP roundtrip");StateValidator.validate(new String(output,"UTF-8"),target);
   System.out.println("PASS archive normal, traversal, extension, limit, and media roundtrip");
  }finally{SafeArchive.removeTree(base);}
 }
}
