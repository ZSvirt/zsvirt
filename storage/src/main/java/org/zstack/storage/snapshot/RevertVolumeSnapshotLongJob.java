package org.zstack.storage.snapshot;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusImpl3;
import org.zstack.header.Constants;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobErrors;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.volume.VolumeVO;
import org.zstack.longjob.LongJobUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

/**
 * Created by kayo on 2018/5/9.
 */
@LongJobFor(APIRevertVolumeFromSnapshotMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class RevertVolumeSnapshotLongJob implements LongJob {
    @Autowired
    protected CloudBus bus;

    protected String auditResourceUuid;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        RevertVolumeSnapshotMsg msg = new RevertVolumeSnapshotMsg();
        APIRevertVolumeFromSnapshotMsg apiMessage = JSONObjectUtil.toObject(job.getJobData(), APIRevertVolumeFromSnapshotMsg.class);
        msg.setSnapshotUuid(apiMessage.getSnapshotUuid());
        msg.setVolumeUuid(apiMessage.getVolumeUuid());
        msg.setTreeUuid(apiMessage.getTreeUuid());
        msg.setSession(apiMessage.getSession());
        bus.makeServiceIdByManagementNodeId(msg, VolumeSnapshotConstant.SERVICE_ID, getRoutedMnId(apiMessage));
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                auditResourceUuid = msg.getVolumeUuid();
                if (reply.isSuccess()) {
                    APIRevertVolumeFromSnapshotEvent evt = new APIRevertVolumeFromSnapshotEvent(ThreadContext.get(Constants.THREAD_CONTEXT_API));

                    completion.success(evt);
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private String getRoutedMnId(APIRevertVolumeFromSnapshotMsg msg) {
        // ApiMsg has been set service id in their way, follow it.
        return CloudBusImpl3.getManagementNodeUUIDFromServiceID(msg.getServiceId());
    }

    @Override
    public Class getAuditType() {
        return VolumeVO.class;
    }

    @Override
    public String getAuditResourceUuid() {
        return auditResourceUuid;
    }
}
