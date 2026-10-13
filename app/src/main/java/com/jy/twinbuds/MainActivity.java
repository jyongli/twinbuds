package com.jy.twinbuds;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.jy.twinbuds.audio.DeviceMonitor;
import com.jy.twinbuds.audio.TestTonePlayer;
import com.jy.twinbuds.core.CapabilityAdvisor;
import com.jy.twinbuds.core.DeviceSnapshot;
import com.jy.twinbuds.core.ListeningSession;
import com.jy.twinbuds.core.ReportFormatter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 单页设置与验证流程；原始媒体仍由系统负责输出，退出助手后无需后台服务。 */
public final class MainActivity extends Activity {
    private static final int CONNECT_PERMISSION = 10;
    private final ListeningSession session = new ListeningSession();
    private DeviceMonitor monitor;
    private TestTonePlayer player;
    private DeviceSnapshot snapshot;
    private Button connectButton;
    private Button playButton;
    private TextView resultView;
    private TextView routeView;
    private String routes = "";
    private String renderedKey = "";
    private boolean resumed;
    private AlertDialog feedbackDialog;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        player = new TestTonePlayer(this);
        connectButton = findViewById(R.id.connect);
        playButton = findViewById(R.id.play);
        resultView = findViewById(R.id.test_result);
        routeView = findViewById(R.id.route_detail);
        setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
        setupInsets();
        connectButton.setOnClickListener(view -> connectOrAuthorize());
        playButton.setOnClickListener(view -> toggleTest());
        findViewById(R.id.refresh).setOnClickListener(view -> { if (monitor != null) monitor.refresh(); });
        findViewById(R.id.guide).setOnClickListener(view -> showGuide());
        findViewById(R.id.report).setOnClickListener(view -> showReport());
    }

    private void setupInsets() {
        // Android 15+ 强制边到边；用系统实际边距避开状态栏、挖孔和手势区域。
        if (Build.VERSION.SDK_INT >= 35) {
            View root = findViewById(R.id.scroll);
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
            root.requestApplyInsets();
        } else {
            if (Build.VERSION.SDK_INT >= 23) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | (Build.VERSION.SDK_INT >= 26 ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0));
            } else getWindow().setStatusBarColor(0xFF245F4C);
            if (Build.VERSION.SDK_INT < 26) getWindow().setNavigationBarColor(0xFF245F4C);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        renderedKey = "";
        // 每次进入界面重新取样，避免从设置页返回时沿用过时的连接和权限状态。
        monitor = new DeviceMonitor(this, this::acceptSnapshot);
        monitor.start();
    }

    @Override protected void onPause() {
        resumed = false;
        if (feedbackDialog != null) feedbackDialog.dismiss();
        feedbackDialog = null;
        player.stop();
        session.cancel();
        routes = "";
        resultView.setText(R.string.pending_test);
        routeView.setVisibility(View.GONE);
        if (monitor != null) monitor.stop();
        monitor = null;
        super.onPause();
    }

    private void acceptSnapshot(DeviceSnapshot value) {
        if (!resumed) return;
        ListeningSession.Phase previous = session.phase();
        session.topologyChanged(value.topologyKey());
        if (previous != ListeningSession.Phase.IDLE && session.phase() == ListeningSession.Phase.IDLE) {
            player.stop();
            if (feedbackDialog != null) feedbackDialog.dismiss();
            feedbackDialog = null;
            routes = "";
            routeView.setVisibility(View.GONE);
            resultView.setText(R.string.topology_changed);
        }
        snapshot = value;
        render();
    }

    private void render() {
        if (snapshot == null) return;
        CapabilityAdvisor.Advice advice = CapabilityAdvisor.assess(snapshot);
        int title;
        int detail;
        switch (advice.status) {
            case NO_BLUETOOTH: title = R.string.no_bluetooth; detail = R.string.no_bluetooth_detail; break;
            case PERMISSION_NEEDED: title = R.string.need_permission; detail = R.string.need_permission_detail; break;
            case BLUETOOTH_OFF: title = R.string.bluetooth_off; detail = R.string.bluetooth_off_detail; break;
            case READ_ERROR: title = R.string.read_error; detail = R.string.device_placeholder; break;
            case NO_DEVICE: title = R.string.no_device; detail = R.string.no_device_detail; break;
            case ONE_DEVICE: title = R.string.one_device; detail = R.string.one_device_detail; break;
            default: title = R.string.ready_check; detail = R.string.ready_check_detail; break;
        }
        ((TextView) findViewById(R.id.status_title)).setText(title);
        ((TextView) findViewById(R.id.status_detail)).setText(advice.status == CapabilityAdvisor.Status.READ_ERROR
                ? snapshot.readError : getString(detail));
        connectButton.setText(snapshot.permissionGranted ? R.string.bluetooth_button : R.string.permission_button);
        ((TextView) findViewById(R.id.guide_summary)).setText(advice.guide == CapabilityAdvisor.Guide.SAMSUNG
                ? R.string.samsung_summary : advice.guide == CapabilityAdvisor.Guide.LE_AUDIO ? R.string.le_summary : R.string.general_summary);
        boolean playing = session.phase() == ListeningSession.Phase.PLAYING;
        playButton.setText(playing ? R.string.stop_test : R.string.play_test);
        boolean testEnabled = playing || (advice.canTest && session.phase() != ListeningSession.Phase.AWAITING_FEEDBACK);
        playButton.setEnabled(testEnabled);
        playButton.setAlpha(testEnabled ? 1f : .45f);
        String key = snapshot.topologyKey();
        if (!key.equals(renderedKey)) {
            renderedKey = key;
            LinearLayout list = findViewById(R.id.device_list);
            list.removeAllViews();
            for (int i = 0; i < snapshot.devices.size(); i++) {
                DeviceSnapshot.Device device = snapshot.devices.get(i);
                TextView name = new TextView(this);
                name.setText(getString(R.string.device_number_name, i + 1, device.name));
                name.setTextColor(0xFF182E2A);
                name.setTextSize(16);
                name.setTypeface(null, Typeface.BOLD);
                name.setPadding(0, dp(10), 0, dp(4));
                list.addView(name);
                TextView state = new TextView(this);
                state.setText(getString(R.string.connected_label, device.transport));
                state.setTextColor(0xFF596A64);
                state.setTextSize(12);
                state.setPadding(0, 0, 0, dp(10));
                list.addView(state);
            }
        }
    }

    private void connectOrAuthorize() {
        if (Build.VERSION.SDK_INT >= 31 && !DeviceMonitor.hasPermission(this)) {
            boolean alreadyAsked = getPreferences(MODE_PRIVATE).getBoolean("asked_connect", false);
            if (alreadyAsked && !shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT)) {
                new AlertDialog.Builder(this).setTitle(R.string.permission_title).setMessage(R.string.permission_reason)
                        .setPositiveButton(R.string.app_settings, (dialog, which) ->
                                launchSettings(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))))
                        .setNegativeButton(R.string.cancel, null).show();
            } else {
                getPreferences(MODE_PRIVATE).edit().putBoolean("asked_connect", true).apply();
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, CONNECT_PERMISSION);
            }
        } else launchSettings(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CONNECT_PERMISSION) {
            if (monitor != null) monitor.refresh();
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permission_reason, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void launchSettings(Intent intent) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException exception) {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (ActivityNotFoundException | SecurityException ignored) {
                Toast.makeText(this, R.string.settings_missing, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showGuide() {
        CapabilityAdvisor.Guide guide = snapshot == null ? CapabilityAdvisor.Guide.GENERAL
                : CapabilityAdvisor.assess(snapshot).guide;
        int message = guide == CapabilityAdvisor.Guide.SAMSUNG ? R.string.samsung_guide
                : guide == CapabilityAdvisor.Guide.LE_AUDIO ? R.string.le_guide : R.string.general_guide;
        new AlertDialog.Builder(this).setTitle(R.string.guide_title).setMessage(message)
                .setPositiveButton(R.string.bluetooth_button, (dialog, which) -> launchSettings(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)))
                .setNegativeButton(R.string.close, null).show();
    }

    private void toggleTest() {
        if (session.phase() == ListeningSession.Phase.PLAYING) {
            player.stop();
            session.cancel();
            routes = "";
            routeView.setVisibility(View.GONE);
            resultView.setText(R.string.test_stopped);
            render();
            return;
        }
        if (monitor == null) return;
        acceptSnapshot(monitor.read());
        if (!CapabilityAdvisor.assess(snapshot).canTest) return;
        routes = "";
        routeView.setVisibility(View.GONE);
        session.start(snapshot.topologyKey());
        resultView.setText(R.string.playing);
        render();
        player.play(new TestTonePlayer.Listener() {
            @Override public void onRoutes(String description) {
                routes = description;
                routeView.setText(getString(R.string.route_observed, description));
                routeView.setVisibility(View.VISIBLE);
            }
            @Override public void onFinished() {
                if (!resumed || monitor == null) return;
                acceptSnapshot(monitor.read());
                session.finish(snapshot.topologyKey());
                render();
                if (session.phase() == ListeningSession.Phase.AWAITING_FEEDBACK) showFeedback();
            }
            @Override public void onError(String message) {
                session.cancel();
                routes = "";
                routeView.setVisibility(View.GONE);
                resultView.setText(message);
                render();
            }
        });
    }

    private void showFeedback() {
        String[] choices = {getString(R.string.feedback_both), getString(R.string.feedback_one), getString(R.string.feedback_none)};
        feedbackDialog = new AlertDialog.Builder(this).setTitle(R.string.feedback_title)
                .setItems(choices, (dialog, which) -> {
                    if (monitor == null) return;
                    acceptSnapshot(monitor.read());
                    ListeningSession.Outcome outcome = which == 0 ? ListeningSession.Outcome.BOTH_HEARD
                            : which == 1 ? ListeningSession.Outcome.ONE_HEARD : ListeningSession.Outcome.NONE_HEARD;
                    if (session.report(outcome, snapshot.topologyKey())) {
                        resultView.setText(which == 0 ? R.string.feedback_success : which == 1 ? R.string.feedback_partial : R.string.feedback_silent);
                    }
                    feedbackDialog = null;
                    render();
                }).setNegativeButton(R.string.feedback_cancel, (dialog, which) -> cancelFeedback())
                .setOnCancelListener(dialog -> cancelFeedback()).create();
        // 对话框标题与页面说明都明确询问两副耳机，不把左右声道当成双设备。
        feedbackDialog.show();
    }

    private void cancelFeedback() {
        session.cancel();
        feedbackDialog = null;
        resultView.setText(R.string.pending_test);
        render();
    }

    private void showReport() {
        if (monitor != null) acceptSnapshot(monitor.read());
        if (snapshot == null) return;
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.getDefault()).format(new Date());
        final String report = ReportFormatter.format(snapshot, session.outcome(), routes, timestamp);
        TextView body = new TextView(this);
        body.setText(report);
        body.setTextSize(14);
        body.setTextColor(0xFF182E2A);
        body.setPadding(dp(24), dp(12), dp(24), dp(12));
        body.setTextIsSelectable(true);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        new AlertDialog.Builder(this).setTitle(R.string.report_title).setView(scroll)
                .setPositiveButton(R.string.copy, (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.report_title), report));
                    Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton(R.string.share, (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, report);
                    try { startActivity(Intent.createChooser(intent, getString(R.string.share_title))); }
                    catch (ActivityNotFoundException exception) { Toast.makeText(this, R.string.no_share_app, Toast.LENGTH_SHORT).show(); }
                }).setNegativeButton(R.string.close, null).show();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
