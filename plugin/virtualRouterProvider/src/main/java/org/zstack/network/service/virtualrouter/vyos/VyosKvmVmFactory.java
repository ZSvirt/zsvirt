package org.zstack.network.service.virtualrouter.vyos;

import org.zstack.appliancevm.ApplianceVmType;

public class VyosKvmVmFactory extends VyosKvmVmBaseFactory {
    @Override
    public ApplianceVmType getApplianceVmType() {
        return VyosVmFactory.applianceVmType;
    }
}
