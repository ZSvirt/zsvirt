package org.zstack.ha;

public interface HostScanExtensionPoint {
    void onScanResult(String hostUuid, HostCheckResult.HostScanResult stage);
}
