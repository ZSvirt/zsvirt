package org.zstack.storage.volume;

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
import org.zstack.header.volume.*;
import org.zstack.utils.gson.JSONObjectUtil;

@LongJobFor(APIFlattenVolumeMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class FlattenVolumeLongJob implements LongJob {
    @Autowired
    private CloudBus bus;

    private String volumeUuid;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        FlattenVolumeMsg msg = JSONObjectUtil.toObject(job.getJobData(), FlattenVolumeMsg.class);
        volumeUuid = msg.getVolumeUuid();
        bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, msg.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                APIFlattenVolumeEvent event = new APIFlattenVolumeEvent(job.getApiId());
                event.setInventory(((FlattenVolumeReply) reply).getInventory());
                completion.success(event);
            }
        });
    }

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        FlattenVolumeMsg msg = JSONObjectUtil.toObject(job.getJobData(), FlattenVolumeMsg.class);
        CancelFlattenVolumeMsg cmsg = new CancelFlattenVolumeMsg();
        cmsg.setCancellationApiId(job.getApiId());
        cmsg.setUuid(msg.getUuid());
        bus.makeTargetServiceIdByResourceUuid(cmsg, VolumeConstant.SERVICE_ID, msg.getUuid());
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
        return VolumeVO.class;
    }

    @Override
    public String getAuditResourceUuid() {
        return volumeUuid;
    }
}
