package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.tacker.PingTracker;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class BackupStoragePingTracker extends PingTracker implements ManagementNodeReadyExtensionPoint, ManagementNodeChangeListener {
    private final static CLogger logger = Utils.getLogger(BackupStoragePingTracker.class);

    private final Map<String, BackupStorageStatus> statusMap = Collections.synchronizedMap(new HashMap<String, BackupStorageStatus>());

    private final Map<String, AtomicInteger> backupStorageDisconnectCount = new ConcurrentHashMap<>();

    private final Map<String, BackupStorageReconnectTask> reconnectTaskMap = Collections.synchronizedMap(new HashMap<String, BackupStorageReconnectTask>());

    @Autowired
    private ResourceDestinationMaker destMaker;

    @Autowired
    protected EventFacade evtf;

    @Override
    public String getResourceName() {
        return "backup storage";
    }

    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public NeedReplyMessage getPingMessage(String resUuid) {
        PingBackupStorageMsg msg = new PingBackupStorageMsg();
        msg.setBackupStorageUuid(resUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, resUuid);
        return msg;
    }

    @Override
    public int getPingInterval() {
        return BackupStorageGlobalConfig.PING_INTERVAL.value(Integer.class);
    }

    @Override
    public int getParallelismDegree() {
        return BackupStorageGlobalConfig.PING_PARALLELISM_DEGREE.value(Integer.class);
    }

    @Override
    public void handleReply(String resourceUuid, MessageReply reply) {
        decideWhatToDoNext(resourceUuid, makeReconnectDecision(resourceUuid, reply));
    }

    @Override
    protected void untrackHook(String resUuid) {
        statusMap.remove(resUuid);

        BackupStorageReconnectTask reconnectTask = reconnectTaskMap.get(resUuid);
        if (reconnectTask != null) {
            reconnectTask.cancel();
            reconnectTaskMap.remove(resUuid);
        }
    }

    @Override
    protected void startHook() {
        BackupStorageGlobalConfig.PING_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                pingIntervalChanged();
            }
        });
    }

    private void reScanBackupStorage() {
        untrackAll();
        statusMap.clear();

        new SQLBatch() {
            @Override
            protected void scripts() {
                long count = sql("select count(bs) from BackupStorageVO bs", Long.class).find();
                sql("select bs.uuid from BackupStorageVO bs", String.class).limit(1000).paginate(count, (List<String> bsUuids) -> {
                    List<String> byUs = bsUuids.stream()
                            .filter(bsUuid ->
                            destMaker.isManagedByUs(bsUuid))
                            .collect(Collectors.toList());

                    track(byUs);
                });
            }
        }.execute();
    }

    @Override
    public void managementNodeReady() {
        reScanBackupStorage();
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
        reScanBackupStorage();
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        reScanBackupStorage();
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {

    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {

    }

    enum ReconnectDecision {
        DoNothing,
        SubmitReconnectTask,
        StopReconnectTask
    }

    @Override
    public boolean start() {
        onBackupStorageStatusChange();
        return super.start();
    }

    private void onBackupStorageStatusChange() {
        evtf.onLocal(BackupStorageCanonicalEvents.BACKUP_STORAGE_STATUS_CHANGED, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                BackupStorageCanonicalEvents.BackupStorageStatusChangedData d = (BackupStorageCanonicalEvents.BackupStorageStatusChangedData) data;
                if (BackupStorageStatus.Connected.toString().equals(d.getNewStatus())) {
                    backupStorageDisconnectCount.remove(d.getBackupStorageUuid());
                } else if (BackupStorageStatus.Disconnected.toString().equals(d.getNewStatus()) &&
                        BackupStorageStatus.Connecting.toString().equals(d.getOldStatus())) {
                    backupStorageDisconnectCount.computeIfAbsent(d.getBackupStorageUuid(), key -> new AtomicInteger(0)).addAndGet(1);
                }
            }
        });
    }

    private void decideWhatToDoNext(String resUuid, ReconnectDecision decision) {
        if (decision == ReconnectDecision.SubmitReconnectTask) {
            submitReconnectTask(resUuid);
        } else if (decision == ReconnectDecision.StopReconnectTask) {
            cancel(resUuid);
        } else if (decision == ReconnectDecision.DoNothing) {
            logger.debug(String.format("[Backup storage Tracker]: do not track backup storage[uuid:%s].", resUuid));
        } else {
            throw new CloudRuntimeException("should not be here");
        }
    }

    private ReconnectDecision makeReconnectDecision(String uuid, MessageReply reply) {
        PingBackupStorageReply r = reply.castReply();
        boolean autoReconnect = BackupStorageGlobalConfig.AUTO_RECONNECT_ON_ERROR.value(Boolean.class);

        if (!autoReconnect) {
            logger.debug(String.format("[Backup storage Tracker]: stop pinging backup storage[uuid: %s] because it's disconnected and connection.autoReconnectOnError is false.", uuid));
            return ReconnectDecision.StopReconnectTask;
        }

        AtomicInteger disconnectCount = backupStorageDisconnectCount.get(uuid);
        int threshold = BackupStorageGlobalConfig.AUTO_RECONNECT_ON_ERROR_MAX_ATTEMPT_NUM.value(Integer.class);
        if (threshold > 0 && disconnectCount != null && disconnectCount.get() >= threshold) {
            logger.warn(String.format("[Backup storage Tracker]: stop pinging backup storage[uuid: %s] because it fail to reconnect too many times.", uuid));
            return ReconnectDecision.StopReconnectTask;
        }

        if (r.getError() != null) {
            boolean checkReconnect = (boolean) r.getError().getOpaque().getOrDefault(BackupStorageErrors.Opaque.NEED_RECONNECT_CHECKING.toString(), false);
            if (checkReconnect) {
                return ReconnectDecision.SubmitReconnectTask;
            }
        }

        boolean isDisconnected = Q.New(BackupStorageVO.class)
                .eq(BackupStorageVO_.uuid, uuid)
                .eq(BackupStorageVO_.status, BackupStorageStatus.Disconnected).isExists();

        if (r.isSuccess() && isDisconnected) {
            return ReconnectDecision.SubmitReconnectTask;
        }

        return ReconnectDecision.DoNothing;
    }

    private void submitReconnectTask(String uuid) {
        BackupStorageReconnectTask reconnectTask = reconnectTaskMap.get(uuid);

        if (reconnectTask != null && !reconnectTask.taskIsCanceled()) {
            return;
        }

        reconnectTask = new BackupStorageReconnectTask(uuid, new NoErrorCompletion() {
            @Override
            public void done() {
                logger.debug(String.format("[Backup storage Tracker]: successfully create reconnect backup storage[uuid: %s] task.", uuid));
            }
        });

        reconnectTaskMap.put(uuid, reconnectTask);
        reconnectTask.start();
    }

    public void cancel(String resUuid) {
        BackupStorageReconnectTask reconnectTask = reconnectTaskMap.get(resUuid);
        if (reconnectTask != null && !reconnectTask.taskIsCanceled()) {
            untrackHook(resUuid);
        }
    }
}
