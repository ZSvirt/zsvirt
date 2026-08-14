package org.zstack.mttyDevice;


/**
 * @author yu.sun
 * @date 2022/11/29 08:57
 **/
public enum MttyDeviceVirtStatus {
    VFIO_MDEV_VIRTUALIZABLE(MttyDeviceConstants.VFIO_MDEV_VIRTUALIZABLE),
    VFIO_MDEV_VIRTUALIZED(MttyDeviceConstants.VFIO_MDEV_VIRTUALIZED),
    UNVIRTUALIZABLE(MttyDeviceConstants.UNVIRTUALIZABLE),
    UNKNOWN(MttyDeviceConstants.UNKNOWN);

    private String value;

    MttyDeviceVirtStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean isEqual(String str) {
        return this.value.equals(str);
    }
}