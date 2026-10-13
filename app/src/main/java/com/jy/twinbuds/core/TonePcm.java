package com.jy.twinbuds.core;

/** 本地生成双声道同内容的短旋律，不采集或保存其他应用声音。 */
public final class TonePcm {
    public static final int SAMPLE_RATE = 44100;
    public static final int DURATION_SECONDS = 4;
    public static short[] generate() {
        int frames = SAMPLE_RATE * DURATION_SECONDS;
        short[] pcm = new short[frames * 2];
        double[] notes = {523.25, 659.25, 783.99, 659.25};
        for (int frame = 0; frame < frames; frame++) {
            int segment = frame / SAMPLE_RATE;
            double second = (frame % SAMPLE_RATE) / (double) SAMPLE_RATE;
            // 每拍 0.72 秒发声、其余静音，首尾 25 毫秒淡入淡出以避免爆音。
            double envelope = Math.max(0, Math.min(1, Math.min(second / .025, (.72 - second) / .025)));
            short sample = (short) (6500 * envelope * Math.sin(2 * Math.PI * notes[segment] * second));
            pcm[2 * frame] = sample;
            pcm[2 * frame + 1] = sample;
        }
        return pcm;
    }
}
