package com.jy.twinbuds;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Build;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

/** 模拟不同版本的真实 Activity 生命周期和 Android 服务边界，不伪造双耳机成功。 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {21, 31, 35})
public class MainActivityTest {
    @Test public void noConnectedHeadsetsNeverEnableAudioTest() {
        if (Build.VERSION.SDK_INT >= 31) Shadows.shadowOf(RuntimeEnvironment.getApplication())
                .grantPermissions(Manifest.permission.BLUETOOTH_CONNECT);
        Shadows.shadowOf(BluetoothAdapter.getDefaultAdapter()).setEnabled(false);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            MainActivity activity = controller.get();
            assertFalse(((Button) activity.findViewById(R.id.play)).isEnabled());
            assertEquals(activity.getString(R.string.bluetooth_off), ((TextView) activity.findViewById(R.id.status_title)).getText().toString());
            activity.findViewById(R.id.connect).performClick();
            Intent opened = Shadows.shadowOf(activity).getNextStartedActivity();
            assertEquals(Settings.ACTION_BLUETOOTH_SETTINGS, opened.getAction());
        }
    }

    @Test public void guidanceAndReportAreAvailableWithoutPermission() {
        if (Build.VERSION.SDK_INT >= 31) Shadows.shadowOf(RuntimeEnvironment.getApplication())
                .denyPermissions(Manifest.permission.BLUETOOTH_CONNECT);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            MainActivity activity = controller.get();
            assertFalse(((Button) activity.findViewById(R.id.play)).isEnabled());
            if (Build.VERSION.SDK_INT >= 31) {
                assertEquals(activity.getString(R.string.need_permission), ((TextView) activity.findViewById(R.id.status_title)).getText().toString());
            }
            activity.findViewById(R.id.guide).performClick();
            AlertDialog guidance = ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(guidance.isShowing());
            guidance.dismiss();
            activity.findViewById(R.id.report).performClick();
            AlertDialog report = ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(report.isShowing());
            report.dismiss();
        }
    }

    @Test public void resumeAfterSettingsRechecksBluetoothState() {
        if (Build.VERSION.SDK_INT >= 31) Shadows.shadowOf(RuntimeEnvironment.getApplication())
                .grantPermissions(Manifest.permission.BLUETOOTH_CONNECT);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            controller.pause().stop();
            Shadows.shadowOf(BluetoothAdapter.getDefaultAdapter()).setEnabled(false);
            controller.restart().start().resume();
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertEquals(controller.get().getString(R.string.bluetooth_off), ((TextView) controller.get()
                    .findViewById(R.id.status_title)).getText().toString());
            assertFalse(((Button) controller.get().findViewById(R.id.play)).isEnabled());
        }
    }
}
