package org.zstack.header.storage.snapshot.group;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by MaJin on 2019/7/9.
 */
@Entity
@Table
@BaseResource
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = VolumeSnapshotGroupRefVO.class, myField = "uuid", targetField = "volumeSnapshotGroupUuid")
        }
)
public class VolumeSnapshotGroupVO extends ResourceVO implements OwnedByAccount {
    @Column
    private Integer snapshotCount;
    @Column
    private String name;
    @Column
    private String description;
    @Column
    private String vmInstanceUuid;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;
    @Transient
    private String accountUuid;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "volumeSnapshotGroupUuid", insertable = false, updatable = false)
    @NoView
    private Set<VolumeSnapshotGroupRefVO> volumeSnapshotRefs = new HashSet<>();

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public Integer getSnapshotCount() {
        return snapshotCount;
    }

    public void setSnapshotCount(Integer snapshotCount) {
        this.snapshotCount = snapshotCount;
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

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public Set<VolumeSnapshotGroupRefVO> getVolumeSnapshotRefs() {
        return volumeSnapshotRefs;
    }

    public void setVolumeSnapshotRefs(Set<VolumeSnapshotGroupRefVO> volumeSnapshotRefs) {
        this.volumeSnapshotRefs = volumeSnapshotRefs;
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
