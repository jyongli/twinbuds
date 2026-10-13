package com.jy.twinbuds.core;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class ReportFormatterTest {
    @Test public void reportOmitsAddressesAndLabelsEvidenceAsUserFeedback() {
        DeviceSnapshot snapshot = new DeviceSnapshot(36, "Samsung", "Test phone", true, true, true,
                DeviceSnapshot.Feature.SUPPORTED, DeviceSnapshot.Feature.UNKNOWN,
                Arrays.asList(new DeviceSnapshot.Device("01:23:45:67:89:AB", "耳机 A", "A2DP")),
                Collections.singletonList("媒体输出：耳机 A"), "");
        String report = ReportFormatter.format(snapshot, ListeningSession.Outcome.BOTH_HEARD, "", "2026-10-13");
        assertTrue(report.contains("耳机 A"));
        assertTrue(report.contains("用户试听反馈"));
        assertTrue(report.contains("不代表其他应用"));
        assertFalse(report.contains("01:23:45:67:89:AB"));
    }

    @Test public void vendorAddressAsProductNameIsAlsoRedacted() {
        DeviceSnapshot snapshot = new DeviceSnapshot(35, "Test", "Model", true, true, true,
                DeviceSnapshot.Feature.UNKNOWN, DeviceSnapshot.Feature.UNKNOWN,
                Arrays.asList(new DeviceSnapshot.Device("address", "耳机 01:23:45:67:89:AB", "A2DP")),
                Arrays.asList("01:23:45:67:89:AB"), "");
        String report = ReportFormatter.format(snapshot, ListeningSession.Outcome.UNTESTED,
                "01:23:45:67:89:AB", "2026-10-13");
        assertFalse(report.contains("01:23:45:67:89:AB"));
    }
}
