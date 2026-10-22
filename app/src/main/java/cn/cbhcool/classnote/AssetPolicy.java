package cn.cbhcool.classnote;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
/** Shared policy for the WebView's local asset route. */
final class AssetPolicy {
 private static final Set<String> ALLOWED=new HashSet<>(Arrays.asList(
  "index.html","core.js","schedule.js","adjust-ui.js","select-ui.js","motion.js",
  "time-axis.js","timetable-import.js","import-ui.js","seed.js","platform.js",
  "sharing.js","photoswipe.umd.min.js","photoswipe.css","photo-viewer.js","app.js","style.css"));
 static boolean isAllowed(String asset){return ALLOWED.contains(asset);}
}
