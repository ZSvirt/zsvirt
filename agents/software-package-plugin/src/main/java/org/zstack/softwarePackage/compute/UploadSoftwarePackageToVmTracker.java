package org.zstack.softwarePackage.compute;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.progress.ActionProgressService;
import org.zstack.core.progress.TaskProgressReporter;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.GetFileDownloadProgressMsg;
import org.zstack.header.host.GetFileDownloadProgressReply;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.UploadFileToVmMsg;
import org.zstack.header.message.MessageReply;
import org.zstack.softwarePackage.header.UploadSoftwarePackageToVmMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.header.Constants.THREAD_CONTEXT_API;
import static org.zstack.softwarePackage.compute.SoftwarePackageCanonicalEvents.SOFTWARE_PACKAGE_TO_VM_TRACK_RESULT_PATH;
import static org.zstack.softwarePackage.SoftwarePackageGlobalConfig.UPLOAD_FAILURE_TOLERANCE_COUNT;
import static org.zstack.softwarePackage.SoftwarePackageGlobalConfig.UPLOAD_MAX_IDLE_IN_SECONDS;
import static org.zstack.softwarePackage.SoftwarePackagePluginErrors.UPLOAD_SOFTWARE_PACKAGE_INTERRUPTED;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class UploadSoftwarePackageToVmTracker {
    private static final CLogger logger = Utils.getLogger(UploadSoftwarePackageToVmTracker.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private EventFacade evtf;

    private String apiId;
    private TaskProgressReporter reporter;

    public void runTrackTask(UploadSoftwarePackageToVmMsg msg, String hostPath,
                             UploadSoftwarePackageToVmSpec spec, UploadSoftwarePackageToVmBackend backend,
                             BooleanSupplier canceled) {
        String uploadTaskUuid = msg.getUploadTaskUuid();
        String hostUuid = msg.getHostUuid();
        apiId = msg.getCancellationApiId() == null
                ? ThreadContext.get(THREAD_CONTEXT_API) : msg.getCancellationApiId();
        Map<String, String> trackerContext = new HashMap<>(ThreadContext.getContext());
        if (apiId != null) {
            trackerContext.put(THREAD_CONTEXT_API, apiId);
        }
        int maxNumOfFailure = UPLOAD_FAILURE_TOLERANCE_COUNT.value(Integer.class);
        long maxIdleSecond = UPLOAD_MAX_IDLE_IN_SECONDS.value(Long.class);

        logger.debug(String.format("starting tracker for software package upload to VM [task: %s, host: %s]",
                uploadTaskUuid, hostUuid));

        thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            private long numError;
            private final long createdTime = System.currentTimeMillis();

            private boolean overMaxIdleTime(long lastOpTimeInMills) {
                long latestTime = Math.max(lastOpTimeInMills, createdTime);
                return System.currentTimeMillis() - latestTime > TimeUnit.SECONDS.toMillis(maxIdleSecond);
            }

            private void markFailure(ErrorCode reason) {
                logger.error(String.format("upload software package to VM failed: %s",
                        reason.getReadableDetails()));
                fireTerminalEvent(msg, reason);
            }

            @Override
            public boolean run() {
                return runWithThreadContext(trackerContext, this::track);
            }

            private boolean track() {
                if (canceled.getAsBoolean()) {
                    markFailure(operr("software package upload to VM[uuid:%s] was canceled",
                            msg.getVmInstanceUuid()));
                    return true;
                }

                GetFileDownloadProgressReply reply = getDownloadProgress(uploadTaskUuid, hostUuid);
                if (!reply.isSuccess()) {
                    if (++numError <= maxNumOfFailure) {
                        return false;
                    }

                    markFailure(reply.getError());
                    return true;
                }

                if (reply.getDownloadSize() == 0 && overMaxIdleTime(createdTime)) {
                    markFailure(operr("upload software package to VM session expired"));
                    return true;
                }

                boolean uploadSuspendedTooLong = !reply.isCompleted() && overMaxIdleTime(reply.getLastOpTime());
                if (uploadSuspendedTooLong && reply.isSupportSuspend()) {
                    ErrorCode error = err(UPLOAD_SOFTWARE_PACKAGE_INTERRUPTED,
                            "uploading has been inactive more than %d sec", maxIdleSecond);
                    if (reply.getError() != null) {
                        error.withCause(reply.getError());
                    }
                    markFailure(error);
                    return true;
                }

                numError = 0;
                if (!reply.isCompleted()) {
                    reportProgress("uploading software package to VM", reply.getProgress());
                    return false;
                }

                reportProgress("success to upload software package to VM", 100);
                copyAndInstallSoftwarePackageToVm(msg, hostPath, spec, backend, canceled);
                return true;
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return 3;
            }

            @Override
            public String getName() {
                return String.format("track software package upload to VM[taskUuid:%s]",
                        uploadTaskUuid);
            }
        });
    }

    private static boolean runWithThreadContext(Map<String, String> context, BooleanSupplier task) {
        Map<String, String> previous = new HashMap<>(ThreadContext.getContext());
        try {
            ThreadContext.clearMap();
            ThreadContext.putAll(context);
            return task.getAsBoolean();
        } finally {
            ThreadContext.clearMap();
            ThreadContext.putAll(previous);
        }
    }

    private void copyAndInstallSoftwarePackageToVm(UploadSoftwarePackageToVmMsg msg, String hostPath,
                                                    UploadSoftwarePackageToVmSpec spec,
                                                    UploadSoftwarePackageToVmBackend backend,
                                                    BooleanSupplier canceled) {
        String uploadTaskUuid = msg.getUploadTaskUuid();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("complete-software-package-upload-to-vm");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "copy-software-package-to-vm";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (canceled.getAsBoolean()) {
                            trigger.fail(operr("software package upload to VM[uuid:%s] was canceled",
                                    msg.getVmInstanceUuid()));
                            return;
                        }

                        ErrorCode targetError = UploadSoftwarePackageToVmTargetChecker.refreshBeforeCopy(msg, backend);
                        if (targetError != null) {
                            trigger.fail(targetError);
                            return;
                        }

                        UploadFileToVmMsg vmMsg = new UploadFileToVmMsg();
                        vmMsg.setHostUuid(msg.getHostUuid());
                        vmMsg.setTaskUuid(uploadTaskUuid);
                        vmMsg.setSourcePath(hostPath);
                        vmMsg.setTargetIp(msg.getTargetIp());
                        vmMsg.setTargetPath(spec.getTargetPath());
                        vmMsg.setUsername(spec.getUsername());
                        vmMsg.setSshPort(spec.getSshPort());
                        vmMsg.setPassword(spec.getPassword());
                        bus.makeTargetServiceIdByResourceUuid(
                                vmMsg, HostConstant.SERVICE_ID, msg.getHostUuid());
                        bus.send(vmMsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "install-software-package-on-vm";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (canceled.getAsBoolean()) {
                            trigger.fail(operr("software package upload to VM[uuid:%s] was canceled",
                                    msg.getVmInstanceUuid()));
                            return;
                        }

                        try {
                            backend.install(msg.getVmInstanceUuid(), msg.getTargetIp(), uploadTaskUuid, canceled,
                                    new Completion(trigger) {
                                        @Override
                                        public void success() {
                                            trigger.next();
                                        }

                                        @Override
                                        public void fail(ErrorCode errorCode) {
                                            trigger.fail(errorCode);
                                        }
                                    });
                        } catch (RuntimeException e) {
                            trigger.fail(operr("failed to install software package on VM[uuid:%s]: %s",
                                    msg.getVmInstanceUuid(), e.getMessage()));
                        }
                    }
                });

                done(new FlowDoneHandler(null) {
                    @Override
                    public void handle(Map data) {
                        fireTerminalEvent(msg, null);
                    }
                });

                error(new FlowErrorHandler(null) {
                    @Override
                    public void handle(ErrorCode errorCode, Map data) {
                        logger.warn(String.format(
                                "failed to complete software package upload task[uuid:%s] to VM[uuid:%s]: %s",
                                uploadTaskUuid, msg.getVmInstanceUuid(), errorCode.getReadableDetails()));
                        fireTerminalEvent(msg, errorCode);
                    }
                });
            }
        });
        chain.start();
    }

    private void fireEvent(UploadSoftwarePackageToVmMsg msg, ErrorCode errorCode) {
        SoftwarePackageCanonicalEvents.SoftwarePackageToVmTrackData data =
                new SoftwarePackageCanonicalEvents.SoftwarePackageToVmTrackData();
        data.setUploadTaskUuid(msg.getUploadTaskUuid());
        data.setError(errorCode);
        evtf.fire(SOFTWARE_PACKAGE_TO_VM_TRACK_RESULT_PATH, data);
    }

    private void fireTerminalEvent(UploadSoftwarePackageToVmMsg msg, ErrorCode errorCode) {
        if (msg.isDeferCleanupToLongJob() ||
                errorCode != null && errorCode.isError(UPLOAD_SOFTWARE_PACKAGE_INTERRUPTED)) {
            fireEvent(msg, errorCode);
            return;
        }

        new UploadSoftwarePackageToVmCleanup().cleanup(
                msg.getHostUuid(), msg.getUploadTaskUuid(), new NoErrorCompletion() {
                    @Override
                    public void done() {
                        fireEvent(msg, errorCode);
                    }
                });
    }

    private void reportProgress(String taskName, long progress) {
        if (reporter == null) {
            reporter = ActionProgressService.taskProgress()
                    .withTotalStep(100L)
                    .withContent(taskName)
                    .withApiId(apiId)
                    .withCurrentStep(progress)
                    .report();
        } else {
            reporter.withCurrentStep(progress)
                    .withContent(taskName)
                    .withApiId(apiId)
                    .report();
        }
    }

    private GetFileDownloadProgressReply getDownloadProgress(String uploadTaskUuid, String hostUuid) {
        GetFileDownloadProgressMsg msg = new GetFileDownloadProgressMsg();
        msg.setTaskUuid(uploadTaskUuid);
        msg.setHostUuid(hostUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        MessageReply reply = bus.call(msg);
        if (reply.isSuccess()) {
            return reply.castReply();
        }

        GetFileDownloadProgressReply progressReply = new GetFileDownloadProgressReply();
        progressReply.setError(reply.getError());
        return progressReply;
    }
}
