package cn.cbhcool.classnote;
import java.nio.file.*;
import java.util.regex.*;
public final class AssetPolicyTest {
 public static void main(String[]args)throws Exception{
  Path assets=Paths.get("app/src/main/assets");String html=new String(Files.readAllBytes(assets.resolve("index.html")),"UTF-8");
  Matcher refs=Pattern.compile("(?:src|href)=\"([^\"]+)\"").matcher(html);int total=0;
  while(refs.find()){String asset=refs.group(1);if(!AssetPolicy.isAllowed(asset))throw new AssertionError("WebView refuses page dependency: "+asset);if(!Files.isRegularFile(assets.resolve(asset)))throw new AssertionError("Missing asset: "+asset);total++;}
  for(String denied:new String[]{"../state.json","state.json","media/a.jpg","unknown.js","/app.js","app.js/extra"})if(AssetPolicy.isAllowed(denied))throw new AssertionError("Unexpected asset allowed: "+denied);
  System.out.println("PASS native asset policy: "+total+" page dependencies reachable; unknown paths denied");
 }
}
