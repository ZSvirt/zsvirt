package org.zstack.ha;

import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.TimeBasedGarbageCollector;
import org.zstack.ha.fencers.KvmFencerManager;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.primary.local.LocalStorageConstants;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.i18n;

/**
 * Created by xing5 on 2017/3/6.
 */
public class NeverStopVmGC extends TimeBasedGarbageCollector {
    private static final CLogger logger = Utils.getLogger(NeverStopVmGC.class);

    @GC
    public VmInstanceInventory vm;

    public static String getGCName(String vmUuid) {
        return String.format("gc-start-never-stop-vm-%s", vmUuid);
    }

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (!HaGlobalConfig.ALL.value(Boolean.class)) {
            logger.debug(String.format("[GC] VMHA is globally disabled, cancel HA for vm[uuid:%s]", vm.getUuid()));
            completion.cancel();
            return;
        }

        VmHaLevel level = Q.New(VmHaVO.class)
                .eq(VmHaVO_.uuid, vm.getUuid())
                .select(VmHaVO_.haLevel)
                .findValue();

        if (level != VmHaLevel.NeverStop) {
            logger.debug(String.format("[GC] the vm[uuid::%s] is not set to NeverStop anymore, quit the process of trying to start it", vm.getUuid()));
            completion.cancel();
            return;
        }

        if (this.EXECUTED_TIMES > HaGlobalConfig.NEVER_STOP_VM_NOTIFICATION_RETRY_TIMES.value(Integer.class)) {
            logger.debug(String.format("Never stop vm GC[uuid:%s] executed over %s times, " +
                    "you can use DeleteGCJob to cancel it", this.getUuid(), HaGlobalConfig.NEVER_STOP_VM_NOTIFICATION_RETRY_TIMES.value(Integer.class).toString()));
        }

        VmInstanceState state = Q.New(VmInstanceVO.class).select(VmInstanceVO_.state)
                .eq(VmInstanceVO_.uuid, vm.getUuid()).findValue();

        if (state == null) {
            // the vm has been expunged
            completion.cancel();
            return;
        }

        if (state == VmInstanceState.Unknown && HaStrategyHelper.getVmHaFencerStrategy(vm.getUuid()) == SelfFencerStrategy.Permissive) {
            logger.debug(String.format("[GC] self fencer strategy if Permissive, cancel HA for vm[uuid:%s]", vm.getUuid()));
            completion.cancel();
            return;
        }

        if (state == VmInstanceState.Destroyed || state == VmInstanceState.Running) {
            // the vm has been destroyed
            completion.cancel();
            return;
        }

        // if host connected but vm state when submit GC job is Unknown
        // cancel the GC job and wait management node sync its actual state from Host.
        if (vm.getState().equals(VmInstanceState.Unknown.toString())) {
            boolean vmHostMayRecovered =Q.New(HostVO.class)
                    .eq(HostVO_.uuid, vm.getHostUuid())
                    .eq(HostVO_.status, HostStatus.Connected)
                    .isExists();

            if (vmHostMayRecovered) {
                logger.debug(String.format("[GC] vm's host[uuid: %s, last host uuid: %s] is connected," +
                                " cancel HA for vm[uuid:%s] state[current: %s, origin:%s]",
                        vm.getHostUuid(), vm.getLastHostUuid(), vm.getUuid(), state, vm.getState()));
                completion.cancel();
                return;
            }
        }

        VmHaTaskTracker tracker = new VmHaTaskTracker(vm.getUuid());
        KvmFencerManager fencerManager = Platform.getComponentLoader().getComponent(KvmFencerManager.class);
        String reason = fencerManager.findVmFencedReasonByTag(vm.getUuid());
        reason = reason == null ? i18n("VM state is not running, try to start it") : reason;
        tracker.track(VmHaTaskTracker.Process.starting, reason);
        logger.debug(String.format("[GC] start HA tracker for VM[%s]: %s", vm.getUuid(), reason));

        List<String> volumePSUuids = vm.getAllVolumes()
                .stream().map(VolumeInventory::getPrimaryStorageUuid)
                .collect(Collectors.toList());

        boolean volumeInLocalPs = false;
        if (!volumePSUuids.isEmpty()) {
            volumeInLocalPs = Q.New(PrimaryStorageVO.class)
                    .in(PrimaryStorageVO_.uuid, volumePSUuids)
                    .eq(PrimaryStorageVO_.type, LocalStorageConstants.LOCAL_STORAGE_TYPE)
                    .isExists();
        }
        
        boolean lastHostDeleted = !dbf.isExist(vm.getLastHostUuid(), HostVO.class);

        VmHaStartMessageSender sender = new VmHaStartMessageSender()
                .withVmInstance(vm.getUuid())
                .withJudgerClass(GCVmHaNeverStopJudger.class.getName())
                .withReasonForHA(reason);
        if (!(volumeInLocalPs || lastHostDeleted)) {
            sender.withAvoidHost(vm.getLastHostUuid());
        }

        sender.send(new Completion(completion) {
            @Override
            public void success() {
                tracker.track(VmHaTaskTracker.Process.success, i18n("VM is started successfully"));
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                // increase retry interval
                updateNextTime();
                tracker.error(errorCode);
                tracker.track(VmHaTaskTracker.Process.failure, i18n("Failed to start the NeverStop VM"));
                completion.fail(errorCode);
            }
        });
    }

    private synchronized void updateNextTime() {
        NEXT_TIME += NEXT_TIME;

        long nextTimeSecs = NEXT_TIME_UNIT.toSeconds(NEXT_TIME);
        long maxIntervalTimeSecs = HaGlobalConfig.NEVER_STOP_VM_GC_MAX_RETRY_INTERVAL_TIME.value(Long.class);
        if(nextTimeSecs > maxIntervalTimeSecs) {
            NEXT_TIME = NEXT_TIME_UNIT.convert(maxIntervalTimeSecs, TimeUnit.SECONDS);
        }
    }
}
