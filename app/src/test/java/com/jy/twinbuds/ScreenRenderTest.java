package com.jy.twinbuds;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import android.widget.ScrollView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;

/** 用 Android 原生绘图运行时生成布局检查图片；不称为真机截图。 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "zh-rCN-w411dp-h891dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ScreenRenderTest {
    @Test public void rendersScrollablePermissionAndTestScreens() throws Exception {
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.BLUETOOTH_CONNECT);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup().visible()) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            MainActivity activity = controller.get();
            View root = activity.getWindow().getDecorView();
            root.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(2340, View.MeasureSpec.EXACTLY));
            root.layout(0, 0, 1080, 2340);
            save(root, "01-home.png");
            ScrollView scroll = activity.findViewById(R.id.scroll);
            scroll.scrollTo(0, scroll.getChildAt(0).getHeight());
            save(root, "02-listening.png");
            assertFalse(activity.findViewById(R.id.play).isEnabled());
        }
    }

    private void save(View root, String name) throws Exception {
        File directory = new File(System.getProperty("twinbuds.screenshots"));
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IllegalStateException("Cannot create screenshot directory");
        Bitmap bitmap = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        try (FileOutputStream stream = new FileOutputStream(new File(directory, name))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        }
        bitmap.recycle();
    }
}
