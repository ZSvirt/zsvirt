package org.zstack.softwarePackage.compute;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.progress.ActionProgressService;
import org.zstack.core.progress.TaskProgressReporter;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.timeout.TimeHelper;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.GetFileDownloadProgressMsg;
import org.zstack.header.host.GetFileDownloadProgressReply;
import org.zstack.header.host.HostConstant;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.longjob.LongJobVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.softwarePackage.entity.SoftwarePackageStatus;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;
import org.zstack.softwarePackage.header.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.err;
import static org.zstack.header.Constants.THREAD_CONTEXT_API;
import static org.zstack.softwarePackage.SoftwarePackageGlobalConfig.*;
import static org.zstack.softwarePackage.SoftwarePackagePluginErrors.*;
import static org.zstack.softwarePackage.compute.SoftwarePackageCanonicalEvents.SOFTWARE_PACKAGE_TRACK_RESULT_PATH;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class UploadSoftwarePackageTracker {
    private static final CLogger logger = Utils.getLogger(UploadSoftwarePackageTracker.class);

    @Autowired
    protected DatabaseFacade databases;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade threads;
    @Autowired
    private EventFacade events;
    @Autowired
    private ErrorFacade errorFacade;
    @Autowired
    private PluginRegistry plugins;
    @Autowired
    private TimeHelper timeHelper;

    private final String apiId = ThreadContext.get(THREAD_CONTEXT_API);
    TaskProgressReporter reporter;

    private ErrorCode addCauseIfPresent(ErrorCode error, ErrorCode cause) {
        if (cause != null) {
            error.withCause(cause);
        }
        return error;
    }

    public void runTrackTask(String softwarePackageUuid, String hostUuid) {
        final int maxNumOfFailure = UPLOAD_FAILURE_TOLERANCE_COUNT.value(Integer.class);
        final long maxIdleSecond = UPLOAD_MAX_IDLE_IN_SECONDS.value(Long.class);

        logger.debug(String.format("starting tracker for software package upload [uuid: %s, host: %s]", softwarePackageUuid, hostUuid));

        threads.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            private long numError = 0;
            private long createdTime = timeHelper.getCurrentTimeMillis();

            private boolean overMaxIdleTime(long lastOpTimeInMills) {
                long latestTime = Long.max(lastOpTimeInMills, createdTime);
                return timeHelper.getCurrentTimeMillis() - latestTime > TimeUnit.SECONDS.toMillis(maxIdleSecond);
            }

            private void markCompletion(final GetFileDownloadProgressReply dr) {
                SoftwarePackageVO vo = databases.findByUuid(softwarePackageUuid, SoftwarePackageVO.class);

                String unzipInstallPath = String.format("%s_%s_%d", vo.getInstallPath(), dr.getMd5sum(), timeHelper.getCurrentTimeMillis());
                vo.setUnzipInstallPath(unzipInstallPath);
                vo = databases.updateAndRefresh(vo);

                List<UploadSoftwarePackageExtensionPoint> exts = plugins.getExtensionList(UploadSoftwarePackageExtensionPoint.class);
                String type = null;
                for (UploadSoftwarePackageExtensionPoint ext : exts) {
                    type = ext.resolveAndPrepareActualType(vo.getType(), vo.getInstallPath(), unzipInstallPath);
                    if (type != null) {
                        break;
                    }
                }
                if (type == null) {
                    vo.setStatus(SoftwarePackageStatus.UploadFailed.toString());
                    databases.updateAndRefresh(vo);
                    fireEvent(null, err(GENERAL_ERROR, "failed to get software package type"));
                    return;
                }

                vo.setType(type);
                vo.setMd5sum(dr.getMd5sum());
                vo.setSize(dr.getSize());
                vo.setStatus(SoftwarePackageStatus.Uploaded.toString());
                vo = databases.updateAndRefresh(vo);

                fireEvent(SoftwarePackageInventory.valueOf(vo), null);
            }

            private void markFailure(ErrorCode reason) {
                logger.error(String.format("upload software package to host failed: %s", reason.getReadableDetails()));

                boolean continuable = apiId != null && Q.New(LongJobVO.class).eq(LongJobVO_.apiId, apiId).isExists();

                fireEvent(null, reason);
                if (reason.isError(UPLOAD_SOFTWARE_PACKAGE_INTERRUPTED) && continuable) {
                    return;
                }

                SoftwarePackageVO vo = databases.findByUuid(softwarePackageUuid, SoftwarePackageVO.class);
                vo.setStatus(SoftwarePackageStatus.UploadFailed.toString());
                databases.updateAndRefresh(vo);
            }

            private void fireEvent(SoftwarePackageInventory inventory, ErrorCode error) {
                SoftwarePackageCanonicalEvents.SoftwarePackageTrackData data = new SoftwarePackageCanonicalEvents.SoftwarePackageTrackData();
                data.uuid = softwarePackageUuid;
                data.inventory = inventory;
                data.setError(error);
                events.fire(SOFTWARE_PACKAGE_TRACK_RESULT_PATH, data);
            }

            @Override
            public boolean run() {
                final GetFileDownloadProgressReply reply = getDownloadProgress(softwarePackageUuid, hostUuid);
                if (!reply.isSuccess()) {
                    if (++numError <= maxNumOfFailure) {
                        return false;
                    }

                    markFailure(reply.getError());
                    return true;
                }

                if (reply.getDownloadSize() == 0 && overMaxIdleTime(createdTime)) {
                    markFailure(err(INVALID_UPLOAD_SESSION, "upload software package session expired"));
                    return true;
                }

                boolean downloadingImageSuspendedTooLong = !reply.isCompleted() && overMaxIdleTime(reply.getLastOpTime());
                if (downloadingImageSuspendedTooLong && reply.isSupportSuspend()) {
                    markFailure(addCauseIfPresent(err(UPLOAD_SOFTWARE_PACKAGE_INTERRUPTED,
                            "uploading has been inactive more than %d sec", maxIdleSecond), reply.getError()));
                    return true;
                }

                // reset the error counter
                numError = 0;

                if (!reply.isCompleted()) {
                    doReportProgress(apiId, "uploading software package", reply.getProgress());
                    return false;
                }

                // upload completed
                doReportProgress(apiId, "success to upload software package", 100);
                markCompletion(reply);
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
                return String.format("tracking upload software package [uuid: %s]", softwarePackageUuid);
            }
        });
    }

    private void doReportProgress(String apiId, String taskName, long progress) {
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

    private GetFileDownloadProgressReply getDownloadProgress(String softwarePackageUuid, String hostUuid) {
        final GetFileDownloadProgressMsg dmsg = new GetFileDownloadProgressMsg();
        dmsg.setTaskUuid(softwarePackageUuid);
        dmsg.setHostUuid(hostUuid);
        bus.makeTargetServiceIdByResourceUuid(dmsg, HostConstant.SERVICE_ID, hostUuid);
        final MessageReply reply = bus.call(dmsg);
        if (reply.isSuccess()) {
            return reply.castReply();
        } else {
            GetFileDownloadProgressReply r = new GetFileDownloadProgressReply();
            r.setError(reply.getError());
            return r;
        }
    }
}
