package org.zstack.pciDevice.virtual.vfio_mdev;

import java.util.Collections;
import java.util.List;

/**
 * Created by GuoYi on 2019-04-18.
 */
public enum MdevDeviceStatus {
    Active,
    Attached,
    Reserved,
    ;

    public static List<MdevDeviceStatus> attachableMdevDeviceStatus = Collections.singletonList(Active);
    public boolean isAttachable() {
        return attachableMdevDeviceStatus.contains(this);
    }
}
