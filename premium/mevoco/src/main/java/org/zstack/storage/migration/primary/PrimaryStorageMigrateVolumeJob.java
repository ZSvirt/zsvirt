package org.zstack.storage.migration.primary;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Constants;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobErrors;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.volume.VolumeVO;
import org.zstack.longjob.LongJobUtils;
import org.zstack.mevoco.MevocoConstants;
import org.zstack.utils.gson.JSONObjectUtil;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

/**
 * Created by GuoYi on 12/7/17.
 */
@LongJobFor(APIPrimaryStorageMigrateVolumeMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageMigrateVolumeJob implements LongJob {
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;

    protected String auditResourceUuid;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        PrimaryStorageMigrateVolumeMsg msg = JSONObjectUtil.toObject(job.getJobData(), PrimaryStorageMigrateVolumeMsg.class);
        bus.makeLocalServiceId(msg, MevocoConstants.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    PrimaryStorageMigrateVolumeReply r = reply.castReply();
                    APIPrimaryStorageMigrateVolumeEvent evt = new APIPrimaryStorageMigrateVolumeEvent(ThreadContext.get(Constants.THREAD_CONTEXT_API));

                    auditResourceUuid = r.getInventory().getUuid();
                    evt.setInventory(r.getInventory());
                    completion.success(evt);
                } else {
                    auditResourceUuid = msg.getVolumeUuid();
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        PrimaryStorageMigrateVolumeMsg msg = JSONObjectUtil.toObject(job.getJobData(), PrimaryStorageMigrateVolumeMsg.class);

        PrimaryStorageCancelMigrateVolumeMsg cmsg = new PrimaryStorageCancelMigrateVolumeMsg();
        cmsg.setVolumeUuid(msg.getVolumeUuid());
        cmsg.setType(msg.getType());
        cmsg.setDstPrimaryStorageUuid(msg.getDstPrimaryStorageUuid());
        cmsg.setSrcPrimaryStorageUuid(msg.getSrcPrimaryStorageUuid());
        cmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        cmsg.setCancellationApiId(job.getApiId());
        cmsg.setSystemTags(msg.getSystemTags());
        bus.makeLocalServiceId(cmsg, MevocoConstants.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success(false);
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
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
