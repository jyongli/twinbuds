package com.jy.twinbuds.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFocusRequest;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import com.jy.twinbuds.core.TonePcm;
import java.util.ArrayList;
import java.util.List;

/** 一条媒体音轨遵循系统路由；完成事件来自播放头，不用计时器伪装播放成功。 */
public final class TestTonePlayer {
    public interface Listener {
        void onRoutes(String description);
        void onFinished();
        void onError(String message);
    }
    private final AudioManager audio;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AudioTrack track;
    private AudioFocusRequest focusRequest;
    private Listener listener;
    private boolean hasFocus;
    private final AudioAttributes attributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();

    public TestTonePlayer(Context context) {
        audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    private final AudioManager.OnAudioFocusChangeListener focusListener = change -> {
        if (change < 0) fail("试听被其他音频打断，请稍后重试。");
    };

    public void play(Listener listener) {
        stop();
        this.listener = listener;
        try {
            int focusResult;
            if (Build.VERSION.SDK_INT >= 26) {
                focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(attributes).setOnAudioFocusChangeListener(focusListener, handler).build();
                focusResult = audio.requestAudioFocus(focusRequest);
            } else {
                focusResult = audio.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            }
            if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                fail("未取得音频播放焦点。请暂停其他媒体后重试。");
                return;
            }
            hasFocus = true;
            short[] pcm = TonePcm.generate();
            AudioFormat format = new AudioFormat.Builder().setSampleRate(TonePcm.SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build();
            track = new AudioTrack(attributes, format, pcm.length * 2, AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE);
            if (track.getState() == AudioTrack.STATE_UNINITIALIZED || track.write(pcm, 0, pcm.length) != pcm.length) {
                fail("未能准备试听音，请重新尝试。");
                return;
            }
            track.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                @Override public void onMarkerReached(AudioTrack completedTrack) {
                    if (completedTrack != track) return;
                    observeRoutes();
                    Listener completed = TestTonePlayer.this.listener;
                    stop();
                    if (completed != null) completed.onFinished();
                }
                @Override public void onPeriodicNotification(AudioTrack ignored) { }
            }, handler);
            track.setNotificationMarkerPosition(pcm.length / 2);
            track.play();
            handler.postDelayed(this::observeRoutes, 650);
            handler.postDelayed(() -> fail("试听播放超时，请检查输出设备后重试。"), 7000);
        } catch (RuntimeException exception) {
            fail("无法播放试听音，请检查系统媒体输出与音量。");
        }
    }

    private void observeRoutes() {
        if (track == null || listener == null) return;
        try {
            List<String> routes = new ArrayList<>();
            if (Build.VERSION.SDK_INT >= 36) {
                for (AudioDeviceInfo device : track.getRoutedDevices()) routes.add(DeviceMonitor.deviceLabel(device));
            } else if (Build.VERSION.SDK_INT >= 23) {
                AudioDeviceInfo device = track.getRoutedDevice();
                if (device != null) routes.add(DeviceMonitor.deviceLabel(device));
            }
            StringBuilder label = new StringBuilder();
            for (String route : routes) {
                if (label.length() > 0) label.append("；");
                label.append(route);
            }
            if (label.length() == 0) label.append("系统未返回本次试听路由，以实际听感为准");
            if (Build.VERSION.SDK_INT < 36) label.append("（此版本不能完整列出多路输出）");
            listener.onRoutes(label.toString());
        } catch (RuntimeException exception) {
            listener.onRoutes("无法读取试听路由，以实际听感为准");
        }
    }

    private void fail(String message) {
        Listener failed = listener;
        stop();
        if (failed != null) failed.onError(message);
    }

    public void stop() {
        handler.removeCallbacksAndMessages(null);
        listener = null;
        if (track != null) {
            AudioTrack previous = track;
            track = null;
            try { previous.stop(); } catch (IllegalStateException ignored) { /* 未开始时也必须释放。 */ }
            previous.release();
        }
        if (hasFocus) {
            if (Build.VERSION.SDK_INT >= 26 && focusRequest != null) audio.abandonAudioFocusRequest(focusRequest);
            else audio.abandonAudioFocus(focusListener);
        }
        hasFocus = false;
        focusRequest = null;
    }
}
