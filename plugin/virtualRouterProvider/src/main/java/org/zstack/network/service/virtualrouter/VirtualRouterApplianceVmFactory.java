package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.appliancevm.*;
import org.zstack.core.db.DatabaseFacade;

/**
 */
public class VirtualRouterApplianceVmFactory implements ApplianceVmSubTypeFactory {
    public static final ApplianceVmType type = new ApplianceVmType(VirtualRouterConstant.VIRTUAL_ROUTER_VM_TYPE);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public ApplianceVmType getApplianceVmType() {
        return type;
    }

    @Override
    public ApplianceVm getSubApplianceVm(ApplianceVmVO apvm) {
        VirtualRouterVmVO vr = dbf.findByUuid(apvm.getUuid(), VirtualRouterVmVO.class);
        return new VirtualRouter(vr);
    }

    @Override
    @Transactional
    public ApplianceVmVO persistApplianceVm(ApplianceVmSpec spec, ApplianceVmVO apvm) {
        VirtualRouterVmVO vr = new VirtualRouterVmVO(apvm);
        VirtualRouterOfferingInventory offering = (VirtualRouterOfferingInventory) spec.getInstanceOffering();
        vr.setPublicNetworkUuid(offering.getPublicNetworkUuid());
        dbf.getEntityManager().persist(vr);
        return vr;
    }

    @Override
    @Transactional
    public void removeApplianceVm(ApplianceVmSpec spec, ApplianceVmVO apvm) {
        dbf.removeByPrimaryKey(apvm.getUuid(), VirtualRouterVmVO.class);
        return;
    }
}
