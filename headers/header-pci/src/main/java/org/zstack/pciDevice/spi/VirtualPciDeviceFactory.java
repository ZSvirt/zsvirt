package org.zstack.pciDevice.spi;

import org.zstack.header.core.Completion;
import org.zstack.pciDevice.virtual.APIGenerateVirtualPciDevicesMsg;
import org.zstack.pciDevice.virtual.APIUngenerateVirtualPciDevicesMsg;

/**
 * Created by GuoYi on 2019-04-24.
 */
public interface VirtualPciDeviceFactory {
    String getVirtTechType();
    void generateVirtualPciDevices(APIGenerateVirtualPciDevicesMsg msg, Completion completion);
    void ungenerateVirtualPciDevices(APIUngenerateVirtualPciDevicesMsg msg, Completion completion);
}
