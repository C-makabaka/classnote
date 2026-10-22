# 课记 Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task. Track completion below and in progress.md.

**Goal:** 交付遵循已批准课表设计、可离线使用的Android APK及可继续开发的源码。

**Architecture:** 原生Java负责可靠文件、图片导入、系统提醒、日历与备份；本地WebView负责既定界面。纯JS领域层隔离日期、验证、CRUD和排序；Android桥接与浏览器验证适配器共用该领域层。

**Tech Stack:** Android API26–35, Java 8 bytecode, HTML/CSS/JavaScript, Node test runner. SDK35 aapt2/d8/apksigner；Android Studio Gradle配置同时交付。

**Spec:** docs/superpowers/specs/2026-09-20-classnote-design.md

## Global Constraints
- 第一周2026-09-07；17周；截图课程原样录入，不植入演示作业。
- 中文界面，课程名称近黑粗体，应用离线可用。
- 私有图片＋原子数据提交，失败不覆盖旧数据；无网络权限。
- 系统日历是用户确认保存的独立副本。
- APK及源码均需持久保存；没有真机验证不能声称通过真机测试。

## Review Focus
- 设备日期位于学期外或跨年，仍可进入课程资料与全部周次。
- 作业截止早于当前时间/提醒偏移早于当前时间，不能不断重复提醒。
- 相机/相册返回时Activity重建，草稿及已选图片不丢失。
- 备份损坏、重复ZIP条目、路径穿越或中途失败，不破坏已有记录。
- 原生桥接不能让外部网页/文件获得私有目录访问能力。

### Task 1: 领域数据与本地存储协议
**Files:** app/src/main/assets/core.js, seed.js; tests/core.test.cjs
**Interfaces:** `Core.currentWeek(start,today)`, `weekRange(start,week)`, `activeLessons(state,week)`, `pendingCount(state,courseId)`, `orderedTasks(state,courseId)`, `validateState(state)`, `saveEntity(state,collection,record)`, `toggleTask(state,id)`, `tombstone(state,collection,id)`.
- [ ] 写测试覆盖2026-09-20为第2周、9/28跨月、12/28跨年；单双周与课号、完成沉底、恢复、空标题和坏引用、坏日期拒绝。
```js
assert.equal(Core.currentWeek('2026-09-07','2026-09-20'),2);
assert.deepEqual(Core.weekRange('2026-09-07',4),['2026-09-28','2026-10-04']);
```
- [ ] `node --test tests/core.test.cjs`：先观察缺失功能失败，再完成真实数据层与种子课表，直到全部通过。
- [ ] 提交领域层并记录接口。

### Task 2: 可操作界面
**Files:** app/src/main/assets/index.html, style.css, app.js, platform.js; tests/ui.cjs
**Interfaces:** 消费Task1；`Platform.load/save`, `pickImages`, `camera`, `calendar`, `backup`, `restore`, `permissions`；原生回调`window.onNativeEvent(type,payload)`。
- [ ] 编写浏览器流程：新作业→课表徽标→完成→灰色底部→重载后保留；新笔记与搜索；查看第4周日期。
- [ ] 实现主页、详情、课程管理、笔记和作业表单、设置；所有用户文字转义，持久化结果成功后才更新界面。
- [ ] 执行Node领域测试与浏览器流程；检查320px表单和课表不横向溢出。
- [ ] 提交界面。

### Task 3: Android能力与备份
**Files:** MainActivity.java, AppStore.java, MediaStore.java, AlarmScheduler.java, ReminderReceiver.java, CameraProvider.java, SafeArchive.java, StateValidator.java; tests/java/ArchiveTest.java
**Interfaces:** JavaScript桥方法与Task2协议一致；使用JSON字符串返回`{ok,value,error}`。通知ID以UUID URI区分，原生重读当前记录。
- [ ] 为安全归档写真实临时文件测试：正常往返、`../`、重复条目、超限条目拒绝。
- [ ] 实现私有源WebView、原子JSON读写、图片复制、相机授予、通知权限与重启重排、日历预填。
- [ ] 实现恢复暂存目录与原目录交换、回滚恢复；验证结构和图片引用后再提交。
- [ ] Android资源、Java、DEX构建；运行归档测试和JS/UI套件。无设备的原生交互列入真机检查清单。
- [ ] 提交原生能力。

### Task 4: 交付与共享扩展
**Files:** docs/SHARING.md, app/src/main/assets/sharing.js, README.md, build.gradle, settings.gradle, app/build.gradle, scripts/build.sh, docs/TESTING.md
**Interfaces:** `SharingPort.pull/push/invite`本地禁用；共享服务端伪代码以space membership校验、按revision提交、不覆盖冲突版本、个人完成状态独立为规则。
- [ ] 完成共享接口与伪代码，Android Studio配置、手动构建脚本、中文使用和测试说明。
- [ ] 全部测试、APK签名校验、独立代码审查；重要问题修复并回归。
- [ ] 打包已签名APK、源码、使用说明。源码包含构建所需开发签名密钥，仅用于个人版本后续覆盖安装，禁止用于公开生产签名。
- [ ] 上传交付并提供下载链接。
