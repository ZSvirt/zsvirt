package org.zstack.pciDevice.virtual;

import org.zstack.header.core.Completion;
import org.zstack.pciDevice.PciDeviceType;
import org.zstack.pciDevice.PciDeviceVO;

import java.util.List;

/**
 * Created by GuoYi on 2019-04-24.
 */
public interface VirtualPciDeviceFactory {
    String getVirtTechType();
    void generateVirtualPciDevices(APIGenerateVirtualPciDevicesMsg msg, Completion completion);
    void ungenerateVirtualPciDevices(APIUngenerateVirtualPciDevicesMsg msg, Completion completion);
}
