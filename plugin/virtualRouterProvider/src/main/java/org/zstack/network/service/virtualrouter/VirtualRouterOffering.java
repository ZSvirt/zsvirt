package org.zstack.network.service.virtualrouter;

import org.zstack.configuration.InstanceOfferingBase;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingVO;

/**
 */
public class VirtualRouterOffering extends InstanceOfferingBase {
    protected VirtualRouterOfferingVO self;

    public VirtualRouterOffering(InstanceOfferingVO vo) {
        super(vo);
        self = (VirtualRouterOfferingVO) vo;
    }

    @Override
    protected InstanceOfferingInventory getInventory() {
        return VirtualRouterOfferingInventory.valueOf(self);
    }
}
