package org.zstack.storage.primary.sharedblock;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@AutoDeleteTag
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = SharedBlockGroupVO.class, myField = "sharedBlockGroupUuid", targetField = "uuid")
        }
)
public class SharedBlockVO extends ResourceVO {
    @Column
    @ForeignKey(parentEntityClass = SharedBlockGroupVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String sharedBlockGroupUuid;

    @Column
    @Enumerated(value = EnumType.STRING)
    private SharedBlockType type;

    @Column
    private String diskUuid;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    @Enumerated(EnumType.STRING)
    private SharedBlockState state;

    @Column
    @Enumerated(EnumType.STRING)
    private SharedBlockStatus status;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uuid")
    @NoView
    private SharedBlockCapacityVO capacity;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getSharedBlockGroupUuid() {
        return sharedBlockGroupUuid;
    }

    public void setSharedBlockGroupUuid(String sharedBlockGroupUuid) {
        this.sharedBlockGroupUuid = sharedBlockGroupUuid;
    }

    public SharedBlockType getType() {
        return type;
    }

    public void setType(SharedBlockType type) {
        this.type = type;
    }

    public String getDiskUuid() {
        return diskUuid;
    }

    public void setDiskUuid(String diskUuid) {
        this.diskUuid = diskUuid;
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

    public SharedBlockState getState() {
        return state;
    }

    public void setState(SharedBlockState state) {
        this.state = state;
    }

    public SharedBlockStatus getStatus() {
        return status;
    }

    public void setStatus(SharedBlockStatus status) {
        this.status = status;
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

    public SharedBlockCapacityVO getCapacity() {
        return capacity;
    }

    public void setCapacity(SharedBlockCapacityVO capacity) {
        this.capacity = capacity;
    }
}
