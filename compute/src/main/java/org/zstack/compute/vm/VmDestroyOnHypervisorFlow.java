package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostErrors;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.DestroyVmOnHypervisorMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmDestroyOnHypervisorFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VmDestroyOnHypervisorFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;

    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        final String hostUuid = spec.getVmInventory().getHostUuid() == null ? spec.getVmInventory().getLastHostUuid() : spec.getVmInventory().getHostUuid();
        if (spec.getVmInventory().getClusterUuid() == null || hostUuid == null) {
            // the vm failed to start because no host available at that time
            // no need to send DestroyVmOnHypervisorMsg
            chain.next();
            return;
        }

        if (!dbf.isExist(hostUuid, HostVO.class)) {
            // An unregistered host cannot have resources managed by this flow.
            chain.next();
            return;
        }

        // Stopped describes power state, not whether a persistent domain remains.
        // Always request idempotent host cleanup before deleting VM resources.
        DestroyVmOnHypervisorMsg msg = new DestroyVmOnHypervisorMsg();
        msg.setVmInventory(spec.getVmInventory());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(chain) {

            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    chain.next();
                    return;
                }

                if (!reply.getError().isError(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE, HostErrors.HOST_IS_DISCONNECTED)) {
                    // A powered-off domain can still retain its disk references.
                    // Only successful cleanup (or a tracked GC) permits deletion.
                    chain.fail(reply.getError());
                    return;
                }

                String gcName = String.format("gc-vm-%s-on-host-%s", spec.getVmInventory().getUuid(), hostUuid);

                DeleteVmGC gc = new DeleteVmGC();
                gc.NAME = gcName;
                gc.hostUuid = hostUuid;
                gc.inventory = spec.getVmInventory();
                if (gc.existedAndNotCompleted()) {
                    logger.debug(String.format("There is already a DeleteVmGC of vm[uuid:%s] " +
                            "on host[uuid:%s], skip.", spec.getVmInventory().getUuid(), hostUuid));
                } else {
                    gc.submit();
                }

                chain.next();
            }
        });
    }
}
