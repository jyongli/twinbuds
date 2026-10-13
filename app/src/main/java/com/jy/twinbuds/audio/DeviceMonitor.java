package com.jy.twinbuds.audio;

import android.Manifest;
import android.annotation.SuppressLint;
import androidx.annotation.RequiresApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.jy.twinbuds.core.DeviceSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 仅在界面前台读取连接，不扫描、不主动配对，也不改变系统音频路由。 */
public final class DeviceMonitor {
    public interface Listener { void onSnapshot(DeviceSnapshot snapshot); }
    private final Context context;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final BluetoothAdapter adapter;
    private final Map<Integer, BluetoothProfile> proxies = new HashMap<>();
    private final Set<Integer> requested = new HashSet<>();
    private boolean running;
    private long requestedAt = -1;

    public DeviceMonitor(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager == null ? null : manager.getAdapter();
    }

    public static boolean hasPermission(Context context) {
        return Build.VERSION.SDK_INT < 31 || context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }

    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (!running) return;
            handler.removeCallbacks(this);
            listener.onSnapshot(read());
            handler.postDelayed(this, 1500);
        }
    };

    public void start() {
        if (running) return;
        running = true;
        handler.post(poll);
    }

    private final BluetoothProfile.ServiceListener serviceListener = new BluetoothProfile.ServiceListener() {
        @Override public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (!running) {
                if (adapter != null) adapter.closeProfileProxy(profile, proxy);
                return;
            }
            proxies.put(profile, proxy);
            refresh();
        }
        @Override public void onServiceDisconnected(int profile) {
            proxies.remove(profile);
            requested.remove(profile);
            requestedAt = -1;
            refresh();
        }
    };

    public void refresh() {
        if (!running) return;
        handler.removeCallbacks(poll);
        handler.post(poll);
    }

    public void stop() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        closeProxies();
    }

    private void closeProxies() {
        if (adapter != null) {
            for (Map.Entry<Integer, BluetoothProfile> entry : proxies.entrySet()) {
                try { adapter.closeProfileProxy(entry.getKey(), entry.getValue()); }
                catch (RuntimeException ignored) { /* 权限被撤回时仍要完成本地清理。 */ }
            }
        }
        proxies.clear();
        requested.clear();
        requestedAt = -1;
    }

    @SuppressLint("MissingPermission") // 每次读取前检查；权限在调用之间变化仍由异常分支处理。
    private void requestProfile(int profile) {
        if (requested.contains(profile)) return;
        requested.add(profile);
        // 拒绝请求也保留首次尝试时间，重试不能无限延长“读取中”。
        if (requestedAt < 0) requestedAt = SystemClock.elapsedRealtime();
        if (!adapter.getProfileProxy(context, serviceListener, profile)) requested.remove(profile);
    }

    @SuppressLint("MissingPermission")
    public DeviceSnapshot read() {
        boolean permission = hasPermission(context);
        boolean enabled = false;
        DeviceSnapshot.Feature le = DeviceSnapshot.Feature.UNKNOWN;
        DeviceSnapshot.Feature broadcast = DeviceSnapshot.Feature.UNKNOWN;
        Map<String, DeviceSnapshot.Device> devices = new LinkedHashMap<>();
        List<String> outputs = new ArrayList<>();
        String error = "";
        try {
            if (adapter != null && permission) {
                enabled = adapter.isEnabled();
                if (enabled) {
                    if (running) requestProfile(BluetoothProfile.A2DP);
                    le = readLeFeature(false);
                    broadcast = readLeFeature(true);
                    if (running && le == DeviceSnapshot.Feature.SUPPORTED) requestLeProfile();
                    for (Map.Entry<Integer, BluetoothProfile> entry : proxies.entrySet()) {
                        String transport = entry.getKey() == BluetoothProfile.A2DP ? "经典蓝牙 A2DP" : "LE Audio";
                        for (BluetoothDevice device : entry.getValue().getConnectedDevices()) {
                            String name = device.getName();
                            devices.put(device.getAddress(), new DeviceSnapshot.Device(device.getAddress(),
                                    name == null || name.trim().isEmpty() ? "未命名耳机" : name, transport));
                        }
                    }
                } else {
                    closeProxies();
                }
            }
            if (Build.VERSION.SDK_INT >= 23) {
                AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                Map<String, DeviceSnapshot.Device> audioDevices = new LinkedHashMap<>();
                for (AudioDeviceInfo device : audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                    String label = deviceLabel(device);
                    outputs.add(label);
                    if (isMediaBluetooth(device.getType()) && permission && enabled) {
                        String identity = Build.VERSION.SDK_INT >= 28 ? device.getAddress() : "";
                        if (identity.isEmpty()) identity = "audio:" + device.getId();
                        audioDevices.put(identity, new DeviceSnapshot.Device(identity,
                                device.getProductName().toString(), "系统蓝牙音频端点"));
                    }
                }
                // 某些厂商不提供 profile 列表，用系统音频端点补充；不把两种来源重复计数。
                if (devices.isEmpty()) devices.putAll(audioDevices);
            }
            if (enabled && permission && proxies.isEmpty() && devices.isEmpty()) {
                error = SystemClock.elapsedRealtime() - requestedAt < 5000
                        ? "正在读取蓝牙连接状态，请稍候…" : "系统暂未返回蓝牙媒体设备列表，请刷新或重新连接耳机。";
            }
        } catch (SecurityException exception) {
            permission = hasPermission(context);
            error = "系统拒绝读取蓝牙状态，请检查附近设备权限。";
            devices.clear();
        } catch (RuntimeException exception) {
            error = "系统蓝牙服务暂不可用，请刷新或重新打开蓝牙。";
            devices.clear();
        }
        return new DeviceSnapshot(Build.VERSION.SDK_INT, Build.MANUFACTURER, Build.MODEL,
                adapter != null, permission, enabled, le, broadcast,
                new ArrayList<>(devices.values()), outputs, error);
    }

    private static DeviceSnapshot.Feature feature(int code) {
        if (code == BluetoothStatusCodes.FEATURE_SUPPORTED) return DeviceSnapshot.Feature.SUPPORTED;
        if (code == BluetoothStatusCodes.FEATURE_NOT_SUPPORTED) return DeviceSnapshot.Feature.NOT_SUPPORTED;
        return DeviceSnapshot.Feature.UNKNOWN;
    }

    private void requestLeProfile() {
        if (Build.VERSION.SDK_INT >= 33) requestProfile(BluetoothProfile.LE_AUDIO);
    }

    private DeviceSnapshot.Feature readLeFeature(boolean broadcast) {
        if (Build.VERSION.SDK_INT < 33 || adapter == null) return DeviceSnapshot.Feature.UNKNOWN;
        try {
            return feature(broadcast ? adapter.isLeAudioBroadcastSourceSupported() : adapter.isLeAudioSupported());
        } catch (RuntimeException exception) {
            // 厂商的可选能力查询失败时仍读取经典蓝牙连接，不阻断整个流程。
            return DeviceSnapshot.Feature.UNKNOWN;
        }
    }

    @RequiresApi(23)
    private static boolean isMediaBluetooth(int type) {
        return type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || (Build.VERSION.SDK_INT >= 31
                && (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
                || (Build.VERSION.SDK_INT >= 33 && type == AudioDeviceInfo.TYPE_BLE_BROADCAST);
    }

    @RequiresApi(23) // Android 5 只使用 A2DP profile；Lint 会检查调用方的版本条件。
    public static String deviceLabel(AudioDeviceInfo device) {
        String type;
        switch (device.getType()) {
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER: type = "手机扬声器"; break;
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE: type = "手机听筒"; break;
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP: type = "蓝牙媒体"; break;
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO: type = "蓝牙通话"; break;
            default: type = "音频输出"; break;
        }
        return type + " · " + device.getProductName();
    }
}
