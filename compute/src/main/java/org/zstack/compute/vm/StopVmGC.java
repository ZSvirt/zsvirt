package org.zstack.compute.vm;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.gc.EventBasedGarbageCollector;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostCanonicalEvents;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 * Created by xing5 on 2017/3/4.
 */
public class StopVmGC extends EventBasedGarbageCollector {
    private static final CLogger logger = Utils.getLogger(StopVmGC.class);

    @GC
    public String hostUuid;
    @GC
    public VmInstanceInventory inventory;

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (!dbf.isExist(hostUuid, HostVO.class)) {
            completion.cancel();
            return;
        }

        VmInstanceState state = Q.New(VmInstanceVO.class).select(VmInstanceVO_.state)
                .eq(VmInstanceVO_.uuid, inventory.getUuid()).findValue();
        if (state == null || state == VmInstanceState.Destroyed) {
            completion.cancel();
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("gc-stop-vm-%s-on-host-%s", inventory.getUuid(), hostUuid));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                StopVmOnHypervisorMsg msg = new StopVmOnHypervisorMsg();
                msg.setVmInventory(inventory);
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
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                ChangeVmStateMsg cmsg = new ChangeVmStateMsg();
                cmsg.setVmInstanceUuid(inventory.getUuid());
                cmsg.setStateEvent(VmInstanceStateEvent.stopped.toString());
                bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, inventory.getUuid());
                bus.send(cmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        logger.warn(String.format("failed to change vm[uuid:%s,name:%s]'s status, however, it has been" +
                                " stopped on the host[uuid:%s]", inventory.getUuid(), inventory.getName(), hostUuid));
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    protected void setup() {
        onEvent(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, (tokens, data) -> {
            HostCanonicalEvents.HostStatusChangedData d = (HostCanonicalEvents.HostStatusChangedData) data;
            return d.getHostUuid().equals(hostUuid) && d.getNewStatus().equals(HostStatus.Connected.toString());
        });

        onEvent(HostCanonicalEvents.HOST_DELETED_PATH, ((tokens, data) -> {
            HostCanonicalEvents.HostDeletedData d = (HostCanonicalEvents.HostDeletedData) data;
            return hostUuid.equals(d.getHostUuid());
        }));

        onEvent(VmCanonicalEvents.VM_FULL_STATE_CHANGED_PATH, ((tokens, data) -> {
            VmCanonicalEvents.VmStateChangedData d = (VmCanonicalEvents.VmStateChangedData) data;
            return d.getVmUuid().equals(inventory.getUuid()) && d.getNewState().equals(VmInstanceState.Destroyed.toString());
        }));
    }
}
