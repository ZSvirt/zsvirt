package org.zstack.pciDevice.header.virtual.vfio_mdev;

public enum MdevDeviceChooser {
    None, // The mdev device is not attached / reserved to any vm
    Device, // Use APIAttachMdevDeviceToVmMsg
    Spec, // Use APIAddMdevDeviceSpecToVmInstanceMsg
}
