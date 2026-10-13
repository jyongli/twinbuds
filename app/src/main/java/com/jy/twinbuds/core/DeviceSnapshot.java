package com.jy.twinbuds.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 一次前台检测结果；设备地址只参与本地去重，不进入报告。 */
public final class DeviceSnapshot {
    public enum Feature { UNKNOWN, SUPPORTED, NOT_SUPPORTED }

    public static final class Device {
        public final String identity;
        public final String name;
        public final String transport;

        public Device(String identity, String name, String transport) {
            this.identity = identity;
            this.name = name;
            this.transport = transport;
        }
    }

    public final int sdk;
    public final String manufacturer;
    public final String model;
    public final boolean hasBluetooth;
    public final boolean permissionGranted;
    public final boolean enabled;
    public final Feature leAudio;
    public final Feature broadcast;
    public final List<Device> devices;
    public final List<String> outputs;
    public final String readError;

    public DeviceSnapshot(int sdk, String manufacturer, String model, boolean hasBluetooth,
            boolean permissionGranted, boolean enabled, Feature leAudio, Feature broadcast,
            List<Device> devices, List<String> outputs, String readError) {
        this.sdk = sdk;
        this.manufacturer = manufacturer;
        this.model = model;
        this.hasBluetooth = hasBluetooth;
        this.permissionGranted = permissionGranted;
        this.enabled = enabled;
        this.leAudio = leAudio;
        this.broadcast = broadcast;
        this.devices = Collections.unmodifiableList(new ArrayList<>(devices));
        this.outputs = Collections.unmodifiableList(new ArrayList<>(outputs));
        this.readError = readError;
    }

    public String topologyKey() {
        List<String> ids = new ArrayList<>();
        for (Device device : devices) ids.add(device.identity + ":" + device.transport);
        Collections.sort(ids);
        // 加入权限、开关和输出列表，避免切换设备后沿用上一轮试听反馈。
        List<String> routes = new ArrayList<>(outputs);
        Collections.sort(routes);
        return permissionGranted + "/" + enabled + "/" + readError + "/" + ids + "/" + routes;
    }
}
