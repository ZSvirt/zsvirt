package org.zstack.ovf;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.AutoOffEventCallback;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.timeout.Timer;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.longjob.CancelLongJobMsg;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobConstants;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobState;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.longjob.LongJobVO_;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.longjob.LongJobGlobalConfig;
import org.zstack.ovf.api.APICreateVmInstanceFromOvfEvent;
import org.zstack.ovf.api.APICreateVmInstanceFromOvfMsg;
import org.zstack.ovf.message.CreateVmInstanceFromOvfMsg;
import org.zstack.ovf.message.CreateVmInstanceFromOvfReply;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.zstack.core.Platform.*;
import static org.zstack.longjob.LongJobUtils.*;
import static org.zstack.ovf.datatype.CreateVmFromOvfCanonicalEvents.*;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.isEmpty;

/**
 * Created by Wenhao.Zhang on 22/03/09
 */
@LongJobFor(APICreateVmInstanceFromOvfMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateVmInstanceFromOvfLongJob implements LongJob {
    private static final CLogger logger = Utils.getLogger(CreateVmInstanceFromOvfLongJob.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected EventFacade evtf;
    @Autowired
    private Timer timer;

    class Handler {
        private String resourceUuid;
        LongJobVO job;
        List<UploadInfo> uploadInfoList = Collections.emptyList();
        AtomicBoolean done = new AtomicBoolean(false);

        public synchronized void track(OvfTrackData data) {
            if (data.getUploadInfos() == null || data.getUploadInfos().isEmpty()) {
                data.setUploadInfos(uploadInfoList);
            } else {
                uploadInfoList = data.getUploadInfos();
            }
            if (done.get()) {
                logger.debug("long job already finished, skip tracking");
            } else {
                setJobResult(job.getUuid(), data);
            }
        }

        public synchronized void success(VmInstanceInventory inventory) {
            if (!done.compareAndSet(false, true)) {
                return;
            }

            OvfTrackData data = new OvfTrackData();
            data.setInventory(inventory);
            data.setStage(STAGE_DONE);
            data.setVmInstanceUuid(resourceUuid);
            data.setInventory(inventory);
            setJobResult(job.getUuid(), data);
        }

        public synchronized void fail(ErrorCode code) {
            if (!done.compareAndSet(false, true)) {
                return;
            }

            OvfTrackData data = new OvfTrackData();
            data.setError(code);
            data.setStage(STAGE_DONE);
            data.setVmInstanceUuid(resourceUuid);
            setJobResult(job.getUuid(), data);
        }

        void startTrack() {
            long offTime = timer.getCurrentTimeMillis() +
                    TimeUnit.SECONDS.toMillis(LongJobGlobalConfig.LONG_JOB_DEFAULT_TIMEOUT.value(Long.class));
            evtf.on(OVF_TRACK_RESULT_PATH, new AutoOffEventCallback<OvfTrackData>() {
                @Override
                protected boolean run(Map<String, String> tokens, OvfTrackData data) {
                    if (data.getVmInstanceUuid().equals(job.getTargetResourceUuid())) {
                        track(data);
                    } else if (offTime < timer.getCurrentTimeMillis()) {
                        return true;
                    }
                    return done.get();
                }
            });
        }
    }
    private Handler handler;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        APICreateVmInstanceFromOvfMsg apiMsg = JSONObjectUtil.toObject(job.getJobData(), APICreateVmInstanceFromOvfMsg.class);
        CreateVmInstanceFromOvfMsg msg = CreateVmInstanceFromOvfMsg.fromApiMessage(apiMsg);
        String resourceUuid = msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid();

        this.handler = new Handler();
        handler.job = job;
        handler.resourceUuid = resourceUuid;

        msg.setResourceUuid(resourceUuid);
        job.setTargetResourceUuid(resourceUuid);

        job.setJobData(JSONObjectUtil.toJsonString(msg));
        dbf.updateAndRefresh(job);

        handler.startTrack();

        bus.makeTargetServiceIdByResourceUuid(msg, OvfManager.SERVICE_ID, msg.getResourceUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    handler.fail(reply.getError());
                    completion.fail(reply.getError());
                    return;
                }
                CreateVmInstanceFromOvfReply caseReply = reply.castReply();
                APICreateVmInstanceFromOvfEvent event = new APICreateVmInstanceFromOvfEvent(apiMsg.getId());
                event.setInventory(caseReply.getInventory());
                handler.success(caseReply.getInventory());
                completion.success(event);
            }
        });
    }

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        if (StringUtils.isEmpty(job.getJobResult())) {
            logger.debug("The cancel OVF long job has not been started. " +
                    "The long job will stop before imageUploadRequired stage (in check-cancel-signal-before-adding-images flow)");
            completion.success(true);
            return;
        }

        OvfTrackData data = JSONObjectUtil.toObject(job.getJobResult(), OvfTrackData.class);
        boolean inPrepareStage = list(STAGE_READY, STAGE_IMAGE_UPLOAD_REQUIRED, STAGE_IMAGE_DOWNLOADING)
                .contains(data.getStage());

        if (!inPrepareStage) {
            completion.fail(operr("cancel OVF long job in %s stage is not support", data.getStage()));
            return;
        }

        logger.debug(String.format("Cancel OVF long job[uuid=%s, stage=%s]", job.getUuid(), data.getStage()));

        final List<String> childrenJobUuidList = Q.New(LongJobVO.class)
                .eq(LongJobVO_.parentUuid, job.getUuid())
                .eq(LongJobVO_.state, LongJobState.Running)
                .select(LongJobVO_.uuid)
                .listValues();

        if (isEmpty(childrenJobUuidList)) {
            completion.success(true);
            return;
        }

        AtomicInteger cancelledSuccessCount = new AtomicInteger(0);
        new While<>(childrenJobUuidList).step((longJobUuid, whileCompletion) -> {
            CancelLongJobMsg cancelMsg = new CancelLongJobMsg();
            cancelMsg.setUuid(longJobUuid);
            bus.makeTargetServiceIdByResourceUuid(cancelMsg, LongJobConstants.SERVICE_ID, longJobUuid);
            bus.send(cancelMsg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        cancelledSuccessCount.incrementAndGet();
                    } else {
                        whileCompletion.addError(reply.getError());
                    }
                    whileCompletion.done();
                }
            });
        }, 3).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (cancelledSuccessCount.get() > 0) {
                    logger.debug(String.format("cancelled image success %d/%d",
                            cancelledSuccessCount.get(), childrenJobUuidList.size()));
                    completion.success(true);
                } else {
                    logger.debug(String.format("cancelled image success 0/%d, failed to cancelled long job",
                            childrenJobUuidList.size()));
                    completion.fail(multiErr(errorCodeList));
                }
            }
        });
    }

    @Override
    public void resume(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        completion.fail(operr("not support"));
    }

    @Override
    public void clean(LongJobVO job, NoErrorCompletion completion) {
        completion.done();
    }

    @Override
    public Class<?> getAuditType() {
        return VmInstanceVO.class;
    }

    @Override
    public String getAuditResourceUuid() {
        return handler.resourceUuid;
    }

    @Nullable
    public static String findCurrentLongJobUuid(String apiId) {
        return Q.New(LongJobVO.class)
                .select(LongJobVO_.uuid)
                .eq(LongJobVO_.apiId, apiId)
                .eq(LongJobVO_.jobName, APICreateVmInstanceFromOvfMsg.class.getSimpleName())
                .findValue();
    }

    public static boolean isCurrentOvfLongJobCancelled(String apiId) {
        return Q.New(LongJobVO.class)
                .in(LongJobVO_.state, list(LongJobState.Canceled, LongJobState.Canceling))
                .eq(LongJobVO_.apiId, apiId)
                .eq(LongJobVO_.jobName, APICreateVmInstanceFromOvfMsg.class.getSimpleName())
                .isExists();
    }
}
