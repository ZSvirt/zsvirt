package org.zstack.header.storage.snapshot;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.*;
import org.zstack.header.volume.VolumeVO;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 */
@Entity
@Table
@EO(EOClazz = VolumeSnapshotEO.class)
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VolumeVO.class, myField = "volumeUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = VolumeSnapshotTreeVO.class, myField = "treeUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = PrimaryStorageVO.class, myField = "primaryStorageUuid", targetField = "uuid"),
        }
)
public class VolumeSnapshotVO extends VolumeSnapshotAO implements OwnedByAccount, ToInventory {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "volumeSnapshotUuid", insertable = false, updatable = false)
    @NoView
    private List<VolumeSnapshotBackupStorageRefVO> backupStorageRefs = new ArrayList<VolumeSnapshotBackupStorageRefVO>();


    @OneToOne(fetch = FetchType.EAGER, cascade = {CascadeType.MERGE})
    @JoinColumn(name = "uuid", referencedColumnName = "volumeSnapshotUuid", insertable = false)
    @NoView
    private VolumeSnapshotGroupRefVO groupRef;

    @Transient
    private String accountUuid;

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public List<VolumeSnapshotBackupStorageRefVO> getBackupStorageRefs() {
        return backupStorageRefs;
    }

    public void setBackupStorageRefs(List<VolumeSnapshotBackupStorageRefVO> backupStorageRefs) {
        this.backupStorageRefs = backupStorageRefs;
    }

    public VolumeSnapshotGroupRefVO getGroupRef() {
        return groupRef;
    }

    public void setGroupRef(VolumeSnapshotGroupRefVO groupRef) {
        this.groupRef = groupRef;
    }

    @Override
    public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
        super.setPrimaryStorageInstallPath(primaryStorageInstallPath);
        if (groupRef != null && primaryStorageInstallPath != null) {
            groupRef.setVolumeSnapshotInstallPath(primaryStorageInstallPath);
        }
    }

    @Override
    public void setVolumeType(String volumeType) {
        super.setVolumeType(volumeType);
        if (groupRef != null && volumeType != null) {
            groupRef.setVolumeType(volumeType);
        }
    }
}
