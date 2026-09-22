package org.zstack.ha.spi;

import org.zstack.ha.message.HostScanResult;

public interface HostScanExtensionPoint {
    void onScanResult(String hostUuid, HostScanResult stage);
}
