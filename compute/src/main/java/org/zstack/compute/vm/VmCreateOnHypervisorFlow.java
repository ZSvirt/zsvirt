package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostErrors;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmCreateOnHypervisorFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmCreateOnHypervisorFlow.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private EventFacade evtf;

    private final List<VmBeforeCreateOnHypervisorExtensionPoint> exts = pluginRgty.getExtensionList(VmBeforeCreateOnHypervisorExtensionPoint.class);

    @Override
    public String name() {
        return "start-vm-on-the-hypervisor";
    }

    private void fireExtensions(VmInstanceSpec spec) {
        for (VmBeforeCreateOnHypervisorExtensionPoint ext : exts) {
            ext.beforeCreateVmOnHypervisor(spec);
        }
    }

    @Override
    public void run(final FlowTrigger chain, final Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        fireExtensions(spec);

        CreateVmOnHypervisorMsg msg = new CreateVmOnHypervisorMsg();
        msg.setVmSpec(spec);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, spec.getDestHost().getUuid());
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    chain.next();
                } else {
                    chain.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void rollback(final FlowRollback trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        DestroyVmOnHypervisorMsg msg = new DestroyVmOnHypervisorMsg();
        msg.setVmInventory(spec.getVmInventory());
        msg.getVmInventory().setHostUuid(spec.getDestHost().getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, spec.getDestHost().getUuid());
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to roll back vm[uuid:%s, name:%s] on host[uuid:%s, ip:%s], %s",
                            spec.getVmInventory().getUuid(), spec.getVmInventory().getName(),
                            spec.getDestHost().getUuid(), spec.getDestHost().getName(), reply.getError()));

                    if (reply.getError().isError(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE)) {
                        String gcName = String.format("gc-vm-%s-on-host-%s", spec.getVmInventory().getUuid(), spec.getDestHost().getUuid());

                        DeleteVmGC gc = new DeleteVmGC();
                        gc.NAME = gcName;
                        gc.hostUuid = spec.getVmInventory().getHostUuid();
                        gc.inventory = spec.getVmInventory();
                        if (gc.existedAndNotCompleted()) {
                            logger.debug(String.format("There is already a DeleteVmGC of vm[uuid:%s] " +
                                    "on host[uuid:%s], skip.", spec.getVmInventory().getUuid(), spec.getDestHost().getUuid()));
                        } else {
                            gc.submit();
                        }
                    } else {
                        VmTracerCanonicalEvents.OperateFailOnHypervisorData data = new VmTracerCanonicalEvents.OperateFailOnHypervisorData();
                        data.setHostUuid(spec.getVmInventory().getHostUuid());
                        data.setVmUuid(spec.getVmInventory().getUuid());
                        data.setOperate(DestroyVmOnHypervisorMsg.class.getSimpleName());
                        data.setResult(reply.getError().toString());

                        evtf.fire(VmTracerCanonicalEvents.VM_OPERATE_FAIL_ON_HYPERVISOR_PATH, data);
                    }
                }
                trigger.rollback();
            }
        });
    }
}
