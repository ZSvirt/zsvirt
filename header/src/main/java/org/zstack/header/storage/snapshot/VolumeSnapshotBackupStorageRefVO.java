package org.zstack.header.storage.snapshot;

import org.zstack.header.storage.backup.BackupStorageEO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;
import java.io.Serializable;

/**
 */
@Entity
@Table
public class VolumeSnapshotBackupStorageRefVO implements Serializable {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column
    @ForeignKey(parentEntityClass = VolumeSnapshotEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String volumeSnapshotUuid;
    @Column
    @ForeignKey(parentEntityClass = BackupStorageEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String backupStorageUuid;
    @Column
    private String installPath;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getVolumeSnapshotUuid() {
        return volumeSnapshotUuid;
    }

    public void setVolumeSnapshotUuid(String volumeSnapshotUuid) {
        this.volumeSnapshotUuid = volumeSnapshotUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
