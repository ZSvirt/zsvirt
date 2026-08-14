package org.zstack.ha;

public interface HaHostDeviceExtensionPoint {
    boolean canDoVmHa(String vmUuid);
}
