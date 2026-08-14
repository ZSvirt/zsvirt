package org.zstack.image;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Constants;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.image.*;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.longjob.LongJob;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.longjob.LongJobUtils.cancelErr;
import static org.zstack.longjob.LongJobUtils.jobCanceled;

/**
 * Created by on camile 2018/2/5.
 */
@LongJobFor(APICreateRootVolumeTemplateFromRootVolumeMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateRootVolumeTemplateFromRootVolumeLongJob implements LongJob {
    private static final CLogger logger = Utils.getLogger(CreateRootVolumeTemplateFromRootVolumeLongJob.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;

    protected String auditResourceUuid;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        CreateRootVolumeTemplateFromRootVolumeMsg msg = JSONObjectUtil.toObject(job.getJobData(), CreateRootVolumeTemplateFromRootVolumeMsg.class);
        if (msg.getResourceUuid() == null) {
            msg.setResourceUuid(Platform.getUuid());
            job.setJobData(JSONObjectUtil.toJsonString(msg));
            dbf.updateAndRefresh(job);
        }
        bus.makeLocalServiceId(msg, ImageConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    CreateRootVolumeTemplateFromRootVolumeReply r = reply.castReply();
                    APICreateRootVolumeTemplateFromRootVolumeEvent evt = new APICreateRootVolumeTemplateFromRootVolumeEvent(ThreadContext.get(Constants.THREAD_CONTEXT_API));

                    auditResourceUuid = r.getInventory().getUuid();
                    evt.setInventory(r.getInventory());
                    if (jobCanceled(job.getUuid())) {
                        deleteAfterCancel(r.getInventory(), job, completion);
                    } else {
                        completion.success(evt);
                    }
                } else {
                    auditResourceUuid = msg.getResourceUuid();
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private void deleteAfterCancel(ImageInventory image, LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        new While<>(image.getBackupStorageRefs()).all((ref, compl) -> {
            ExpungeImageMsg emsg = new ExpungeImageMsg();
            emsg.setBackupStorageUuid(ref.getBackupStorageUuid());
            emsg.setImageUuid(image.getUuid());
            bus.makeTargetServiceIdByResourceUuid(emsg, ImageConstant.SERVICE_ID, image.getUuid());
            bus.send(emsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                    }

                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.fail(cancelErr(job.getUuid()));
            }
        });
    }

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        CreateRootVolumeTemplateFromRootVolumeMsg origin = JSONObjectUtil.toObject(job.getJobData(), CreateRootVolumeTemplateFromRootVolumeMsg.class);
        CancelCreateRootVolumeTemplateFromRootVolumeMsg msg = new CancelCreateRootVolumeTemplateFromRootVolumeMsg();
        msg.setImageUuid(origin.getResourceUuid());
        msg.setRootVolumeUuid(origin.getRootVolumeUuid());
        msg.setCancellationApiId(job.getApiId());
        bus.makeLocalServiceId(msg, ImageConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    completion.success(false);
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
