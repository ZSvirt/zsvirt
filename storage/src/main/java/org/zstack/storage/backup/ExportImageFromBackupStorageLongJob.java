package org.zstack.storage.backup;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.Constants;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageBackupStorageRefVO_;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.utils.gson.JSONObjectUtil;


/**
 * Created by on camile 2018/3/7.
 */
@LongJobFor(APIExportImageFromBackupStorageMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ExportImageFromBackupStorageLongJob implements LongJob {
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;

    protected String auditResourceUuid;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        ExportImageFromBackupStorageMsg msg = JSONObjectUtil.toObject(job.getJobData(), ExportImageFromBackupStorageMsg.class);
        msg.setImageName(Q.New(ImageVO.class).select(ImageVO_.name).eq(ImageVO_.uuid, msg.getImageUuid()).findValue());
        bus.makeLocalServiceId(msg, BackupStorageConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    ExportImageFromBackupStorageReply r = reply.castReply();
                    APIExportImageFromBackupStorageEvent evt = new APIExportImageFromBackupStorageEvent(ThreadContext.get(Constants.THREAD_CONTEXT_API));

                    SQL.New(ImageBackupStorageRefVO.class)
                            .eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getBackupStorageUuid())
                            .eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid())
                            .set(ImageBackupStorageRefVO_.exportUrl, r.getImageUrl())
                            .set(ImageBackupStorageRefVO_.exportMd5Sum, r.getMd5sum())
                            .update();

                    auditResourceUuid = msg.getImageUuid();
                    evt.setImageUrl(r.getImageUrl());
                    evt.setExportMd5Sum(r.getMd5sum());
                    completion.success(evt);
                } else {
                    auditResourceUuid = msg.getImageUuid();
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        ExportImageFromBackupStorageMsg msg = JSONObjectUtil.toObject(job.getJobData(), ExportImageFromBackupStorageMsg.class);

        CancelJobBackupStorageMsg cmsg = new CancelJobBackupStorageMsg();
        cmsg.setBackupStorageUuid(msg.getBackupStorageUuid());
        cmsg.setCancellationApiId(job.getApiId());
        bus.makeTargetServiceIdByResourceUuid(cmsg, BackupStorageConstant.SERVICE_ID, cmsg.getBackupStorageUuid());
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
        return ImageVO.class;
    }

    @Override
    public String getAuditResourceUuid() {
        return auditResourceUuid;
    }
}
