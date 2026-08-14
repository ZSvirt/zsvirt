package org.zstack.compute.vm.metadata.dirty;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.metadata.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class VmMetadataDirtyMarker implements VmMetadataDirtyService, Component, ManagementNodeChangeListener, ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmMetadataDirtyMarker.class);

    /**
     * Base delay for exponential backoff on flush failure: delay = BASE * 2^retryCount
     */
    private static final int RETRY_BASE_DELAY_SEC = 10;

    /**
     * Delay before running upgrade full-refresh, gives cluster time to stabilize after upgrade
     */
    private static final long UPGRADE_REFRESH_DELAY_SEC = 600;
    /**
     * Page size for bulk INSERT dirty marks during upgrade full-refresh, limits per-SQL impact
     */
    private static final int UPGRADE_REFRESH_BATCH_SIZE = 10;

    /**
     * Delay before reclaiming orphan dirty rows after a management node leaves
     */
    private static final long NODE_LEFT_DELAY_SEC = 10;
    /**
     * Max times upgrade refresh can be rescheduled due to recent nodeLeft events.
     * After this limit, proceed regardless to avoid indefinite postponement.
     */
    private static final int MAX_UPGRADE_REFRESH_RESCHEDULES = 3;
    /**
     * Minutes a claimed dirty row sits unfinished before another node can steal it
     */
    private static final long TRIGGER_FLUSH_STALE_MIN = 10;

    /**
     * Page size for first-boot initialization INSERT IGNORE of dirty rows for all UserVms
     */
    private static final int INIT_BATCH_SIZE = 10;
    /**
     * Sleep between initialization batches to limit DB pressure on first boot
     */
    private static final long INIT_BATCH_DELAY_SEC = 10;

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    private final AtomicBoolean pollerStopped = new AtomicBoolean(false);
    private final AtomicBoolean zombieCleanupStopped = new AtomicBoolean(false);

    private final AtomicInteger globalFlushInFlight = new AtomicInteger(0);
    private final AtomicBoolean upgradeRefreshScheduled = new AtomicBoolean(false);
    private final AtomicInteger upgradeRefreshRescheduleCount = new AtomicInteger(0);

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        stopPoller();
        stopZombieCleanupTask();
        return true;
    }

    @Override
    public void managementNodeReady() {
        startPoller();
        startZombieCleanupTask();

        VmGlobalConfig.VM_METADATA_ENABLED.installUpdateExtension((oldValue, newValue) -> {
            boolean wasEnabled = Boolean.parseBoolean(oldValue.value());
            boolean nowEnabled = Boolean.parseBoolean(newValue.value());
            if (!wasEnabled && nowEnabled) {
                logger.info("[MetadataDirty] vm.metadata.enabled toggled from false to true, starting batch initialization");
                submitBatchInitialization();
                scheduleUpgradeRefreshIfNeeded();
            } else if (wasEnabled && !nowEnabled) {
                logger.info("[MetadataDirty] vm.metadata.enabled toggled from true to false, cleaning up PathFingerprints");
                cleanupPathFingerprints();
            }
        });

        scheduleUpgradeRefreshIfNeeded();
    }

    private volatile long lastNodeLeftTimestamp = 0;

    @Override
    public boolean markDirty(String vmInstanceUuid) {
        return markDirty(vmInstanceUuid, false);
    }

    @Override
    public boolean markDirty(String vmInstanceUuid, boolean storageStructureChange) {
        if (!VmGlobalConfig.VM_METADATA_ENABLED.value(Boolean.class)) {
            logger.debug(String.format("[MetadataDirty] markDirty SKIPPED: vm=%s, vm.metadata.enabled=false",
                    vmInstanceUuid));
            return false;
        }

        logger.debug(String.format("[MetadataDirty] markDirty ENTER: vm=%s, storageStructureChange=%s", vmInstanceUuid, storageStructureChange));

        int inserted = nativeExecute(
                "INSERT IGNORE INTO VmMetadataDirtyVO " +
                        "(vmInstanceUuid, dirtyVersion, storageStructureChange, createDate, lastOpDate) " +
                        "VALUES (:vmUuid, 1, :ssc, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "vmUuid", vmInstanceUuid,
                "ssc", storageStructureChange ? 1 : 0);

        if (inserted == 0) {
            // Preserve retryCount/nextRetryTime: resetting them on every markDirty
            // defeats exponential backoff when the underlying failure is persistent
            // (e.g. PS down). High-frequency APIs (batch tag ops) would otherwise
            // cause a flush storm against an unreachable PS. The poller will naturally
            // retry with the latest dirtyVersion once nextRetryTime expires.
            String updateSql = storageStructureChange
                    ? "UPDATE VmMetadataDirtyVO SET dirtyVersion = dirtyVersion + 1, storageStructureChange = 1 WHERE vmInstanceUuid = :vmUuid"
                    : "UPDATE VmMetadataDirtyVO SET dirtyVersion = dirtyVersion + 1 WHERE vmInstanceUuid = :vmUuid";
            int updated = SQL.New(updateSql).param("vmUuid", vmInstanceUuid).execute();

            if (updated == 0) {
                nativeExecute("INSERT IGNORE INTO VmMetadataDirtyVO (vmInstanceUuid, dirtyVersion, storageStructureChange, createDate, lastOpDate) " +
                        "VALUES (:vmUuid, 1, :ssc, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", "vmUuid", vmInstanceUuid, "ssc", storageStructureChange ? 1 : 0);
                logger.debug(String.format("[MetadataDirty] markDirty: vm=%s, INSERT retry (race condition recovery)", vmInstanceUuid));
            } else {
                logger.debug(String.format("[MetadataDirty] markDirty: vm=%s, existing row bumped dirtyVersion", vmInstanceUuid));
            }
        } else {
            logger.debug(String.format("[MetadataDirty] markDirty: vm=%s, new VmMetadataDirtyVO inserted", vmInstanceUuid));
        }

        triggerFlushForVm(vmInstanceUuid);
        return true;
    }

    private void triggerFlushForVm(String vmUuid) {
        String myId = Platform.getManagementServerId();
        long staleMinutes = TRIGGER_FLUSH_STALE_MIN;
        Timestamp staleCutoff = Timestamp.from(Instant.now().minus(Duration.ofMinutes(staleMinutes)));
        String staleId = findStaleClaimOwner(vmUuid, Duration.ofMinutes(staleMinutes));

        String sql;
        if (staleId != null) {
            sql = "UPDATE VmMetadataDirtyVO SET managementNodeUuid = :myId, lastClaimTime = CURRENT_TIMESTAMP " +
                    "WHERE vmInstanceUuid = :vmUuid AND (managementNodeUuid IS NULL OR (managementNodeUuid = :staleId AND lastClaimTime < :staleCutoff)) " +
                    "AND (nextRetryTime IS NULL OR nextRetryTime <= CURRENT_TIMESTAMP)";
        } else {
            sql = "UPDATE VmMetadataDirtyVO SET managementNodeUuid = :myId, lastClaimTime = CURRENT_TIMESTAMP " +
                    "WHERE vmInstanceUuid = :vmUuid AND managementNodeUuid IS NULL " +
                    "AND (nextRetryTime IS NULL OR nextRetryTime <= CURRENT_TIMESTAMP)";
        }

        SQL query = SQL.New(sql)
                .param("myId", myId)
                .param("vmUuid", vmUuid);
        if (staleId != null) {
            query.param("staleId", staleId).param("staleCutoff", staleCutoff);
        }
        int claimed = query.execute();

        if (claimed == 0) {
            logger.debug(String.format("[MetadataDirty] triggerFlushForVm SKIP: vm=%s, claim failed " +
                    "(row may be claimed by another node, or nextRetryTime not yet reached)", vmUuid));
            return;
        }

        logger.debug(String.format("[MetadataDirty] triggerFlushForVm CLAIMED: vm=%s, mnUuid=%s", vmUuid, myId));

        VmMetadataDirtyVO dirty = dbf.findByUuid(vmUuid, VmMetadataDirtyVO.class);
        if (dirty == null) {
            logger.debug(String.format("[MetadataDirty] triggerFlushForVm: vm=%s, dirty row disappeared after claim " +
                    "(VM deleted by FK cascade?)", vmUuid));
            return;
        }

        submitFlushTask(dirty);
    }

    private void submitFlushTask(VmMetadataDirtyVO dirty) {
        final String vmUuid = dirty.getVmInstanceUuid();

        int maxConcurrent = VmGlobalConfig.VM_METADATA_FLUSH_CONCURRENCY.value(Integer.class);
        int current;
        do {
            current = globalFlushInFlight.get();
            if (current >= maxConcurrent) {
                logger.debug(String.format("[MetadataDirty] submitFlushTask THROTTLED: vm=%s, inFlight=%d >= maxConcurrent=%d, releasing claim",
                        vmUuid, current, maxConcurrent));
                releaseClaim(vmUuid);
                return;
            }
        } while (!globalFlushInFlight.compareAndSet(current, current + 1));

        logger.debug(String.format("[MetadataDirty] submitFlushTask: vm=%s, dirtyVersion=%d, storageStructureChange=%s, retryCount=%d, inFlight=%d",
                vmUuid, dirty.getDirtyVersion(), dirty.isStorageStructureChange(), dirty.getRetryCount(), globalFlushInFlight.get()));

        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return String.format("update-vm-%s-metadata", vmUuid);
            }

            @Override
            public int getSyncLevel() {
                return 1;
            }

            @Override
            protected int getMaxPendingTasks() {
                return 1;
            }

            @Override
            protected String getDeduplicateString() {
                return getSyncSignature();
            }

            @Override
            protected void exceedMaxPendingCallback() {
                globalFlushInFlight.decrementAndGet();
                // Do NOT releaseClaim here — the in-flight or pending task on the same
                // chain still holds the claim. Releasing it would cause their doFlush()
                // to see "Lost claim" and discard the flush result.
                // The claim will be recycled by zombie cleanup or the next poll cycle.
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final AtomicBoolean decremented = new AtomicBoolean(false);
                Runnable safeChainNext = () -> {
                    if (decremented.compareAndSet(false, true)) {
                        globalFlushInFlight.decrementAndGet();
                    }
                    chain.next();
                };
                try {
                    doFlush(dirty, safeChainNext);
                } catch (Throwable t) {
                    logger.warn(String.format("[MetadataDirty] doFlush crashed for vm=%s", vmUuid), t);
                    if (decremented.compareAndSet(false, true)) {
                        globalFlushInFlight.decrementAndGet();
                    }
                    chain.next();
                }
            }

            @Override
            public String getName() {
                return String.format("update-vm-%s-metadata-task", vmUuid);
            }
        });
    }

    private void releaseClaim(String vmUuid) {
        logger.debug(String.format("[MetadataDirty] releaseClaim: vm=%s", vmUuid));
        SQL.New(VmMetadataDirtyVO.class).eq(VmMetadataDirtyVO_.vmInstanceUuid, vmUuid).set(VmMetadataDirtyVO_.managementNodeUuid, null).update();
    }

    private void doFlush(VmMetadataDirtyVO dirty, Runnable chainNext) {
        String vmUuid = dirty.getVmInstanceUuid();

        logger.debug(String.format("[MetadataDirty] doFlush ENTER: vm=%s, dirtyVersion=%d, storageStructureChange=%s",
                vmUuid, dirty.getDirtyVersion(), dirty.isStorageStructureChange()));

        VmMetadataDirtyVO latestDirty = dbf.findByUuid(vmUuid, VmMetadataDirtyVO.class);
        if (latestDirty == null) {
            logger.debug(String.format("[MetadataDirty] doFlush ABORT: vm=%s, dirty row gone (VM deleted or already flushed)", vmUuid));
            chainNext.run();
            return;
        }

        long snapshotVersion = latestDirty.getDirtyVersion();
        if (!Platform.getManagementServerId().equals(latestDirty.getManagementNodeUuid())) {
            logger.warn(String.format("[MetadataDirty] doFlush ABORT: Lost claim on vm[uuid:%s], expected mnUuid=%s but got %s",
                    vmUuid, Platform.getManagementServerId(), latestDirty.getManagementNodeUuid()));
            chainNext.run();
            return;
        }

        logger.debug(String.format("[MetadataDirty] doFlush: sending UpdateVmInstanceMetadataMsg for vm=%s, " +
                "snapshotVersion=%d, storageStructureChange=%s", vmUuid, snapshotVersion, latestDirty.isStorageStructureChange()));

        UpdateVmInstanceMetadataMsg msg = new UpdateVmInstanceMetadataMsg();
        msg.setVmInstanceUuid(vmUuid);
        msg.setStorageStructureChange(latestDirty.isStorageStructureChange());
        bus.makeLocalServiceId(msg, VmInstanceConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                try {
                    if (reply.isSuccess()) {
                        String metadata = null;
                        if (reply instanceof UpdateVmInstanceMetadataReply) {
                            metadata = ((UpdateVmInstanceMetadataReply) reply).getMetadata();
                        }
                        logger.debug(String.format("[MetadataDirty] doFlush: vm=%s UpdateVmInstanceMetadataMsg SUCCESS, " +
                                "metadataPresent=%s", vmUuid, metadata != null));
                        onFlushSuccess(vmUuid, snapshotVersion, Platform.getManagementServerId(), metadata);
                    } else {
                        logger.warn(String.format("[MetadataDirty] doFlush: vm=%s UpdateVmInstanceMetadataMsg FAILED: %s",
                                vmUuid, reply.getError()));
                        onFlushFailure(vmUuid, Platform.getManagementServerId(), reply.getError());
                    }
                } finally {
                    chainNext.run();
                }
            }
        });
    }

    private void onFlushSuccess(String vmUuid, long snapshotVersion, String claimOwner, String metadata) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                int deleted = SQL.New(VmMetadataDirtyVO.class).eq(VmMetadataDirtyVO_.vmInstanceUuid, vmUuid)
                        .eq(VmMetadataDirtyVO_.dirtyVersion, snapshotVersion).hardDelete();

                if (deleted == 0) {
                    // New changes arrived during flush. Release the claim only if
                    // we still own it — another MN may have already stolen it via
                    // cleanupZombieClaims or triggerFlushForVm.
                    SQL.New(VmMetadataDirtyVO.class)
                            .eq(VmMetadataDirtyVO_.vmInstanceUuid, vmUuid)
                            .eq(VmMetadataDirtyVO_.managementNodeUuid, claimOwner)
                            .set(VmMetadataDirtyVO_.managementNodeUuid, null)
                            .set(VmMetadataDirtyVO_.retryCount, 0)
                            .set(VmMetadataDirtyVO_.nextRetryTime, null)
                            .update();

                    logger.debug(String.format("[MetadataDirty] onFlushSuccess: vm=%s has new changes during flush " +
                            "(snapshotVersion=%d), released for re-processing", vmUuid, snapshotVersion));
                } else {
                    logger.debug(String.format("[MetadataDirty] onFlushSuccess: vm=%s flush completed, " +
                            "dirty row removed (snapshotVersion=%d)", vmUuid, snapshotVersion));
                }

                // Save fingerprint inside the same transaction so that
                // dirty-row deletion and fingerprint upsert are atomic.
                VmMetadataFlushStateVO fp = dbf.findByUuid(vmUuid, VmMetadataFlushStateVO.class);
                Timestamp now = new Timestamp(System.currentTimeMillis());
                if (fp == null) {
                    fp = new VmMetadataFlushStateVO();
                    fp.setVmInstanceUuid(vmUuid);
                    fp.setMetadataSnapshot(metadata);
                    fp.setLastFlushFinishTime(now);
                    fp.setPendingStaleRecovery(false);
                    fp.setStaleRecoveryCount(0);
                    persist(fp);
                    logger.debug(String.format("[MetadataDirty] onFlushSuccess: vm=%s, new fingerprint created", vmUuid));
                } else {
                    fp.setMetadataSnapshot(metadata);
                    fp.setLastFlushFinishTime(now);
                    fp.setPendingStaleRecovery(false);
                    fp.setStaleRecoveryCount(0);
                    merge(fp);
                    logger.debug(String.format("[MetadataDirty] onFlushSuccess: vm=%s, fingerprint updated", vmUuid));
                }
            }
        }.execute();
    }

    private void onFlushFailure(String vmUuid, String claimOwner, ErrorCode error) {
        int maxRetry = VmGlobalConfig.VM_METADATA_FLUSH_MAX_RETRY.value(Integer.class);

        // Atomic DB increment: avoids read-modify-write race on retryCount.
        // MySQL evaluates SET left-to-right, so retryCount in the nextRetryTime expression
        // is already the post-increment value. Backoff: BASE_DELAY * 2^retryCount seconds.
        int updated = nativeExecute(
                "UPDATE VmMetadataDirtyVO" +
                        " SET retryCount = retryCount + 1," +
                        "     managementNodeUuid = NULL," +
                        "     nextRetryTime = TIMESTAMPADD(SECOND," +
                        "         :baseDelay * (1 << LEAST(retryCount, 10))," +
                        "         CURRENT_TIMESTAMP)" +
                        " WHERE vmInstanceUuid = :vmUuid" +
                        "   AND managementNodeUuid = :claimOwner" +
                        "   AND retryCount < :maxRetry",
                "baseDelay", (long) RETRY_BASE_DELAY_SEC,
                "vmUuid", vmUuid,
                "claimOwner", claimOwner,
                "maxRetry", maxRetry);

        if (updated > 0) {
            // Re-read to get actual retryCount for accurate logging / backoff delay.
            VmMetadataDirtyVO dirty = dbf.findByUuid(vmUuid, VmMetadataDirtyVO.class);
            if (dirty != null) {
                logger.warn(String.format("[MetadataDirty] metadata update for vm[uuid:%s] failed " +
                                "(retry %d/%d), next retry at %s. Error: %s",
                        vmUuid, dirty.getRetryCount(), maxRetry, dirty.getNextRetryTime(), error));
            }
            return;
        }

        // UPDATE matched 0 rows. Either: (a) dirty row gone, (b) claim stolen, or (c) retryCount >= maxRetry.
        VmMetadataDirtyVO dirty = dbf.findByUuid(vmUuid, VmMetadataDirtyVO.class);
        if (dirty == null) {
            logger.debug(String.format("[MetadataDirty] onFlushFailure: vm=%s, dirty row gone (VM deleted?), skip", vmUuid));
            return;
        }

        if (!claimOwner.equals(dirty.getManagementNodeUuid())) {
            logger.warn(String.format("[MetadataDirty] onFlushFailure: vm=%s, claim stolen " +
                            "(expected=%s, actual=%s), skip to avoid overwriting new owner's state",
                    vmUuid, claimOwner, dirty.getManagementNodeUuid()));
            return;
        }

        // retryCount >= maxRetry: escalate to stale recovery
        logger.error(String.format("[MetadataDirty] metadata update for vm[uuid:%s] failed " +
                "after %d retries, marking as stale. VmMetadataMaintenanceManager will retry " +
                "independently. Error: %s", vmUuid, dirty.getRetryCount(), error));

        // Atomic: mark fingerprint as stale + delete dirty row in a single transaction
        // to prevent a window where the dirty row is gone but stale flag is not set.
        new SQLBatch() {
            @Override
            protected void scripts() {
                VmMetadataFlushStateVO fp = findByUuid(vmUuid, VmMetadataFlushStateVO.class);
                if (fp == null) {
                    fp = new VmMetadataFlushStateVO();
                    fp.setVmInstanceUuid(vmUuid);
                    fp.setMetadataSnapshot(null);
                    fp.setLastFlushFinishTime(new Timestamp(System.currentTimeMillis()));
                    fp.setPendingStaleRecovery(true);
                    fp.setStaleRecoveryCount(0);
                    persist(fp);
                    logger.debug(String.format("[MetadataDirty] onFlushFailure: vm=%s, new fingerprint created with pendingStaleRecovery=true", vmUuid));
                } else {
                    fp.setPendingStaleRecovery(true);
                    merge(fp);
                    logger.debug(String.format("[MetadataDirty] onFlushFailure: vm=%s, existing fingerprint updated with pendingStaleRecovery=true", vmUuid));
                }

                sql(VmMetadataDirtyVO.class)
                        .eq(VmMetadataDirtyVO_.vmInstanceUuid, vmUuid)
                        .eq(VmMetadataDirtyVO_.managementNodeUuid, claimOwner)
                        .delete();
                logger.debug(String.format("[MetadataDirty] onFlushFailure: vm=%s, dirty row deleted after max-retry exhaustion", vmUuid));
            }
        }.execute();
    }

    private synchronized void startPoller() {
        pollerStopped.set(false);
        thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return VmGlobalConfig.VM_METADATA_FLUSH_POLL_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "vm-metadata-dirty-poller";
            }

            @Override
            public boolean run() {
                // CancelablePeriodicTask: return true = cancel, false = continue
                if (pollerStopped.get()) {
                    return true;
                }
                claimAndFlush();
                return pollerStopped.get();
            }
        });
        logger.info("[MetadataDirty] poller started");
    }

    private synchronized void stopPoller() {
        pollerStopped.set(true);
        logger.info("[MetadataDirty] poller stopped");
    }

    private void claimAndFlush() {
        if (!VmGlobalConfig.VM_METADATA_ENABLED.value(Boolean.class)) {
            logger.trace("[MetadataDirty] claimAndFlush SKIP: vm.metadata.enabled=false");
            return;
        }

        List<VmMetadataDirtyVO> claimed = claimDirtyRows();
        if (!claimed.isEmpty()) {
            logger.info(String.format("[MetadataDirty] claimAndFlush (poller): claimed %d dirty rows: %s",
                    claimed.size(), claimed.stream()
                            .map(d -> d.getVmInstanceUuid() + "(v" + d.getDirtyVersion() + ")")
                            .collect(Collectors.joining(", "))));
        }
        for (VmMetadataDirtyVO dirty : claimed) {
            submitFlushTask(dirty);
        }
    }

    private List<VmMetadataDirtyVO> claimDirtyRows() {
        String myId = Platform.getManagementServerId();

        // Use DB timestamp as the exact claimTime written into UPDATE, so the
        // subsequent SELECT can precisely identify rows claimed in THIS round.
        // Using a shared claimTime (instead of CURRENT_TIMESTAMP in UPDATE) avoids
        // mixing in rows concurrently claimed by triggerFlushForVm() on the same MN.
        Timestamp claimTime = new SQLBatchWithReturn<Timestamp>() {
            @Override
            protected Timestamp scripts() {
                return (Timestamp) dbf.getEntityManager()
                        .createNativeQuery("SELECT CURRENT_TIMESTAMP")
                        .getSingleResult();
            }
        }.execute();

        // Atomic claim: single UPDATE-LIMIT avoids the SELECT→UPDATE race where
        // multiple MNs SELECT the same candidates then compete on UPDATE.
        int claimed = nativeExecute(
                "UPDATE VmMetadataDirtyVO " +
                        "SET managementNodeUuid = :myId, lastClaimTime = :claimTime " +
                        "WHERE managementNodeUuid IS NULL " +
                        "AND (nextRetryTime IS NULL OR nextRetryTime <= CURRENT_TIMESTAMP) " +
                        "ORDER BY lastOpDate ASC, vmInstanceUuid ASC " +
                        "LIMIT :batchSize",
                "myId", myId,
                "claimTime", claimTime,
                "batchSize", VmGlobalConfig.VM_METADATA_FLUSH_BATCH_SIZE.value(Integer.class));

        if (claimed == 0) {
            return Collections.emptyList();
        }

        // Precise match: only return rows stamped with THIS round's claimTime,
        // not rows from triggerFlushForVm() or previous polls still being flushed.
        return Q.New(VmMetadataDirtyVO.class)
                .eq(VmMetadataDirtyVO_.managementNodeUuid, myId)
                .eq(VmMetadataDirtyVO_.lastClaimTime, claimTime)
                .list();
    }

    private String findStaleClaimOwner(String vmUuid, Duration staleThreshold) {
        Timestamp cutoff = Timestamp.from(Instant.now().minus(staleThreshold));
        return Q.New(VmMetadataDirtyVO.class)
                .eq(VmMetadataDirtyVO_.vmInstanceUuid, vmUuid)
                .notNull(VmMetadataDirtyVO_.managementNodeUuid)
                .lt(VmMetadataDirtyVO_.lastClaimTime, cutoff)
                .select(VmMetadataDirtyVO_.managementNodeUuid)
                .findValue();
    }

    private int nativeExecute(String sql, Object... kvPairs) {
        return new SQLBatchWithReturn<Integer>() {
            @Override
            protected Integer scripts() {
                javax.persistence.Query q = dbf.getEntityManager().createNativeQuery(sql);
                for (int i = 0; i < kvPairs.length; i += 2) {
                    String key = (String) kvPairs[i];
                    Object val = kvPairs[i + 1];
                    if (val instanceof java.util.Collection) {
                        // JPA native query setParameter does not expand collection for IN clause;
                        // Hibernate's setParameterList handles it correctly.
                        q.unwrap(org.hibernate.query.NativeQuery.class)
                                .setParameterList(key, (java.util.Collection<?>) val);
                    } else {
                        q.setParameter(key, val);
                    }
                }
                return q.executeUpdate();
            }
        }.execute();
    }

    private void cleanupZombieClaims() {
        long thresholdMinutes = VmGlobalConfig.VM_METADATA_FLUSH_ZOMBIE_CLAIM_THRESHOLD.value(Long.class);
        Timestamp cutoff = Timestamp.from(Instant.now().minus(Duration.ofMinutes(thresholdMinutes)));

        logger.debug(String.format("[MetadataDirty] cleanupZombieClaims: thresholdMinutes=%d, cutoff=%s",
                thresholdMinutes, cutoff));

        int cleaned = SQL.New("UPDATE VmMetadataDirtyVO " +
                        "SET managementNodeUuid = NULL, lastClaimTime = NULL " +
                        "WHERE managementNodeUuid IS NOT NULL " +
                        "AND lastClaimTime < :cutoff")
                .param("cutoff", cutoff)
                .execute();

        if (cleaned > 0) {
            logger.info(String.format("[MetadataDirty] cleanupZombieClaims released %d zombie claim(s) " +
                    "(threshold=%d minutes)", cleaned, thresholdMinutes));
        }
    }

    private void submitFullRefresh(String currentVersion) {
        logger.info(String.format("[MetadataDirty] metadata full refresh: starting for version %s", currentVersion));

        String lastUuid = "";
        int totalProcessed = 0;

        while (true) {
            List<String> batch = SQL.New("SELECT v.uuid FROM VmInstanceVO v " +
                            "WHERE v.type = 'UserVm' AND v.uuid > :lastUuid " +
                            "ORDER BY v.uuid ASC", String.class)
                    .param("lastUuid", lastUuid)
                    .limit(UPGRADE_REFRESH_BATCH_SIZE)
                    .list();

            if (batch.isEmpty()) {
                break;
            }

            nativeExecute("INSERT INTO VmMetadataDirtyVO " +
                            "(vmInstanceUuid, dirtyVersion, storageStructureChange, createDate, lastOpDate) " +
                            "SELECT v.uuid, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM VmInstanceVO v " +
                            "WHERE v.uuid IN (:uuids) " +
                            "ON DUPLICATE KEY UPDATE dirtyVersion = dirtyVersion + 1, storageStructureChange = 1",
                    "uuids", batch);

            totalProcessed += batch.size();
            lastUuid = batch.get(batch.size() - 1);
        }

        logger.info(String.format("[MetadataDirty] metadata full refresh: %d VMs processed for version %s",
                totalProcessed, currentVersion));

        VmGlobalConfig.VM_METADATA_LAST_REFRESH_VERSION.updateValue(currentVersion);
    }

    private void submitBatchInitialization() {
        thdf.submit(new org.zstack.core.thread.Task<Void>() {
            @Override
            public String getName() {
                return "metadata-batch-initialization";
            }

            @Override
            public Void call() {
                try {
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("[MetadataDirty] batch initialization startup delay interrupted");
                    return null;
                }

                if (!VmGlobalConfig.VM_METADATA_ENABLED.value(Boolean.class)) {
                    logger.info("[MetadataDirty] vm.metadata.enabled toggled back to false before initialization, skip");
                    return null;
                }

                int batchSize = INIT_BATCH_SIZE;
                String lastUuid = "";
                int totalInitialized = 0;

                while (true) {
                    if (!VmGlobalConfig.VM_METADATA_ENABLED.value(Boolean.class)) {
                        logger.info(String.format("[MetadataDirty] vm.metadata.enabled disabled during initialization, " +
                                "abort. initialized=%d", totalInitialized));
                        break;
                    }

                    int initialized = nativeExecute(
                            "INSERT IGNORE INTO VmMetadataDirtyVO (vmInstanceUuid, dirtyVersion, storageStructureChange, createDate, lastOpDate) " +
                                    "SELECT v.uuid, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM VmInstanceVO v " +
                                    "LEFT JOIN VmMetadataDirtyVO d ON v.uuid = d.vmInstanceUuid " +
                                    "WHERE v.type = 'UserVm' AND v.uuid > :lastUuid AND d.vmInstanceUuid IS NULL " +
                                    "ORDER BY v.uuid ASC LIMIT :batchSize",
                            "lastUuid", lastUuid,
                            "batchSize", batchSize);

                    totalInitialized += initialized;

                    List<String> batchUuids = SQL.New("SELECT v.uuid FROM VmInstanceVO v " +
                                    "WHERE v.type = 'UserVm' AND v.uuid > :lastUuid " +
                                    "ORDER BY v.uuid ASC", String.class)
                            .param("lastUuid", lastUuid)
                            .limit(batchSize)
                            .list();

                    if (batchUuids.isEmpty()) {
                        break;
                    }
                    lastUuid = batchUuids.get(batchUuids.size() - 1);

                    logger.info(String.format("[MetadataDirty] metadata initialization batch completed: " +
                            "%d VMs in this batch, %d total", initialized, totalInitialized));

                    try {
                        TimeUnit.SECONDS.sleep(INIT_BATCH_DELAY_SEC);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn("[MetadataDirty] metadata initialization interrupted");
                        break;
                    }
                }

                logger.info(String.format("[MetadataDirty] metadata initialization complete: %d VMs total",
                        totalInitialized));
                return null;
            }
        });
    }

    private void cleanupPathFingerprints() {
        thdf.submit(new org.zstack.core.thread.Task<Void>() {
            @Override
            public String getName() {
                return "metadata-cleanup-path-fingerprints";
            }

            @Override
            public Void call() {
                String lastUuid = "";
                int totalDeleted = 0;
                int batchSize = 1000;

                while (true) {
                    List<String> batch = SQL.New(
                                    "SELECT vmInstanceUuid FROM VmMetadataFlushStateVO " +
                                            "WHERE vmInstanceUuid > :lastUuid " +
                                            "ORDER BY vmInstanceUuid ASC", String.class)
                            .param("lastUuid", lastUuid)
                            .limit(batchSize)
                            .list();

                    if (batch.isEmpty()) {
                        break;
                    }

                    int deleted = SQL.New("DELETE FROM VmMetadataFlushStateVO " +
                                    "WHERE vmInstanceUuid IN (:uuids)")
                            .param("uuids", batch)
                            .execute();
                    totalDeleted += deleted;
                    lastUuid = batch.get(batch.size() - 1);
                }

                if (totalDeleted > 0) {
                    logger.info(String.format("[MetadataDirty] cleaned up %d PathFingerprint rows " +
                            "after metadata feature disabled", totalDeleted));
                }
                return null;
            }
        });
    }

    private void scheduleUpgradeRefreshIfNeeded() {
        if (!VmGlobalConfig.VM_METADATA_ENABLED.value(Boolean.class)) {
            logger.debug("[MetadataDirty] scheduleUpgradeRefreshIfNeeded SKIP: vm.metadata.enabled=false");
            return;
        }

        String currentVersion = dbf.getDbVersion();
        String lastRefreshVersion = VmGlobalConfig.VM_METADATA_LAST_REFRESH_VERSION.value(String.class);

        if (currentVersion.equals(lastRefreshVersion)) {
            logger.debug("[MetadataDirty] DB version matches lastRefreshVersion, no upgrade refresh needed");
            return;
        }

        // Guard: prevent exponential timer accumulation under cluster churn.
        // Only one scheduled refresh at a time; the timer callback resets the flag.
        if (!upgradeRefreshScheduled.compareAndSet(false, true)) {
            logger.debug("[MetadataDirty] upgrade refresh already scheduled, skip duplicate");
            return;
        }

        long delaySec = UPGRADE_REFRESH_DELAY_SEC;
        logger.info(String.format("[MetadataDirty] DB version %s != lastRefreshVersion %s, " +
                "scheduling upgrade refresh after %ds delay", currentVersion, lastRefreshVersion, delaySec));

        thdf.submitTimeoutTask(() -> {
            boolean rescheduled = false;
            try {
                // Re-check: version may have changed, or feature may be disabled
                if (!VmGlobalConfig.VM_METADATA_ENABLED.value(Boolean.class)) {
                    return;
                }
                String recheckVersion = dbf.getDbVersion();
                if (!recheckVersion.equals(currentVersion)) {
                    logger.warn("[MetadataDirty] DB version changed during upgrade refresh delay, skip");
                    return;
                }
                String recheckLastRefresh = VmGlobalConfig.VM_METADATA_LAST_REFRESH_VERSION.value(String.class);
                if (recheckVersion.equals(recheckLastRefresh)) {
                    logger.info("[MetadataDirty] another MN already completed upgrade refresh, skip");
                    return;
                }

                // M3 recent-nodeLeft check: if nodeLeft within last 15 min, reschedule
                // (up to MAX_UPGRADE_REFRESH_RESCHEDULES times to avoid indefinite postponement)
                long recentNodeLeftWindowMs = 15L * 60 * 1000;
                if (System.currentTimeMillis() - lastNodeLeftTimestamp < recentNodeLeftWindowMs
                        && upgradeRefreshRescheduleCount.incrementAndGet() <= MAX_UPGRADE_REFRESH_RESCHEDULES) {
                    logger.info(String.format("[MetadataDirty] recent nodeLeft detected, rescheduling upgrade refresh (%d/%d)",
                            upgradeRefreshRescheduleCount.get(), MAX_UPGRADE_REFRESH_RESCHEDULES));
                    upgradeRefreshScheduled.set(false);  // reset before re-entry so CAS can succeed
                    scheduleUpgradeRefreshIfNeeded();  // re-enter with fresh delay
                    rescheduled = true;
                    return;
                }

                submitFullRefresh(recheckVersion);
                upgradeRefreshRescheduleCount.set(0);
            } finally {
                // Release the scheduling guard so subsequent calls can schedule again.
                // Skip if the reschedule branch already handed off the guard to a
                // new timer via scheduleUpgradeRefreshIfNeeded().
                if (!rescheduled) {
                    upgradeRefreshScheduled.set(false);
                }
            }
        }, TimeUnit.SECONDS, delaySec);
    }

    private synchronized void startZombieCleanupTask() {
        zombieCleanupStopped.set(false);
        thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return 60;  // fixed 60s — frequent enough relative to zombie threshold (default 15min)
            }

            @Override
            public String getName() {
                return "vm-metadata-zombie-claim-cleanup";
            }

            @Override
            public boolean run() {
                // CancelablePeriodicTask: return true = cancel, false = continue
                if (zombieCleanupStopped.get()) {
                    return true;
                }
                cleanupZombieClaims();
                return zombieCleanupStopped.get();
            }
        });
        logger.info("[MetadataDirty] zombie claim cleanup task started (interval=60s)");
    }

    private synchronized void stopZombieCleanupTask() {
        zombieCleanupStopped.set(true);
        logger.info("[MetadataDirty] zombie claim cleanup task stopped");
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        long delaySec = NODE_LEFT_DELAY_SEC;
        logger.info(String.format("[MetadataDirty] node[%s] left, scheduling claim and flush after %ds delay", inv.getUuid(), delaySec));

        lastNodeLeftTimestamp = System.currentTimeMillis();

        thdf.submitTimeoutTask(this::claimAndFlush, TimeUnit.SECONDS, delaySec);
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {
    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {
    }
}
