# 放大后的边缘切图修复

需求：用户反馈1.4.4图片放大后无法切换。

根因：photo-viewer.js将PhotoSwipe原生allowPanToNext配置为false，阻止了放大图片在边缘继续导航；官方默认值为true。

实施：仅恢复此选项，不修改手势库。先平移到边缘、松手再向外滑切图；下一张适配屏幕。

验证：tests/photo-zoom-swipe.cjs真实CDP触摸覆盖横图下一张、竖图上一张、内部平移、短拖/取消、多指残留与首尾边界。旧版5项中前两项失败，其他三项通过。修改后重跑该脚本、photo-latency、photo-immersive、photo-swipe-animation和repair-ui，并完成SDK35构建/同签名验证。

交付：1.4.5，versionCode 11，APK和完整源码沿用原文件身份更新。没有真机或模拟器，不宣称真机体验已验证。

结果：新增5/5、既有图片9+6+3及repair-ui 5组通过；独立审查通过；SDK35、签名与APK资源一致性通过。
