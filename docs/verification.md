# 交付验证记录

日期：2026-10-13。版本：同听 1.0.0（开发签名测试版）。

## 已执行并通过

命令：`gradlew.bat --offline --console=plain testDebugUnitTest lintDebug assembleDebug`

- **30 项检查通过，0 失败**：17 项纯 Java 行为测试，12 项 Android 框架模拟行为测试，1 项布局渲染检查。
- 核心判断：设备连接数量不等于双输出成功；拒绝权限、关闭蓝牙、无设备、读取错误时禁用试听；一个 LE 组端点允许主动试听但不会自动认定成功。
- 试听状态：播放结束前不接受结果；新试听、取消、离开页面及已观测到的连接变化使反馈失效。
- 提示音：4 秒、44.1 kHz、16-bit 双声道同内容，峰值限制与首尾静音检查通过。
- 报告：连接信息和用户反馈有来源标识；不导出地址标识符，并清除厂商可能放入产品名或路由名中的 MAC 地址。
- Robolectric 4.15.1：在 API 21 / 31 / 35（Android 5 / 12 / 15）分别验证启动、无耳机禁止试听、拒绝权限仍能打开指引和报告、蓝牙设置跳转、返回时重新检测、服务不返回列表时不无限显示读取中。
- Android Lint：**0 错误，3 个依赖版本更新提示**。保留已验证的 Gradle 9.3.1、编译期 annotation 1.8.1 与支持 API 21 的 Robolectric 4.15.1。未关闭 Lint 错误检查。
- Android 原生图形运行时渲染：已目视检查首页与下半页；中文没有重叠，操作按钮和报告入口可见，长页面可滚动。输出位于 `artifacts/screenshots/01-home.png` 与 `02-listening.png`。这些是框架渲染预览，不是真机截图。
- APK 编译通过。`aapt dump badging` 确认 applicationId `com.jy.twinbuds`、minSdk 21、targetSdk 36、版本号 1.0.0，存在启动 Activity。
- 最终交付 APK 经 `apksigner verify --verbose` 检查，v1 / v2 签名验证通过。工具另给出标准 Gradle META-INF 条目的 JAR 签名提示，验证返回码为 0。
- APK 权限仅包含 API 30 及以下的 `BLUETOOTH` 与 `BLUETOOTH_CONNECT`；没有网络、麦克风、位置、扫描或后台服务权限。

## 回归证据

实现阶段已观察到缺失兼容性判断、试听状态与 PCM 生成对应的断言失败，再实现并取得通过。补充测试复现了 profile 请求重试不断重置开始时间、厂商产品名可能携带 MAC 两个问题；修复后全部 30 项检查通过。可选 LE 能力接口异常现在降级为未知，不阻断其他连接信息读取。

## 未完成的设备验证

- 当前没有连接真实 Android 手机或蓝牙耳机，**没有验证两副实体耳机同时出声**。
- Android 模拟器启动失败：现有 QEMU 进程退出码为 `0xC0000135`，本机加速检查还报告未安装 Android Emulator hypervisor driver。本次未完成模拟器 APK 安装或实体设备安装测试。
- 未执行 Android 16 的框架模拟或真机验证；API 36 多路路由接口只通过编译和版本门禁静态检查。
- 未测全部手机品牌、Android 小版本、通话、受保护媒体、不同耳机之间的延迟和长期连接稳定性。
- 最终 APK 是开发签名包，不是商店发布包。

以上限制不影响已经生成的 APK 与源码交付，但不能将框架模拟、代码编译或用户一次试听结果描述为全设备兼容认证。

## APK 校验

文件：`dist/TwinBuds-1.0.0-debug.apk`，897289 字节。

SHA-256：`F01342865BF511E463980049F4A7CD8D89BB18F018AC16EB4F1FD2DDDA3C8C79`
