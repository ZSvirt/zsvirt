package org.zstack.scheduler;

import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.scheduler.snapshot.CreateVolumeSnapshotGroupJob;

import java.util.Objects;

import static org.zstack.core.Platform.argerr;

public class VolumeSnapshotGroupJobFactory implements SchedulerJobFactory {
    @Override
    public SchedulerJob createSchedulerJob(CreateSchedulerJobDescMsg msg) {
        return new CreateVolumeSnapshotGroupJob(msg);
    }

    @Override
    public String getJobType() {
        return SchedulerType.VOLUME_SNAPSHOT_GROUP;
    }

    @Override
    public String getJobClassName() {
        return CreateVolumeSnapshotGroupJob.class.getName();
    }

    @Override
    public ErrorCode validateMsg(CreateSchedulerJobDescMsg msg) {
        if (!Objects.equals(msg.getType(), SchedulerType.VOLUME_SNAPSHOT_GROUP)) {
            return null;
        }

        VolumeVO volume = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getTargetResourceUuid()).find();
        if (volume == null) {
            return argerr("the volume[%s] is not available. " +
                    "check if the volume exists.", msg.getTargetResourceUuid());
        }
        if (volume.getType() != VolumeType.Root) {
            return argerr("the volume[%s] is not root volume", msg.getTargetResourceUuid());
        }

        VmInstanceVO vm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, volume.getVmInstanceUuid()).find();
        if (vm == null) {
            return argerr("the vm of the root volume[%s] is not available. " +
                    "check if the vm exists.", msg.getTargetResourceUuid());
        }

        if (msg.getParameters() == null || msg.getParameters().get(SchedulerJobParameters.snapshotGroupMax) == null) {
            return null;
        }

        try {
            Long.valueOf(msg.getParameters().get(SchedulerJobParameters.snapshotGroupMax));
        } catch (NumberFormatException e) {
            return argerr("snapshotGroupMaxNumber : %s format error because %s",
                    msg.getParameters().get(SchedulerJobParameters.snapshotGroupMax), e.getMessage());
        }
        return null;
    }
}
