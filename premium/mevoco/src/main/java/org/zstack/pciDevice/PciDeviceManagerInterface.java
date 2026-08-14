package org.zstack.pciDevice;

import org.zstack.pciDevice.virtual.VirtualPciDeviceFactory;

public interface PciDeviceManagerInterface {
    PciDeviceTypeFactory getPciDeviceTypeFactory(PciDeviceType type);
    PciDeviceBackend getPciDeviceBackendByHostUuid(String hostUuid);
}
