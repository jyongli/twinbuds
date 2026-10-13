package com.jy.twinbuds.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class TonePcmTest {
    @Test public void fourSecondStereoToneHasEqualChannelsAndNoClipping() {
        short[] pcm = TonePcm.generate();
        assertEquals(44100 * 4 * 2, pcm.length);
        int peak = 0;
        for (int i = 0; i < pcm.length; i += 2) {
            assertEquals(pcm[i], pcm[i + 1]);
            peak = Math.max(peak, Math.abs((int) pcm[i]));
        }
        assertTrue(peak > 1000);
        assertTrue(peak <= 7000);
        assertEquals(0, pcm[0]);
        assertEquals(0, pcm[pcm.length - 1]);
    }
}
