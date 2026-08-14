package org.zstack.network.service.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class Vip {
    @Autowired
    private CloudBus bus;

    private String uuid;
    private ModifyVipAttributesStruct struct;

    public Vip(String uuid) {
        this.uuid = uuid;
    }

    public ModifyVipAttributesStruct getStruct() {
        return struct;
    }

    public void setStruct(ModifyVipAttributesStruct struct) {
        this.struct = struct;
    }

    public void acquire(Completion completion) {
        AcquireVipMsg msg = new AcquireVipMsg();
        msg.setVipUuid(uuid);
        msg.setStruct(struct);

        bus.makeTargetServiceIdByResourceUuid(msg, VipConstant.SERVICE_ID, uuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    throw new OperationFailureException(reply.getError());
                }

                completion.success();
            }
        });
    }

    public void release(Completion completion) {
        ReleaseVipMsg msg = new ReleaseVipMsg();
        msg.setVipUuid(uuid);
        msg.setStruct(struct);

        bus.makeTargetServiceIdByResourceUuid(msg, VipConstant.SERVICE_ID, uuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    throw new OperationFailureException(reply.getError());
                }

                completion.success();
            }
        });
    }

    public void stop(Completion completion) {
        StopVipMsg msg = new StopVipMsg();
        msg.setVipUuid(uuid);
        msg.setStruct(struct);

        bus.makeTargetServiceIdByResourceUuid(msg, VipConstant.SERVICE_ID, uuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    throw new OperationFailureException(reply.getError());
                }

                completion.success();
            }
        });
    }
}
