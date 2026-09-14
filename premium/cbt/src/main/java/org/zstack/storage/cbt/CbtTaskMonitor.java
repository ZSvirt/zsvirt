package org.zstack.storage.cbt;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.cbt.*;
import org.zstack.header.core.Completion;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.header.volume.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

public class CbtTaskMonitor implements VmStateChangedExtensionPoint, ResizeVolumeExtensionPoint {
    private static final CLogger logger = Utils.getLogger(CbtTaskMonitor.class);

    @Autowired
    private CloudBus bus;

    @Override
    public void vmStateChanged(VmInstanceInventory vm, VmInstanceState oldState, VmInstanceState newState) {
        if (newState != VmInstanceState.Migrating && newState != VmInstanceState.VolumeMigrating) {
            return;
        }

        CbtTaskVO task = getCbtTask(vm.getUuid());
        if (task == null || task.getStatus() != CbtTaskStatus.Running) {
            return;
        }

        DisableCbtTaskMsg msg = new DisableCbtTaskMsg();
        msg.setUuid(task.getUuid());
        msg.setForce(true);
        bus.makeTargetServiceIdByResourceUuid(msg, CbtBackupConstant.SERVICE_ID, vm.getUuid());
        MessageReply reply = bus.call(msg);
        if (!reply.isSuccess()) {
            logger.warn(String.format("failed to disable CBT task[uuid: %s] before VM[uuid: %s] migration: %s",
                    task.getUuid(), vm.getUuid(), reply.getError()));
        }
    }

    @Override
    public void beforeResizeVolume(VolumeVO volume, long resize, VolumeType type, ResizeVolumeStruct struct, Completion completion) {
        if (volume.getVmInstanceUuid() == null) {
            completion.success();
            return;
        }

        CbtTaskVO task = getCbtTask(volume.getVmInstanceUuid());
        if (task != null && task.getStatus() != CbtTaskStatus.Stopped) {
            completion.fail(operr("The VM[%s] for volume[%s] is protected by CBT task[%s], cannot resize now.",
                    volume.getVmInstanceUuid(), volume.getUuid(), task.getUuid()));
            return;
        }

        completion.success();
    }

    private CbtTaskVO getCbtTask(String vmUuid) {
        String taskUuid = Q.New(CbtTaskResourceRefVO.class)
                .eq(CbtTaskResourceRefVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .eq(CbtTaskResourceRefVO_.resourceUuid, vmUuid)
                .select(CbtTaskResourceRefVO_.taskUuid)
                .findValue();
        return taskUuid == null ? null : Q.New(CbtTaskVO.class).eq(CbtTaskVO_.uuid, taskUuid).find();
    }
}
