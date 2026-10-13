package com.jy.twinbuds.core;

/** 本地纯文本报告，明确区分系统能力、连接信息与用户试听反馈。 */
public final class ReportFormatter {
    public static String format(DeviceSnapshot snapshot, ListeningSession.Outcome outcome,
            String observedRoutes, String timestamp) {
        StringBuilder text = new StringBuilder("同听 · 兼容性报告\n");
        text.append("时间：").append(timestamp).append("\n");
        text.append("设备：").append(snapshot.manufacturer).append(" ").append(snapshot.model).append("\n");
        text.append("Android API：").append(snapshot.sdk).append("\n");
        text.append("蓝牙硬件：").append(snapshot.hasBluetooth ? "存在" : "未检测到").append("\n");
        text.append("连接权限：").append(snapshot.permissionGranted ? "已具备" : "未授权").append("\n");
        text.append("蓝牙开关：").append(!snapshot.permissionGranted ? "未读取"
                : snapshot.enabled ? "已开启" : "未开启").append("\n");
        text.append("LE Audio：").append(featureLabel(snapshot.leAudio)).append("\n");
        text.append("LE 广播源：").append(featureLabel(snapshot.broadcast)).append("\n");
        text.append("检测说明：").append(snapshot.readError.isEmpty() ? "检测完成" : snapshot.readError).append("\n");
        text.append("\n已连接媒体设备（连接不等于正在播放）：\n");
        if (snapshot.devices.isEmpty()) text.append("未读取到媒体蓝牙设备\n");
        for (DeviceSnapshot.Device device : snapshot.devices) {
            text.append("• ").append(device.name).append(" / ").append(device.transport).append("\n");
        }
        text.append("\n系统可见输出（不等于当前路由）：\n");
        if (snapshot.outputs.isEmpty()) text.append("无可用信息；Android 5 不提供此项接口\n");
        for (String output : snapshot.outputs) text.append("• ").append(output).append("\n");
        text.append("\n本次测试音路由：").append(observedRoutes.isEmpty() ? "未取得" : observedRoutes).append("\n");
        text.append("用户试听反馈：").append(outcomeLabel(outcome)).append("\n");
        text.append("\n本结果只对应本轮试听，不代表其他应用、受保护内容或通话也会双路播放。\n");
        text.append("系统决定音频输出；同听不能为不支持的硬件开启双音频。\n");
        text.append("未采集音频；报告不含蓝牙地址，不自动上传。设备名和型号会在你主动分享时发出。\n");
        // 部分厂商用 MAC 作为产品名或路由名，导出时同样脱敏。
        return text.toString().replaceAll("(?i)(?<![0-9a-f])(?:[0-9a-f]{2}[:-]){5}[0-9a-f]{2}(?![0-9a-f])", "[蓝牙地址已隐藏]");
    }

    public static String featureLabel(DeviceSnapshot.Feature value) {
        switch (value) {
            case SUPPORTED: return "系统报告支持，仍需兼容耳机与系统设置";
            case NOT_SUPPORTED: return "系统报告不支持此项；不排除厂商其他双音频方案";
            default: return "未知 / 系统未提供信息";
        }
    }

    public static String outcomeLabel(ListeningSession.Outcome outcome) {
        switch (outcome) {
            case BOTH_HEARD: return "两副都听到（用户确认）";
            case ONE_HEARD: return "只有一副听到（用户反馈）";
            case NONE_HEARD: return "两副都没听到（用户反馈）";
            default: return "尚未验证";
        }
    }
}
