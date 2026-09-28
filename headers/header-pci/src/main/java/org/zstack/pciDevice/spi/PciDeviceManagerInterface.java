package org.zstack.pciDevice.spi;

import org.zstack.pciDevice.header.PciDeviceType;

public interface PciDeviceManagerInterface {
    PciDeviceTypeFactory getPciDeviceTypeFactory(PciDeviceType type);
    PciDeviceBackend getPciDeviceBackendByHostUuid(String hostUuid);
}
