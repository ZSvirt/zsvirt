package org.zstack.compute.vm;

import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.TimeBasedGarbageCollector;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by mingjian.deng on 2020/5/14.
 */
public class UnknownVmGC extends TimeBasedGarbageCollector {
    private static final CLogger logger = Utils.getLogger(UnknownVmGC.class);

    @GC
    public String vmUuid;
    @GC
    public String vmState;
    @GC
    public String hostUuid;

    public static String getGCName(String vmUuid) {
        return String.format("gc-set-vm-%s-unknown", vmUuid);
    }

    @Override
    protected void triggerNow(GCCompletion completion) {
        VmInstanceVO vo = dbf.findByUuid(vmUuid, VmInstanceVO.class);
        if (vo == null || !vo.getState().toString().equals(vmState)) {
            logger.debug(String.format("vm [%s] has been deleted or its state has been changed, cancel the gc job", vmUuid));
            completion.cancel();
            return;
        }

        if (vo.getState() == VmInstanceState.Unknown) {
            logger.debug(String.format("vm is already been set to Unknown, cancel job %s", NAME));
            completion.cancel();
            return;
        }

        VmStateChangedOnHostMsg msg = new VmStateChangedOnHostMsg();
        msg.setVmInstanceUuid(vmUuid);
        msg.setVmStateAtTracingMoment(VmInstanceState.valueOf(vmState));
        msg.setHostUuid(hostUuid);
        msg.setStateOnHost(VmInstanceState.Unknown);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if(!reply.isSuccess()){
                    completion.fail(reply.getError());
                } else {
                    logger.debug(String.format("the host[uuid:%s] disconnected, change the VM[uuid:%s]' state to Unknown", hostUuid, vmUuid));
                    completion.success();
                }
            }
        });
    }
}
