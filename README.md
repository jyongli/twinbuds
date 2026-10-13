# 同听 TwinBuds

一个原生 Android 双蓝牙耳机设置与试听助手，基础兼容目标为 **Android 5.0（API 21）及以上**，使用中文界面。

**双音频由手机系统提供。** 此应用不会为不支持的手机增加双蓝牙输出，也不承诺覆盖所有手机、耳机或媒体应用。应用安装兼容范围与双音频硬件支持范围是不同的验收项。

## 使用

1. 将 `dist/TwinBuds-1.0.0-debug.apk` 传到手机，点击安装。若系统询问，只为此次使用的文件管理器或浏览器授予安装来源权限。这是开发签名的测试安装包。
2. 打开「同听」。Android 12+ 按提示允许读取附近设备；拒绝后仍能查看操作指引。Android 5–11 使用安装时的蓝牙读取权限。
3. 点击「打开蓝牙设置」，分别连接两副耳机，并检查“媒体音频”是否开启。
4. 按应用中的对应指引，在系统媒体输出 / 音频共享中启用两副耳机。三星、LE Audio 与通用系统分别提供说明；应用不会调用不公开的厂商设置接口。
5. 返回同听，先暂停其他音乐或视频并调低媒体音量，再点击「播放试听音」。分别询问两位听者是否听到，记录本轮反馈。
6. 再打开实际要使用的音乐或视频应用验证。系统共享功能不需要同听在后台保持运行。

若连接第二副耳机时第一副停止播放，且系统输出界面只能单选，普通应用无法强制解决。支持 LE Audio、蓝牙 5.x、连接数量为 2，均不能单独证明双音频可用。

## 功能与边界

- 读取实际连接的 A2DP / LE Audio 设备，而不是把配对列表当作连接列表。
- 读取系统可见音频端点；已连接设备、可用输出、本次试听路由分开表达。
- 用一条系统媒体音轨播放本地生成的 4 秒旋律，首尾淡入淡出。不会捕获手机内录或麦克风，不改变系统音量，不创建两条假独立蓝牙音轨。
- Android 16+ 读取本次试听的多输出路由。Android 6–15 的路由接口信息不完整，明确标注限制；Android 5 只使用蓝牙 profile 信息。
- 播放头完成才允许记录试听反馈。退出应用、试听中断、已观测到的连接或输出列表变化会清除旧验证结果。反馈只说明本轮用户听感，不能证明后续第三方应用或通话的输出。
- 本地报告支持预览、复制和由用户主动分享。报告包含手机型号与耳机名称，不包含蓝牙地址。
- 无网络、位置、录音、扫描、通知、后台服务权限；不采集或上传音频。唯一保存的偏好是是否请求过附近设备权限。

## 构建

需要 JDK 17、Android SDK Platform 36 和 Build Tools 36.0.0。项目固定使用 Gradle 9.3.1、Android Gradle Plugin 9.1.0；Gradle Wrapper 校验下载的 SHA-256。

通过 Android Studio 打开工程，设置 Gradle JDK 为 17、SDK 为本机已安装位置；或在 `local.properties` 中设置 `sdk.dir`。Windows 的盘符冒号需写成 `C\:/...`。本机路径文件不包含在源码包中。

```powershell
# JAVA_HOME 应指向本机 JDK 17；只影响本次终端。
$env:JAVA_HOME = '你的 JDK 17 目录'
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

macOS / Linux 使用 `./gradlew` 执行同样的任务。首次运行需要联网下载构建与测试依赖，应用本身不联网。APK 默认生成于 `app/build/outputs/apk/debug/app-debug.apk`。

测试包含纯 Java 行为测试、Robolectric Android 5 / 12 / 15 框架模拟测试与 Android 原生图形运行时布局预览。框架模拟不等于模拟器安装成功，更不能验证实体蓝牙链路。`artifacts/screenshots/` 是生成的布局预览目录，不属于真机截图。

## 工程结构

- `core/CapabilityAdvisor`：保守的兼容性建议，不推断双输出成功。
- `core/ListeningSession`：试听状态、用户反馈和连接变化失效。
- `core/TonePcm`：本地 PCM 短旋律。
- `core/ReportFormatter`：区分证据来源的兼容性报告。
- `audio/DeviceMonitor`：前台设备状态采样、权限与版本分支。
- `audio/TestTonePlayer`：音频焦点、播放头完成、实际路由观察与资源释放。
- `MainActivity` 与 `ui/HeadphonesView`：原生中文界面和矢量绘图。

## 设备验收

当前工程不包含任何“所有设备可用”的认证。具体交付验证记录见 `docs/verification.md`。实体设备应逐项验证：

- 两副耳机同时听到测试音，而非同一副的两个耳塞。
- 实际音乐、视频应用都能按预期输出；检查各应用自己的路由选择。
- 每副单独断连、重连、关闭蓝牙、撤销权限后，界面能更新且不保留过期的成功反馈。
- 试听期间退出应用或收到电话，提示音立即停止；通话输出不属于本应用承诺。
- 两副耳机可能存在不同延迟。本应用不提供延迟补偿或强制同步。

## 官方资料

- [Android 多设备路由](https://source.android.com/docs/core/audio/combined-audio-routing)
- [Android AudioRouting](https://developer.android.com/reference/android/media/AudioRouting)
- [三星双耳机设置](https://www.samsung.com.cn/support/mobile-devices/how-can-samsung-connect-two-bluetooth-headsets-at-the-same-time/)
- [Pixel 音频共享及耳机要求](https://support.google.com/pixelphone/answer/16483797?hl=zh-Hans)

尚未发布应用商店。用于后续发布时，请自行配置正式签名并在目标设备上完成硬件验收。
