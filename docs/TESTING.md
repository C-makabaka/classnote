# 验证记录与真机检查

## 已执行的验证

- Node领域测试：当前周、学期外、跨月跨年日期、单双周、7门示例课程10个时段、完成沉底／恢复／计数、输入验证、照片文件名、墓碑、分周课程重叠分组。
- Chromium真实页面流程：新增作业→完成→重新加载保留→恢复；通知对应作业路由；新增笔记；相册图片选择；照片接收确认、重复投递去重、拒绝错误草稿、草稿重载后照片保留；320px无横向页面溢出。
- Java真实临时目录测试：ZIP正常解包／图片往返、路径穿越和未知扩展名拒绝、展开大小上限、拒绝导出无法恢复的超大文件。
- Java数据验证测试：拒绝字符串／小数型整数、非文本简称、UTF-8字节超限；避免导入后无法加载或保存后变成不可读取状态。
- SDK35 aapt2资源编译、Eclipse Java编译、D8生成DEX、zipalign、apksigner v2/v3签名验证。
- 独立只读代码审查一轮，修复恢复类型不一致、字符／字节上限不一致、照片提前确认、恢复后提醒错误阻断界面刷新；增加并发旧页面保存保护。

构建存在Android系统兼容方法的弃用警告（如旧版状态栏设置和ProgressDialog），没有Java编译错误。未使用真正的安卓设备／模拟器进行运行验证，也没有把浏览器页面通过等同于Android全部功能通过。

## 可复现测试命令

```bash
node --test tests/*.test.cjs

# 安装Playwright及浏览器，或设置CHROME_BIN到已有Chromium可执行文件。
CHROME_BIN=/path/to/chrome-headless-shell node tests/ui.cjs
CHROME_BIN=/path/to/chrome-headless-shell node tests/schedule-ui.cjs
CHROME_BIN=/path/to/chrome-headless-shell node tests/ui-polish.cjs
CHROME_BIN=/path/to/chrome-headless-shell node tests/import-ui.cjs

# Java测试使用Eclipse编译器及org.json:json:20240303作为测试依赖。
node tests/make-fixtures.cjs
java -jar /path/to/ecj.jar -17 -cp /path/to/json.jar -d test-results/java tests/java/*.java app/src/main/java/cn/cbhcool/classnote/SafeArchive.java app/src/main/java/cn/cbhcool/classnote/AttachmentFiles.java app/src/main/java/cn/cbhcool/classnote/StateValidator.java app/src/main/java/cn/cbhcool/classnote/ScheduleValidator.java
java -cp test-results/java:/path/to/json.jar cn.cbhcool.classnote.ArchiveTest
java -cp test-results/java:/path/to/json.jar cn.cbhcool.classnote.ValidatorTest
```

## 1.1新增验证

- 独立审查修复：撤销时原时段被新课占用则拒绝；停课不改写归档课程历史，两条均先复现失败再修复。
- 临时换课只影响单次，作业及提醒字段不变，后续周保持原安排。
- 两课对调原子交换；停课范围、多日期补课、单双周来源、重复补课拒绝。
- 从指定周起长期变更，保留此前历史和未来单次例外。
- 日期跨年、学期范围、时间冲突阻止、来源引用与 before/after 完整重放。
- 笔记的实际日期和原日期；旧数据没有 adjustments 仍可使用。
- 真实浏览器跑通四种选择框、预览、确认、撤销、冲突提示、周末列、重载保持、长按与首次关闭点击；320px宽度检查。
- Java 校验有效调课备份以及损坏来源、篡改课程、改变学期的拒绝行为。

## 首次安装后真机检查

1. 课表显示本周日期，点击健康急救／客舱课显示黄色和制服说明。
2. 从相册导入2张照片并拍1张，保存笔记；退出重开，图片和顺序仍在。试旋转屏幕后继续草稿。
3. 新增一条未来5分钟截止、截止时提醒的作业；允许通知和精确提醒，锁屏确认收到提醒。
4. 编辑另一条未来作业的截止时间，确认旧时刻不再提醒；勾选完成，确认角标减少且提醒取消。
5. 点击“添加到系统日历”，确认标题、时间和要求正确，在日历内自行保存。
6. 导出备份，添加临时记录，再从备份恢复，确认恢复后的照片和文字完整；恢复前保留备份文件。
7. 覆盖升级后确认原有笔记和作业仍在；临时移动一节课到周末，长按打开选择框，预览确认，再在调整记录中撤销。
8. 导出含调课记录的备份并恢复，核对实际日期与原安排。
9. 如厂商省电设置限制后台，允许该应用后台运行后再测试通知。不要在尚未验证提醒时把它作为唯一提交提醒渠道。

## 留待真实设备验证的环境条件

OEM相机和文件选择器差异、系统通知实际时序、后台被系统回收、旋转时的原生回调、设备断电时文件系统行为、文件提供者写入失败。这些无法在当前没有Android运行设备的环境中认证。

## 1.2界面验证

新增浏览器验证320/390/430/768像素七列宽度、周日可见、应用内选择框选择/搜索/返回、长表单右上角保存、空标题校验、预览后编辑失效、学期保存重载。既有作业、笔记照片草稿和四类调课流程继续回归。

桌面图标使用原始生成PNG封装为API26自适应资源，构建校验资源可链接；实际桌面裁切、设备字体缩放、软键盘和触摸仍需手机检查。

## 1.3通用版验证

首次启动空课表；旧课表移至测试夹具，仅用于兼容回归，生产应用不包含个人课表。JSON解析、字段校验、冲突、重复、原数据保留、剪贴板提示词、预览编辑失效、重新载入、点击遮罩关闭与动画已覆盖。Java校验新增空状态与JSON导入结果；导入结果经原ZIP实现导出和恢复后字节一致且再次通过校验。

系统应用信息图标缓存、实际桌面裁切和原生剪贴板需要设备验证；不以浏览器测试替代这些系统行为。

## 1.3.1 草稿交互
`CHROME_BIN=/path/to/chrome node tests/draft-ui.cjs` 验证空表单直接返回、保存/放弃/继续编辑、启动无原生提示、手动恢复、底部导航离开、正式记录不被放弃操作修改，以及新建不覆盖旧草稿。照片重载测试改为主动点击草稿入口。390px面板截图已检查。

## 1.4.0 时间线
`node --test tests/time-axis.test.cjs`；`CHROME_BIN=/path/to/chrome node tests/time-axis-ui.cjs`。检查起止点、课间、午休、半秒内位置更新与0.25秒线性动画、跨周隐藏、时间编辑校验和持久化。Java校验和ZIP往返覆盖新增periodTimes。


## 1.4.1 修复验证（2026-09-29）

- Node：34项领域用例通过。新增无主题文字/照片笔记、空白笔记拒绝和旧版笔记兼容检查。
- Chromium：既有6个页面流程通过，新增5组交互流程通过。覆盖作业正文/标题入口、长列表后的编辑页定位、编辑持久化和删除；照片移除取消/确认；无主题笔记保存；通过CDP实际触摸事件验证左右切换和双指缩放，并检查页面缩放比例及横向溢出。
- Java：旧数据、调课、类型/容量校验、归档往返检查通过；新增空主题笔记、纯照片笔记ZIP往返、Unicode空白笔记拒绝。
- 原生资源加载：AssetPolicyTest验证HTML引用的所有脚本/样式均在WebView加载名单中且文件存在，未知路径拒绝。曾复现新照片脚本被拦截，已加入加载名单。
- APK使用SDK35构建，v2/v3签名验证通过。1.4.0和1.4.1签名证书SHA-256一致，包名一致，versionCode由6增至7。
- 独立代码审查发现并修复：原生加载名单遗漏照片模块、Java/JS Unicode空白判定不一致。

新增命令：

```bash
node --test tests/*.test.cjs
CHROME_BIN=/path/to/chrome node tests/repair-ui.cjs
node tests/make-fixtures.cjs
java -jar /path/to/ecj.jar -17 -cp /path/to/json.jar -d test-results/java tests/java/*.java app/src/main/java/cn/cbhcool/classnote/{SafeArchive,AttachmentFiles,StateValidator,ScheduleValidator,AssetPolicy}.java
java -cp test-results/java:/path/to/json.jar cn.cbhcool.classnote.ValidatorTest
java -cp test-results/java:/path/to/json.jar cn.cbhcool.classnote.ArchiveTest
java -cp test-results/java cn.cbhcool.classnote.AssetPolicyTest
```

没有真实Android设备或模拟器，尚未实测覆盖安装、系统相机/提醒与Android WebView手势。手机验收重点：直接覆盖1.4.0后检查旧记录 → 打开/编辑/删除作业 → 两张以上照片滑动及缩放 → 点空白和系统返回退出 → 无主题记录保存并导出ZIP。


## 1.4.2 沉浸式图片预览（2026-10-08）

- 先在1.4.1实测复现：缩放后点空白退出导致底层按钮收到点击；延迟点击回归用例失败。修复后同一用例通过，下一次独立触摸仍正常。
- `CHROME_BIN=/path/to/chrome node tests/photo-immersive.cjs`：全屏无工具栏、单击照片退出、兼容点击防穿透、双击/双指缩放、平移、剩余手指、取消、左右切换边界、系统返回入口、重开/焦点、下滑退出/短距离回位。
- 34项Node领域测试通过；photo-immersive、repair-ui、ui、schedule-ui、ui-polish、import-ui、draft-ui、time-axis-ui八个浏览器测试脚本全部通过。
- SDK35资源/Java/DEX构建通过（22条弃用API警告）；APK v2/v3签名验证通过，新旧证书相同，包名相同，versionCode 7→8。
- 未连接Android真机/模拟器；系统栏隐藏恢复、系统返回手势、不同厂商WebView和覆盖安装需手机验收。原数据结构与存储代码未改动。


## 1.4.3 PhotoSwipe 跟手动画（2026-10-08）

- 先复现1.4.2横向拖动期间图片不移动、松手直接换图；新动画回归测试失败后接入PhotoSwipe 5.4.4。
- `CHROME_BIN=/path/to/chrome node tests/photo-swipe-animation.cjs` 验证真实CDP触摸下的跟手移动、邻图同时进入、松手后持续移动、未达阈值回弹、首尾边界及横竖图比例。
- 34项领域测试通过，9个浏览器测试脚本通过。升级后的photo-immersive覆盖6组手势/退出/防穿透流程，repair-ui覆盖既有作业和照片入口。
- 独立只读审查发现上滑也会关闭，先用真实触摸复现失败，再加入verticalDrag方向限制；重跑图片动画、沉浸手势和repair-ui全部通过。
- Java AssetPolicyTest确认16项页面引用均可经原生加载名单访问，未知路径仍拒绝。
- SDK35构建成功（22条既有弃用API警告）；APK v2/v3签名验证成功，证书与1.4.2一致，包名一致，versionCode 8→9。APK内包含完整MIT许可证及PhotoSwipe JS/CSS，离线加载。
- 未进行安卓真机/模拟器验收；设备上的动画流畅度、系统栏隐藏恢复、EXIF照片和覆盖安装仍需手机检查。


## 1.4.4 图片预览延迟修复（2026-10-08）

- 在旧版复现六项失败：已载入缩略图仍等待整组尺寸、首段80px拖动丢失、立即返回等待开场、空白点击等待双击、手势中加载原图、未知邻图阻塞首图。
- `CHROME_BIN=/path/to/chrome node tests/photo-latency.cjs` 扩展为9组：上述六项、取消触摸/损坏图后重开、原图失败保留缩略图、迟到邻图不取消当前放大图的原图升级。
- 独立只读审查复现迟到邻图发出change通知取消升级计时器，新增回归先失败，再在change后重新安排当前图片升级。
- 3个图片脚本和7个既有UI脚本全部回归；34项领域测试；AssetPolicyTest确认16项页面依赖可访问，未知路径拒绝。
- APK使用SDK35构建，32条兼容API弃用警告，无编译错误；v2/v3签名有效，证书与1.4.3相同；versionCode 9→10，包名不变。
- PhotoSwipe运行文件从带标注的本地修改源码用esbuild 0.25.12生成；源码包包括重建脚本和完整MIT许可证。
- 未使用Android真机/模拟器。稳定系统栏边距采用官方getInsetsIgnoringVisibility（API30+）/stable inset（API26–29）逻辑并通过编译，无法用Chromium认证实际系统栏、IME、方向变化或设备帧率。

手机验收：覆盖安装 → 连续打开/返回照片，观察记录页位置 → 缓慢左右拖动和快速切图 → 双击/双指放大后等待原图变清晰 → 放大后点空白和系统返回 → 从有键盘的编辑页看图 → 横竖屏与不同方向EXIF照片 → 恢复备份后照片正确。

开关场动画已取消，滑动弹簧和缩放动画保留。空白和系统返回不等双击；点图片仍保留220ms双击识别时间。隐藏系统栏对应区域填黑且保留稳定边距，预览和原页面不因栏可见性改变而重新排版。


## 1.4.5 放大后边缘切图（2026-10-08）

- 根因：allowPanToNext为false，阻止放大后切换；仅恢复该配置为true，原生代码及PhotoSwipe手势源码无本轮修改。
- 新增 `CHROME_BIN=/path/to/chrome node tests/photo-zoom-swipe.cjs`，实际CDP触摸覆盖5组流程。旧版前后切换2项失败，其他3项通过；修改后5/5全部通过。放大图内部平移，到边缘松手后重新向外滑切图，下一张适配屏幕；短拖/取消、多指残留、首尾边界均检查。
- photo-latency（9组）、photo-immersive（6组）、photo-swipe-animation（3组）、repair-ui（5组）全部通过。
- 独立只读审查确认配置符合手势库的边缘新手势语义，并独立重跑新增用例5/5；没有重要/严重发现。
- SDK35构建通过；v2/v3签名有效，证书与1.4.4相同；versionCode 10→11，包名不变；APK内当前配置与本地源码字节一致。
- 未用Android真机或模拟器实测；手机验收：双击/双指放大 → 左右拖动到图片边缘 → 松手后向外滑动切到相邻图 → 检查短拖回弹与关闭无误触。


## 1.5.0 笔记文件附件（2026-10-22）

- `node --test tests/*.test.cjs`：37项领域测试；附件独立内容、保存/重载/编辑/删除、旧笔记兼容、文件路径/类型/大小/数量/重复项校验。
- `CHROME_BIN=/path/to/chrome node tests/attachments-ui.cjs`：多文件选择、原文件名/大小、仅附件笔记、草稿重载、重复结果去重、拒绝错误草稿、打开下载字节一致、移除取消/确认、保存后重载、320px无横向溢出。截图已检查。
- Java `AttachmentTest`：附件原字节与元数据经ZIP往返、原生校验、恶意路径/错误长度/MIME/重复项、64MiB流式边界与超限拒绝；导入确认按批次token，覆盖Android转义MIME斜线后的确认，以及放弃/更换草稿后释放待接收批次。
- 既有ui、draft-ui、repair-ui、photo-zoom-swipe、import-ui、schedule-ui共6个浏览器脚本通过；ArchiveTest、ValidatorTest、AssetPolicyTest通过。
- SDK35资源、Java、DEX、APK构建成功；32条既有兼容API弃用警告，无编译错误；v2/v3签名验证通过。包名不变，versionCode 11→12；沿用原密钥，签名证书与1.4.5比对一致。
- 未连接Android真机或模拟器。手机验收：覆盖安装 → 旧笔记/照片仍在 → 多选PDF/PPT/音频 → 仅附件保存 → 重开/外部阅读器 → 再次添加 → 草稿放弃后重新添加 → 导出ZIP再恢复。

原生系统文件选择器、外部阅读器接收只读URI、进程回收和实际覆盖安装需手机验证。附件选择后复制进应用，单文件64MiB、每笔记100个；已移除文件的内部副本与原有照片一样保留在数据目录中，不影响历史/草稿，完整备份会包含这些副本。
