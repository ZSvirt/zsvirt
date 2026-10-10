package org.zstack.ha;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.Q;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.ha.entity.VmHaVO;
import org.zstack.ha.entity.VmHaVO_;
import org.zstack.ha.gc.HaGcManager;
import org.zstack.ha.header.HaGlobalConfig;
import org.zstack.ha.header.VmHaLevel;
import org.zstack.header.Component;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.zstack.ha.header.VmHaLevel.*;

/**
 * Scheduler for:
 * 1. setup VM self-fencer (on host): if VM nic down/ host disconnect, the fencer will kill the VM
 */
public class HaScheduler implements Component, ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(HaScheduler.class);

    public static final String TASK_NAME_STOPPED_VM_SCANNER = "NeverStopVmScanTracker";
    public static final String TASK_NAME_REFRESH_FENCERS = "ha-sync-state-retry-setup-cancel-self-fencer-task";

    public final Object lock = new Object();
    public Future<Void> neverStopVmScanTracker = null;
    private Future<Void> syncSelfFencerTracker;

    @Autowired
    private ThreadFacade threadFacade;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private VmHaService vmHaService;
    @Autowired
    private HaGcManager haGcManager;

    @Override
    public boolean start() {
        HaGlobalConfig.NEVER_STOP_VM_SCAN_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> setupHaScheduler());
        HaGlobalConfig.HA_SYNC_SELF_FENCER_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> startSyncSelfFencer());
        return true;
    }

    @Override
    public void managementNodeReady() {
        setupHaScheduler();
        startSyncSelfFencer();
    }

    @Override
    public boolean stop() {
        synchronized (lock) {
            clearScheduler();
            clearSyncSelfFencer();
        }
        return false;
    }

    public void setupHaScheduler() {
        synchronized (lock) {
            clearScheduler();

            final long interval = HaGlobalConfig.NEVER_STOP_VM_SCAN_INTERVAL.value(Long.class);
            neverStopVmScanTracker = threadFacade.submitPeriodicTask(new PeriodicTask() {
                @Override
                @ExceptionSafe
                public void run() {
                    scanHaVmNow();
                }

                @Override
                public TimeUnit getTimeUnit() {
                    return TimeUnit.SECONDS;
                }

                @Override
                public long getInterval() {
                    return interval;
                }

                @Override
                public String getName() {
                    return TASK_NAME_STOPPED_VM_SCANNER;
                }
            });
        }
    }

    /**
     * This method is also for UT
     */
    public void scanHaVmNow() {
        if (HaHelper.isVmHaEnabled()) {
            logger.debug("start GC job for never stop HA VM");
            try {
                setGCJobForNeverStopHaVm();
            } catch (Exception e) {
                logger.warn("failed to execute GC job for never stop HA VM", e);
                throw new RuntimeException(e);
            }
        }
    }

    public void clearScheduler() {
        synchronized (lock) {
            if (neverStopVmScanTracker != null) {
                neverStopVmScanTracker.cancel(true);
                neverStopVmScanTracker = null;
            }
        }
    }

    private void startSyncSelfFencer() {
        synchronized (lock) {
            clearSyncSelfFencer();
            logger.debug(String.format("start ha-sync-state-retry-setup-cancel-self-fencer-task at interval %s",
                    HaGlobalConfig.HA_SYNC_SELF_FENCER_INTERVAL.value()));
            int interval = HaGlobalConfig.HA_SYNC_SELF_FENCER_INTERVAL.value(Integer.class);
            syncSelfFencerTracker = threadFacade.submitPeriodicTask(new HaSyncSelfFencerTask(interval));
        }
    }

    private void clearSyncSelfFencer() {
        if (syncSelfFencerTracker != null) {
            syncSelfFencerTracker.cancel(true);
            syncSelfFencerTracker = null;
        }
    }

    private void setGCJobForNeverStopHaVm() {
        final Set<VmInstanceState> needHaVmStates = new HashSet<>();
        needHaVmStates.add(VmInstanceState.Unknown);
        needHaVmStates.add(VmInstanceState.Stopped);

        final Set<VmHaLevel> needHaVmHaLevels = new HashSet<>();
        needHaVmHaLevels.add(NeverStop);
        needHaVmHaLevels.add(OnHostFailure);

        List<Tuple> tupleList = Q.New(VmInstanceVO.class, VmHaVO.class)
            .table0()
                .in(VmInstanceVO_.state, needHaVmStates)
                .eq(VmInstanceVO_.uuid).table1(VmHaVO_.uuid)
                .select(VmInstanceVO_.state, VmInstanceVO_.type)
            .table1()
                .isNull(VmHaVO_.inhibitionReason)
                .in(VmHaVO_.haLevel, needHaVmHaLevels)
                .select(VmHaVO_.uuid, VmHaVO_.haLevel)
            .listTuple();

        if (tupleList.isEmpty()) {
            return;
        }

        CollectionUtils.safeForEach(tupleList, tuple -> {
            VmInstanceState vmState = tuple.get(0, VmInstanceState.class);
            String vmType = tuple.get(1, String.class);
            String vmUuid = tuple.get(2, String.class);
            VmHaLevel level = tuple.get(3, VmHaLevel.class);

            if (!destinationMaker.isManagedByUs(vmUuid)) {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("skip HA scheduler for VM[%s], level=%s: not managed by us", vmUuid, level));
                }
                return;
            }

            String hostUuid;
            HostStatus hostStatus;

            if (vmState == VmInstanceState.Unknown) {
                Tuple tuple0 = Q.New(HostVO.class, VmInstanceVO.class)
                    .table0()
                        .eq(HostVO_.uuid).table1(VmInstanceVO_.hostUuid)
                        .select(HostVO_.uuid, HostVO_.status)
                    .table1()
                        .eq(VmInstanceVO_.uuid, vmUuid)
                    .findTuple();
                hostUuid = tuple0 == null ? null : tuple0.get(0, String.class);
                hostStatus = tuple0 == null ? null : tuple0.get(1, HostStatus.class);
            } else { // Stopped, Destroyed, or other
                Tuple tuple0 = Q.New(HostVO.class, VmInstanceVO.class)
                    .table0()
                        .eq(HostVO_.uuid).table1(VmInstanceVO_.lastHostUuid)
                        .select(HostVO_.uuid, HostVO_.status)
                    .table1()
                        .eq(VmInstanceVO_.uuid, vmUuid)
                    .findTuple();
                hostUuid = tuple0 == null ? null : tuple0.get(0, String.class);
                hostStatus = tuple0 == null ? null : tuple0.get(1, HostStatus.class);
            }

            // if vm's host connected means host is recovered from failure
            // use startVmWithStateSyncBefore which will check vm state before

            if (hostStatus == HostStatus.Connected) {
                logger.debug(String.format("ready to HA scheduler for VM[%s], level=%s: setup VmHaTaskTracker", vmUuid, level));
                vmHaService.setupHaTrackerForNeverStopVm(vmUuid, hostUuid);
            } else if (vmState == VmInstanceState.Unknown) {
                vmHaService.setupHaTrackerForOnHostFailureVm(vmUuid, true);
            } else {
                haGcManager.submitNeverStopVmGCJob(vmUuid);
            }
        });
    }
}
