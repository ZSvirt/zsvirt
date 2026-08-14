package org.zstack.storage.backup.imagestore;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobErrors;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.longjob.LongJobUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

@LongJobFor(APIReclaimSpaceFromImageStoreMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ReclaimSpaceFromImageStoreLongJob implements LongJob {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        APIReclaimSpaceFromImageStoreMsg msg = JSONObjectUtil.toObject(job.getJobData(), APIReclaimSpaceFromImageStoreMsg.class);

        APIReclaimSpaceFromImageStoreEvent evt = new APIReclaimSpaceFromImageStoreEvent(msg.getId());
        ReclaimSpaceFromImageStoreMsg rmsg = new ReclaimSpaceFromImageStoreMsg();
        rmsg.setUuid(msg.getUuid());
        bus.makeTargetServiceIdByResourceUuid(rmsg, BackupStorageConstant.SERVICE_ID, rmsg.getUuid());
        bus.send(rmsg, new CloudBusCallBack(rmsg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    job.setJobResult(JSONObjectUtil.toJsonString(reply));
                    dbf.update(job);
                    completion.success(evt);
                }
            }
        });
    }
}
