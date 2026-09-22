package org.zstack.ha.spi;

public interface HaHostDeviceExtensionPoint {
    boolean canDoVmHa(String vmUuid);
}
