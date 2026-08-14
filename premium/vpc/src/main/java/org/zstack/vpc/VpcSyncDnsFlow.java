package org.zstack.vpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vpc.VpcConstants;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.vyos.*;

import java.util.List;
import java.util.Map;

public class VpcSyncDnsFlow implements VyosPostStartFlowExtensionPoint, VyosPostCreateFlowExtensionPoint,
        VyosPostRebootFlowExtensionPoint, VyosProvisionConfigFlowExtensionPoint {
    @Autowired
    private VpcRouterDnsBackend dnsBackend;
    @Autowired
    private VpcManager vpcManager;

    private Flow createSyncFlow() {
        return new NoRollbackFlow(){
            String __name__ = "VpcSyncDnsFlow";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
                if (vr == null || !VpcConstants.VPC_VROUTER_VM_TYPE.equals(vr.getApplianceVmType())) {
                    trigger.next();
                    return;
                }

                List<String> dns = vpcManager.getAllDnsFromVpcRouter(vr.getUuid());
                if(dns == null || dns.isEmpty()) {
                    trigger.next();
                    return;
                }

                dnsBackend.applyDnsToVpcRouter(vr.getUuid(), false, new Completion(trigger) {
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
}
