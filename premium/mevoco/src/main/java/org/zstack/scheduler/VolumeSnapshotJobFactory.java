package org.zstack.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;
import org.zstack.header.volume.VolumeVO;
import org.zstack.scheduler.snapshot.CreateVolumeSnapshotJob;
import org.zstack.storage.ceph.CephConstants;

import java.util.List;
import java.util.Objects;

import static org.zstack.core.Platform.argerr;

public class VolumeSnapshotJobFactory implements SchedulerJobFactory {
    @Autowired
    private transient DatabaseFacade dbf;
    private List<String> supportSnapshotsRetentionJobPrimaryStorage;

    public List<String> getSupportSnapshotsRetentionJobPrimaryStorage() {
        return supportSnapshotsRetentionJobPrimaryStorage;
    }

    public void setSupportSnapshotsRetentionJobPrimaryStorage(List<String> supportSnapshotsRetentionJobPrimaryStorage) {
        this.supportSnapshotsRetentionJobPrimaryStorage = supportSnapshotsRetentionJobPrimaryStorage;
    }

    @Override
    public SchedulerJob createSchedulerJob(CreateSchedulerJobDescMsg msg) {
        return new CreateVolumeSnapshotJob(msg);
    }

    @Override
    public String getJobType() {
        return SchedulerType.VOLUME_SNAPSHOT;
    }

    @Override
    public String getJobClassName() {
        return CreateVolumeSnapshotJob.class.getName();
    }

    public ErrorCode validateMsg(CreateSchedulerJobDescMsg msg) {
        if (!Objects.equals(msg.getType(), SchedulerType.VOLUME_SNAPSHOT)) {
            return null;
        }

        VolumeVO volume = dbf.findByUuid(msg.getTargetResourceUuid(), VolumeVO.class);
        if (volume == null) {
            return argerr("the volume[%s] is not available. " +
                    "check if the volume exists.", msg.getTargetResourceUuid());
        }

        String sql = "select ps.type from VolumeVO vol, PrimaryStorageVO ps " +
                "where ps.uuid=vol.primaryStorageUuid and vol.uuid=:volUuid";
        String type = SQL.New(sql).param("volUuid", msg.getTargetResourceUuid()).find();

        if (!supportSnapshotsRetentionJobPrimaryStorage.contains(type)) {
            if (msg.getParameters() != null && msg.getParameters().get(SchedulerJobParameters.snapshotMax) != null) {
                return argerr("the volume[%s] does not support snapshots retention", msg.getTargetResourceUuid());
            }
            return null;
        }

        if (msg.getParameters() == null || msg.getParameters().get(SchedulerJobParameters.snapshotMax) == null) {
            return null;
        }

        try {
            Long.valueOf(msg.getParameters().get(SchedulerJobParameters.snapshotMax));
        } catch (NumberFormatException e) {
            return argerr("snapshotMaxNumber : %s format error because %s",
                    msg.getParameters().get(SchedulerJobParameters.snapshotMax), e.getMessage());
        }

        return null;
    }
}
