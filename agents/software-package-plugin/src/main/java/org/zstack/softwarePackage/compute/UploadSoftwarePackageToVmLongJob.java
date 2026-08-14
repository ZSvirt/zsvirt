package org.zstack.softwarePackage.compute;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.AutoOffEventCallback;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;
import org.zstack.core.db.Q;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.CancelHostTaskMsg;
import org.zstack.header.host.CancelHostTaskReply;
import org.zstack.header.host.HostConstant;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobErrors;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobState;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.longjob.LongJobVO_;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.CancelTaskResult;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.softwarePackage.SoftwarePackageConstant;
import org.zstack.softwarePackage.header.APIUploadSoftwarePackageToVmEvent;
import org.zstack.softwarePackage.header.APIUploadSoftwarePackageToVmMsg;
import org.zstack.softwarePackage.header.UploadSoftwarePackageToVmMsg;
import org.zstack.softwarePackage.header.UploadSoftwarePackageToVmReply;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.longjob.LongJobUtils.cancelErr;
import static org.zstack.longjob.LongJobUtils.interruptedErr;
import static org.zstack.longjob.LongJobUtils.jobCanceled;
import static org.zstack.longjob.LongJobUtils.setJobError;
import static org.zstack.longjob.LongJobUtils.setJobResult;
import static org.zstack.softwarePackage.compute.SoftwarePackageCanonicalEvents.SOFTWARE_PACKAGE_TO_VM_TRACK_RESULT_PATH;
import static org.zstack.softwarePackage.SoftwarePackagePluginErrors.UPLOAD_SOFTWARE_PACKAGE_INTERRUPTED;

@LongJobFor(APIUploadSoftwarePackageToVmMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class UploadSoftwarePackageToVmLongJob implements LongJob {
    private static final CLogger logger = Utils.getLogger(UploadSoftwarePackageToVmLongJob.class);
    private static final String OWNER_LOCK = "upload-software-package-to-vm-longjob";
    private static final String OWNER_SESSION_UUID = Platform.getUuid();

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private SoftwarePackageManager softwarePackageManager;

    private String auditResourceUuid;

    class UploadSoftwarePackageToVmCompletion
            extends ReturnValueCompletion<APIUploadSoftwarePackageToVmEvent> {
        private final APIUploadSoftwarePackageToVmEvent event;
        private final LongJobVO job;
        private final UploadSoftwarePackageToVmMsg msg;
        private final ReturnValueCompletion<APIEvent> completion;
        private final AtomicBoolean done = new AtomicBoolean(false);

        UploadSoftwarePackageToVmCompletion(APIUploadSoftwarePackageToVmEvent event,
                                            LongJobVO job,
                                            UploadSoftwarePackageToVmMsg msg,
                                            ReturnValueCompletion<APIEvent> completion) {
            super(completion);
            this.event = event;
            this.job = job;
            this.msg = msg;
            this.completion = completion;
        }

        @Override
        public void success(APIUploadSoftwarePackageToVmEvent result) {
            if (!done.compareAndSet(false, true)) {
                return;
            }

            if (jobCanceled(job.getUuid())) {
                ErrorCode error = cancelErr(job.getUuid());
                setJobError(job.getUuid(), error);
                cleanupStagedFile(msg, new NoErrorCompletion(completion) {
                    @Override
                    public void done() {
                        completion.fail(error);
                    }
                });
                return;
            }

            setJobResult(job.getUuid(), result);
            cleanupStagedFile(msg, new NoErrorCompletion(completion) {
                @Override
                public void done() {
                    completion.success(result);
                }
            });
        }

        @Override
        public void fail(ErrorCode error) {
            if (!done.compareAndSet(false, true)) {
                return;
            }

            boolean canceled = jobCanceled(job.getUuid());
            ErrorCode terminalError = canceled ? cancelErr(job.getUuid(), error) : error;
            if (!canceled && terminalError.isError(LongJobErrors.INTERRUPTED)) {
                event.setError(terminalError);
                setJobResult(job.getUuid(), event);
                completion.fail(terminalError);
                return;
            }

            setJobError(job.getUuid(), terminalError);
            cleanupStagedFile(msg, new NoErrorCompletion(completion) {
                @Override
                public void done() {
                    completion.fail(terminalError);
                }
            });
        }

        public synchronized void track(String uploadUrl) {
            if (done.get()) {
                return;
            }
            event.setUploadUrl(uploadUrl);
            setJobResult(job.getUuid(), event);
        }

        AutoOffEventCallback startTrack() {
            AutoOffEventCallback callback = new AutoOffEventCallback() {
                @Override
                protected boolean run(Map tokens, Object data) {
                    SoftwarePackageCanonicalEvents.SoftwarePackageToVmTrackData result =
                            (SoftwarePackageCanonicalEvents.SoftwarePackageToVmTrackData) data;
                    if (!job.getUuid().equals(result.getUploadTaskUuid())) {
                        return false;
                    }

                    if (result.getError() == null) {
                        success(event);
                    } else if (jobCanceled(job.getUuid())) {
                        fail(cancelErr(job.getUuid(), result.getError()));
                    } else if (result.getError().isError(UPLOAD_SOFTWARE_PACKAGE_INTERRUPTED)) {
                        fail(interruptedErr(job.getUuid(), result.getError()));
                    } else {
                        fail(result.getError());
                    }
                    return true;
                }
            };
            evtf.on(SOFTWARE_PACKAGE_TO_VM_TRACK_RESULT_PATH, callback);
            return callback;
        }
    }

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        APIUploadSoftwarePackageToVmMsg apiMsg = JSONObjectUtil.toObject(
                job.getJobData(), APIUploadSoftwarePackageToVmMsg.class);
        UploadSoftwarePackageToVmMsg msg = UploadSoftwarePackageToVmMsg.fromApiMessage(apiMsg, job.getUuid());
        msg.setOwnerSessionUuid(OWNER_SESSION_UUID);
        msg.setCancellationApiId(job.getApiId());
        msg.setDeferCleanupToLongJob(true);
        auditResourceUuid = msg.getVmInstanceUuid();
        job.setTargetResourceUuid(msg.getVmInstanceUuid());

        ErrorCode targetError = UploadSoftwarePackageToVmTargetChecker.refreshForUpload(msg);
        if (targetError != null) {
            setJobError(job.getUuid(), targetError);
            completion.fail(targetError);
            return;
        }

        job.setJobData(JSONObjectUtil.toJsonString(msg));
        dbf.updateAndRefresh(job);

        APIUploadSoftwarePackageToVmEvent event = buildEvent(job, msg.getUploadTaskUuid());
        ErrorCode ownerError = claimOwner(job, event);
        if (ownerError != null) {
            setJobError(job.getUuid(), ownerError);
            completion.fail(ownerError);
            return;
        }

        UploadSoftwarePackageToVmCompletion uploadCompletion =
                new UploadSoftwarePackageToVmCompletion(event, job, msg, completion);
        AutoOffEventCallback trackCallback = msg.needTrack()
                ? uploadCompletion.startTrack() : null;
        bus.makeLocalServiceId(msg, SoftwarePackageConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(uploadCompletion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    handleSuccess(reply);
                    return;
                }

                if (trackCallback != null) {
                    evtf.off(trackCallback);
                }
                uploadCompletion.fail(reply.getError());
            }

            private void handleSuccess(MessageReply reply) {
                if (jobCanceled(job.getUuid())) {
                    if (trackCallback != null) {
                        evtf.off(trackCallback);
                    }
                    uploadCompletion.fail(cancelErr(job.getUuid()));
                    return;
                }

                if (msg.needTrack()) {
                    UploadSoftwarePackageToVmReply uploadReply = reply.castReply();
                    uploadCompletion.track(uploadReply.getUploadUrl());
                    return;
                }

                uploadCompletion.success(event);
            }
        });
    }

    private ErrorCode claimOwner(LongJobVO job, APIUploadSoftwarePackageToVmEvent initialResult) {
        GLock lock = new GLock(OWNER_LOCK, 30);
        try {
            lock.lock(60);
            List<LongJobVO> candidates = Q.New(LongJobVO.class)
                    .eq(LongJobVO_.jobName, APIUploadSoftwarePackageToVmMsg.class.getSimpleName())
                    .notIn(LongJobVO_.state, LongJobState.finalStates)
                    .list();
            LongJobVO owner = candidates.stream()
                    .filter(candidate -> candidate.getJobResult() != null)
                    .min(Comparator.comparing(LongJobVO::getCreateDate)
                            .thenComparing(LongJobVO::getUuid))
                    .orElseGet(() -> candidates.stream()
                            .min(Comparator.comparing(LongJobVO::getCreateDate)
                                    .thenComparing(LongJobVO::getUuid))
                            .orElse(job));
            if (!owner.getUuid().equals(job.getUuid())) {
                return operr("another UploadSoftwarePackageToVm long job[uuid:%s] is running",
                        owner.getUuid());
            }
            setJobResult(job.getUuid(), initialResult);
            return null;
        } catch (CloudRuntimeException e) {
            return operr("failed to acquire UploadSoftwarePackageToVm owner lock: %s", e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void resume(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        UploadSoftwarePackageToVmMsg msg = JSONObjectUtil.toObject(
                job.getJobData(), UploadSoftwarePackageToVmMsg.class);
        if (msg.getCancellationApiId() == null) {
            msg.setCancellationApiId(job.getApiId());
        }
        msg.setDeferCleanupToLongJob(true);
        auditResourceUuid = msg.getVmInstanceUuid();
        APIUploadSoftwarePackageToVmEvent event = buildEvent(job, msg.getUploadTaskUuid());
        UploadSoftwarePackageToVmCompletion uploadCompletion =
                new UploadSoftwarePackageToVmCompletion(event, job, msg, completion);
        AutoOffEventCallback trackCallback = null;

        if (!Objects.equals(OWNER_SESSION_UUID, msg.getOwnerSessionUuid())) {
            uploadCompletion.fail(err(SysErrors.NOT_SUPPORTED,
                    "cannot resume UploadSoftwarePackageToVm long job[uuid:%s] after management node restart",
                    job.getUuid()));
            return;
        }

        job.setJobData(JSONObjectUtil.toJsonString(msg));
        dbf.updateAndRefresh(job);

        try {
            UploadSoftwarePackageToVmBackend backend =
                    softwarePackageManager.getUploadSoftwarePackageToVmBackend(msg.getType());
            ErrorCode targetError = UploadSoftwarePackageToVmTargetChecker.refreshBeforeCopy(msg, backend);
            if (targetError != null) {
                uploadCompletion.fail(targetError);
                return;
            }

            UploadSoftwarePackageToVmSpec spec = backend.getUploadSpec(msg.getUploadTaskUuid());
            String hostPath = buildHostPath(msg);

            setJobResult(job.getUuid(), event);
            trackCallback = uploadCompletion.startTrack();
            new UploadSoftwarePackageToVmTracker().runTrackTask(
                    msg, hostPath, spec, backend, () -> jobCanceled(job.getUuid()));
        } catch (RuntimeException e) {
            if (trackCallback != null) {
                evtf.off(trackCallback);
            }
            uploadCompletion.fail(operr(
                    "failed to resume UploadSoftwarePackageToVm long job[uuid:%s]: %s",
                    job.getUuid(), e.getMessage()));
        }
    }

    static APIUploadSoftwarePackageToVmEvent buildEvent(LongJobVO job, String uploadTaskUuid) {
        APIUploadSoftwarePackageToVmEvent event = new APIUploadSoftwarePackageToVmEvent(job.getApiId());
        if (job.getJobResult() != null) {
            APIUploadSoftwarePackageToVmEvent persisted = JSONObjectUtil.toObject(
                    job.getJobResult(), APIUploadSoftwarePackageToVmEvent.class);
            event.setUploadUrl(persisted.getUploadUrl());
        }
        event.setUploadTaskUuid(uploadTaskUuid);
        return event;
    }

    private String buildHostPath(UploadSoftwarePackageToVmMsg msg) {
        URI uri = URI.create(msg.getUrl());
        String path = uri.getPath();
        String fileName = path != null && !path.isEmpty() && !path.endsWith("/")
                ? path.substring(path.lastIndexOf('/') + 1) : uri.getAuthority();
        return String.format("/var/lib/zstack/software-package/vm-upload/%s/%s",
                msg.getUploadTaskUuid(), fileName);
    }

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        UploadSoftwarePackageToVmMsg uploadMsg = JSONObjectUtil.toObject(
                job.getJobData(), UploadSoftwarePackageToVmMsg.class);
        String hostUuid = uploadMsg.getHostUuid();

        CancelHostTaskMsg msg = new CancelHostTaskMsg();
        msg.setHostUuid(hostUuid);
        msg.setCancellationApiId(job.getApiId());
        msg.setAllowTaskNotFound(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                CancelHostTaskReply cancelReply = reply.castReply();
                if (cancelReply.getCancelResult() == CancelTaskResult.TASK_NOT_FOUND) {
                    completion.success(false);
                    return;
                }
                completion.success(false);
            }
        });
    }

    @Override
    public void clean(LongJobVO job, NoErrorCompletion completion) {
        UploadSoftwarePackageToVmMsg msg = JSONObjectUtil.toObject(
                job.getJobData(), UploadSoftwarePackageToVmMsg.class);
        cancel(job, new ReturnValueCompletion<Boolean>(completion) {
            @Override
            public void success(Boolean ignored) {
                cleanupStagedFile(msg, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to clean UploadSoftwarePackageToVm long job[uuid:%s]: %s",
                        job.getUuid(), errorCode.getReadableDetails()));
                cleanupStagedFile(msg, completion);
            }
        });
    }

    private void cleanupStagedFile(UploadSoftwarePackageToVmMsg msg, NoErrorCompletion completion) {
        new UploadSoftwarePackageToVmCleanup().cleanup(
                msg.getHostUuid(), msg.getUploadTaskUuid(), completion);
    }

    @Override
    public Class<?> getAuditType() {
        return VmInstanceVO.class;
    }

    @Override
    public String getAuditResourceUuid() {
        return auditResourceUuid;
    }
}
