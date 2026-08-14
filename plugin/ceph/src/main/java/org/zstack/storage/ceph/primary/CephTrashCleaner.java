package org.zstack.storage.ceph.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.db.Q;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.CleanUpStorageTrashOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.storage.ceph.CephGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CephTrashCleaner implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(CephTrashCleaner.class);
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected ResourceDestinationMaker destMaker;
    @Autowired
    protected CloudBus bus;

    private Future<Void> gcThread;

    private GlobalConfig cleanupIntervalConfig() {
        return CephGlobalConfig.TRASH_CLEANUP_INTERVAL;
    }

    @Override
    public void managementNodeReady() {
        startGC();
    }

    private void startGC() {
        cleanupIntervalConfig().installUpdateExtension((oldConfig, newConfig) -> startGCThread());
        startGCThread();
    }

    synchronized private void startGCThread() {
        if (gcThread != null) {
            gcThread.cancel(true);
        }

        logger.debug(String.format("%s starts with the interval %s secs", this.getClass().getSimpleName(), cleanupIntervalConfig().value(Long.class)));

        gcThread = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return cleanupIntervalConfig().value(Long.class);
            }

            @Override
            public String getName() {
                return "ceph-trash-cleanup-thread";
            }

            @Override
            public void run() {
                cleanup(null, true);
            }
        });
    }

    protected void cleanup(String primaryStorageUuid, boolean needDestinationCheck) {
        List<String> cephPsUuids = new ArrayList<>();
        if (primaryStorageUuid == null) {
            cephPsUuids = Q.New(CephPrimaryStorageVO.class)
                    .select(CephPrimaryStorageVO_.uuid)
                    .listValues();
        } else {
            cephPsUuids.add(primaryStorageUuid);
        }

        new While<>(cephPsUuids).step((psUuid, comp) -> {
            if (needDestinationCheck && !destMaker.isManagedByUs(psUuid)) {
                comp.done();
                return;
            }

            CleanUpStorageTrashOnPrimaryStorageMsg cmsg = new CleanUpStorageTrashOnPrimaryStorageMsg();
            cmsg.setForce(false);
            cmsg.setPrimaryStorageUuid(psUuid);
            bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, psUuid);
            bus.send(cmsg, new CloudBusCallBack(comp) {
                @Override
                public void run(MessageReply reply) {
                    comp.done();
                }
            });
        }, 3).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
            }
        });
    }
}
