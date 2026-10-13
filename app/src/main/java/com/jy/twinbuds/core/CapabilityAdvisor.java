package com.jy.twinbuds.core;

import java.util.Locale;

/** 只给出下一步建议；连接数量和厂商能力都不是播放成功证据。 */
public final class CapabilityAdvisor {
    public enum Status { NO_BLUETOOTH, PERMISSION_NEEDED, BLUETOOTH_OFF, READ_ERROR,
        NO_DEVICE, ONE_DEVICE, READY_TO_CHECK }
    public enum Guide { SAMSUNG, LE_AUDIO, GENERAL }

    public static final class Advice {
        public final Status status;
        public final Guide guide;
        public final boolean canTest;
        public Advice(Status status, Guide guide, boolean canTest) {
            this.status = status;
            this.guide = guide;
            this.canTest = canTest;
        }
    }

    public static Advice assess(DeviceSnapshot snapshot) {
        Guide guide = snapshot.manufacturer.toLowerCase(Locale.ROOT).contains("samsung")
                ? Guide.SAMSUNG
                : snapshot.leAudio == DeviceSnapshot.Feature.SUPPORTED
                    || snapshot.broadcast == DeviceSnapshot.Feature.SUPPORTED
                        ? Guide.LE_AUDIO : Guide.GENERAL;
        if (!snapshot.hasBluetooth) return new Advice(Status.NO_BLUETOOTH, guide, false);
        if (!snapshot.permissionGranted) return new Advice(Status.PERMISSION_NEEDED, guide, false);
        if (!snapshot.enabled) return new Advice(Status.BLUETOOTH_OFF, guide, false);
        if (!snapshot.readError.isEmpty()) return new Advice(Status.READ_ERROR, guide, false);
        if (snapshot.devices.isEmpty()) return new Advice(Status.NO_DEVICE, guide, false);
        // LE Audio 可将一组设备隐藏在单个端点后，因此一个端点也允许用户主动试听。
        return new Advice(snapshot.devices.size() == 1 ? Status.ONE_DEVICE : Status.READY_TO_CHECK,
                guide, true);
    }
}
