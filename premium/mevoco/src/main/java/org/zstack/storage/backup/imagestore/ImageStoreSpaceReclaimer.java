package org.zstack.storage.backup.imagestore;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.storage.backup.BackupStorageCanonicalEvents;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.BackupStorageErrors;
import org.zstack.header.storage.backup.BackupStorageStatus;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by MaJin on 2019/4/18.
 */
public class ImageStoreSpaceReclaimer implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ImageStoreSpaceReclaimer.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ResourceDestinationMaker destMaker;
    @Autowired
    protected EventFacade evtf;

    protected Future<Void> gcThread;
    private AtomicBoolean skipGc = new AtomicBoolean(true);
    
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public void managementNodeReady() {
        startGc();
    }

    private void startGc() {
        ImageStoreGlobalConfig.RECLAIM_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> {
            startGcThread();
        });

        startGcThread();
    }

    private synchronized void startGcThread() {
        if (gcThread != null) {
            gcThread.cancel(true);
        }

        if (ImageStoreGlobalConfig.RECLAIM_INTERVAL.value(Long.class) <= 0) {
            logger.debug(String.format("%s disabled because interval set to %s secs",
                    this.getClass().getSimpleName(), ImageStoreGlobalConfig.RECLAIM_INTERVAL.value(Long.class)));
            return;
        }

        evtf.on(BackupStorageCanonicalEvents.BACKUP_STORAGE_DISCONNECTED, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                BackupStorageCanonicalEvents.DisconnectedData disconnectedData = (BackupStorageCanonicalEvents.DisconnectedData) data;
                if (disconnectedData.getReason().isError(BackupStorageErrors.OTHER_NODE_MANAGE_ERROR)) {
                    skipGc.compareAndSet(false, true);
                }
            }
        });

        logger.debug(String.format("%s starts with the interval %s secs", this.getClass().getSimpleName(), ImageStoreGlobalConfig.RECLAIM_INTERVAL.value(Long.class)));
        gcThread = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return ImageStoreGlobalConfig.RECLAIM_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "image-store-reclaim-thread";
            }

            @Override
            public void run() {
                if (skipGc.compareAndSet(true, false)) {
                    logger.debug("skip reclaim.");
                    return;
                }

                reclaim();
            }
        });
    }

    private void reclaim() {
        Timestamp lastGCTime = Timestamp.valueOf(simpleDateFormat.format(
                System.currentTimeMillis() - ImageStoreGlobalConfig.RECLAIM_INTERVAL.value(Long.class) * 1000));

        List<String> bsUuids = Q.New(ImageStoreBackupStorageVO.class)
                .select(ImageStoreBackupStorageVO_.uuid)
                .lte(ImageStoreBackupStorageVO_.createDate, lastGCTime)
                .eq(ImageStoreBackupStorageVO_.status, BackupStorageStatus.Connected)
                .listValues();
        bsUuids.removeIf(it -> !destMaker.isManagedByUs(it));
        for (String bsUuid : bsUuids) {
            ReclaimSpaceFromImageStoreMsg msg = new ReclaimSpaceFromImageStoreMsg();
            msg.setUuid(bsUuid);
            bus.makeLocalServiceId(msg, BackupStorageConstant.SERVICE_ID);
            bus.send(msg);
        }
    }
}
