package org.zstack.routeProtocol;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.protocol.NetworkRouterAreaRefVO;
import org.zstack.header.protocol.NetworkRouterAreaRefVO_;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vpc.VpcConstants;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.vyos.*;
import org.zstack.vpc.ha.VpcHaGroupOperator;

import java.util.Map;

public class RouteProtocolSyncFlow implements VyosPostStartFlowExtensionPoint, VyosPostCreateFlowExtensionPoint,
        VyosPostRebootFlowExtensionPoint, VyosProvisionConfigFlowExtensionPoint, VyosPostDestroyFlowExtensionPoint {
    @Autowired
    private RouteProtocolOspfBackend ospfBackend;
    @Autowired
    private DatabaseFacade dbf;

    private Flow createSyncFlow() {
        return new NoRollbackFlow(){
            @Override
            public void run(FlowTrigger trigger, Map data) {
                VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
                VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

                if (vr == null || !VpcConstants.VPC_VROUTER_VM_TYPE.equals(vr.getApplianceVmType())) {
                    trigger.next();
                    return;
                }

                /* when vpc is starting, rebooting, no need to send empty ospf config */
                boolean applyEmpty = true;
                if (spec != null) {
                    if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.Reboot ||
                            spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.Start ||
                            spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.NewCreate) {
                        applyEmpty = false;
                    }
                }

                ospfBackend.applyOspfToAgent(vr.getUuid(), applyEmpty, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        };
    }

    @Override
    public Flow vyosPostCreateFlow() {
        return createSyncFlow();
    }

    @Override
    public Flow vyosPostRebootFlow() {
        return createSyncFlow();
    }

    @Override
    public Flow vyosProvisionConfigFlow() {
        return createSyncFlow();
    }

    @Override
    public Flow vyosPostStartFlow() {
        return createSyncFlow();
    }

    @Override
    public Flow vyosPostDestroyFlow() {
        return new Flow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                VmInstanceSpec spec = (VmInstanceSpec)data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                VmInstanceInventory vm = spec.getVmInventory();
                ApplianceVmVO apvo = dbf.findByUuid(vm.getUuid(), ApplianceVmVO.class);

                boolean isLastRouter = VpcHaGroupOperator.isLastHaGroupRouter(vm.getUuid());

                if (!isLastRouter) {
                    trigger.next();
                    return;
                }

                if (apvo.isHaEnabled()) {
                    SQL.New(NetworkRouterAreaRefVO.class).eq(NetworkRouterAreaRefVO_.vRouterUuid, VpcHaGroupOperator.getVpcHaGroupUuid(apvo.getUuid())).delete();
                } else {
                    SQL.New(NetworkRouterAreaRefVO.class).eq(NetworkRouterAreaRefVO_.vRouterUuid, vm.getUuid()).delete();
                }

                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                trigger.rollback();
            }
        };
    }
}
