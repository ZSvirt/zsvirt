package org.zstack.pciDevice;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by weiwang on 07/07/2017.
 */
public enum PciDeviceStatus {
    Active,
    System,
    Attached,
    Reserved,
    ;

    public static List<PciDeviceStatus> attachablePciDeviceStatus = asList(System, Active);
    public boolean isAttachable() {
        return attachablePciDeviceStatus.contains(this);
    }
}
