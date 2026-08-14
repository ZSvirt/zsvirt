package org.zstack.header.storage.database.backup;

import org.zstack.header.storage.backup.BackupStorageEO;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;


@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = DatabaseBackupVO.class, myField = "databaseBackupUuid", targetField = "uuid")
        },
        friends = {
                @EntityGraph.Neighbour(type = BackupStorageVO.class, myField = "backupStorageUuid", targetField = "uuid")
        }
)
public class DatabaseBackupStorageRefVO implements ToInventory {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    @ForeignKey(parentEntityClass = DatabaseBackupVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String databaseBackupUuid;

    @Column
    @ForeignKey(parentEntityClass = BackupStorageEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String backupStorageUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private DatabaseBackupStatus status;

    @Column
    private String installPath;

    @Column
    private String exportUrl;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDatabaseBackupUuid() {
        return databaseBackupUuid;
    }

    public void setDatabaseBackupUuid(String databaseBackupUuid) {
        this.databaseBackupUuid = databaseBackupUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public DatabaseBackupStatus getStatus() {
        return status;
    }

    public void setStatus(DatabaseBackupStatus status) {
        this.status = status;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
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

    public String getExportUrl() {
        return exportUrl;
    }

    public void setExportUrl(String exportUrl) {
        this.exportUrl = exportUrl;
    }
}
