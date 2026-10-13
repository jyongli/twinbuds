package com.jy.twinbuds.core;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class CapabilityAdvisorTest {
    private DeviceSnapshot snapshot(String manufacturer, boolean permission, boolean enabled,
            int count, DeviceSnapshot.Feature le) {
        List<DeviceSnapshot.Device> devices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            devices.add(new DeviceSnapshot.Device("device-" + i, "耳机 " + i, "A2DP"));
        }
        return new DeviceSnapshot(36, manufacturer, "Test phone", true, permission, enabled,
                le, le, devices, Collections.emptyList(), "");
    }

    @Test public void twoConnectedHeadsetsStillRequireListeningVerification() {
        CapabilityAdvisor.Advice result = CapabilityAdvisor.assess(
                snapshot("Samsung", true, true, 2, DeviceSnapshot.Feature.SUPPORTED));
        assertEquals(CapabilityAdvisor.Status.READY_TO_CHECK, result.status);
        assertTrue(result.canTest);
        assertEquals(CapabilityAdvisor.Guide.SAMSUNG, result.guide);
    }

    @Test public void permissionDenialDisablesTestingEvenWithStaleDeviceList() {
        CapabilityAdvisor.Advice result = CapabilityAdvisor.assess(
                snapshot("Samsung", false, true, 2, DeviceSnapshot.Feature.SUPPORTED));
        assertEquals(CapabilityAdvisor.Status.PERMISSION_NEEDED, result.status);
        assertFalse(result.canTest);
    }

    @Test public void bluetoothOffDisablesTesting() {
        CapabilityAdvisor.Advice result = CapabilityAdvisor.assess(
                snapshot("Google", true, false, 2, DeviceSnapshot.Feature.SUPPORTED));
        assertEquals(CapabilityAdvisor.Status.BLUETOOTH_OFF, result.status);
        assertFalse(result.canTest);
    }

    @Test public void leSupportWithoutHeadphonesDoesNotEnableTest() {
        CapabilityAdvisor.Advice result = CapabilityAdvisor.assess(
                snapshot("Google", true, true, 0, DeviceSnapshot.Feature.SUPPORTED));
        assertEquals(CapabilityAdvisor.Status.NO_DEVICE, result.status);
        assertEquals(CapabilityAdvisor.Guide.LE_AUDIO, result.guide);
        assertFalse(result.canTest);
    }

    @Test public void unknownManufacturerAndLegacyPhoneRemainUnverified() {
        CapabilityAdvisor.Advice result = CapabilityAdvisor.assess(
                snapshot("Other", true, true, 2, DeviceSnapshot.Feature.UNKNOWN));
        assertEquals(CapabilityAdvisor.Status.READY_TO_CHECK, result.status);
        assertEquals(CapabilityAdvisor.Guide.GENERAL, result.guide);
    }

    @Test public void groupedEndpointCanStillBeCheckedByListening() {
        CapabilityAdvisor.Advice result = CapabilityAdvisor.assess(
                snapshot("Google", true, true, 1, DeviceSnapshot.Feature.SUPPORTED));
        assertEquals(CapabilityAdvisor.Status.ONE_DEVICE, result.status);
        assertTrue(result.canTest);
    }

    @Test public void readFailureMustNotClaimNoDevicesOrAllowTest() {
        DeviceSnapshot value = new DeviceSnapshot(36, "Test", "Model", true, true, true,
                DeviceSnapshot.Feature.UNKNOWN, DeviceSnapshot.Feature.UNKNOWN,
                Collections.emptyList(), Collections.emptyList(), "蓝牙服务暂不可用");
        assertEquals(CapabilityAdvisor.Status.READ_ERROR, CapabilityAdvisor.assess(value).status);
        assertFalse(CapabilityAdvisor.assess(value).canTest);
    }

    @Test public void noBluetoothHardwareRemainsUsableAsGuideOnly() {
        DeviceSnapshot value = new DeviceSnapshot(21, "Test", "Model", false, true, false,
                DeviceSnapshot.Feature.UNKNOWN, DeviceSnapshot.Feature.UNKNOWN,
                Collections.emptyList(), Collections.emptyList(), "");
        assertEquals(CapabilityAdvisor.Status.NO_BLUETOOTH, CapabilityAdvisor.assess(value).status);
    }
}
