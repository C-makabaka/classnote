# Implementation ledger — plan: docs/superpowers/plans/2026-09-20-classnote.md

用户明确说“开始吧”，已批准预览与开学日期；直接执行，无重复授权。
新建独立项目分支 feature/android-classnote，无现有仓库或用户代码需要隔离。
Task1→Task2：JSON状态及Core接口；Task2→Task3：Platform桥接JSON结果协议；接口一致。
环境：有Java17运行时，无JDK/SDK；下载官方SDK与Eclipse编译器到scratch，构建不需要系统包安装。

Task 1: complete — 9个领域测试通过，真实课表与输入验证完成。
Task 2: complete — Chromium流程通过，新增/完成/恢复/重载、笔记、多照片、草稿、通知路由、320px布局已验证。
Task 3: complete — Android编译和DEX成功；Java归档及验证测试通过；本机数据、媒体、提醒、日历、备份功能已接入。
Task 4: complete — 共享边界与伪代码、源码与个人签名、构建脚本、使用说明齐全；APK签名v2/v3校验通过。

独立审查：/root/android_review，一轮只读审查。修复四项Important：恢复验证类型不一致、UTF-8字节限额不一致、照片提前确认、恢复后提醒重建异常阻止界面刷新。后两项结合控制流检查、照片回归流程和构建验证；真实Activity销毁及系统提醒仍需真机验证。
补充数据保护：保存时核对当前文件版本，拒绝恢复/另一Activity更新后用旧页面覆盖新数据。
其他审查问题：导出与恢复容量统一并测试；通知打开Intent使用唯一URI；全部周次的时段重叠聚合成可展开的分周课程，并经领域测试验证。
尚未认证的环境行为：OEM相机、通知到达时刻、Activity原生重建、掉电文件系统、文件选择器写入失败。已在TESTING.md明确列出。
无外部仓库或目标合并分支；保留本地feature/android-classnote，交付源码压缩包。

## 1.1 调课更新（2026-09-20）
用户授权选择框与四种情况。计划：docs/superpowers/plans/2026-09-20-schedule-adjustments.md。
- 领域测试先红（缺少 Schedule），实现后四类和撤销通过。
- 浏览器测试先红（缺少入口），实现后选择框、预览、撤销、冲突、笔记关联、重载通过。
- Native 验证先红（损坏 before 未拒绝），加入 ScheduleValidator 后通过。
- 决策：按倒序撤销；基础时段与学期锚点在有日志时锁定；长期调整保留之前单次例外；无来源课的补课拒绝。
- 保留 schema1 的可选扩展，不重置旧记录；覆盖更新保持原签名，versionCode2。
- 独立审查进行中；无安卓真机/模拟器，仍需用户设备测试系统能力。

独立审查完成：
- Important：撤销后新课程占用原时段会冲突；回归测试先失败，改为验证被撤销事务所有恢复键。
- 重新分级为 Important：节假日停课误改已归档课程的历史；回归测试先失败，停课只作用于当前可见课程。
- Minor deferred：课程详情的通用入口未预选该课程，用户需手选；长按具体课次已正确预选。说明写入使用指南。
- 审查未认证的实际Android/WebView、原生恢复事务与覆盖安装继续列为真机验证边界；Java规则由本次本地测试和APK构建验证。
最终验证：24/24 Node；原界面和调课界面两套 Playwright 流程；Java ArchiveTest / ValidatorTest（含全部四类调课及重放）通过。SDK35编译、DEX、zipalign、v2/v3签名验证通过，13条既有兼容API弃用警告。签名证书 SHA256 与1.0一致，包名相同，versionCode2。工作分支保留，不涉及外部仓库或发布。

## 1.2界面优化（2026-09-20）
用户授权：七天自适应、统一选择框风格、右上角保存；随后要求生成简约明亮桌面图标。
根因：周末CSS固定min-width470px导致横向溢出；原生select唤起系统白色弹框；保存按钮放在长表单尾部。
改为七列minmax网格、应用内选择面板、统一sticky编辑栏与form关联提交按钮，保留底层原生select作为表单数据源。
ui-polish先红（320px周末溢出），实现后七列宽度、选择/搜索/返回、保存位置、预览失效和重载均通过。图标通过内置图像生成工具生成，原图复制进Android资源，不改写笔记资料。
1.2最终验证：24项领域测试、原界面／四类调课／界面优化三套浏览器流程、Java归档和状态校验全部通过。独立只读审查未发现Critical或Important问题。图标构建曾发现颜色必须用资源引用，改为@color后aapt2及整包构建通过。证书与1.1一致，versionCode3，v2/v3签名有效。尚未在真机上运行1.2，手机字体缩放、桌面裁切和系统软键盘为设备验证边界。

## 1.3 通用课表与交互
用户授权首次空课表、JSON导入、复制AI提示词、保留ZIP文件导入导出；图标透明前景/白底增加留白；动画与点击遮罩关闭。
采用公开课表格式classnote-timetable v1，导入预览后追加，同名和冲突拒绝，已有资料要求相同学期锚点。不删除升级用户的已有资料。旧个人课表只保留在测试夹具。
RED：fresh-install仍有7门课及无导入入口；GREEN：空状态、导入与旧数据保留通过。动画退出改为异步后，既有UI测试改为等待面板移除。
生成透明RGBA图标，白色adaptive背景；清除旧资源并统一application/round/activity引用。1.2包本身引用新图标，无法在无真机情况下确认用户系统设置缓存的具体原因。

### 1.3.0 final verification
- Independent review found a double-back race during selector exit animation. Added regression (failed before fix); retained active sheet until animation completes. Regression now passes.
- 29 domain tests, all four browser suites, Java archive/validator suites passed. Fresh/imported state survives ZIP archive round-trip.
- Rebuilt signed 1.3.0 (code 4); same signing certificate as prior versions. APK manifest application/activity icons both target ic_classnote; obsolete icon resources absent.
- No physical Android device available: native clipboard, OEM launcher refresh, and device animation remain device checks.

## 1.3.1 草稿修复
根因：beginForm立即持久化空草稿，启动使用原生confirm恢复；取消恢复未清除，所以每次弹窗。新增回归先复现失败后修复：对比表单初始值判断修改，启动直接首页，显式草稿入口，主题底部面板保存/放弃/继续；硬件返回、遮罩关闭保留编辑，单草稿覆盖前提示。新UI已截图检查；草稿及照片回归通过。未做Android真机验证。

## 1.4.0 当前时间线
按用户作息表加入14节时间；去掉课表绿边，当前周今天列绿色虚线。每250ms校准加250ms线性过渡，秒/毫秒参与计算；课间使用相邻节次间隙。设置可修改时间且学期保存保留字段；JS/Java校验和ZIP兼容。位置/时间编辑浏览器测试、原生校验和ZIP往返通过。无Android真机。

## 2026-09-29 / 1.4.1
修复作业点击入口和编辑页定位；统一删除/移除确认；添加记录内照片浏览器；主题可选，原生和JS校验同步。保留签名与数据结构，版本码7。验证详情见 docs/TESTING.md。

## 2026-10-08 / 1.4.2
移除看图工具栏，改为全屏手势交互；修复抬手关闭后的点击穿透，取消及多指手势不误退出。加入原生沉浸模式。34项领域测试、8个浏览器测试脚本和APK构建/签名检查通过；未进行安卓真机验证。

## 2026-10-08 / 1.4.3
图片浏览改用PhotoSwipe5.4.4（MIT，源码与许可证随源码包提供，运行文件随APK离线打包）。真实触摸验证跟手移动、松手过渡和边界回弹；保留沉浸UI、缩放、下滑退出和点击防穿透。修正开源库默认上滑退出行为。34项领域、9个浏览器脚本、原生资源加载名单、APK构建与同证书检查通过，未做真机验收。
