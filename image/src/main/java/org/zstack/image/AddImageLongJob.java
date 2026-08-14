package org.zstack.image;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.AutoOffEventCallback;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.image.*;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.longjob.UseApiTimeout;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.CancelTaskResult;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.ImageHashAlgorithm;
import org.zstack.longjob.LongJobGlobalConfig;
import org.zstack.longjob.LongJobUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.zstack.core.Platform.operr;
import static org.zstack.longjob.LongJobUtils.*;


/**
 * Created by on camile 2018/2/2.
 */
@UseApiTimeout(APIAddImageMsg.class)
@LongJobFor(APIAddImageMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AddImageLongJob implements LongJob {
    private static final CLogger logger = Utils.getLogger(AddImageLongJob.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected EventFacade evtf;

    protected String auditResourceUuid;

    class AddImageCompletion<T extends ImageInventory> extends ReturnValueCompletion<T> {
        APIAddImageEvent event;
        LongJobVO job;
        ReturnValueCompletion<APIEvent> completion;
        AtomicBoolean done = new AtomicBoolean(false);

        AddImageCompletion(APIAddImageEvent event, LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
            super(completion);
            this.job = job;
            this.event = event;
            this. completion = completion;
        }

        @Override
        public void success(ImageInventory image) {
            if (done.compareAndSet(false, true)) {
                event.setInventory(image);
                job = setJobResult(job.getUuid(), event);
                completion.success(event);
                calculateImageHash(image);
            }
        }

        @Override
        public void fail(ErrorCode err) {
            if (done.compareAndSet(false, true)) {
                job = setJobError(job.getUuid(), err);
                completion.fail(err);
            }
        }

        public void track(ImageInventory inv) {
            if (!done.get()) {
                event.setInventory(inv);
                job = setJobResult(job.getUuid(), event);
            }
        }

        void startTrack() {
            long offTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(LongJobGlobalConfig.LONG_JOB_DEFAULT_TIMEOUT.value(Long.class));
            evtf.on(ImageCanonicalEvents.IMAGE_TRACK_RESULT_PATH, new AutoOffEventCallback() {
                @Override
                protected boolean run(Map tokens, Object d) {
                    ImageCanonicalEvents.ImageTrackData data = (ImageCanonicalEvents.ImageTrackData) d;
                    if (data.getUuid().equals(job.getTargetResourceUuid())) {
                        handleResult(data);
                        return true;
                    } else if (offTime < System.currentTimeMillis()) {
                        return true;
                    }

                    return false;
                }

                private void handleResult(ImageCanonicalEvents.ImageTrackData data) {
                    if (data.isSuccess()) {
                        success(data.getInventory());
                    } else if (data.getError().isError(ImageErrors.UPLOAD_IMAGE_INTERRUPTED)){
                        fail(LongJobUtils.interruptedErr(job.getUuid(), data.getError()));
                    } else {
                        fail(data.getError());
                    }
                }
            });
        }

        void calculateImageHash(ImageInventory image) {
            for (ImageBackupStorageRefInventory ref : image.getBackupStorageRefs()) {
                CalculateImageHashMsg msg = new CalculateImageHashMsg();
                msg.setAlgorithm(ImageHashAlgorithm.MD5.toString());
                msg.setUuid(image.getUuid());
                msg.setBackupStorageUuid(ref.getBackupStorageUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, ImageConstant.SERVICE_ID, msg.getImageUuid());
                bus.send(msg);
            }
        }
    }

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> compl) {
        AddImageMsg msg = JSONObjectUtil.toObject(job.getJobData(), AddImageMsg.class);
        if (msg.getResourceUuid() == null) {
            msg.setResourceUuid(Platform.getUuid());
            job.setJobData(JSONObjectUtil.toJsonString(msg));
            job.setTargetResourceUuid(msg.getResourceUuid());
            dbf.updateAndRefresh(job);
        } else {
            job.setTargetResourceUuid(msg.getResourceUuid());
            dbf.updateAndRefresh(job);
        }

        APIAddImageEvent evt = new APIAddImageEvent(job.getApiId());

        AddImageCompletion completion = new AddImageCompletion(evt, job, compl);
        if (msg.needTrack()) {
            completion.startTrack();
        }

        bus.makeTargetServiceIdByResourceUuid(msg, ImageConstant.SERVICE_ID, msg.getResourceUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    handleSuccess(reply);
                } else {
                    auditResourceUuid = msg.getResourceUuid();
                    completion.fail(reply.getError());
                }
            }

            private void handleSuccess(MessageReply reply) {
                AddImageReply r = reply.castReply();

                auditResourceUuid = r.getInventory().getUuid();
                if (jobCanceled(job.getUuid())) {
                    cleanImage(msg, completion, cancelErr(job.getUuid()));
                } else if (msg.needTrack()) {
                    completion.track(r.getInventory());
                } else {
                    completion.success(r.getInventory());
                }
            }
        });
    }

    @Override
    public void resume(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        AddImageMsg msg = JSONObjectUtil.toObject(job.getJobData(), AddImageMsg.class);
        ImageVO image = Q.New(ImageVO.class).eq(ImageVO_.uuid, msg.getResourceUuid()).find();
        if (msg.needTrack() && image != null && !image.getBackupStorageRefs().isEmpty()) {
            APIAddImageEvent evt = new APIAddImageEvent(job.getApiId());
            new AddImageCompletion(evt, job, completion).startTrack();
            new UploadImageTracker().trackUpload(image, image.getBackupStorageRefs().iterator().next());
            return;
        }

        ImageDeletionMsg dmsg = buildDeletionMsg(msg);
        bus.send(dmsg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("delete image [%s] failed after management node restarted", msg.getResourceUuid()));
                }

                completion.fail(operr("Failed because management node restarted."));
            }
        });
    }

    private void cleanImage(AddImageMsg msg, AddImageCompletion completion, ErrorCode err) {
        ImageDeletionMsg dmsg = buildDeletionMsg(msg);
        bus.send(dmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.fail(err);
                    return;
                }

                logger.warn(String.format("failed to clean canceled image [uuid:%s]: %s",
                        msg.getResourceUuid(), reply.getError().getReadableDetails()));
            }
        });
    }

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        CancelAddImageMsg msg = new CancelAddImageMsg();
        AddImageMsg amsg = JSONObjectUtil.toObject(job.getJobData(), AddImageMsg.class);
        msg.setMsg(amsg);
        msg.setImageUuid(amsg.getResourceUuid());
        msg.setCancellationApiId(job.getApiId());
        msg.setAllowTaskNotFound(true);
        bus.makeTargetServiceIdByResourceUuid(msg, ImageConstant.SERVICE_ID, msg.getImageUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    CancelDownloadImageReply creply = reply.castReply();
                    if (creply.getCancelResult() == CancelTaskResult.TASK_NOT_FOUND) {
                        cleanAndCompleteCancel(job, completion);
                        return;
                    }
                    completion.success(false);
                } else if (reply.getError().isError(SysErrors.RESOURCE_NOT_FOUND)) {
                    completion.success(true);
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private void cleanAndCompleteCancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        AddImageMsg amsg = JSONObjectUtil.toObject(job.getJobData(), AddImageMsg.class);
        ImageDeletionMsg dmsg = buildDeletionMsg(amsg);
        bus.send(dmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success(true);
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void clean(LongJobVO job, NoErrorCompletion completion) {
        AddImageMsg amsg = JSONObjectUtil.toObject(job.getJobData(), AddImageMsg.class);
        ImageDeletionMsg dmsg = buildDeletionMsg(amsg);
        bus.send(dmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                completion.done();
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

    private ImageDeletionMsg buildDeletionMsg(AddImageMsg msg) {
        ImageDeletionMsg dmsg = new ImageDeletionMsg();
        dmsg.setImageUuid(msg.getResourceUuid());
        dmsg.setForceDelete(true);
        dmsg.setBackupStorageUuids(msg.getBackupStorageUuids());
        dmsg.setDeletionPolicy(ImageDeletionPolicyManager.ImageDeletionPolicy.Direct.toString());
        bus.makeTargetServiceIdByResourceUuid(dmsg, ImageConstant.SERVICE_ID, dmsg.getImageUuid());
        return dmsg;
    }


}
