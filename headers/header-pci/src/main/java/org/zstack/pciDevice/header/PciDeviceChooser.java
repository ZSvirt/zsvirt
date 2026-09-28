package org.zstack.pciDevice.header;

public enum PciDeviceChooser {
    None, // The pci device is not attached / reserved to any vm
    Device, // Use APIAttachPciDeviceToVmMsg
    Spec, // Use APIAddPciDeviceSpecToVmInstanceMsg
}
