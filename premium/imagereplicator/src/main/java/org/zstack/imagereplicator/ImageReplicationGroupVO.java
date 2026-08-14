package org.zstack.imagereplicator;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = ImageReplicationGroupBackupStorageRefVO.class, myField = "uuid", targetField = "replicationGroupUuid")
        }
)
public class ImageReplicationGroupVO extends ResourceVO implements OwnedByAccount {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "replicationGroupUuid", insertable = false, updatable = false)
    @NoView
    private Set<ImageReplicationGroupBackupStorageRefVO> backupStorageRefs = new HashSet<>();

    @Column
    @Index
    private String name;

    @Column
    private String description;

    @Column
    @Enumerated(EnumType.STRING)
    private ReplicationGroupState state;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

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

    public ReplicationGroupState getState() {
        return state;
    }

    public void setState(ReplicationGroupState state) {
        this.state = state;
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

    public Set<ImageReplicationGroupBackupStorageRefVO> getBackupStorageRefs() {
        return backupStorageRefs;
    }

    public void setBackupStorageRefs(Set<ImageReplicationGroupBackupStorageRefVO> backupStorageRefs) {
        this.backupStorageRefs = backupStorageRefs;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
}
