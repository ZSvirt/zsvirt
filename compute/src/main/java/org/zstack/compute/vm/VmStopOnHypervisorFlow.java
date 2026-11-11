package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostErrors;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;

import java.util.Map;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmStopOnHypervisorFlow extends NoRollbackFlow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected PluginRegistry pluginRegistry;

    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        StopVmOnHypervisorMsg msg = new StopVmOnHypervisorMsg();
        msg.setVmInventory(spec.getVmInventory());
        if (spec.getMessage() instanceof StopVmMessage) {
           msg.setType(((StopVmMessage)spec.getMessage()).getType());
        } else if (spec.getMessage() instanceof RebootVmInstanceMsg) {
            msg.setType(((RebootVmInstanceMsg) spec.getMessage()).getType());
        }

        for (BeforeStopVmOnHypervisorExtensionPoint ext : pluginRegistry.getExtensionList(BeforeStopVmOnHypervisorExtensionPoint.class)) {
            ext.beforeStopVmOnHypervisor(spec, msg);
        }

        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, spec.getVmInventory().getHostUuid());
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    data.put(VmStopOnHypervisorFlow.class.getName(), true);
                    chain.next();
                    return;
                }

                if (!spec.isGcOnStopFailure() && reply.getError().isError(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE)) {
                    ErrorCode realError = reply.getError().getCause();
                    if (realError == null) {
                        realError = operr(reply.getError().getDetails());
                    }
                    chain.fail(realError);
                    return;
                }

                if (spec.isGcOnStopFailure() && reply.getError().isError(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE)) {

                    StopVmGC gc = new StopVmGC();
                    gc.inventory = spec.getVmInventory();
                    gc.hostUuid = spec.getVmInventory().getHostUuid();
                    gc.NAME = String.format("gc-stop-vm-%s-%s-on-host-%s", gc.inventory.getUuid(),
                            gc.inventory.getName(), gc.hostUuid);
                    gc.submit();

                    chain.next();
                    return;
                }

                chain.fail(reply.getError());
            }
        });
    }
}
