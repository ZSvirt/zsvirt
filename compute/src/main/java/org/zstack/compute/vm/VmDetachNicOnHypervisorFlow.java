package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.host.DetachNicFromVmOnHypervisorMsg;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by frank on 7/18/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmDetachNicOnHypervisorFlow extends NoRollbackFlow {
    protected static final CLogger logger = Utils.getLogger(VmDetachNicOnHypervisorFlow.class);

    @Autowired
    private CloudBus bus;

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final String hostUuid = spec.getVmInventory().getHostUuid() == null ?
                spec.getVmInventory().getLastHostUuid() :
                spec.getVmInventory().getHostUuid();
        if (hostUuid == null) {
            logger.info(String.format("Skip detaching nic on hypervisor: host of VM[uuid=%s] is not set",
                    spec.getVmInventory().getUuid()));
            trigger.next();
            return;
        }

        DetachNicFromVmOnHypervisorMsg msg = new DetachNicFromVmOnHypervisorMsg();
        msg.setHostUuid(hostUuid);
        msg.setVmInstanceUuid(spec.getVmInventory().getUuid());
        msg.setNic(spec.getDestNics().get(0));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    trigger.next();
                } else {
                    boolean ignoreError = (boolean) data.get(VmInstanceConstant.Params.ignoreDetachError.toString());
                    if (ignoreError) {
                        Pattern pattern = Pattern.compile(VmInstanceConstant.DETACH_NIC_FAILED_REGEX);
                        if (pattern.matcher(reply.getError().getDetails()).matches()) {
                            logger.warn(reply.getError().toString());
                            trigger.next();
                            return;
                        }
                    }

                    trigger.fail(reply.getError());
                }
            }
        });
    }
}
