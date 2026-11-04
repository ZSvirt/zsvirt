package org.zstack.usbDevice.spi;

/**
 * Created by MaJin on 2019/12/10.
 */
public interface SyncUsbDeviceExtensionPoint {
    void afterSyncUsbDevice(String hostUuid);
}
