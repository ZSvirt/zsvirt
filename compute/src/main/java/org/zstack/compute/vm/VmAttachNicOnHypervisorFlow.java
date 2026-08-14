package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmAttachNicOnHypervisorMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAttachNicOnHypervisorFlow extends NoRollbackFlow {
    protected static final CLogger logger = Utils.getLogger(VmAttachNicOnHypervisorFlow.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        VmNicInventory nic = spec.getDestNics().get(0);
        final String hostUuid = spec.getVmInventory().getHostUuid() == null ?
                spec.getVmInventory().getLastHostUuid() :
                spec.getVmInventory().getHostUuid();
        if (hostUuid == null) {
            logger.info(String.format("Skip attaching nic on hypervisor: host of VM[uuid=%s] is not set",
                    spec.getVmInventory().getUuid()));
            trigger.next();
            return;
        }

        VmAttachNicOnHypervisorMsg msg = new VmAttachNicOnHypervisorMsg();
        msg.setHostUuid(hostUuid);
        msg.setNicInventory(nic);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    trigger.fail(reply.getError());
                } else {
                    trigger.next();
                }
            }
        });
    }
}
