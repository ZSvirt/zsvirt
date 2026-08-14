package org.zstack.header.baremetal.chassis;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobErrors;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.longjob.LongJobUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

/**
 * Created by GuoYi on 2018-10-08.
 */
@LongJobFor(APIBatchCreateBaremetalChassisMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BatchCreateBaremetalChassisJob implements LongJob {
    @Autowired
    protected DatabaseFacade dbf;

    @Autowired
    protected CloudBus bus;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        BatchCreateBaremetalChassisMsg msg = JSONObjectUtil.toObject(job.getJobData(), BatchCreateBaremetalChassisMsg.class);
        bus.makeLocalServiceId(msg, BaremetalChassisConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                String jobData = JSONObjectUtil.toJsonString(msg.getBaremetalChassisInfo());
                if (jobData.length() > 4096) {
                    jobData = jobData.substring(0, 4096) + "......";
                }

                LongJobVO newestJob = dbf.reload(job);
                newestJob.setJobData(jobData);

                if (reply.isSuccess()) {
                    BatchCreateBaremetalChassisReply rly = reply.castReply();
                    newestJob.setJobResult(JSONObjectUtil.toJsonString(rly.getResults()));
                    dbf.update(newestJob);
                    completion.success(null);
                } else {
                    dbf.update(newestJob);
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
