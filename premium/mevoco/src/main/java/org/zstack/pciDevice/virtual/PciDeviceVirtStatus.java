package org.zstack.pciDevice.virtual;

import org.zstack.pciDevice.PciDeviceConstants;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by GuoYi on 2019-04-19.
 */
public enum PciDeviceVirtStatus {
    UNVIRTUALIZABLE(PciDeviceConstants.UNVIRTUALIZABLE),
    SRIOV_VIRTUALIZABLE(PciDeviceConstants.SRIOV_VIRTUALIZABLE),
    VFIO_MDEV_VIRTUALIZABLE(PciDeviceConstants.VFIO_MDEV_VIRTUALIZABLE),
    SRIOV_VIRTUALIZED(PciDeviceConstants.SRIOV_VIRTUALIZED),
    VFIO_MDEV_VIRTUALIZED(PciDeviceConstants.VFIO_MDEV_VIRTUALIZED),
    SRIOV_VIRTUAL(PciDeviceConstants.SRIOV_VIRTUAL),
    VIRTUALIZED_BYPASS_ZSTACK(PciDeviceConstants.VIRTUALIZED_BYPASS_ZSTACK),
    UNKNOWN(PciDeviceConstants.UNKNOWN);

    private String value;

    PciDeviceVirtStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean isEqual(String str) {
        return this.value.equals(str);
    }

    public static List<PciDeviceVirtStatus> attachablePciDeviceVirtStatus = asList(
            UNVIRTUALIZABLE,
            SRIOV_VIRTUAL,
            SRIOV_VIRTUALIZABLE,
            VFIO_MDEV_VIRTUALIZABLE
    );
    public boolean isAttachable() {
        return attachablePciDeviceVirtStatus.contains(this);
    }
}