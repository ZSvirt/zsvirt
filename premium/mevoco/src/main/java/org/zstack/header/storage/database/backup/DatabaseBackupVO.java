package org.zstack.header.storage.database.backup;

import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.volume.VolumeType;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = DatabaseBackupStorageRefVO.class, myField = "uuid", targetField = "databaseBackupUuid"),
        }
)
public class DatabaseBackupVO extends ResourceVO implements ToInventory{
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "databaseBackupUuid", insertable = false, updatable = false)
    @NoView
    private Set<DatabaseBackupStorageRefVO> backupStorageRefs = new HashSet<>();

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private Long size;

    @Column
    @Enumerated(EnumType.STRING)
    private DatabaseBackupState state;

    @Column
    @Enumerated(EnumType.STRING)
    private DatabaseBackupStatus status;

    @Column
    private String metadata;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public DatabaseBackupState getState() {
        return state;
    }

    public void setState(DatabaseBackupState state) {
        this.state = state;
    }

    public DatabaseBackupStatus getStatus() {
        return status;
    }

    public void setStatus(DatabaseBackupStatus status) {
        this.status = status;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
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


    public Set<DatabaseBackupStorageRefVO> getBackupStorageRefs() {
        return backupStorageRefs;
    }

    public void setBackupStorageRefs(Set<DatabaseBackupStorageRefVO> backupStorageRefs) {
        this.backupStorageRefs = backupStorageRefs;
    }
}
