package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.utils.gson.JSONObjectUtil;

@LongJobFor(APIFlattenVmInstanceMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class FlattenVmLongJob implements LongJob {
    @Autowired
    private CloudBus bus;

    private String vmInstanceUuid;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        FlattenVmInstanceMsg msg = JSONObjectUtil.toObject(job.getJobData(), FlattenVmInstanceMsg.class);
        vmInstanceUuid = msg.getUuid();
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, msg.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                APIFlattenVmInstanceEvent event = new APIFlattenVmInstanceEvent(job.getApiId());
                event.setInventory(((FlattenVmInstanceReply) reply).getInventory());
                completion.success(event);
            }
        });
    }

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        FlattenVmInstanceMsg msg = JSONObjectUtil.toObject(job.getJobData(), FlattenVmInstanceMsg.class);
        CancelFlattenVmInstanceMsg cmsg = new CancelFlattenVmInstanceMsg();
        cmsg.setCancellationApiId(job.getApiId());
        cmsg.setUuid(msg.getUuid());
        bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, msg.getUuid());
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                completion.success(false);
            }
        });
    }

    @Override
    public Class getAuditType() {
        return VmInstanceVO.class;
    }

    @Override
    public String getAuditResourceUuid() {
        return vmInstanceUuid;
    }
}
