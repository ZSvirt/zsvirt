package org.zstack.pciDevice;

public enum PciDeviceChooser {
    None, // The pci device is not attached / reserved to any vm
    Device, // Use APIAttachPciDeviceToVmMsg
    Spec, // Use APIAddPciDeviceSpecToVmInstanceMsg
}
