package org.zstack.network.hostNetworkInterface;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.network.l2.L2NetworkGetInterfaceExtensionPoint;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostNetworkInterfaceHelper {
    @Autowired
    private PluginRegistry pluginRgty;

    public boolean isPhysicalInterfaceOccupiedByL2Network(String interfaceName, String hostUuid) {
        for (L2NetworkGetInterfaceExtensionPoint ext : pluginRgty.getExtensionList(L2NetworkGetInterfaceExtensionPoint.class)) {
            if (ext.isPhysicalInterfaceOccupied(interfaceName, hostUuid)) {
                return true;
            }
        }

        return false;
    }
}
