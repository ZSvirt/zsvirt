package org.zstack.ipsec.vyos;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.service.VirtualRouterHaTask;
import org.zstack.ipsec.IPsecConnectionInventory;
import org.zstack.ipsec.vyos.VyosIPsecConstants.Param;
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalProperty;
import org.zstack.network.service.virtualrouter.VirtualRouterManager;
import org.zstack.network.service.virtualrouter.VirtualRouterStruct;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;
import org.zstack.network.service.virtualrouter.vyos.VyosOfferingSelector;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2016/11/10.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VyosCreateIPsecFlow extends NoRollbackFlow {

    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    private VyosIPsecBackend bkd;
    @Autowired
    private VirtualRouterHaBackend haBackend;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        List<L3NetworkInventory> L3Networks = (List<L3NetworkInventory>) data.get(Param.GUEST_L3);
        IPsecConnectionInventory inv = (IPsecConnectionInventory) data.get(Param.IPSEC_STRUCT);

        VirtualRouterStruct s = new VirtualRouterStruct();
        s.setL3Network(L3Networks.get(0));
        s.setApplianceVmType(VyosConstants.VYOS_VM_TYPE);
        s.setProviderType(VyosConstants.VYOS_ROUTER_PROVIDER_TYPE);
        s.setVirtualRouterOfferingSelector(new VyosOfferingSelector());
        s.setApplianceVmAgentPort(VirtualRouterGlobalProperty.AGENT_PORT);

        vrMgr.acquireVirtualRouterVm(s, new ReturnValueCompletion<VirtualRouterVmInventory>(trigger) {
            @Override
            public void success(VirtualRouterVmInventory vr) {

                bkd.createIpsecConnection(vr, inv, new Completion(trigger) {
                    @Override
                    public void success() {
                        submitCreateIpsecToHaRouter(vr, inv, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(operr("create ipsec to ha route failed, because %s", errorCode.getDescription()));
                            }
                        });
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }

    private void submitCreateIpsecToHaRouter(VirtualRouterVmInventory vrInv, IPsecConnectionInventory inv, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(VyosIPsecBackend.CREATE_IPSEC_TASK);
        task.setOriginRouterUuid(vrInv.getUuid());
        task.setJsonData(JSONObjectUtil.toJsonString(inv));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }
}
