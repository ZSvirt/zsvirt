package org.zstack.imagereplicator;

import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = ImageReplicationGroupVO.class, joinColumn = "replicationGroupUuid"),
        @SoftDeletionCascade(parent = BackupStorageVO.class, joinColumn = "backupStorageUuid")
})
public class ImageReplicationGroupBackupStorageRefVO implements Serializable {
    @Column
    @Id
    private String backupStorageUuid;

    @Column
    private String replicationGroupUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getReplicationGroupUuid() {
        return replicationGroupUuid;
    }

    public void setReplicationGroupUuid(String replicationGroupUuid) {
        this.replicationGroupUuid = replicationGroupUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
