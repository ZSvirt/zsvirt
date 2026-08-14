package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.CheckVmStateOnHypervisorMsg;
import org.zstack.header.host.CheckVmStateOnHypervisorReply;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostErrors;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.DestroyVmOnHypervisorMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.Map;

import static org.zstack.core.Platform.operr;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmDestroyOnHypervisorFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VmDestroyOnHypervisorFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;

    private void getVmStateOnHypervisor(final String hostUuid, String vmUuid, final ReturnValueCompletion<VmInstanceState> completion) {
        CheckVmStateOnHypervisorMsg msg = new CheckVmStateOnHypervisorMsg();
        msg.setVmInstanceUuids(Collections.singletonList(vmUuid));
        msg.setHostUuid(hostUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("unable to check state of the vm[uuid:%s] on the host[uuid:%s], %s",
                            vmUuid, hostUuid, reply.getError()));
                    completion.fail(reply.getError());
                    return;
                }

                CheckVmStateOnHypervisorReply r = reply.castReply();
                String state = r.getStates().get(vmUuid);
                if (state == null) {
                    completion.fail(operr("null state of the vm[uuid:%s] on the host[uuid:%s]", vmUuid, hostUuid));
                    return;
                }

                completion.success(VmInstanceState.valueOf(state));
            }
        });
    }

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

        if (VmInstanceState.Stopped.toString().equals(spec.getVmInventory().getState())) {
            chain.next();
            return;
        }

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

                if (!reply.getError().isError(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE)) {
                    /*continue to run the chain if the vm has been shutoff in hypervisor*/
                    getVmStateOnHypervisor(hostUuid, spec.getVmInventory().getUuid(), new ReturnValueCompletion<VmInstanceState>(chain) {
                        @Override
                        public void success(VmInstanceState state) {
                            if (VmInstanceState.Stopped.equals(state)) {
                                chain.next();
                            } else {
                                chain.fail(reply.getError());
                            }
                        }
                        @Override
                        public void fail(ErrorCode errorCode) {
                            chain.fail(errorCode.causedBy(reply.getError()));
                        }
                    });
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
