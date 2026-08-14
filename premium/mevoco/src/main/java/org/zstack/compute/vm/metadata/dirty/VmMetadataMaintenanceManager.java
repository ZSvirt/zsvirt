package org.zstack.compute.vm.metadata.dirty;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.compute.vm.metadata.VmMetadataBuilderUtils;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.vm.metadata.VmMetadataDirtyVO;
import org.zstack.header.vm.metadata.VmMetadataDirtyVO_;
import org.zstack.header.vm.metadata.VmMetadataFlushStateVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class VmMetadataMaintenanceManager implements Component, ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmMetadataMaintenanceManager.class);
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VmMetadataDirtyMarker dirtyMarker;
    @Autowired
    private ResourceDestinationMaker destMaker;

    private final AtomicBoolean contentDriftStopped = new AtomicBoolean(false);
    private final AtomicBoolean orphanStopped = new AtomicBoolean(false);
    private final AtomicBoolean staleRecoveryStopped = new AtomicBoolean(false);

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        stopAllTasks();
        return true;
    }

    @Override
    public void managementNodeReady() {
        startAllTasks();
    }

    private synchronized void startAllTasks() {
        // 1. content drift detector
        contentDriftStopped.set(false);
        thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return VmGlobalConfig.VM_METADATA_MAINTENANCE_CONTENT_DRIFT_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "vm-metadata-content-drift-detector";
            }

            @Override
            public boolean run() {
                // CancelablePeriodicTask: return true = cancel, false = continue
                if (contentDriftStopped.get()) {
                    return true;
                }
                detectContentDrift();
                return contentDriftStopped.get();
            }
        });

        // 2. orphan detector
        orphanStopped.set(false);
        thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return VmGlobalConfig.VM_METADATA_MAINTENANCE_ORPHAN_CHECK_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "vm-metadata-orphan-detector";
            }

            @Override
            public boolean run() {
                // CancelablePeriodicTask: return true = cancel, false = continue
                if (orphanStopped.get()) {
                    return true;
                }
                detectOrphans();
                return orphanStopped.get();
            }
        });

        // 3. stale recovery
        staleRecoveryStopped.set(false);
        thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return VmGlobalConfig.VM_METADATA_MAINTENANCE_STALE_RECOVERY_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "vm-metadata-stale-recovery";
            }

            @Override
            public boolean run() {
                // CancelablePeriodicTask: return true = cancel, false = continue
                if (staleRecoveryStopped.get()) {
                    return true;
                }
                recoverStaleVms();
                return staleRecoveryStopped.get();
            }
        });

        logger.info("[VmMetadataMaintenanceManager] all 3 periodic tasks started");
    }

    private synchronized void stopAllTasks() {
        contentDriftStopped.set(true);
        orphanStopped.set(true);
        staleRecoveryStopped.set(true);
        logger.info("[VmMetadataMaintenanceManager] all periodic tasks stopped");
    }

    private void detectContentDrift() {
        if (!VmGlobalConfig.VM_METADATA_ENABLED.value(Boolean.class)) {
            return;
        }

        logger.debug("[MetadataContentDrift] detectContentDrift START");

        String lastUuid = "";
        int driftCount = 0;
        int totalChecked = 0;
        int skippedByHashRing = 0;

        while (true) {
            List<VmMetadataFlushStateVO> batch = SQL.New(
                            "SELECT fp FROM VmMetadataFlushStateVO fp " +
                                    "WHERE fp.metadataSnapshot IS NOT NULL " +
                                    "AND fp.vmInstanceUuid > :lastUuid " +
                                    "ORDER BY fp.vmInstanceUuid ASC",
                            VmMetadataFlushStateVO.class)
                    .param("lastUuid", lastUuid)
                    .limit(VmGlobalConfig.VM_METADATA_MAINTENANCE_CONTENT_DRIFT_BATCH_SIZE.value(Integer.class))
                    .list();

            if (batch.isEmpty()) {
                break;
            }

            for (VmMetadataFlushStateVO fp : batch) {
                String vmUuid = fp.getVmInstanceUuid();

                try {
                    if (!destMaker.isManagedByUs(vmUuid)) {
                        skippedByHashRing++;
                        continue;
                    }

                    totalChecked++;

                    String currentMetadata = VmMetadataBuilderUtils.buildVmInstanceMetadata(dbf, vmUuid);
                    if (currentMetadata == null) {
                        continue;
                    }

                    if (!currentMetadata.equals(fp.getMetadataSnapshot())) {
                        logger.info(String.format("[MetadataContentDrift] drift detected for vm=%s, marking dirty", vmUuid));
                        dirtyMarker.markDirty(vmUuid, true);
                        driftCount++;
                    }
                } catch (Exception e) {
                    logger.warn(String.format("[MetadataContentDrift] failed to process vm=%s, continue", vmUuid), e);
                }
            }

            lastUuid = batch.get(batch.size() - 1).getVmInstanceUuid();

            if (contentDriftStopped.get()) {
                logger.info("[MetadataContentDrift] stop flag set, aborting drift check between batches");
                break;
            }

            // Rate-limit DB pressure: sleep between batches
            try {
                TimeUnit.SECONDS.sleep(VmGlobalConfig.VM_METADATA_MAINTENANCE_CONTENT_DRIFT_BATCH_SLEEP_SEC.value(Long.class));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("[MetadataContentDrift] interrupted during batch sleep, aborting drift check");
                break;
            }
        }

        if (totalChecked > 0 || skippedByHashRing > 0) {
            logger.info(String.format("[MetadataContentDrift] checked %d VMs, drift detected: %d, skippedByHashRing: %d",
                    totalChecked, driftCount, skippedByHashRing));
        }
    }

    private void detectOrphans() {
        if (!VmGlobalConfig.VM_METADATA_ENABLED.value(Boolean.class)) {
            return;
        }

        logger.debug("[MetadataOrphanDetector] detectOrphans START");

        List<String> claimedMnUuids = Q.New(VmMetadataDirtyVO.class)
                .select(VmMetadataDirtyVO_.managementNodeUuid)
                .notNull(VmMetadataDirtyVO_.managementNodeUuid)
                .groupBy(VmMetadataDirtyVO_.managementNodeUuid)
                .listValues();

        if (claimedMnUuids.isEmpty()) {
            logger.debug("[MetadataOrphanDetector] no claimed MN UUIDs found, skip");
            return;
        }

        logger.debug(String.format("[MetadataOrphanDetector] found %d claimed MN UUIDs: %s",
                claimedMnUuids.size(), claimedMnUuids));

        Set<String> aliveMnUuids = new HashSet<>(Q.New(ManagementNodeVO.class)
                .select(ManagementNodeVO_.uuid)
                .in(ManagementNodeVO_.uuid, claimedMnUuids)
                .listValues());

        List<String> deadMnUuids = claimedMnUuids.stream()
                .filter(mnUuid -> !aliveMnUuids.contains(mnUuid))
                .collect(Collectors.toList());

        if (deadMnUuids.isEmpty()) {
            return;
        }

        int released = SQL.New(VmMetadataDirtyVO.class)
                .in(VmMetadataDirtyVO_.managementNodeUuid, deadMnUuids)
                .set(VmMetadataDirtyVO_.managementNodeUuid, null)
                .set(VmMetadataDirtyVO_.lastClaimTime, null)
                .update();

        if (released > 0) {
            logger.warn(String.format(
                    "[MetadataOrphanDetector] released %d orphan dirty row(s) claimed by dead MN(s) %s",
                    released, deadMnUuids));
        }
    }

    private void recoverStaleVms() {
        if (!VmGlobalConfig.VM_METADATA_ENABLED.value(Boolean.class)) {
            return;
        }

        logger.debug("[MetadataStaleRecovery] recoverStaleVms START");

        int maxCycles = VmGlobalConfig.VM_METADATA_MAINTENANCE_STALE_RECOVERY_MAX_CYCLES.value(Integer.class);

        List<VmMetadataFlushStateVO> staleVms = SQL.New(
                        "SELECT fp FROM VmMetadataFlushStateVO fp WHERE fp.pendingStaleRecovery = 1 " +
                                "ORDER BY fp.staleRecoveryCount ASC, fp.vmInstanceUuid ASC",
                        VmMetadataFlushStateVO.class)
                .limit(VmGlobalConfig.VM_METADATA_MAINTENANCE_STALE_RECOVERY_BATCH_SIZE.value(Integer.class))
                .list();

        if (staleVms.isEmpty()) {
            return;
        }

        int requeued = 0;
        int circuitBroken = 0;
        int skippedByHashRing = 0;

        for (VmMetadataFlushStateVO fp : staleVms) {
            String vmUuid = fp.getVmInstanceUuid();

            try {
                if (!destMaker.isManagedByUs(vmUuid)) {
                    skippedByHashRing++;
                    continue;
                }

                if (fp.getStaleRecoveryCount() >= maxCycles) {
                    SQL.New("UPDATE VmMetadataFlushStateVO " +
                                    "SET pendingStaleRecovery = 0, staleRecoveryCount = 0 WHERE vmInstanceUuid = :vmUuid")
                            .param("vmUuid", vmUuid)
                            .execute();

                    logger.warn(String.format("VM [%s] metadata stale recovery exceeded %d cycles, entering permanent-stale. " +
                            "Use APIUpdateVmMetadataMsg to manually trigger.", vmUuid, maxCycles));
                    circuitBroken++;
                    continue;
                }

                boolean markSuccess = dirtyMarker.markDirty(vmUuid, true);

                if (markSuccess) {
                    // Only increment staleRecoveryCount here; keep pendingStaleRecovery = true.
                    // The actual flush success path (savePathFingerprint) will clear both
                    // pendingStaleRecovery and staleRecoveryCount when metadata is written.
                    // Guard with pendingStaleRecovery = 1 to avoid a race: if savePathFingerprint()
                    // already cleared the row between markDirty() returning and this SQL executing,
                    // the UPDATE becomes a no-op instead of blindly writing staleRecoveryCount = 1.
                    SQL.New("UPDATE VmMetadataFlushStateVO " +
                                    "SET staleRecoveryCount = staleRecoveryCount + 1 " +
                                    "WHERE vmInstanceUuid = :vmUuid AND pendingStaleRecovery = 1")
                            .param("vmUuid", vmUuid)
                            .execute();
                    requeued++;
                } else {
                    logger.warn(String.format("[MetadataStaleRecovery] markDirty failed for vm=%s, " +
                            "keeping pendingStaleRecovery=true for next retry cycle", vmUuid));
                }
            } catch (Exception e) {
                logger.warn(String.format("[MetadataStaleRecovery] failed to process vm=%s, continue", vmUuid), e);
            }
        }

        logger.info(String.format("[MetadataStaleRecovery] processed %d stale VMs: requeued=%d, circuitBroken=%d, skippedByHashRing=%d",
                staleVms.size(), requeued, circuitBroken, skippedByHashRing));
    }
}
