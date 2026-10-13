package com.jy.twinbuds;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.os.Looper;
import com.jy.twinbuds.audio.DeviceMonitor;
import com.jy.twinbuds.core.DeviceSnapshot;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {21, 31, 35})
public class DeviceMonitorTest {
    @Test public void unavailableProfileServiceDoesNotShowLoadingForever() {
        if (Build.VERSION.SDK_INT >= 31) Shadows.shadowOf(RuntimeEnvironment.getApplication())
                .grantPermissions(Manifest.permission.BLUETOOTH_CONNECT);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        Shadows.shadowOf(adapter).setEnabled(true);
        Shadows.shadowOf(adapter).setProfileProxy(BluetoothProfile.A2DP, null);
        AtomicReference<DeviceSnapshot> latest = new AtomicReference<>();
        DeviceMonitor monitor = new DeviceMonitor(RuntimeEnvironment.getApplication(), latest::set);
        try {
            monitor.start();
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(7));
            assertNotNull(latest.get());
            assertFalse(latest.get().readError.contains("请稍候"));
            assertTrue(latest.get().readError.contains("暂未返回"));
        } finally { monitor.stop(); }
    }
}
