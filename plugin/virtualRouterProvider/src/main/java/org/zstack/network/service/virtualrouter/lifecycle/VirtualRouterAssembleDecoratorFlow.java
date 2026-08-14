package org.zstack.network.service.virtualrouter.lifecycle;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.appliancevm.ApplianceVmPostLifeCycleInfo;
import org.zstack.appliancevm.ApplianceVmSpec;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant.Param;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.network.service.virtualrouter.vip.VipConfigProxy;
import org.zstack.utils.CollectionUtils;

import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterAssembleDecoratorFlow extends NoRollbackFlow {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VipConfigProxy vipConfigProxy;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        ApplianceVmPostLifeCycleInfo info;
        if (spec.getCurrentVmOperation() == VmOperation.NewCreate) {
            final ApplianceVmSpec aspec = spec.getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec.class);
            info = new ApplianceVmPostLifeCycleInfo();
            info.setDefaultRouteL3Network(aspec.getDefaultRouteL3Network());
            VmNicInventory mgmtNic = CollectionUtils.findOneOrNull(spec.getDestNics(),
                    arg -> arg.getL3NetworkUuid().equals(aspec.getManagementNic().getL3NetworkUuid()));
            info.setManagementNic(mgmtNic);
            data.put(Param.IS_NEW_CREATED.toString(), true);
        } else {
            info = (ApplianceVmPostLifeCycleInfo) data.get(ApplianceVmConstant.Params.applianceVmInfoForPostLifeCycle.toString());
        }

        VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(dbf.findByUuid(spec.getVmInventory().getUuid(), VirtualRouterVmVO.class));
        if (spec.getCurrentVmOperation() == VmOperation.Destroy) {
            data.put(Param.VR_UUID.toString(), spec.getVmInventory().getUuid());
            data.put(Param.IS_HA_ROUTER.toString(), vrInv.isHaEnabled());
            data.put(VirtualRouterConstant.Param.VR.toString(), spec.getVmInventory());
        } else {
            data.put(VirtualRouterConstant.Param.VR.toString(), vrInv);
            data.put(VirtualRouterConstant.Param.VR_NIC.toString(), vrInv.getPublicNic());
        }

        trigger.next();
    }

}
