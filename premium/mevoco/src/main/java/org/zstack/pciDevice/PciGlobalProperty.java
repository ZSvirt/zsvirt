package org.zstack.pciDevice;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * author:kaicai.hu
 * Date:2020/11/20
 */
@GlobalPropertyDefinition
public class PciGlobalProperty {
    @GlobalProperty(name = "syncPciDeviceOfferingRef", defaultValue = "false")
    public static boolean SYNC_PCI_DEVICE_OFFERING_REF;
}
