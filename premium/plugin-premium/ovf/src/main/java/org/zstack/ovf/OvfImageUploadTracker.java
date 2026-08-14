package org.zstack.ovf;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CanonicalEventEmitter;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.JobResultError;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.longjob.LongJobState;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.longjob.LongJobVO_;
import org.zstack.ovf.datatype.CreateVmFromOvfBundle;
import org.zstack.ovf.datatype.CreateVmFromOvfImageParam;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import static org.zstack.ovf.datatype.CreateVmFromOvfCanonicalEvents.*;
import static org.zstack.ovf.datatype.CreateVmFromOvfParamType.*;
import static org.zstack.ovf.datatype.OvfErrors.*;
import static org.zstack.core.Platform.*;

import static java.util.Arrays.asList;

/**
 * Created by Wenhao.Zhang on 22/03/09
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class OvfImageUploadTracker {
    private static final CLogger logger = Utils.getLogger(OvfImageUploadTracker.class);

    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private ErrorFacade errorFacade;

    private static final List<String> allowedInstallPathPrefix = asList(
            "http://", "https://"
    );

    public static class OvfTrackCanonicalEvent extends CanonicalEventEmitter {
        final OvfTrackData data;

        public OvfTrackCanonicalEvent(OvfTrackData data) {
            this.data = data;
        }

        public OvfTrackCanonicalEvent(String stage, String vmInstanceUuid, List<UploadInfo> infoList) {
            this(new OvfTrackData());
            data.setStage(stage);
            data.setVmInstanceUuid(vmInstanceUuid);
            data.setUploadInfos(infoList);
        }

        public OvfTrackCanonicalEvent(String stage, String vmInstanceUuid, ErrorCode errorCode) {
            this(new OvfTrackData());
            data.setStage(stage);
            data.setVmInstanceUuid(vmInstanceUuid);
            data.setError(errorCode);
        }

        public void fire() {
            fire(OVF_TRACK_RESULT_PATH, data);
        }
    }

    private static class LongJobContext {
        CreateVmFromOvfImageParam param;
        String longJobUuid;
        String installPathForUpload = null;
        boolean upload = false;
        boolean done = false;
        boolean fail = false;
        // maybe upload ovf image job is suspended or canceled
        boolean suspend = false;
        ErrorCode error; // job(APIAddImageMsg).jobResult is not a ErrorCode
    }

    private List<LongJobContext> contexts;
    private boolean allUploadJobsAcquired = false;
    private boolean allJobsDone = false;
    private Completion completion;
    private CreateVmFromOvfBundle bundle;
    private BooleanSupplier cancelChecker = () -> false;

    public void registerCancelChecker(BooleanSupplier cancelChecker) {
        this.cancelChecker = cancelChecker;
    }

    public OvfImageUploadTracker(CreateVmFromOvfBundle bundle) {
        this.bundle = bundle;
        List<CreateVmFromOvfImageParam> params = bundle.getParams();
        contexts = new ArrayList<>(params.size());
        for (CreateVmFromOvfImageParam param : params) {
            if (param.getLongJobUuid() == null) {
                continue;
            }

            LongJobContext context = new LongJobContext();
            context.param = param;
            context.longJobUuid = param.getLongJobUuid();
            context.upload = Objects.equals(param.getType(), Upload);
            contexts.add(context);
        }
    }

    public void startTrack() {
        List<String> longJobUuids = contexts.stream().map(context -> context.longJobUuid).collect(Collectors.toList());
        logger.debug(String.format("start track long jobs[uuid:%s]", longJobUuids));
        thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            @Override
            public boolean run() {
                return checkIfTrackerCancelled() || doTrack();
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return 2;
            }

            @Override
            public String getName() {
                return OvfImageUploadTracker.class.getSimpleName();
            }
        });
    }

    public void waitForCompleted(Completion completion) {
        this.completion = completion;
    }

    /**
     * @return true if cancel track task
     */
    private boolean checkIfTrackerCancelled() {
        boolean cancelled = this.cancelChecker.getAsBoolean();
        if (!cancelled) {
            return false;
        }

        completion.fail(err(JOB_CANCELLED, "cancel create OVF VM process during image downloading"));
        return true;
    }

    /**
     * @return true if cancel track task
     */
    private boolean doTrack() {
        final Map<String, LongJobState> jobUuidStateMap = fetchLongJobStatesFromDB();
        for (LongJobContext context : contexts) {
            trackLongJobContext(context, jobUuidStateMap.get(context.longJobUuid));
        }

        List<LongJobContext> failJobs = CollectionUtils.filter(contexts, context -> context.fail || context.suspend);
        if (!failJobs.isEmpty()) {
            ErrorCode error;
            if (failJobs.size() == 1) {
                error = failJobs.get(0).error;
            } else {
                error = errorFacade.stringToOperationError("some add image long jobs execute fail",
                        CollectionUtils.transform(failJobs, context -> context.error));
            }

            completion.fail(error);
            emitFailCanonicalEvent(error);
            return true;
        }

        if (!allJobsDone) {
            boolean allMatch = contexts.stream().allMatch(context -> context.done);
            if (allMatch) {
                allJobsDone = true;
                completion.success();
                return true;
            }
        }

        if (!allUploadJobsAcquired) {
            boolean hasUploadJobs = contexts.stream().anyMatch(context -> context.upload);
            if (hasUploadJobs) {
                boolean anyInstallPathNotReady = contexts.stream()
                        .anyMatch(context -> context.upload && StringUtils.isEmpty(context.installPathForUpload));
                if (!anyInstallPathNotReady) {
                    allUploadJobsAcquired = true;
                    emitImageUploadRequiredCanonicalEvent();
                }
            } else {
                allUploadJobsAcquired = true;
                emitImageDownloadingCanonicalEvent();
            }
        }

        return false;
    }

    private void emitImageUploadRequiredCanonicalEvent() {
        List<UploadInfo> needUploadContexts = contexts.stream()
                .filter(context -> context.upload)
                .map(OvfImageUploadTracker::convertContextToUploadInfo)
                .collect(Collectors.toList());
        new OvfTrackCanonicalEvent(STAGE_IMAGE_UPLOAD_REQUIRED, bundle.getVmInstanceUuid(), needUploadContexts).fire();
    }

    private void emitImageDownloadingCanonicalEvent() {
        new OvfTrackCanonicalEvent(STAGE_IMAGE_DOWNLOADING, bundle.getVmInstanceUuid(), Collections.emptyList()).fire();
    }

    private void emitFailCanonicalEvent(ErrorCode code) {
        new OvfTrackCanonicalEvent(STAGE_IMAGE_DOWNLOADING, // fail when downloading image
                bundle.getVmInstanceUuid(), code).fire();
    }

    private static UploadInfo convertContextToUploadInfo(LongJobContext context) {
        UploadInfo info = new UploadInfo();
        info.setUuid(context.param.getUuid());
        info.setFileName(context.param.getFileName());
        info.setInstallPath(context.installPathForUpload);
        return info;
    }

    /**
     * @return LongJobUuid - LongJobState map
     */
    private Map<String, LongJobState> fetchLongJobStatesFromDB() {
        final List<String> jobUuidList = CollectionUtils.transform(contexts, context -> context.longJobUuid);
        if (jobUuidList.isEmpty()) {
            return Collections.emptyMap();
        }

        final List<Tuple> tuples = Q.New(LongJobVO.class)
                .select(LongJobVO_.uuid, LongJobVO_.state)
                .in(LongJobVO_.uuid, jobUuidList)
                .listTuple();
        return CollectionUtils.toMap(tuples,
                tuple -> tuple.get(0, String.class),
                tuple -> tuple.get(1, LongJobState.class));
    }

    private void trackLongJobContext(LongJobContext context, LongJobState state) {
        if (state == null) {
            logger.warn(String.format(
                    "failed to track long job[uuid:%s] because LongJobVO is null", context.longJobUuid));
            context.fail = true;
            return;
        }

        switch (state) {
        case Failed: {
            context.fail = true;
            context.done = true;
            String cause = fetchUploadErrorCause(context);
            context.error = cause == null ?
                    err(OVF_UPLOAD_FAIL, "OVF long job[uuid:%s] execute fail", context.longJobUuid) :
                    err(OVF_UPLOAD_FAIL, "OVF long job[uuid:%s] execute fail", context.longJobUuid).withCause(operr(cause));
        } return;
        case Canceled: case Canceling: case Suspended:
            context.suspend = true;
            context.done = true;
            String cause = fetchUploadErrorCause(context);
            context.error = cause == null ?
                    err(OVF_UPLOAD_SUSPENDED, "OVF long job[uuid:%s] suspended", context.longJobUuid) :
                    err(OVF_UPLOAD_SUSPENDED, "OVF long job[uuid:%s] suspended", context.longJobUuid).withCause(operr(cause));
            return;
        case Succeeded:
            context.done = true;
            return;
        }

        if (context.upload && StringUtils.isEmpty(context.installPathForUpload)) {
            ImageVO vo = Q.New(ImageVO.class)
                    .eq(ImageVO_.uuid, context.param.getUuid())
                    .find();
            if (vo == null || vo.getBackupStorageRefs() == null || vo.getBackupStorageRefs().isEmpty()) {
                return;
            }

            ImageBackupStorageRefVO ref = vo.getBackupStorageRefs().iterator().next();
            boolean matched = allowedInstallPathPrefix
                    .stream()
                    .anyMatch(ref.getInstallPath()::startsWith);
            if (!matched) {
                return;
            }

            context.installPathForUpload = ref.getInstallPath();
            logger.debug(String.format("install path of image [uuid:%s] is %s",
                    context.param.getUuid(), context.installPathForUpload));
        }
    }

    private String fetchUploadErrorCause(LongJobContext context) {
        final String jobResult = Q.New(LongJobVO.class)
                .eq(LongJobVO_.uuid, context.longJobUuid)
                .select(LongJobVO_.jobResult)
                .findValue();
        JobResultError error = JSONObjectUtil.toObject(jobResult, JobResultError.class);
        return error.getCause();
    }
}
