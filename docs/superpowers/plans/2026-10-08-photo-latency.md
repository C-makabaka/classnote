# 图片预览延迟修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 消除打开、返回和起步拖动的人为等待，避免系统栏切换导致页面尺寸跳动。

**Architecture:** 由已加载的缩略图提供尺寸与首帧，邻图独立加载；放大停止后异步升级原图。PhotoSwipe继续管理手势和回弹，小范围修改起步位移与双击等待。Android保持系统栏对应的稳定边距，并用黑色填充隐藏栏区域。

**Tech Stack:** Android SDK35、WebView、JavaScript、PhotoSwipe 5.4.4、Playwright。

**Spec:** 本文需求来自本轮用户反馈：图片不跟手、打开有UI变动及短暂卡顿、返回延迟；此前图片独占、无按钮、防点击穿透需求仍有效。

## Global Constraints

- 保留包名、签名和现有数据结构；版本1.4.4，versionCode 10。
- 离线加载；MIT许可证和可重建的修改源码随包交付。
- 单击退出、双击/双指缩放、左右切图、下滑退出、放大后平移、防点击穿透继续有效。
- 没有Android设备，不宣称真机帧率或系统栏运行验证。

## Review Focus

- 未载入或损坏的邻图不能拖住已载入的当前图。
- 原图请求结束时已关闭或重开预览，不能污染新会话。
- 放大期间不能替换图层造成手势中断。
- 恢复备份后相同文件名不能命中旧图片缓存。
- 横竖图、EXIF方向、键盘与Android26–35稳定边距兼容。

### Task 1: 即时缩略图预览与渐进原图

**Files:** app/src/main/assets/{app.js,photo-viewer.js,platform.js}, tests/photo-latency.cjs

**Interfaces:** showPhoto传入name、label、msrc、width、height；PhotoViewer.open(items,index,origin)沿用现有调用；Platform.media(name,thumb)保留并提供会话缓存键。

- [x] 先写已载入缩略图不等尺寸、不请求原图、原图迟到与未知邻图的浏览器测试，运行确认旧版失败。
- [x] 从thumb尺寸创建PhotoSwipe；未知尺寸单独异步测量；原图仅在停止放大后解码并升级，失败保留thumb。
- [x] 重跑新增用例与既有图片流程。

### Task 2: 起步跟手与即时退出

**Files:** vendor/photoswipe-5.4.4/src/js/gestures/gestures.js, scripts/build-photoswipe.cjs, app/src/main/assets/{photoswipe.umd.min.js,photo-viewer.js}, docs/THIRD_PARTY.md

**Interfaces:** PhotoSwipe全局构造器与现有事件接口不变；小范围源码修改可用固定版本esbuild重新构建。

- [x] 新增单次move即保留完整位移、打开后马上返回、空白处立即退出测试，运行确认旧版失败。
- [x] 关闭开关场输入等待；拖动识别阈值6px且保留识别前位移；双击等待220ms；空白处不等待双击；缩放动画保留。
- [x] 重跑latency、immersive和swipe测试，核对双击、防穿透及多指取消。

### Task 3: 原生稳定视口、缓存与发布

**Files:** app/src/main/java/cn/cbhcool/classnote/MainActivity.java, 版本文件、README、docs/TESTING.md

**Interfaces:** Native.immersive(boolean)沿用；系统栏可隐藏，但WebView边距不随可见性变化；媒体URL按页面/恢复会话区分。

- [x] Android30+用getInsetsIgnoringVisibility，26–29使用稳定布局标志及stable insets；隐藏栏区域填黑。
- [x] 媒体响应允许私有缓存，恢复备份更换URL会话键；不改变资产缓存策略。
- [x] 跑图片及既有UI、领域、原生资产名单测试；SDK35编译并校验签名、版本和包内容。
- [x] 独立审查一次，处理重要问题；保存APK及源码。

## Execution ledger

- 使用已有独立源码目录；项目没有git历史，保留文档及测试日志而不创建无关仓库。
- 用户已授权修复及交付，直接实施。

- 旧版新增六项延迟测试全部失败，修改后扩展八项全部通过。
- 三个图片脚本及七个既有UI脚本通过；34项领域测试与16项原生资产名单通过。
- SDK35构建通过；最终独立审查与交付记录将补在TESTING.md。
- 没有真机；固定系统栏边距保留为黑色区域，避免页面因栏可见性变化而重新布局。

- 最终审查发现迟到邻图发出change通知取消当前原图升级；新增第九组回归复现失败，change后重新排队修复，完整测试与构建重跑。
- 上传按已有APK和源码的版本保护替换，保留升级链。
