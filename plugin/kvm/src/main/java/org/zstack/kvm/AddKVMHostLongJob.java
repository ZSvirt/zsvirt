package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.APIAddHostEvent;
import org.zstack.header.host.AddHostReply;
import org.zstack.header.host.HostConstant;
import org.zstack.header.longjob.*;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.longjob.LongJobUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

/**
 * Created by Qi Le on 2020/5/13
 */
@UseApiTimeout(APIAddKVMHostMsg.class)
@LongJobFor(APIAddKVMHostMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AddKVMHostLongJob implements LongJob {
    @Autowired
    protected DatabaseFacade dbf;

    @Autowired
    protected CloudBus bus;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        AddKVMHostMsg msg = JSONObjectUtil.toObject(job.getJobData(), AddKVMHostMsg.class);
        bus.makeLocalServiceId(msg, HostConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    AddHostReply hostReply = reply.castReply();
                    LongJobUtils.setJobResult(job.getUuid(), hostReply.getInventory());
                    APIAddHostEvent event = new APIAddHostEvent();
                    event.setInventory(hostReply.getInventory());
                    completion.success(event);
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }
}
