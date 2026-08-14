package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmStartOnHypervisorFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmStartOnHypervisorFlow.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;

    private final List<VmBeforeStartOnHypervisorExtensionPoint> exts = pluginRgty.getExtensionList(VmBeforeStartOnHypervisorExtensionPoint.class);

    private void fireExtensions(VmInstanceSpec spec) {
        for (VmBeforeStartOnHypervisorExtensionPoint ext : exts) {
            ext.beforeStartVmOnHypervisor(spec);
        }
    }

    @Override
    public void run(final FlowTrigger chain, final Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        fireExtensions(spec);

        StartVmOnHypervisorMsg msg = new StartVmOnHypervisorMsg();
        msg.setVmSpec(spec);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, spec.getDestHost().getUuid());
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    data.put(VmStartOnHypervisorFlow.class.getName(), true);
                    chain.next();
                } else {
                    chain.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void rollback(final FlowRollback chain, Map data) {
        if (!data.containsKey(VmStartOnHypervisorFlow.class.getName())) {
            chain.rollback();
            return;
        }

        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        StopVmOnHypervisorMsg msg = new StopVmOnHypervisorMsg();
        msg.setVmInventory(spec.getVmInventory());
        msg.getVmInventory().setHostUuid(spec.getDestHost().getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, spec.getDestHost().getUuid());
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to stop vm[uuid:%s] on host[uuid:%s], %s", spec.getVmInventory().getUuid(), spec.getDestHost().getUuid(), reply.getError()));
                }
                chain.rollback();
            }
        });
    }
}
