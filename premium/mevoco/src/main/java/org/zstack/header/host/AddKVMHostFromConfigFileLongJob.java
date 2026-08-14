package org.zstack.header.host;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.longjob.*;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.kvm.APIAddKVMHostMsg;
import org.zstack.longjob.LongJobGlobalConfig;
import org.zstack.longjob.LongJobUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

@UseApiTimeout(APIAddKVMHostMsg.class)
@LongJobFor(APIAddKVMHostFromConfigFileMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AddKVMHostFromConfigFileLongJob implements LongJob {
    @Autowired
    protected DatabaseFacade dbf;

    @Autowired
    protected CloudBus bus;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        AddKVMHostFromConfigFileMsg msg = JSONObjectUtil.toObject(job.getJobData(), AddKVMHostFromConfigFileMsg.class);
        msg.setTimeout(TimeUnit.SECONDS.toMillis(LongJobGlobalConfig.LONG_JOB_DEFAULT_TIMEOUT.value(Long.class)));
        bus.makeLocalServiceId(msg, HostConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    AddHostFromConfigFileReply r = reply.castReply();
                    LongJobUtils.setJobResult(job.getUuid(), r.getResults());
                    completion.success(null);
                } else if (reply.isCanceled()) {
                    AddHostFromConfigFileReply r = reply.castReply();
                    LongJobUtils.setJobResult(job.getUuid(), r.getResults());
                    completion.fail(reply.getError());
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        completion.success(false);
    }
}
