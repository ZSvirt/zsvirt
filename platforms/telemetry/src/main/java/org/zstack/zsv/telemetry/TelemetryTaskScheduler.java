package org.zstack.zsv.telemetry;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zsv.telemetry.header.TelemetryGlobalConfig;
import org.zstack.zsv.telemetry.header.TelemetryRunCollectMsg;
import org.zstack.zsv.telemetry.header.TelemetryRunUploadMsg;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TelemetryTaskScheduler implements Component, ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(TelemetryTaskScheduler.class);

    @Autowired
    private ThreadFacade threadFacade;
    @Autowired
    private CloudBus bus;

    private Future<Void> collectTask;
    private Future<Void> uploadTask;

    public synchronized void startTasks() {
        stopTasks();

        collectTask = threadFacade.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            @Override
            public boolean run() {
                try {
                    dispatchCollectToElectedNode();
                } catch (Throwable t) {
                    logger.warn(String.format("collect task error: %s", t.getMessage()), t);
                }
                return false; // keep periodic task running
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return TelemetryGlobalConfig.COLLECT_INTERVAL_SECONDS.value(Long.class);
            }

            @Override
            public String getName() {
                return "telemetry-collect";
            }
        });

        uploadTask = threadFacade.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            @Override
            public boolean run() {
                try {
                    dispatchUploadToElectedNode();
                } catch (Throwable t) {
                    logger.warn(String.format("upload task error: %s", t.getMessage()), t);
                }
                return false; // keep periodic task running
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return TelemetryGlobalConfig.UPLOAD_INTERVAL_SECONDS.value(Long.class);
            }

            @Override
            public String getName() {
                return "telemetry-upload";
            }
        }, TelemetryConstant.UPLOAD_INITIAL_DELAY_SECONDS);

        logger.debug("started collect/upload periodic tasks");
    }

    public synchronized void stopTasks() {
        if (collectTask != null) {
            collectTask.cancel(true);
            collectTask = null;
        }

        if (uploadTask != null) {
            uploadTask.cancel(true);
            uploadTask = null;
        }

        logger.debug("stopped collect/upload periodic tasks");
    }

    public synchronized boolean isTasksRunning() {
        return collectTask != null && uploadTask != null;
    }

    public synchronized void syncTasksWithConsent() {
        if (TelemetryUtils.isConsentGranted()) {
            startTasks();
        } else {
            stopTasks();
        }
    }

    private void dispatchCollectToElectedNode() {
        if (!TelemetryUtils.isConsentGranted() || !TelemetryUtils.isCoordinator()) {
            return;
        }

        String executorUuid = TelemetryUtils.electRandomManagementNode();
        if (executorUuid == null) {
            return;
        }

        TelemetryRunCollectMsg msg = new TelemetryRunCollectMsg();
        bus.makeServiceIdByManagementNodeId(msg, TelemetryConstant.SERVICE_ID, executorUuid);
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("collect on node[%s] failed: %s",
                            executorUuid, reply.getError()));
                }
            }
        });
        logger.debug(String.format("dispatched collect to management node[%s]", executorUuid));
    }

    private void dispatchUploadToElectedNode() {
        if (!TelemetryUtils.isConsentGranted() || !TelemetryUtils.isCoordinator()) {
            return;
        }

        String executorUuid = TelemetryUtils.electRandomManagementNode();
        if (executorUuid == null) {
            return;
        }

        TelemetryRunUploadMsg msg = new TelemetryRunUploadMsg();
        bus.makeServiceIdByManagementNodeId(msg, TelemetryConstant.SERVICE_ID, executorUuid);
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("upload on node[%s] failed: %s",
                            executorUuid, reply.getError()));
                }
            }
        });
        logger.debug(String.format("dispatched upload to management node[%s]", executorUuid));
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        stopTasks();
        return true;
    }

    @Override
    public void managementNodeReady() {
        syncTasksWithConsent();
    }
}
