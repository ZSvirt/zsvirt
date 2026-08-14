package org.zstack.directory;


import org.zstack.header.vo.*;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * @author shenjin
 * @date 2022/11/29 10:54
 */
@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = ResourceVO.class, joinColumn = "resourceUuid"),
        @SoftDeletionCascade(parent = DirectoryVO.class, joinColumn = "directoryUuid")
})
public class ResourceDirectoryRefVO implements ToInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column
    @ForeignKey(parentEntityClass = ResourceVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String resourceUuid;

    @Column
    @ForeignKey(parentEntityClass = DirectoryVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String directoryUuid;

    @Column
    private String resourceType;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getDirectoryUuid() {
        return directoryUuid;
    }

    public void setDirectoryUuid(String directoryUuid) {
        this.directoryUuid = directoryUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
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
