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
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
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
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.kvm.KvmRunShellMsg;
import org.zstack.kvm.KvmRunShellReply;
import org.zstack.softwarePackage.SoftwarePackageConstant;
import org.zstack.softwarePackage.header.APIUploadSoftwarePackageToVmEvent;
import org.zstack.softwarePackage.header.APIUploadSoftwarePackageToVmMsg;
import org.zstack.softwarePackage.header.UploadSoftwarePackageToVmMsg;
import org.zstack.softwarePackage.header.UploadSoftwarePackageToVmReply;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

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
    private static final String OWNER_SESSION_UUID = UUID.randomUUID().toString().replace("-", "");

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private SoftwarePackageManager softwarePackageManager;

    private String auditResourceUuid;

    private UploadSoftwarePackageToVmBackend getUploadSoftwarePackageToVmBackend(String type) {
        if (softwarePackageManager == null) {
            softwarePackageManager = Platform.getComponentLoader().getComponent(SoftwarePackageManager.class);
        }
        return softwarePackageManager.getUploadSoftwarePackageToVmBackend(type);
    }

    static class CompletionGuard {
        private boolean done;

        synchronized boolean complete(Runnable terminalWrite) {
            if (done) {
                return false;
            }

            done = true;
            terminalWrite.run();
            return true;
        }

        synchronized void track(Runnable progressWrite) {
            if (!done) {
                progressWrite.run();
            }
        }
    }

    static boolean completeTerminal(CompletionGuard guard, Runnable terminalWrite,
                                    Runnable markUploadSettled) {
        return guard.complete(() -> {
            try {
                terminalWrite.run();
            } finally {
                markUploadSettled.run();
            }
        });
    }

    static String ownerSessionUuid() {
        return OWNER_SESSION_UUID;
    }

    static boolean isOwnedByCurrentProcess(String ownerSessionUuid) {
        return Objects.equals(OWNER_SESSION_UUID, ownerSessionUuid);
    }

    class UploadSoftwarePackageToVmCompletion
            extends ReturnValueCompletion<APIUploadSoftwarePackageToVmEvent> {
        private final APIUploadSoftwarePackageToVmEvent event;
        private final LongJobVO job;
        private final ReturnValueCompletion<APIEvent> completion;
        private final UploadSoftwarePackageToVmBackend backend;
        private final String uploadTaskUuid;
        private final CompletionGuard guard = new CompletionGuard();

        UploadSoftwarePackageToVmCompletion(APIUploadSoftwarePackageToVmEvent event,
                                            LongJobVO job,
                                            ReturnValueCompletion<APIEvent> completion,
                                            UploadSoftwarePackageToVmBackend backend,
                                            String uploadTaskUuid) {
            super(completion);
            this.event = event;
            this.job = job;
            this.completion = completion;
            this.backend = backend;
            this.uploadTaskUuid = uploadTaskUuid;
        }

        @Override
        public void success(APIUploadSoftwarePackageToVmEvent result) {
            completeTerminal(guard, () -> {
                setJobResult(job.getUuid(), result);
                completion.success(result);
            }, this::markUploadSettled);
        }

        @Override
        public void fail(ErrorCode error) {
            completeTerminal(guard, () -> {
                boolean canceled = jobCanceled(job.getUuid());
                ErrorCode terminalError = canceled ? cancelErr(job.getUuid(), error) : error;
                if (!canceled && terminalError.isError(LongJobErrors.INTERRUPTED)) {
                    event.setError(terminalError);
                    setJobResult(job.getUuid(), event);
                    completion.fail(terminalError);
                    return;
                }

                setJobError(job.getUuid(), terminalError);
                completion.fail(terminalError);
            }, this::markUploadSettled);
        }

        private void markUploadSettled() {
            if (backend != null) {
                backend.markUploadSettled(uploadTaskUuid);
            }
        }

        public void track(String uploadUrl) {
            guard.track(() -> {
                event.setUploadUrl(uploadUrl);
                setJobResult(job.getUuid(), event);
            });
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
        msg.setOwnerSessionUuid(ownerSessionUuid());
        msg.setCancellationApiId(job.getApiId());
        auditResourceUuid = msg.getVmInstanceUuid();
        job.setTargetResourceUuid(msg.getVmInstanceUuid());

        UploadSoftwarePackageToVmBackend backend;
        ErrorCode targetError;
        try {
            backend = getUploadSoftwarePackageToVmBackend(msg.getType());
            targetError = UploadSoftwarePackageToVmTargetChecker.refreshForUpload(msg, backend);
        } catch (OperationFailureException e) {
            setJobError(job.getUuid(), e.getErrorCode());
            completion.fail(e.getErrorCode());
            return;
        }
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
                new UploadSoftwarePackageToVmCompletion(
                        event, job, completion, backend, msg.getUploadTaskUuid());
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
                if (msg.needTrack()) {
                    if (jobCanceled(job.getUuid())) {
                        evtf.off(trackCallback);
                        uploadCompletion.fail(cancelErr(job.getUuid()));
                        return;
                    }
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
        resumeInternal(job, () -> completion);
    }

    void resumeInternal(LongJobVO job, Supplier<ReturnValueCompletion<APIEvent>> completionSupplier) {
        ReturnValueCompletion<APIEvent> completion = completionSupplier.get();
        UploadSoftwarePackageToVmMsg msg = JSONObjectUtil.toObject(
                job.getJobData(), UploadSoftwarePackageToVmMsg.class);
        if (msg.getCancellationApiId() == null) {
            msg.setCancellationApiId(job.getApiId());
        }
        auditResourceUuid = msg.getVmInstanceUuid();
        APIUploadSoftwarePackageToVmEvent event = buildEvent(job, msg.getUploadTaskUuid());
        if (!isOwnedByCurrentProcess(msg.getOwnerSessionUuid())) {
            UploadSoftwarePackageToVmCompletion uploadCompletion =
                    new UploadSoftwarePackageToVmCompletion(
                            event, job, completion, null, msg.getUploadTaskUuid());
            ErrorCode restartError = buildRestartError(job);
            cleanupTemporaryFiles(msg, new Completion(uploadCompletion) {
                @Override
                public void success() {
                    failUpload(uploadCompletion, restartError);
                }

                @Override
                public void fail(ErrorCode cleanupError) {
                    logger.warn(String.format(
                            "failed to clean interrupted UploadSoftwarePackageToVm long job[uuid:%s]: %s",
                            job.getUuid(), cleanupError.getReadableDetails()));
                    failUpload(uploadCompletion, restartError);
                }
            });
            return;
        }

        UploadSoftwarePackageToVmBackend backend;
        try {
            backend = getUploadSoftwarePackageToVmBackend(msg.getType());
        } catch (OperationFailureException e) {
            setJobError(job.getUuid(), e.getErrorCode());
            completion.fail(e.getErrorCode());
            return;
        }
        UploadSoftwarePackageToVmCompletion uploadCompletion =
                new UploadSoftwarePackageToVmCompletion(
                        event, job, completion, backend, msg.getUploadTaskUuid());
        AutoOffEventCallback trackCallback = null;

        job.setJobData(JSONObjectUtil.toJsonString(msg));
        dbf.updateAndRefresh(job);

        try {
            ErrorCode targetError = refreshBeforeCopyForResume(msg, backend);
            if (targetError != null) {
                uploadCompletion.fail(targetError);
                return;
            }

            UploadSoftwarePackageToVmSpec spec = backend.getUploadSpec(
                    msg.getUploadTaskUuid(), msg.getUploadTimestamp());
            String hostPath = SoftwarePackageUploadPath.buildHostPath(
                    msg.getUrl(), msg.getUploadTaskUuid());

            trackCallback = startResumeTracking(
                    job, event, msg, hostPath, spec, backend, uploadCompletion);
        } catch (RuntimeException e) {
            if (trackCallback != null) {
                evtf.off(trackCallback);
            }
            uploadCompletion.fail(operr(
                    "failed to resume UploadSoftwarePackageToVm long job[uuid:%s]: %s",
                    job.getUuid(), e.getMessage()));
        }
    }

    protected ErrorCode refreshBeforeCopyForResume(UploadSoftwarePackageToVmMsg msg,
                                                    UploadSoftwarePackageToVmBackend backend) {
        return UploadSoftwarePackageToVmTargetChecker.refreshBeforeCopy(msg, backend);
    }

    protected AutoOffEventCallback startResumeTracking(LongJobVO job,
                                                        APIUploadSoftwarePackageToVmEvent event,
                                                        UploadSoftwarePackageToVmMsg msg,
                                                        String hostPath,
                                                        UploadSoftwarePackageToVmSpec spec,
                                                        UploadSoftwarePackageToVmBackend backend,
                                                        UploadSoftwarePackageToVmCompletion completion) {
        setJobResult(job.getUuid(), event);
        AutoOffEventCallback trackCallback = completion.startTrack();
        new UploadSoftwarePackageToVmTracker().runTrackTask(
                msg, hostPath, spec, backend, () -> jobCanceled(job.getUuid()));
        return trackCallback;
    }

    protected void failUpload(UploadSoftwarePackageToVmCompletion completion, ErrorCode error) {
        completion.fail(error);
    }

    protected ErrorCode buildRestartError(LongJobVO job) {
        return err(SysErrors.NOT_SUPPORTED,
                "cannot resume UploadSoftwarePackageToVm long job[uuid:%s] after management node restart",
                job.getUuid());
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

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        UploadSoftwarePackageToVmMsg uploadMsg = JSONObjectUtil.toObject(
                job.getJobData(), UploadSoftwarePackageToVmMsg.class);
        String hostUuid = uploadMsg.getHostUuid();
        UploadSoftwarePackageToVmBackend backend;
        try {
            backend = getUploadSoftwarePackageToVmBackend(uploadMsg.getType());
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
            return;
        }
        if (!backend.isUploadCancelable(uploadMsg.getUploadTaskUuid())) {
            completion.success(false);
            return;
        }

        CancelHostTaskMsg msg = new CancelHostTaskMsg();
        msg.setHostUuid(hostUuid);
        msg.setCancellationApiId(job.getApiId());
        msg.setAllowTaskNotFound(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    if (reply.getError().isError(SysErrors.RESOURCE_NOT_FOUND)) {
                        completeCancellationCleanup(uploadMsg, completion);
                        return;
                    }
                    completion.fail(reply.getError());
                    return;
                }

                CancelHostTaskReply cancelReply = reply.castReply();
                boolean uploadCancelable = backend.isUploadCancelable(uploadMsg.getUploadTaskUuid());
                if (!UploadSoftwarePackageToVmCancellationPolicy.isCompleted(
                        cancelReply.getCancelResult(), uploadCancelable)) {
                    completion.success(false);
                    return;
                }
                completeCancellationCleanup(uploadMsg, completion);
            }
        });
    }

    @Override
    public void clean(LongJobVO job, NoErrorCompletion completion) {
        UploadSoftwarePackageToVmMsg msg = JSONObjectUtil.toObject(
                job.getJobData(), UploadSoftwarePackageToVmMsg.class);
        UploadSoftwarePackageToVmBackend backend;
        try {
            backend = getUploadSoftwarePackageToVmBackend(msg.getType());
        } catch (OperationFailureException e) {
            logger.warn(String.format("failed to prepare cleanup for UploadSoftwarePackageToVm long job[uuid:%s]: %s",
                    job.getUuid(), e.getErrorCode().getReadableDetails()));
            completion.done();
            return;
        }

        backend.runAfterUploadFinished(msg.getUploadTaskUuid(), () -> cleanupTemporaryFiles(msg,
                new Completion(completion) {
                    @Override
                    public void success() {
                        completion.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format(
                                "failed to clean UploadSoftwarePackageToVm long job[uuid:%s]: %s",
                                job.getUuid(), errorCode.getReadableDetails()));
                        completion.done();
                    }
                }));
    }

    private void completeCancellationCleanup(UploadSoftwarePackageToVmMsg msg,
                                             ReturnValueCompletion<Boolean> completion) {
        cleanupTemporaryFiles(msg, new Completion(completion) {
            @Override
            public void success() {
                completion.success(true);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    protected void cleanupTemporaryFiles(UploadSoftwarePackageToVmMsg msg, Completion completion) {
        ErrorCode vmCleanupError = cleanupVmTemporaryFiles(msg);
        if (msg.getHostUuid() == null || msg.getHostUuid().trim().isEmpty()) {
            completion.fail(vmCleanupError == null
                    ? operr("cannot clean software package upload task[uuid:%s]: host UUID is missing",
                    msg.getUploadTaskUuid()) : vmCleanupError);
            return;
        }

        KvmRunShellMsg cleanupMsg = new KvmRunShellMsg();
        cleanupMsg.setHostUuid(msg.getHostUuid());
        try {
            cleanupMsg.setScript(SoftwarePackageUploadPath.buildHostCleanupCommand(
                    msg.getUploadTaskUuid()));
        } catch (IllegalArgumentException e) {
            completion.fail(operr("cannot clean software package upload task[uuid:%s]: %s",
                    msg.getUploadTaskUuid(), e.getMessage()));
            return;
        }
        bus.makeTargetServiceIdByResourceUuid(
                cleanupMsg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(cleanupMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    if (reply.getError().isError(SysErrors.RESOURCE_NOT_FOUND) && vmCleanupError == null) {
                        completion.success();
                    } else {
                        completion.fail(reply.getError());
                    }
                    return;
                }

                KvmRunShellReply cleanupReply = reply.castReply();
                if (cleanupReply.getReturnCode() != 0) {
                    completion.fail(operr(
                            "failed to clean software package upload task[uuid:%s] on host[uuid:%s], return code: %s, error: %s",
                            msg.getUploadTaskUuid(), msg.getHostUuid(), cleanupReply.getReturnCode(),
                            cleanupReply.getStderr()));
                    return;
                }
                if (vmCleanupError != null) {
                    completion.fail(vmCleanupError);
                    return;
                }
                completion.success();
            }
        });
    }

    private ErrorCode cleanupVmTemporaryFiles(UploadSoftwarePackageToVmMsg msg) {
        if (msg.getTargetIp() == null || msg.getTargetIp().trim().isEmpty()
                || msg.getUploadTimestamp() <= 0) {
            return operr("cannot clean software package upload task[uuid:%s] on VM: target is incomplete",
                    msg.getUploadTaskUuid());
        }
        try {
            UploadSoftwarePackageToVmBackend backend =
                    getUploadSoftwarePackageToVmBackend(msg.getType());
            return backend.cleanupUpload(msg.getTargetIp(), msg.getUploadTimestamp());
        } catch (OperationFailureException e) {
            return e.getErrorCode();
        } catch (RuntimeException e) {
            return operr("failed to clean software package upload task[uuid:%s] on VM: %s",
                    msg.getUploadTaskUuid(), e.getMessage());
        }
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
